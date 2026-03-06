from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path

from . import blockbench_exporter
from .minecraft_json_generator import (
    generate_cube_block_model,
    generate_item_model,
    generate_single_variant_blockstate,
    write_json,
)


@dataclass
class ModelTargets:
    block_models: Path
    item_models: Path
    blockstates: Path
    blockbench: Path


class ModelGenerator:
    def __init__(self, targets: ModelTargets, dry_run: bool = False) -> None:
        self.targets = targets
        self.dry_run = dry_run

    def generate_block_and_item_models(self, block_id: str) -> int:
        changed = 0
        changed += int(
            write_json(
                self.targets.block_models / f"{block_id}.json",
                generate_cube_block_model(block_id),
                dry_run=self.dry_run,
            )
        )
        changed += int(
            write_json(
                self.targets.blockstates / f"{block_id}.json",
                generate_single_variant_blockstate(block_id),
                dry_run=self.dry_run,
            )
        )
        changed += int(
            write_json(
                self.targets.item_models / f"{block_id}.json",
                generate_item_model(block_id, is_block_item=True),
                dry_run=self.dry_run,
            )
        )
        return changed

    def generate_item_model(self, item_id: str) -> int:
        return int(
            write_json(
                self.targets.item_models / f"{item_id}.json",
                generate_item_model(item_id, is_block_item=False),
                dry_run=self.dry_run,
            )
        )

    def export_blockbench_templates(self, base_name: str, include_drill: bool = False) -> int:
        changed = 0
        changed += int(
            blockbench_exporter.write_bbmodel(
                self.targets.blockbench / f"{base_name}_ore_block.bbmodel",
                blockbench_exporter.ore_block_model(f"{base_name}_ore"),
                dry_run=self.dry_run,
            )
        )
        changed += int(
            blockbench_exporter.write_bbmodel(
                self.targets.blockbench / f"{base_name}_machine_casing.bbmodel",
                blockbench_exporter.machine_casing_model(f"{base_name}_machine_casing"),
                dry_run=self.dry_run,
            )
        )
        changed += int(
            blockbench_exporter.write_bbmodel(
                self.targets.blockbench / f"{base_name}_tool.bbmodel",
                blockbench_exporter.tool_model(f"{base_name}_pickaxe"),
                dry_run=self.dry_run,
            )
        )
        changed += int(
            blockbench_exporter.write_bbmodel(
                self.targets.blockbench / f"{base_name}_armor_icon.bbmodel",
                blockbench_exporter.armor_icon_model(f"{base_name}_helmet"),
                dry_run=self.dry_run,
            )
        )
        if include_drill:
            changed += int(
                blockbench_exporter.write_bbmodel(
                    self.targets.blockbench / f"{base_name}_drill.bbmodel",
                    blockbench_exporter.drill_model(f"{base_name}_drill"),
                    dry_run=self.dry_run,
                )
            )
        return changed
