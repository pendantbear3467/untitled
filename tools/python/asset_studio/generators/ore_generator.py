from __future__ import annotations

from asset_studio.generators.block_generator import BlockGenerator
from asset_studio.generators.datapack_generator import DatapackGenerator
from asset_studio.workspace.workspace_manager import AssetStudioContext


class OreGenerator:
    def __init__(self, context: AssetStudioContext) -> None:
        self.context = context
        self.block_generator = BlockGenerator(context)
        self.datapack_generator = DatapackGenerator(context)

    def generate(self, material: str, tier: int, texture_style: str) -> None:
        ore_id = f"{material}_ore"
        storage_id = f"{material}_block"

        self.block_generator.generate(ore_id, material=material, texture_style=texture_style)
        self.block_generator.generate(storage_id, material=material, texture_style=texture_style)
        self.datapack_generator.generate_ore_bundle(material=material)

        self.context.asset_db.upsert_material(material, {"tier": tier, "texture_style": texture_style, "kind": "ore"})
