from __future__ import annotations

import shutil

from asset_studio.project.workspace_manager import AssetStudioContext


def export_pack_command(context: AssetStudioContext, target: str) -> int:
    exports = context.workspace_root / "exports"
    exports.mkdir(parents=True, exist_ok=True)

    if target in {"resourcepack", "forge", "fabric"}:
        destination = exports / f"{target}_assets"
        _copy_tree(context.workspace_root / "assets", destination)
        print(f"Exported {target} assets to {destination}")
        return 0

    if target == "datapack":
        destination = exports / "datapack"
        _copy_tree(context.workspace_root / "data", destination)
        print(f"Exported datapack to {destination}")
        return 0

    raise ValueError(f"Unsupported export target: {target}")


def _copy_tree(source, destination) -> None:
    if not source.exists():
        destination.mkdir(parents=True, exist_ok=True)
        return
    if destination.exists():
        shutil.rmtree(destination)
    shutil.copytree(source, destination)
