from __future__ import annotations

import json
import tempfile
import unittest
from pathlib import Path

from asset_studio.validation.datapack_validator import validate_datapack
from asset_studio.validation.texture_validator import validate_textures
from compiler.dependency_resolver import DependencyResolver
from extremecraft_sdk.definitions.definition_types import AddonSpec, ContentDefinition


class _Context:
    def __init__(self, workspace_root: Path) -> None:
        self.workspace_root = workspace_root


class FrameworkValidationTests(unittest.TestCase):
    def test_dependency_resolver_detects_registry_conflicts(self) -> None:
        with tempfile.TemporaryDirectory() as temp_dir:
            workspace = Path(temp_dir)
            (workspace / "registry_snapshot.json").write_text(
                json.dumps({"item_ids": ["mythril_ingot"]}, indent=2) + "\n",
                encoding="utf-8",
            )

            addon = AddonSpec(
                name="conflict_pack",
                namespace="conflict_pack",
                version="1.0.0",
                definitions=[
                    ContentDefinition(
                        type="item",
                        id="mythril_ingot",
                        payload={"type": "item", "id": "mythril_ingot"},
                        source_path=workspace / "addons" / "conflict_pack" / "definitions" / "item.json",
                    )
                ],
            )

            resolution = DependencyResolver(workspace_root=workspace).resolve(addon)
            self.assertTrue(any(conflict.kind == "duplicate_id" for conflict in resolution.conflicts))

    def test_missing_item_texture_is_reported(self) -> None:
        with tempfile.TemporaryDirectory() as temp_dir:
            workspace = Path(temp_dir)
            model_dir = workspace / "assets" / "models" / "item"
            model_dir.mkdir(parents=True, exist_ok=True)
            (model_dir / "mythril_sword.json").write_text(
                json.dumps(
                    {
                        "parent": "item/generated",
                        "textures": {"layer0": "extremecraft:item/mythril_sword"},
                    },
                    indent=2,
                )
                + "\n",
                encoding="utf-8",
            )

            issues = validate_textures(_Context(workspace))
            self.assertTrue(any("Missing texture" in issue.message for issue in issues))

    def test_invalid_datapack_recipe_is_reported(self) -> None:
        with tempfile.TemporaryDirectory() as temp_dir:
            workspace = Path(temp_dir)
            recipe_dir = workspace / "data" / "recipes"
            recipe_dir.mkdir(parents=True, exist_ok=True)
            (recipe_dir / "broken_recipe.json").write_text(
                json.dumps({"type": "minecraft:crafting_shaped", "pattern": ["A"]}, indent=2) + "\n",
                encoding="utf-8",
            )

            issues = validate_datapack(_Context(workspace))
            self.assertTrue(any(issue.message == "Invalid recipe shape" for issue in issues))


if __name__ == "__main__":
    unittest.main()
