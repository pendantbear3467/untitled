from __future__ import annotations

import json
from dataclasses import asdict, dataclass
from pathlib import Path

from asset_studio.plugins.plugin_api import PluginMetadata


@dataclass(frozen=True)
class PluginManifest:
    name: str
    version: str
    dependencies: tuple[str, ...]
    compatible_platform_version: str
    entrypoint: str


class PluginMarketplace:
    """Marketplace metadata index for installed Asset Studio plugins."""

    def __init__(self, workspace_root: Path) -> None:
        self.workspace_root = workspace_root
        self.marketplace_root = workspace_root / "plugin_marketplace"
        self.marketplace_root.mkdir(parents=True, exist_ok=True)

    def manifest_from_metadata(self, metadata: PluginMetadata) -> PluginManifest:
        return PluginManifest(
            name=metadata.name,
            version=metadata.version,
            dependencies=tuple(metadata.dependencies),
            compatible_platform_version=metadata.compatible_platform_version,
            entrypoint=metadata.entrypoint,
        )

    def write_index(self, metadata: dict[str, PluginMetadata]) -> Path:
        manifests = [asdict(self.manifest_from_metadata(entry)) for entry in sorted(metadata.values(), key=lambda item: item.name)]
        path = self.marketplace_root / "index.json"
        path.write_text(json.dumps({"plugins": manifests}, indent=2) + "\n", encoding="utf-8")
        return path

    def read_index(self) -> list[PluginManifest]:
        path = self.marketplace_root / "index.json"
        if not path.exists():
            return []

        payload = json.loads(path.read_text(encoding="utf-8"))
        manifests: list[PluginManifest] = []
        for entry in payload.get("plugins", []):
            if not isinstance(entry, dict):
                continue
            manifests.append(
                PluginManifest(
                    name=str(entry.get("name", "unknown")),
                    version=str(entry.get("version", "0.0.0")),
                    dependencies=tuple(str(dep) for dep in entry.get("dependencies", [])),
                    compatible_platform_version=str(entry.get("compatible_platform_version", "*")),
                    entrypoint=str(entry.get("entrypoint", "")),
                )
            )
        return manifests
