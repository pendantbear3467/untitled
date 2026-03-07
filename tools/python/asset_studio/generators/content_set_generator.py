from __future__ import annotations

from asset_studio.generators.armor_generator import ArmorGenerator
from asset_studio.generators.block_generator import BlockGenerator
from asset_studio.generators.machine_generator import MachineGenerator
from asset_studio.generators.ore_generator import OreGenerator
from asset_studio.generators.tool_generator import ToolGenerator


class ContentSetGenerator:
    """Batch generator for full material content sets."""

    def __init__(self, context) -> None:
        self.context = context

    def generate_material_set(self, material: str, tier: int = 4, style: str = "metallic") -> list[str]:
        generated: list[str] = []

        OreGenerator(self.context).generate(material=material, tier=tier, texture_style=style)
        generated.extend([f"{material}_ore", f"{material}_block"])

        ArmorGenerator(self.context).generate(material=material, tier=tier, texture_style=style)
        generated.extend([f"{material}_helmet", f"{material}_chestplate", f"{material}_leggings", f"{material}_boots"])

        tool_specs = [
            ("pickaxe", 7.0, 10.0),
            ("axe", 9.0, 8.0),
            ("shovel", 5.0, 10.0),
            ("sword", 10.0, 7.0),
            ("hoe", 4.0, 8.0),
        ]
        for suffix, damage, speed in tool_specs:
            tool_id = f"{material}_{suffix}"
            ToolGenerator(self.context).generate(
                tool_name=tool_id,
                material=material,
                durability=1800,
                attack_damage=damage,
                mining_speed=speed,
                tier=tier,
                texture_style=style,
            )
            generated.append(tool_id)

        machine_block = f"{material}_machine_casing"
        MachineGenerator(self.context).generate(machine_name=machine_block, material=material, texture_style="industrial")
        generated.append(machine_block)

        component_block = f"{material}_component_block"
        BlockGenerator(self.context).generate(block_name=component_block, material=material, texture_style=style)
        generated.append(component_block)

        return generated
