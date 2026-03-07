from __future__ import annotations

import json
from dataclasses import dataclass
from pathlib import Path


@dataclass
class AssetDatabase:
    path: Path

    def _load(self) -> dict:
        if not self.path.exists():
            return {"materials": {}, "items": {}, "machines": {}}
        return json.loads(self.path.read_text(encoding="utf-8"))

    def _save(self, data: dict) -> None:
        self.path.parent.mkdir(parents=True, exist_ok=True)
        self.path.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8")

    def upsert_material(self, name: str, payload: dict) -> None:
        data = self._load()
        data.setdefault("materials", {})[name] = payload
        self._save(data)

    def upsert_item(self, name: str, payload: dict) -> None:
        data = self._load()
        data.setdefault("items", {})[name] = payload
        self._save(data)

    def upsert_machine(self, name: str, payload: dict) -> None:
        data = self._load()
        data.setdefault("machines", {})[name] = payload
        self._save(data)
