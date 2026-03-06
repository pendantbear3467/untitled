from __future__ import annotations

from asset_studio.generators.item_generator import ItemGenerator
from asset_studio.minecraft.recipe_templates import shaped_pickaxe_recipe
from asset_studio.project.workspace_manager import AssetStudioContext


class ToolGenerator:
    def __init__(self, context: AssetStudioContext) -> None:
        self.context = context
        self.item_gen = ItemGenerator(context)

    def generate(
        self,
        tool_name: str,
        material: str,
        durability: int,
        attack_damage: float,
        mining_speed: float,
        tier: int,
        texture_style: str,
    ) -> None:
        texture = self.context.texture_engine.generate_tool_texture(material=material, style=texture_style, tool_name=tool_name)
        self.context.write_texture(self.context.workspace_root / "assets" / "textures" / "item" / f"{tool_name}.png", texture.image)

        self.item_gen.write_item_model(tool_name)
        self.context.write_json(
            self.context.workspace_root / "data" / "recipes" / f"{tool_name}.json",
            shaped_pickaxe_recipe(tool_name=tool_name, material=material),
        )

        self.context.append_to_tag(
            self.context.workspace_root / "data" / "tags" / "tools" / "pickaxes.json",
            f"extremecraft:{tool_name}",
        )
        self.context.add_lang_entry("item.extremecraft." + tool_name, tool_name.replace("_", " ").title())
        self.context.asset_db.upsert_item(
            tool_name,
            {
                "material": material,
                "durability": durability,
                "attack_damage": attack_damage,
                "mining_speed": mining_speed,
                "tier": tier,
                "texture_style": texture_style,
                "type": "tool",
            },
        )
