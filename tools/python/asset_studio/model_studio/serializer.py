from __future__ import annotations

import json
from dataclasses import dataclass, field
from pathlib import Path

from asset_studio.model_studio.models import FaceMapping, ModelBone, ModelCube, ModelDocument, Vec3

MODEL_STUDIO_FORMAT = "cube-model-studio"


@dataclass
class ModelImportResult:
    document: ModelDocument
    warnings: list[str] = field(default_factory=list)
    errors: list[str] = field(default_factory=list)


def _vec_to_list(value: Vec3) -> list[float]:
    return [value.x, value.y, value.z]


def cube_to_dict(cube: ModelCube) -> dict:
    return {
        "id": cube.id,
        "from": _vec_to_list(cube.from_pos),
        "to": _vec_to_list(cube.to_pos),
        "pivot": _vec_to_list(cube.pivot),
        "rotation": _vec_to_list(cube.rotation),
        "inflate": cube.inflate,
        "texture": cube.texture,
        "mirror": cube.mirror,
        "faces": {
            face: {
                "texture": mapping.texture,
                "uv": list(mapping.uv),
                "rotation": mapping.rotation,
            }
            for face, mapping in sorted(cube.faces.items())
        },
    }


def bone_to_dict(bone: ModelBone) -> dict:
    return {
        "id": bone.id,
        "pivot": _vec_to_list(bone.pivot),
        "rotation": _vec_to_list(bone.rotation),
        "cubes": list(bone.cubes),
        "children": list(bone.children),
        "parent": bone.parent,
        "metadata": dict(bone.metadata),
    }


def document_to_dict(document: ModelDocument) -> dict:
    return {
        "schemaVersion": document.schema_version,
        "documentType": MODEL_STUDIO_FORMAT,
        "name": document.name,
        "modelType": document.model_type,
        "textureSize": [document.texture_width, document.texture_height],
        "metadata": dict(document.metadata),
        "bones": [bone_to_dict(bone) for bone in sorted(document.bones.values(), key=lambda item: item.id)],
        "cubes": [cube_to_dict(cube) for cube in sorted(document.cubes.values(), key=lambda item: item.id)],
    }


def save_document(document: ModelDocument, path: Path) -> Path:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(document_to_dict(document), indent=2) + "\n", encoding="utf-8")
    return path


def load_document(payload_or_path: dict | Path) -> ModelImportResult:
    warnings: list[str] = []
    errors: list[str] = []
    payload = payload_or_path
    if isinstance(payload_or_path, Path):
        payload = json.loads(payload_or_path.read_text(encoding="utf-8"))
    assert isinstance(payload, dict)

    if payload.get("documentType") not in {None, MODEL_STUDIO_FORMAT}:
        warnings.append(f"Unexpected model document type: {payload.get('documentType')}")

    cubes: dict[str, ModelCube] = {}
    for cube_payload in payload.get("cubes", []):
        cube_id = str(cube_payload.get("id", "")).strip()
        if not cube_id:
            errors.append("Cube missing id")
            continue
        if cube_id in cubes:
            errors.append(f"Duplicate cube id: {cube_id}")
            continue
        faces_payload = cube_payload.get("faces") or {}
        cubes[cube_id] = ModelCube(
            id=cube_id,
            from_pos=_vec(cube_payload.get("from", [0, 0, 0])),
            to_pos=_vec(cube_payload.get("to", [16, 16, 16])),
            pivot=_vec(cube_payload.get("pivot", [0, 0, 0])),
            rotation=_vec(cube_payload.get("rotation", [0, 0, 0])),
            inflate=float(cube_payload.get("inflate", 0.0)),
            texture=str(cube_payload.get("texture", "")),
            mirror=bool(cube_payload.get("mirror", False)),
            faces={
                str(face): FaceMapping(
                    texture=str(mapping.get("texture", "")),
                    uv=tuple(float(entry) for entry in mapping.get("uv", [0, 0, 16, 16])),
                    rotation=int(mapping.get("rotation", 0)),
                )
                for face, mapping in faces_payload.items()
            },
        )

    bones: dict[str, ModelBone] = {}
    for bone_payload in payload.get("bones", []):
        bone_id = str(bone_payload.get("id", "")).strip()
        if not bone_id:
            errors.append("Bone missing id")
            continue
        if bone_id in bones:
            errors.append(f"Duplicate bone id: {bone_id}")
            continue
        bones[bone_id] = ModelBone(
            id=bone_id,
            pivot=_vec(bone_payload.get("pivot", [0, 0, 0])),
            rotation=_vec(bone_payload.get("rotation", [0, 0, 0])),
            cubes=[str(item) for item in bone_payload.get("cubes", [])],
            children=[str(item) for item in bone_payload.get("children", [])],
            parent=str(bone_payload.get("parent")) if bone_payload.get("parent") else None,
            metadata=dict(bone_payload.get("metadata") or {}),
        )

    texture_size = payload.get("textureSize") or [64, 64]
    document = ModelDocument(
        name=str(payload.get("name", "untitled_model")),
        model_type=str(payload.get("modelType", "block")),
        schema_version=int(payload.get("schemaVersion", 1)),
        texture_width=int(texture_size[0]),
        texture_height=int(texture_size[1]),
        cubes=cubes,
        bones=bones,
        metadata=dict(payload.get("metadata") or {}),
    )
    document.ensure_root_bone()
    return ModelImportResult(document=document, warnings=warnings, errors=errors)


def _vec(value: list[float] | tuple[float, float, float]) -> Vec3:
    x, y, z = value
    return Vec3(float(x), float(y), float(z))
