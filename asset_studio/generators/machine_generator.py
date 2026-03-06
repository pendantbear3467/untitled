from __future__ import annotations

from asset_studio.generators.block_generator import BlockGenerator
from asset_studio.minecraft.loot_templates import simple_self_drop
from asset_studio.workspace.workspace_manager import AssetStudioContext


class MachineGenerator:
    def __init__(self, context: AssetStudioContext) -> None:
        self.context = context
        self.block_gen = BlockGenerator(context)

    def generate(self, machine_name: str, material: str, texture_style: str) -> None:
        self.block_gen.generate(block_name=machine_name, material=material, texture_style=texture_style)
        self.context.write_json(
            self.context.workspace_root / "data" / "loot_tables" / "blocks" / f"{machine_name}.json",
            simple_self_drop(machine_name),
        )
        self.context.asset_db.upsert_machine(machine_name, {"material": material, "texture_style": texture_style})
