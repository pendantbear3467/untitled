from __future__ import annotations

import argparse
from pathlib import Path

from asset_studio.registry.registry_diff_viewer import RegistryDiffViewer
from asset_studio.registry.registry_history import RegistryHistory
from asset_studio.registry.registry_scanner import scan_registry_files


def register_registry_commands(parser: argparse.ArgumentParser) -> None:
    sub = parser.add_subparsers(dest="registry_command", required=True)

    scan = sub.add_parser("scan", help="Scan Java registries and write snapshot")
    scan.add_argument("--source", default="src/main/java")
    scan.add_argument("--output", default=None, help="Snapshot output path (default: workspace/registry_snapshot.json)")
    scan.add_argument("--save-history", default=None, help="Also save snapshot to registry history with this name")

    diff = sub.add_parser("diff", help="Compare two registry snapshots")
    diff.add_argument("before")
    diff.add_argument("after")

    history = sub.add_parser("history", help="Registry history operations")
    history_sub = history.add_subparsers(dest="history_command", required=True)
    history_sub.add_parser("list", help="List saved snapshots")


def run_registry_command(args: argparse.Namespace, context) -> int:
    if args.registry_command == "scan":
        return _run_scan(args, context)

    if args.registry_command == "diff":
        history = RegistryHistory(context.workspace_root)
        before = history.load_snapshot(args.before)
        after = history.load_snapshot(args.after)
        diff = RegistryDiffViewer().diff(before, after)

        print(f"Items: +{len(diff.added_items)} / -{len(diff.removed_items)}")
        print(f"Blocks: +{len(diff.added_blocks)} / -{len(diff.removed_blocks)}")
        if diff.renamed_items:
            print("Potential renamed IDs:")
            for rename in diff.renamed_items[:20]:
                print(f"- {rename.old_id} -> {rename.new_id} ({rename.score:.2f})")
        if diff.conflicts:
            print("Conflicts:")
            for conflict in diff.conflicts:
                print(f"- {conflict.id}: {conflict.reason}")
        return 0

    if args.registry_command == "history" and args.history_command == "list":
        history = RegistryHistory(context.workspace_root)
        records = history.list_snapshots()
        for record in records:
            print(f"{record.name} :: {record.created_at} :: {record.path}")
        print(f"Total snapshots: {len(records)}")
        return 0

    raise ValueError(f"Unsupported registry command: {args.registry_command}")


def run_registry_scan_command(args: argparse.Namespace, context) -> int:
    result = scan_registry_files(context.repo_root / args.source)
    output_path = context.workspace_root / "registry_snapshot.json" if args.output is None else context.repo_root / args.output
    result.write_json(output_path)

    if getattr(args, "save_history", None):
        RegistryHistory(context.workspace_root).save_snapshot(args.save_history, result)

    print(f"Scanned {result.files_scanned} files")
    print(
        "Detected "
        f"items={len(result.item_ids)} blocks={len(result.block_ids)} machines={len(result.machine_ids)} "
        f"cables={len(result.cable_ids)} ores={len(result.ore_ids)} materials={len(result.material_ids)}"
    )
    print(f"Snapshot: {output_path}")
    return 0


def _run_scan(args: argparse.Namespace, context) -> int:
    result = scan_registry_files(context.repo_root / args.source)
    output_path = context.workspace_root / "registry_snapshot.json" if args.output is None else Path(args.output)
    result.write_json(output_path)

    if args.save_history:
        history_record = RegistryHistory(context.workspace_root).save_snapshot(args.save_history, result)
        print(f"History snapshot: {history_record.path}")

    print(f"Scanned {result.files_scanned} files")
    print(
        "Detected "
        f"items={len(result.item_ids)} blocks={len(result.block_ids)} machines={len(result.machine_ids)} "
        f"cables={len(result.cable_ids)} ores={len(result.ore_ids)} materials={len(result.material_ids)}"
    )
    print(f"Snapshot: {output_path}")
    return 0
