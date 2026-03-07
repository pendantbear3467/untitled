from __future__ import annotations

import re
from pathlib import Path

from asset_studio.registry.registry_snapshot import RegistrySnapshot


REGISTER_PATTERN = re.compile(r'\.register\("([a-z0-9_]+)"')
ITEM_HINT = re.compile(r"DeferredRegister<Item>|RegistryObject<Item>")
BLOCK_HINT = re.compile(r"DeferredRegister<Block>|RegistryObject<Block>")


def scan_registry_files(source_dir: Path) -> RegistrySnapshot:
    items: set[str] = set()
    blocks: set[str] = set()
    machines: set[str] = set()
    cables: set[str] = set()
    armor: set[str] = set()
    ores: set[str] = set()
    materials: set[str] = set()

    files_scanned = 0

    for java_file in source_dir.rglob("*.java"):
        files_scanned += 1
        content = java_file.read_text(encoding="utf-8", errors="ignore")
        ids = REGISTER_PATTERN.findall(content)

        is_item_holder = bool(ITEM_HINT.search(content))
        is_block_holder = bool(BLOCK_HINT.search(content))

        for entry_id in ids:
            if is_item_holder:
                items.add(entry_id)
            if is_block_holder:
                blocks.add(entry_id)

            if any(token in entry_id for token in ["machine", "generator", "reactor", "crusher", "assembler", "smelter"]):
                machines.add(entry_id)
            if "cable" in entry_id:
                cables.add(entry_id)
            if any(entry_id.endswith(suffix) for suffix in ["helmet", "chestplate", "leggings", "boots"]):
                armor.add(entry_id)
            if entry_id.endswith("_ore"):
                ores.add(entry_id)
                materials.add(entry_id.removesuffix("_ore"))
            if entry_id.endswith("_ingot"):
                materials.add(entry_id.removesuffix("_ingot"))

    return RegistrySnapshot(
        files_scanned=files_scanned,
        item_ids=sorted(items),
        block_ids=sorted(blocks),
        machine_ids=sorted(machines),
        cable_ids=sorted(cables),
        armor_ids=sorted(armor),
        ore_ids=sorted(ores),
        material_ids=sorted(materials),
    )
