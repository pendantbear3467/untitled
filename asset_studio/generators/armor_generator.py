from __future__ import annotations

from asset_studio.generators.item_generator import ItemGenerator
from asset_studio.workspace.workspace_manager import AssetStudioContext


class ArmorGenerator:
    def __init__(self, context: AssetStudioContext) -> None:
        self.context = context
        self.item_gen = ItemGenerator(context)

    def generate(self, material: str, tier: int, texture_style: str) -> None:
        for slot in ["helmet", "chestplate", "leggings", "boots"]:
            item_id = f"{material}_{slot}"
            tex = self.context.texture_engine.generate_armor_texture(material=material, style=texture_style, slot=slot)
            self.context.write_texture(self.context.workspace_root / "assets" / "textures" / "item" / f"{item_id}.png", tex.image)
            self.item_gen.write_item_model(item_id)
            self.context.add_lang_entry("item.extremecraft." + item_id, item_id.replace("_", " ").title())

        self.context.asset_db.upsert_material(material, {"tier": tier, "texture_style": texture_style, "kind": "armor"})
