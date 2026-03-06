from __future__ import annotations

import json

from asset_studio.validation.validator import ValidationIssue
from asset_studio.workspace.workspace_manager import AssetStudioContext


def validate_models(context: AssetStudioContext) -> list[ValidationIssue]:
    issues: list[ValidationIssue] = []
    model_root = context.workspace_root / "assets" / "models"
    for model_file in model_root.rglob("*.json"):
        try:
            payload = json.loads(model_file.read_text(encoding="utf-8"))
        except json.JSONDecodeError:
            continue
        parent = str(payload.get("parent", ""))
        if parent and not (
            parent.startswith("item/")
            or parent.startswith("block/")
            or parent.startswith("extremecraft:")
            or parent == "minecraft:item/generated"
        ):
            issues.append(ValidationIssue("warning", "model", str(model_file), f"Invalid parent: {parent}"))
    return issues
