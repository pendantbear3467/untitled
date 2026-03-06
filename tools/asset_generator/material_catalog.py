from __future__ import annotations

import json
from dataclasses import dataclass
from pathlib import Path


@dataclass(frozen=True)
class MaterialDefinition:
    name: str
    color: str
    tier: int
    glow: bool = False


DEFAULT_MATERIALS = {
    "materials": [
        {"name": "titanium", "color": "#9bb7d4", "tier": 3, "glow": False},
        {"name": "uranium", "color": "#62ff3a", "tier": 4, "glow": True},
        {"name": "silver", "color": "#cfd9e8", "tier": 2, "glow": False},
        {"name": "mythril", "color": "#57d6ff", "tier": 5, "glow": True},
        {"name": "adamantium", "color": "#7f8c9a", "tier": 6, "glow": False},
        {"name": "quantum_alloy", "color": "#4ef0c9", "tier": 7, "glow": True},
    ]
}


class MaterialCatalog:
    def __init__(self, path: Path) -> None:
        self.path = path

    def ensure_exists(self) -> None:
        if self.path.exists():
            return
        self.path.parent.mkdir(parents=True, exist_ok=True)
        self.path.write_text(json.dumps(DEFAULT_MATERIALS, indent=2) + "\n", encoding="utf-8")

    def load(self) -> list[MaterialDefinition]:
        self.ensure_exists()
        data = json.loads(self.path.read_text(encoding="utf-8"))
        materials_raw = data.get("materials", [])
        materials: list[MaterialDefinition] = []
        seen: set[str] = set()

        for raw in materials_raw:
            name = str(raw.get("name", "")).strip().lower()
            if not name:
                continue
            if name in seen:
                continue
            seen.add(name)
            materials.append(
                MaterialDefinition(
                    name=name,
                    color=str(raw.get("color", "#808080")),
                    tier=int(raw.get("tier", 1)),
                    glow=bool(raw.get("glow", False)),
                )
            )
        return materials
