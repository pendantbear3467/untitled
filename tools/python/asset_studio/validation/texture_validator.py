from __future__ import annotations

import json

from asset_studio.validation.issue_types import ValidationIssue
from asset_studio.workspace.workspace_manager import AssetStudioContext


def validate_textures(context: AssetStudioContext) -> list[ValidationIssue]:
    issues: list[ValidationIssue] = []
    item_models = context.workspace_root / "assets" / "models" / "item"
    texture_root = context.workspace_root / "assets" / "textures" / "item"

    for model_file in item_models.glob("*.json"):
        try:
            payload = json.loads(model_file.read_text(encoding="utf-8"))
        except json.JSONDecodeError:
            continue
        texture_ref = payload.get("textures", {}).get("layer0", "")
        if not texture_ref.startswith("extremecraft:item/"):
            continue
        item_id = texture_ref.split("/")[-1]
        texture = texture_root / f"{item_id}.png"
        if not texture.exists():
            issues.append(ValidationIssue("warning", "texture", str(model_file), f"Missing texture {texture.name}"))

    return issues
