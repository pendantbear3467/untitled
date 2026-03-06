from __future__ import annotations

from asset_studio.validation.validator import ValidationIssue
from asset_studio.workspace.workspace_manager import AssetStudioContext


def validate_datapack(context: AssetStudioContext) -> list[ValidationIssue]:
    issues: list[ValidationIssue] = []
    recipes = context.workspace_root / "data" / "recipes"
    blockstates = context.workspace_root / "assets" / "blockstates"
    tool_textures = context.workspace_root / "assets" / "textures" / "item"

    for pickaxe_texture in tool_textures.glob("*_pickaxe.png"):
        recipe_file = recipes / f"{pickaxe_texture.stem}.json"
        if not recipe_file.exists():
            issues.append(ValidationIssue("warning", "datapack", str(recipe_file), "Missing recipe for tool"))

    for block_model in (context.workspace_root / "assets" / "models" / "block").glob("*.json"):
        blockstate_file = blockstates / f"{block_model.stem}.json"
        if not blockstate_file.exists():
            issues.append(ValidationIssue("warning", "datapack", str(blockstate_file), "Missing blockstate"))

    return issues
