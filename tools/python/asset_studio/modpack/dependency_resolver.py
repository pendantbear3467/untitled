from __future__ import annotations

import json
from pathlib import Path


class ModpackDependencyResolver:
    """Resolves modpack dependencies from local manifest definitions."""

    def __init__(self, workspace_root: Path) -> None:
        self.workspace_root = workspace_root

    def resolve(self, modpack_name: str) -> list[dict]:
        path = self.workspace_root / "modpacks" / f"{modpack_name}.json"
        if not path.exists():
            return [
                {
                    "id": "extremecraft-core",
                    "source": "local",
                    "required": True,
                }
            ]

        payload = json.loads(path.read_text(encoding="utf-8"))
        mods = payload.get("mods", [])
        result: list[dict] = []
        for mod in mods:
            if isinstance(mod, str):
                result.append({"id": mod, "source": "local", "required": True})
            elif isinstance(mod, dict):
                result.append(mod)
        if not any(entry.get("id") == "extremecraft-core" for entry in result):
            result.insert(0, {"id": "extremecraft-core", "source": "local", "required": True})
        return result
