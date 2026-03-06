from __future__ import annotations

import json
from dataclasses import dataclass
from datetime import UTC, datetime
from pathlib import Path

from asset_studio.registry.registry_snapshot import RegistrySnapshot


@dataclass
class SnapshotRecord:
    name: str
    path: Path
    created_at: str


class RegistryHistory:
    def __init__(self, workspace_root: Path) -> None:
        self.history_root = workspace_root / "registry_history"
        self.history_root.mkdir(parents=True, exist_ok=True)

    def save_snapshot(self, name: str, snapshot: RegistrySnapshot) -> SnapshotRecord:
        path = self.history_root / f"{name}.json"
        snapshot.write_json(path)
        return SnapshotRecord(name=name, path=path, created_at=datetime.now(UTC).isoformat())

    def list_snapshots(self) -> list[SnapshotRecord]:
        records: list[SnapshotRecord] = []
        for path in sorted(self.history_root.glob("*.json")):
            created_at = datetime.fromtimestamp(path.stat().st_mtime, tz=UTC).isoformat()
            records.append(SnapshotRecord(name=path.stem, path=path, created_at=created_at))
        return records

    def load_snapshot(self, name_or_path: str) -> RegistrySnapshot:
        path = Path(name_or_path)
        if not path.exists():
            path = self.history_root / f"{name_or_path}.json"
        if not path.exists():
            raise FileNotFoundError(f"Snapshot not found: {name_or_path}")

        payload = json.loads(path.read_text(encoding="utf-8"))
        return RegistrySnapshot(
            files_scanned=int(payload.get("files_scanned", 0)),
            item_ids=list(payload.get("item_ids", [])),
            block_ids=list(payload.get("block_ids", [])),
            machine_ids=list(payload.get("machine_ids", [])),
            cable_ids=list(payload.get("cable_ids", [])),
            armor_ids=list(payload.get("armor_ids", [])),
            ore_ids=list(payload.get("ore_ids", [])),
            material_ids=list(payload.get("material_ids", [])),
        )
