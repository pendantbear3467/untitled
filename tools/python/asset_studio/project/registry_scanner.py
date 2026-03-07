from __future__ import annotations

import re
from dataclasses import dataclass
from pathlib import Path


REGISTER_PATTERN = re.compile(r'\.register\("([a-z0-9_]+)"')
ITEM_HINT = re.compile(r"DeferredRegister<Item>|RegistryObject<Item>")
BLOCK_HINT = re.compile(r"DeferredRegister<Block>|RegistryObject<Block>")
MACHINE_HINT = re.compile(r"machine|generator|crusher|assembler", re.IGNORECASE)


@dataclass
class RegistryScanResult:
    files_scanned: int
    item_ids: list[str]
    block_ids: list[str]
    machine_ids: list[str]

    @property
    def item_count(self) -> int:
        return len(self.item_ids)

    @property
    def block_count(self) -> int:
        return len(self.block_ids)

    def to_dict(self) -> dict:
        return {
            "files_scanned": self.files_scanned,
            "item_ids": self.item_ids,
            "block_ids": self.block_ids,
            "machine_ids": self.machine_ids,
        }


def scan_registry_files(source_dir: Path) -> RegistryScanResult:
    item_ids: set[str] = set()
    block_ids: set[str] = set()
    machine_ids: set[str] = set()
    files_scanned = 0

    for java_file in source_dir.rglob("*.java"):
        files_scanned += 1
        content = java_file.read_text(encoding="utf-8", errors="ignore")
        matches = REGISTER_PATTERN.findall(content)

        is_item_file = bool(ITEM_HINT.search(content))
        is_block_file = bool(BLOCK_HINT.search(content))

        for entry_id in matches:
            if is_item_file:
                item_ids.add(entry_id)
            if is_block_file:
                block_ids.add(entry_id)
            if MACHINE_HINT.search(entry_id):
                machine_ids.add(entry_id)

    return RegistryScanResult(
        files_scanned=files_scanned,
        item_ids=sorted(item_ids),
        block_ids=sorted(block_ids),
        machine_ids=sorted(machine_ids),
    )
