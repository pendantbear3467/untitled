from __future__ import annotations

import shutil
from pathlib import Path


class AssetBuilder:
    """Collect generated workspace assets into a module output structure."""

    def build(self, workspace_root: Path, module_root: Path) -> Path:
        assets_src = workspace_root / "assets"
        assets_dst = module_root / "src" / "main" / "resources" / "assets" / "extremecraft"
        self._copy_tree(assets_src, assets_dst)
        return assets_dst

    def _copy_tree(self, source: Path, destination: Path) -> None:
        if destination.exists():
            shutil.rmtree(destination)
        if source.exists():
            destination.parent.mkdir(parents=True, exist_ok=True)
            shutil.copytree(source, destination)
        else:
            destination.mkdir(parents=True, exist_ok=True)
