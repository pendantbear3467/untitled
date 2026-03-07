from __future__ import annotations

import json
import shutil
from pathlib import Path

from asset_studio.minecraft.advancement_templates import basic_advancement
from asset_studio.minecraft.loot_templates import simple_self_drop
from asset_studio.minecraft.recipe_templates import ore_smelting_recipe, shaped_pickaxe_recipe
from asset_studio.minecraft.tag_templates import block_ore_tag, tool_pickaxe_tag
from asset_studio.minecraft.worldgen_templates import biome_modifier, configured_ore_feature, placed_ore_feature
from extremecraft_sdk.definitions.definition_types import AddonSpec


class DatapackBuilder:
    """Builds datapack payload for module artifacts with generated data roots."""

    _NAMESPACED_ROOTS = {"recipes", "loot_tables", "tags", "advancements", "worldgen", "forge"}

    def build(self, workspace_root: Path, module_root: Path, addon: AddonSpec | None = None) -> Path:
        namespace = addon.namespace if addon else "extremecraft"
        data_src = workspace_root / "data"
        data_root = module_root / "src" / "main" / "resources" / "data"

        if data_root.exists():
            shutil.rmtree(data_root)
        data_root.mkdir(parents=True, exist_ok=True)

        self._copy_workspace_data(data_src, data_root, namespace)
        if addon is not None:
            self._compile_addon_data(data_root, addon)

        return data_root / namespace

    def _copy_workspace_data(self, source_root: Path, output_root: Path, namespace: str) -> None:
        if not source_root.exists():
            return

        for path in source_root.rglob("*.json"):
            relative = path.relative_to(source_root)
            parts = relative.parts
            if not parts:
                continue

            if parts[0] in self._NAMESPACED_ROOTS:
                target = output_root / namespace / relative
            else:
                target = output_root / relative

            target.parent.mkdir(parents=True, exist_ok=True)
            shutil.copy2(path, target)

    def _compile_addon_data(self, data_root: Path, addon: AddonSpec) -> None:
        namespace_root = data_root / addon.namespace

        for definition in addon.definitions:
            definition_type = definition.type
            definition_id = definition.id
            payload = definition.payload

            if definition_type == "recipe":
                recipe_payload = payload.get("recipe") if isinstance(payload.get("recipe"), dict) else payload
                self._write_json(namespace_root / "recipes" / f"{definition_id}.json", recipe_payload)
                continue

            if definition_type == "worldgen":
                ore_id = str(payload.get("ore_id", f"{definition_id}_ore"))
                configured = payload.get("configured_feature") if isinstance(payload.get("configured_feature"), dict) else configured_ore_feature(ore_id)
                placed = payload.get("placed_feature") if isinstance(payload.get("placed_feature"), dict) else placed_ore_feature(ore_id)
                modifier = payload.get("biome_modifier") if isinstance(payload.get("biome_modifier"), dict) else biome_modifier(ore_id)

                self._write_json(namespace_root / "worldgen" / "configured_feature" / f"{ore_id}.json", configured)
                self._write_json(namespace_root / "worldgen" / "placed_feature" / f"{ore_id}.json", placed)
                self._write_json(namespace_root / "forge" / "biome_modifier" / f"add_{ore_id}.json", modifier)
                continue

            if definition_type in {"block", "machine"}:
                loot = payload.get("loot_table") if isinstance(payload.get("loot_table"), dict) else simple_self_drop(definition_id)
                self._write_json(namespace_root / "loot_tables" / "blocks" / f"{definition_id}.json", loot)
                continue

            if definition_type in {"tool", "weapon"}:
                material = str(payload.get("material", "iron"))
                recipe = payload.get("recipe") if isinstance(payload.get("recipe"), dict) else shaped_pickaxe_recipe(tool_name=definition_id, material=material)
                self._write_json(namespace_root / "recipes" / f"{definition_id}.json", recipe)
                self._append_tag(namespace_root / "tags" / "tools" / "pickaxes.json", f"{addon.namespace}:{definition_id}")
                continue

            if definition_type == "material":
                self._compile_material_bundle(namespace_root, addon.namespace, definition_id)

    def _compile_material_bundle(self, namespace_root: Path, namespace: str, material_id: str) -> None:
        ore_block = f"{material_id}_ore"
        ingot = f"{material_id}_ingot"
        pickaxe = f"{material_id}_pickaxe"

        self._write_json(
            namespace_root / "recipes" / f"{ingot}_from_smelting.json",
            ore_smelting_recipe(ingredient=f"{namespace}:{ore_block}", result=f"{namespace}:{ingot}"),
        )
        self._write_json(namespace_root / "loot_tables" / "blocks" / f"{ore_block}.json", simple_self_drop(ore_block))

        self._append_tag(namespace_root / "tags" / "blocks" / "ores.json", f"{namespace}:{ore_block}")
        self._append_tag(namespace_root / "tags" / "tools" / "pickaxes.json", f"{namespace}:{pickaxe}")

        self._write_json(
            namespace_root / "advancements" / f"mine_{ore_block}.json",
            basic_advancement(title=f"Mine {material_id.title()} Ore", item=f"{namespace}:{ore_block}"),
        )
        self._write_json(namespace_root / "worldgen" / "configured_feature" / f"{ore_block}.json", configured_ore_feature(ore_block))
        self._write_json(namespace_root / "worldgen" / "placed_feature" / f"{ore_block}.json", placed_ore_feature(ore_block))
        self._write_json(namespace_root / "forge" / "biome_modifier" / f"add_{ore_block}.json", biome_modifier(ore_block))

    def _append_tag(self, path: Path, value: str) -> None:
        payload = {"replace": False, "values": []}
        if path.exists():
            payload = json.loads(path.read_text(encoding="utf-8"))
            payload.setdefault("replace", False)
            payload.setdefault("values", [])

        if value not in payload["values"]:
            payload["values"].append(value)

        # Normalize known tag shapes after append.
        if path.name == "ores.json":
            payload = block_ore_tag(payload["values"])
        elif path.name == "pickaxes.json":
            payload = tool_pickaxe_tag(payload["values"])

        self._write_json(path, payload)

    def _write_json(self, path: Path, payload: dict) -> None:
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(json.dumps(payload, indent=2) + "\n", encoding="utf-8")
