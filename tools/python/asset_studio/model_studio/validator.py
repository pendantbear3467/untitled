from __future__ import annotations

from dataclasses import dataclass, field

from asset_studio.model_studio.models import ModelDocument

_ALLOWED_MODEL_TYPES = {"block", "item", "entity"}
_ALLOWED_FACES = {"north", "south", "east", "west", "up", "down"}


@dataclass(frozen=True)
class ModelValidationIssue:
    severity: str
    code: str
    target_id: str | None
    message: str


@dataclass
class ModelValidationReport:
    issues: list[ModelValidationIssue] = field(default_factory=list)

    @property
    def errors(self) -> list[ModelValidationIssue]:
        return [issue for issue in self.issues if issue.severity == "error"]

    @property
    def warnings(self) -> list[ModelValidationIssue]:
        return [issue for issue in self.issues if issue.severity == "warning"]


class ModelDocumentValidator:
    def validate(self, document: ModelDocument) -> ModelValidationReport:
        issues: list[ModelValidationIssue] = []
        if document.model_type not in _ALLOWED_MODEL_TYPES:
            issues.append(ModelValidationIssue("error", "model-type", None, f"Unsupported model type: {document.model_type}"))
        if document.texture_width <= 0 or document.texture_height <= 0:
            issues.append(ModelValidationIssue("error", "texture-size", None, "Texture size must be positive"))
        if "root" not in document.bones:
            issues.append(ModelValidationIssue("error", "root-bone", None, "Model document is missing the root bone"))

        assigned_cubes: set[str] = set()
        for cube in document.cubes.values():
            if cube.from_pos.x >= cube.to_pos.x or cube.from_pos.y >= cube.to_pos.y or cube.from_pos.z >= cube.to_pos.z:
                issues.append(ModelValidationIssue("error", "cube-bounds", cube.id, "Cube from/to coordinates must define a positive volume"))
            if not cube.texture and not cube.faces:
                issues.append(ModelValidationIssue("warning", "texture", cube.id, "Cube has no texture assignment"))
            for face, mapping in cube.faces.items():
                if face not in _ALLOWED_FACES:
                    issues.append(ModelValidationIssue("error", "face", cube.id, f"Unsupported face mapping: {face}"))
                if len(mapping.uv) != 4:
                    issues.append(ModelValidationIssue("error", "uv", cube.id, f"Face '{face}' must define exactly four UV coordinates"))
            if any(abs(component) > 360 for component in (cube.rotation.x, cube.rotation.y, cube.rotation.z)):
                issues.append(ModelValidationIssue("warning", "rotation-range", cube.id, "Cube rotation exceeds +/-360 degrees"))

        for bone in document.bones.values():
            if bone.parent and bone.parent not in document.bones:
                issues.append(ModelValidationIssue("error", "bone-parent", bone.id, f"Missing parent bone: {bone.parent}"))
            if bone.parent == bone.id:
                issues.append(ModelValidationIssue("error", "bone-parent", bone.id, "Bone cannot be parented to itself"))
            for cube_id in bone.cubes:
                if cube_id not in document.cubes:
                    issues.append(ModelValidationIssue("error", "bone-cube", bone.id, f"Missing cube reference: {cube_id}"))
                assigned_cubes.add(cube_id)
            for child_id in bone.children:
                if child_id not in document.bones:
                    issues.append(ModelValidationIssue("error", "bone-child", bone.id, f"Missing child bone: {child_id}"))

        for cube_id in document.cubes:
            if cube_id not in assigned_cubes:
                issues.append(ModelValidationIssue("warning", "unassigned-cube", cube_id, "Cube is not assigned to any bone"))

        return ModelValidationReport(issues=issues)
