from __future__ import annotations

import json
import zipfile
from dataclasses import dataclass
from pathlib import Path

from asset_studio.modpack.dependency_resolver import ModpackDependencyResolver
from asset_studio.modpack.modpack_manifest import ModpackManifest


@dataclass
class ModpackBuildResult:
    name: str
    manifest_path: Path
    archive_path: Path


class ModpackBuilder:
    """Builds distributable modpack manifests and archives."""

    def __init__(self, context) -> None:
        self.context = context
        self.resolver = ModpackDependencyResolver(context.workspace_root)

    def build(self, modpack_name: str) -> ModpackBuildResult:
        mods = self.resolver.resolve(modpack_name)
        manifest = ModpackManifest(name=modpack_name, mods=mods, overrides=["config", "scripts", "kubejs"])

        build_root = self.context.workspace_root / "build" / "modpacks" / modpack_name
        build_root.mkdir(parents=True, exist_ok=True)
        (build_root / "mods").mkdir(parents=True, exist_ok=True)
        (build_root / "config").mkdir(parents=True, exist_ok=True)
        (build_root / "scripts").mkdir(parents=True, exist_ok=True)

        manifest_path = build_root / "manifest.json"
        manifest_path.write_text(json.dumps(manifest.to_dict(), indent=2) + "\n", encoding="utf-8")

        archive_path = self.context.workspace_root / "build" / "modpacks" / f"{modpack_name}.zip"
        with zipfile.ZipFile(archive_path, "w", compression=zipfile.ZIP_DEFLATED) as archive:
            archive.write(manifest_path, Path("modpack") / manifest_path.name)
            self._pack_optional_override(archive, self.context.workspace_root / "assets", "workspace/assets")
            self._pack_optional_override(archive, self.context.workspace_root / "data", "workspace/data")
            self._pack_optional_override(archive, self.context.repo_root / "config", "config")

        return ModpackBuildResult(name=modpack_name, manifest_path=manifest_path, archive_path=archive_path)

    def _pack_optional_override(self, archive: zipfile.ZipFile, source: Path, rel_path: str) -> None:
        if not source.exists():
            return
        for file in source.rglob("*"):
            if file.is_file():
                archive.write(file, Path("overrides") / Path(rel_path) / file.relative_to(source))
