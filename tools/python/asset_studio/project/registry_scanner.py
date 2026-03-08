from __future__ import annotations

from dataclasses import dataclass

from asset_studio.registry.registry_scanner import scan_registry_files as _scan_registry_files
from asset_studio.registry.registry_snapshot import RegistrySnapshot


@dataclass
class RegistryScanResult(RegistrySnapshot):
    @property
    def item_count(self) -> int:
        return len(self.item_ids)

    @property
    def block_count(self) -> int:
        return len(self.block_ids)


def scan_registry_files(source_dir):
    snapshot = _scan_registry_files(source_dir)
    return RegistryScanResult(**snapshot.to_dict())
