from __future__ import annotations

import json
from dataclasses import dataclass, field
from pathlib import Path


@dataclass
class RegistrySnapshot:
    files_scanned: int = 0
    item_ids: list[str] = field(default_factory=list)
    block_ids: list[str] = field(default_factory=list)
    machine_ids: list[str] = field(default_factory=list)
    cable_ids: list[str] = field(default_factory=list)
    armor_ids: list[str] = field(default_factory=list)
    ore_ids: list[str] = field(default_factory=list)
    material_ids: list[str] = field(default_factory=list)

    def to_dict(self) -> dict:
        return {
            "files_scanned": self.files_scanned,
            "item_ids": self.item_ids,
            "block_ids": self.block_ids,
            "machine_ids": self.machine_ids,
            "cable_ids": self.cable_ids,
            "armor_ids": self.armor_ids,
            "ore_ids": self.ore_ids,
            "material_ids": self.material_ids,
        }

    def write_json(self, path: Path) -> None:
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(json.dumps(self.to_dict(), indent=2) + "\n", encoding="utf-8")
