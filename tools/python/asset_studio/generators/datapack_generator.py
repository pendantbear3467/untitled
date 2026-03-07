from __future__ import annotations

from asset_studio.minecraft.advancement_templates import basic_advancement
from asset_studio.minecraft.loot_templates import simple_self_drop
from asset_studio.minecraft.recipe_templates import ore_smelting_recipe
from asset_studio.minecraft.tag_templates import block_ore_tag, tool_pickaxe_tag
from asset_studio.minecraft.worldgen_templates import biome_modifier, configured_ore_feature, placed_ore_feature
from asset_studio.workspace.workspace_manager import AssetStudioContext


class DatapackGenerator:
    def __init__(self, context: AssetStudioContext) -> None:
        self.context = context

    def generate_ore_bundle(self, material: str) -> None:
        ore_block = f"{material}_ore"
        ingot = f"{material}_ingot"

        self.context.write_json(
            self.context.workspace_root / "data" / "recipes" / f"{ingot}_from_smelting.json",
            ore_smelting_recipe(ingredient=f"extremecraft:{ore_block}", result=f"extremecraft:{ingot}"),
        )
        self.context.write_json(
            self.context.workspace_root / "data" / "loot_tables" / "blocks" / f"{ore_block}.json",
            simple_self_drop(ore_block),
        )
        self.context.write_json(
            self.context.workspace_root / "data" / "tags" / "blocks" / "ores.json",
            block_ore_tag([f"extremecraft:{ore_block}"]),
        )
        self.context.write_json(
            self.context.workspace_root / "data" / "advancements" / f"mine_{ore_block}.json",
            basic_advancement(title=f"Mine {material.title()} Ore", item=f"extremecraft:{ore_block}"),
        )
        self.context.write_json(
            self.context.workspace_root / "data" / "worldgen" / "configured_feature" / f"{ore_block}.json",
            configured_ore_feature(ore_block),
        )
        self.context.write_json(
            self.context.workspace_root / "data" / "worldgen" / "placed_feature" / f"{ore_block}.json",
            placed_ore_feature(ore_block),
        )
        self.context.write_json(
            self.context.workspace_root / "data" / "forge" / "biome_modifier" / f"add_{ore_block}.json",
            biome_modifier(ore_block),
        )

    def generate_material_support(self, material: str, tier: int) -> None:
        self.generate_ore_bundle(material)
        self.context.write_json(
            self.context.workspace_root / "data" / "tags" / "tools" / "pickaxes.json",
            tool_pickaxe_tag([f"extremecraft:{material}_pickaxe"]),
        )
        self.context.asset_db.upsert_material(material, {"tier": tier, "kind": "material"})
