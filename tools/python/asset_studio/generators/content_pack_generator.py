from __future__ import annotations

from asset_studio.generators.armor_generator import ArmorGenerator
from asset_studio.generators.block_generator import BlockGenerator
from asset_studio.generators.datapack_generator import DatapackGenerator
from asset_studio.generators.item_generator import ItemGenerator
from asset_studio.generators.machine_generator import MachineGenerator
from asset_studio.generators.ore_generator import OreGenerator
from asset_studio.generators.tool_generator import ToolGenerator


class ContentPackGenerator:
    """Generates a full material ecosystem for large mod content packs."""

    def __init__(self, context) -> None:
        self.context = context

    def generate_material_set(self, material: str, tier: int = 4, style: str = "metallic") -> list[str]:
        generated: list[str] = []

        OreGenerator(self.context).generate(material=material, tier=tier, texture_style=style)
        generated.extend([f"{material}_ore", f"{material}_block"])

        # Core material items.
        item_gen = ItemGenerator(self.context)
        for suffix in ["ingot", "nugget"]:
            item_id = f"{material}_{suffix}"
            tex = self.context.texture_engine.generate_ingot_texture(material=material, style=style)
            self.context.write_texture(self.context.workspace_root / "assets" / "textures" / "item" / f"{item_id}.png", tex.image)
            item_gen.write_item_model(item_id)
            self.context.add_lang_entry(f"item.extremecraft.{item_id}", item_id.replace("_", " ").title())
            generated.append(item_id)

        for tool_kind in ["pickaxe", "axe", "shovel", "hoe", "sword"]:
            tool_id = f"{material}_{tool_kind}"
            ToolGenerator(self.context).generate(
                tool_name=tool_id,
                material=material,
                durability=1500,
                attack_damage=7.0,
                mining_speed=8.0,
                tier=tier,
                texture_style=style,
            )
            generated.append(tool_id)

        ArmorGenerator(self.context).generate(material=material, tier=tier, texture_style=style)
        generated.extend([f"{material}_helmet", f"{material}_chestplate", f"{material}_leggings", f"{material}_boots"])

        machine_name = f"{material}_assembler"
        MachineGenerator(self.context).generate(machine_name=machine_name, material=material, texture_style="industrial")
        generated.append(machine_name)

        BlockGenerator(self.context).generate(block_name=f"{material}_bricks", material=material, texture_style=style)
        generated.append(f"{material}_bricks")

        DatapackGenerator(self.context).generate_material_support(material=material, tier=tier)
        generated.append(f"{material}_datapack")

        return generated
