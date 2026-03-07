from __future__ import annotations

import json
from pathlib import Path

from asset_studio.validation.issue_types import ValidationIssue
from asset_studio.workspace.workspace_manager import AssetStudioContext


def validate_datapack(context: AssetStudioContext) -> list[ValidationIssue]:
    issues: list[ValidationIssue] = []
    datapack_root = context.workspace_root / "data"

    if not datapack_root.exists():
        return issues

    issues.extend(_validate_json_shapes(datapack_root))
    issues.extend(_validate_generation_gaps(context))
    issues.extend(_validate_worldgen_pairs(datapack_root))
    return issues


def _validate_json_shapes(datapack_root: Path) -> list[ValidationIssue]:
    issues: list[ValidationIssue] = []

    for json_file in datapack_root.rglob("*.json"):
        try:
            payload = json.loads(json_file.read_text(encoding="utf-8"))
        except json.JSONDecodeError as exc:
            issues.append(ValidationIssue("error", "datapack", str(json_file), f"Invalid JSON: {exc}"))
            continue

        rel = json_file.relative_to(datapack_root)
        rel_parts = rel.parts
        if not rel_parts:
            continue

        if "recipes" in rel_parts and not _is_valid_recipe(payload):
            issues.append(ValidationIssue("error", "datapack", str(json_file), "Invalid recipe shape"))
            continue

        if "loot_tables" in rel_parts and not _is_valid_loot_table(payload):
            issues.append(ValidationIssue("error", "datapack", str(json_file), "Invalid loot table shape"))
            continue

        if "tags" in rel_parts and not _is_valid_tag(payload):
            issues.append(ValidationIssue("error", "datapack", str(json_file), "Invalid tag shape"))
            continue

        if "advancements" in rel_parts and not _is_valid_advancement(payload):
            issues.append(ValidationIssue("error", "datapack", str(json_file), "Invalid advancement shape"))

    return issues


def _validate_generation_gaps(context: AssetStudioContext) -> list[ValidationIssue]:
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


def _validate_worldgen_pairs(datapack_root: Path) -> list[ValidationIssue]:
    issues: list[ValidationIssue] = []

    configured_dirs = [path for path in datapack_root.rglob("configured_feature") if path.is_dir()]
    for configured_dir in configured_dirs:
        placed_dir = configured_dir.parent / "placed_feature"
        for configured_file in configured_dir.glob("*.json"):
            paired = placed_dir / configured_file.name
            if not paired.exists():
                issues.append(
                    ValidationIssue(
                        "warning",
                        "datapack",
                        str(configured_file),
                        f"Missing placed_feature pair: {paired.name}",
                    )
                )

    return issues


def _is_valid_recipe(payload: object) -> bool:
    if not isinstance(payload, dict):
        return False
    if "type" not in payload:
        return False

    if payload["type"] == "minecraft:smelting":
        return "ingredient" in payload and "result" in payload

    if payload["type"].startswith("minecraft:crafting"):
        return "result" in payload

    return True


def _is_valid_loot_table(payload: object) -> bool:
    return isinstance(payload, dict) and isinstance(payload.get("pools"), list)


def _is_valid_tag(payload: object) -> bool:
    return isinstance(payload, dict) and isinstance(payload.get("values"), list)


def _is_valid_advancement(payload: object) -> bool:
    if not isinstance(payload, dict):
        return False
    criteria = payload.get("criteria")
    return isinstance(criteria, dict) and bool(criteria)
