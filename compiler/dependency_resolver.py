from __future__ import annotations

import json
from dataclasses import dataclass, field
from pathlib import Path

from extremecraft_sdk.definitions.definition_types import AddonSpec


@dataclass
class Conflict:
    kind: str
    identifier: str
    message: str


@dataclass
class DependencyResolution:
    dependencies: list[str] = field(default_factory=list)
    conflicts: list[Conflict] = field(default_factory=list)


class DependencyResolver:
    """Resolves addon dependencies and detects registry id conflicts."""

    def __init__(self, workspace_root: Path) -> None:
        self.workspace_root = workspace_root

    def resolve(self, addon: AddonSpec) -> DependencyResolution:
        resolution = DependencyResolution(dependencies=list(addon.dependencies))
        known_ids = self._load_existing_ids()

        for definition in addon.definitions:
            key = f"{definition.type}:{definition.id}"
            if key in known_ids:
                resolution.conflicts.append(
                    Conflict(
                        kind="duplicate_id",
                        identifier=key,
                        message=f"Definition '{key}' conflicts with existing registry entries",
                    )
                )
            known_ids.add(key)

        return resolution

    def _load_existing_ids(self) -> set[str]:
        snapshot_path = self.workspace_root / "registry_snapshot.json"
        if not snapshot_path.exists():
            return set()

        payload = json.loads(snapshot_path.read_text(encoding="utf-8"))
        existing: set[str] = set()
        for key, definition_type in [
            ("item_ids", "item"),
            ("block_ids", "block"),
            ("machine_ids", "machine"),
            ("material_ids", "material"),
            ("ore_ids", "worldgen"),
        ]:
            for entry_id in payload.get(key, []):
                existing.add(f"{definition_type}:{entry_id}")
        return existing
