from __future__ import annotations

from pathlib import Path

from asset_studio.model_studio.models import FaceMapping, ModelBone, ModelCube, ModelDocument, Vec3
from asset_studio.model_studio.preview import build_preview_payload
from asset_studio.model_studio.serializer import MODEL_STUDIO_FORMAT, ModelImportResult, load_document, save_document
from asset_studio.model_studio.validator import ModelDocumentValidator, ModelValidationReport


class ModelStudioEngine:
    def __init__(self, root: Path) -> None:
        self.root = root
        self.root.mkdir(parents=True, exist_ok=True)
        self.validator = ModelDocumentValidator()

    def create_document(self, name: str, *, model_type: str = "block") -> ModelDocument:
        document = ModelDocument(name=name, model_type=model_type)
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

    def validate_document(self, document: ModelDocument) -> ModelValidationReport:
        return self.validator.validate(document)

    def preview_payload(self, document: ModelDocument) -> dict:
        return build_preview_payload(document)

    def add_bone(self, document: ModelDocument, bone: ModelBone, *, parent_id: str | None = None) -> None:
        document.bones[bone.id] = bone
        if parent_id and parent_id in document.bones:
            bone.parent = parent_id
            document.bones[parent_id].children.append(bone.id)

    def add_cube(self, document: ModelDocument, cube: ModelCube, *, bone_id: str = "root") -> None:
        document.cubes[cube.id] = cube
        document.bones.setdefault("root", ModelBone(id="root"))
        document.bones[bone_id].cubes.append(cube.id)

    def search(self, document: ModelDocument, query: str) -> dict[str, list[str]]:
        needle = query.strip().lower()
        bone_ids = [bone_id for bone_id in sorted(document.bones) if not needle or needle in bone_id.lower()]
        cube_ids = [cube_id for cube_id in sorted(document.cubes) if not needle or needle in cube_id.lower()]
        return {"bones": bone_ids, "cubes": cube_ids}
