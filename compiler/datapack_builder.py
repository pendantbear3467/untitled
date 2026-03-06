from __future__ import annotations

import shutil
from pathlib import Path


class DatapackBuilder:
    """Builds datapack payload for module artifacts."""

    def build(self, workspace_root: Path, module_root: Path) -> Path:
        data_src = workspace_root / "data"
        data_dst = module_root / "src" / "main" / "resources" / "data" / "extremecraft"

        if data_dst.exists():
            shutil.rmtree(data_dst)
        if data_src.exists():
            data_dst.parent.mkdir(parents=True, exist_ok=True)
            shutil.copytree(data_src, data_dst)
        else:
            data_dst.mkdir(parents=True, exist_ok=True)

        return data_dst
