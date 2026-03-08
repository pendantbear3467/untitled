from __future__ import annotations

import json
from pathlib import Path
from uuid import uuid4

from asset_studio.model_studio.models import FaceMapping, ModelBone, ModelCube, ModelDocument, Vec3
from asset_studio.model_studio.preview import build_preview_payload
from asset_studio.model_studio.serializer import MODEL_RUNTIME_FORMAT, MODEL_STUDIO_FORMAT, ModelImportResult, load_document, save_document
from asset_studio.model_studio.validator import ModelDocumentValidator, ModelValidationReport


class ModelStudioEngine:
    def __init__(self, root: Path) -> None:
        self.root = root
        self.root.mkdir(parents=True, exist_ok=True)
        self.validator = ModelDocumentValidator()

    def create_document(self, name: str, *, model_type: str = "block") -> ModelDocument:
        document = ModelDocument(name=name, model_type=model_type, namespace=self._load_modid(), metadata={"runtimeContract": MODEL_RUNTIME_FORMAT})
        document.ensure_root_bone()
        document.cubes["body"] = ModelCube(
            id="body",
            from_pos=Vec3(0.0, 0.0, 0.0),
            to_pos=Vec3(16.0, 16.0, 16.0),
            texture="textures/block/default.png",
            faces={face: FaceMapping(texture="#0") for face in ["north", "south", "east", "west", "up", "down"]},
        )
        document.bones["root"].cubes.append("body")
        return document

    def create_cube(self, cube_id: str | None = None, *, x: float = 0.0, y: float = 0.0, z: float = 0.0, width: float = 16.0, height: float = 16.0, depth: float = 16.0) -> ModelCube:
        cube_name = cube_id or f"cube_{uuid4().hex[:6]}"
        return ModelCube(
            id=cube_name,
            from_pos=Vec3(x, y, z),
            to_pos=Vec3(x + width, y + height, z + depth),
            texture="textures/block/default.png",
            faces={face: FaceMapping(texture="#0") for face in ["north", "south", "east", "west", "up", "down"]},
        )

    def document_path(self, name: str) -> Path:
        return self.root / f"{name}.model.json"

    def list_documents(self) -> list[str]:
        return sorted(path.stem.replace(".model", "") for path in self.root.glob("*.model.json"))

    def save_document(self, document: ModelDocument) -> Path:
        return save_document(document, self.document_path(document.name))

    def load_document(self, name_or_path: str | Path) -> ModelImportResult:
        path = Path(name_or_path)
        if not path.suffix:
            path = self.document_path(str(name_or_path))
        return load_document(path)

    def import_document(self, path: Path) -> ModelImportResult:
        return load_document(path)

    def export_document(self, document: ModelDocument, path: Path | None = None, *, format: str = MODEL_STUDIO_FORMAT) -> Path:
        if format != MODEL_STUDIO_FORMAT:
            raise ValueError(f"Unsupported model export format: {format}")
        target = path or self.document_path(document.name)
        return save_document(document, target)

    def runtime_export_path(self, document: ModelDocument) -> Path:
        return self.root.parents[1] / "assets" / document.namespace / "studio" / "models" / f"{document.name}.json"

    def export_runtime_document(self, document: ModelDocument, path: Path | None = None) -> Path:
        target = path or self.runtime_export_path(document)
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(json.dumps(self.build_runtime_definition(document), indent=2) + "\n", encoding="utf-8")
        return target

    def build_runtime_definition(self, document: ModelDocument) -> dict:
        return {
            "schemaVersion": 1,
            "documentType": MODEL_RUNTIME_FORMAT,
            "studioFormat": MODEL_STUDIO_FORMAT,
            "modelId": document.name,
            "namespace": document.namespace,
            "resourceId": f"{document.namespace}:studio/models/{document.name}",
            "modelType": document.model_type,
            "textureSize": [document.texture_width, document.texture_height],
            "bones": [
                {
                    "id": bone.id,
                    "pivot": [bone.pivot.x, bone.pivot.y, bone.pivot.z],
                    "rotation": [bone.rotation.x, bone.rotation.y, bone.rotation.z],
                    "parent": bone.parent,
                    "children": list(bone.children),
                    "cubes": list(bone.cubes),
                    "metadata": dict(bone.metadata),
                }
                for bone in sorted(document.bones.values(), key=lambda item: item.id)
            ],
            "cubes": [
                {
                    "id": cube.id,
                    "from": [cube.from_pos.x, cube.from_pos.y, cube.from_pos.z],
                    "to": [cube.to_pos.x, cube.to_pos.y, cube.to_pos.z],
                    "pivot": [cube.pivot.x, cube.pivot.y, cube.pivot.z],
                    "rotation": [cube.rotation.x, cube.rotation.y, cube.rotation.z],
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
                    "metadata": dict(cube.metadata),
                }
                for cube in sorted(document.cubes.values(), key=lambda item: item.id)
            ],
            "previewCompatible": True,
            "metadata": dict(document.metadata),
        }

    def validate_document(self, document: ModelDocument) -> ModelValidationReport:
        return self.validator.validate(document)

    def preview_payload(self, document: ModelDocument) -> dict:
        return build_preview_payload(document)

    def add_bone(self, document: ModelDocument, bone: ModelBone, *, parent_id: str | None = None) -> None:
        document.bones[bone.id] = bone
        if parent_id and parent_id in document.bones:
            bone.parent = parent_id
            if bone.id not in document.bones[parent_id].children:
                document.bones[parent_id].children.append(bone.id)

    def remove_bone(self, document: ModelDocument, bone_id: str) -> None:
        if bone_id == "root":
            raise ValueError("The root bone cannot be removed")
        bone = document.bones.pop(bone_id)
        if bone.parent and bone.parent in document.bones:
            document.bones[bone.parent].children = [child for child in document.bones[bone.parent].children if child != bone_id]
        for child_id in bone.children:
            if child_id in document.bones:
                document.bones[child_id].parent = "root"
                document.bones["root"].children.append(child_id)
        for cube_id in bone.cubes:
            if cube_id not in document.bones["root"].cubes:
                document.bones["root"].cubes.append(cube_id)

    def add_cube(self, document: ModelDocument, cube: ModelCube, *, bone_id: str = "root") -> None:
        document.cubes[cube.id] = cube
        document.bones.setdefault("root", ModelBone(id="root"))
        document.bones.setdefault(bone_id, ModelBone(id=bone_id))
        if cube.id not in document.bones[bone_id].cubes:
            document.bones[bone_id].cubes.append(cube.id)

    def remove_cube(self, document: ModelDocument, cube_id: str) -> None:
        document.cubes.pop(cube_id, None)
        for bone in document.bones.values():
            bone.cubes = [item for item in bone.cubes if item != cube_id]

    def duplicate_cube(self, document: ModelDocument, cube_id: str, *, offset: Vec3 | None = None) -> ModelCube:
        cube = document.cubes[cube_id]
        delta = offset or Vec3(1.0, 1.0, 1.0)
        duplicate = ModelCube(
            id=f"{cube.id}_copy_{uuid4().hex[:4]}",
            from_pos=Vec3(cube.from_pos.x + delta.x, cube.from_pos.y + delta.y, cube.from_pos.z + delta.z),
            to_pos=Vec3(cube.to_pos.x + delta.x, cube.to_pos.y + delta.y, cube.to_pos.z + delta.z),
            pivot=Vec3(cube.pivot.x, cube.pivot.y, cube.pivot.z),
            rotation=Vec3(cube.rotation.x, cube.rotation.y, cube.rotation.z),
            inflate=cube.inflate,
            texture=cube.texture,
            mirror=cube.mirror,
            faces={face: FaceMapping(texture=mapping.texture, uv=tuple(mapping.uv), rotation=mapping.rotation) for face, mapping in cube.faces.items()},
            metadata=dict(cube.metadata),
        )
        self.add_cube(document, duplicate, bone_id=self.find_cube_bone(document, cube_id) or "root")
        return duplicate

    def assign_cube_to_bone(self, document: ModelDocument, cube_id: str, bone_id: str) -> None:
        if bone_id not in document.bones:
            raise ValueError(f"Unknown bone: {bone_id}")
        for bone in document.bones.values():
            bone.cubes = [existing for existing in bone.cubes if existing != cube_id]
        document.bones[bone_id].cubes.append(cube_id)

    def find_cube_bone(self, document: ModelDocument, cube_id: str) -> str | None:
        for bone_id, bone in document.bones.items():
            if cube_id in bone.cubes:
                return bone_id
        return None

    def search(self, document: ModelDocument, query: str) -> dict[str, list[str]]:
        needle = query.strip().lower()
        bone_ids = [bone_id for bone_id in sorted(document.bones) if not needle or needle in bone_id.lower()]
        cube_ids = [cube_id for cube_id in sorted(document.cubes) if not needle or needle in cube_id.lower()]
        return {"bones": bone_ids, "cubes": cube_ids}

    def _load_modid(self) -> str:
        project_file = self.root.parents[1] / "project.json"
        if not project_file.exists():
            return "extremecraft"
        try:
            payload = json.loads(project_file.read_text(encoding="utf-8"))
        except json.JSONDecodeError:
            return "extremecraft"
        return str(payload.get("modid", "extremecraft"))
