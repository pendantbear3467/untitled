from __future__ import annotations

import argparse

from asset_studio.registry.registry_scanner import scan_registry_files
from asset_studio.workspace.workspace_manager import AssetStudioContext


def register_registry_commands(parser: argparse.ArgumentParser) -> None:
    parser.add_argument("--source", default="src/main/java")
    parser.add_argument("--output", default=None, help="Snapshot output path (default: workspace/registry_snapshot.json)")


def run_registry_scan_command(args: argparse.Namespace, context: AssetStudioContext) -> int:
    result = scan_registry_files(context.repo_root / args.source)
    output_path = context.workspace_root / "registry_snapshot.json" if args.output is None else context.repo_root / args.output
    result.write_json(output_path)
    print(f"Scanned {result.files_scanned} files")
    print(
        "Detected "
        f"items={len(result.item_ids)} blocks={len(result.block_ids)} machines={len(result.machine_ids)} "
        f"cables={len(result.cable_ids)} ores={len(result.ore_ids)} materials={len(result.material_ids)}"
    )
    print(f"Snapshot: {output_path}")
    return 0
