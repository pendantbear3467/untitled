from __future__ import annotations

from dataclasses import dataclass

from asset_studio.registry.registry_snapshot import RegistrySnapshot


@dataclass
class RegistryDiff:
    added_items: list[str]
    removed_items: list[str]
    added_blocks: list[str]
    removed_blocks: list[str]


def diff_snapshots(old: RegistrySnapshot, new: RegistrySnapshot) -> RegistryDiff:
    old_items = set(old.item_ids)
    new_items = set(new.item_ids)
    old_blocks = set(old.block_ids)
    new_blocks = set(new.block_ids)

    return RegistryDiff(
        added_items=sorted(new_items - old_items),
        removed_items=sorted(old_items - new_items),
        added_blocks=sorted(new_blocks - old_blocks),
        removed_blocks=sorted(old_blocks - new_blocks),
    )
