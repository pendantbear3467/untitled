from __future__ import annotations

import json
from dataclasses import dataclass

from asset_studio.project.workspace_manager import AssetStudioContext


@dataclass
class ValidationIssue:
    severity: str
    path: str
    message: str


def validate_assets(context: AssetStudioContext) -> list[ValidationIssue]:
    issues: list[ValidationIssue] = []

    assets_root = context.workspace_root / "assets"
    data_root = context.workspace_root / "data"

    # JSON validity scan.
    for root in [assets_root, data_root]:
        for json_file in root.rglob("*.json"):
            try:
                json.loads(json_file.read_text(encoding="utf-8"))
            except json.JSONDecodeError as exc:
                issues.append(ValidationIssue("error", str(json_file), f"Broken JSON: {exc}"))

    # Missing textures from item models.
    for model_file in (assets_root / "models" / "item").glob("*.json"):
        try:
            payload = json.loads(model_file.read_text(encoding="utf-8"))
        except json.JSONDecodeError:
            continue
        texture_ref = payload.get("textures", {}).get("layer0", "")
        if not texture_ref.startswith("extremecraft:item/"):
            continue
        item_id = texture_ref.split("/")[-1]
        texture_path = assets_root / "textures" / "item" / f"{item_id}.png"
        if not texture_path.exists():
            issues.append(ValidationIssue("warning", str(model_file), f"Missing texture: {texture_path.name}"))

    # Missing recipes for generated tools.
    for texture_file in (assets_root / "textures" / "item").glob("*_pickaxe.png"):
        recipe = data_root / "recipes" / f"{texture_file.stem}.json"
        if not recipe.exists():
            issues.append(ValidationIssue("warning", str(recipe), "Missing recipe for tool"))

    # Basic parent validation.
    for block_model in (assets_root / "models" / "block").glob("*.json"):
        try:
            payload = json.loads(block_model.read_text(encoding="utf-8"))
        except json.JSONDecodeError:
            continue
        parent = str(payload.get("parent", ""))
        if parent and not parent.startswith("block/") and not parent.startswith("extremecraft:"):
            issues.append(ValidationIssue("warning", str(block_model), f"Suspicious model parent: {parent}"))

    return issues
