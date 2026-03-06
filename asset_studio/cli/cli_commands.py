from __future__ import annotations

import argparse
from pathlib import Path

from asset_studio.blockbench.bbmodel_exporter import export_bbmodel
from asset_studio.blockbench.bbmodel_importer import import_bbmodel
from asset_studio.cli.build_commands import build_command
from asset_studio.cli.compile_commands import register_compile_commands, run_compile_command
from asset_studio.cli.generate_commands import register_generate_commands, run_generate_command
from asset_studio.cli.modpack_commands import register_modpack_commands, run_modpack_command
from asset_studio.cli.pack_commands import export_pack_command
from asset_studio.cli.project_commands import register_project_commands, run_project_command
from asset_studio.cli.registry_commands import register_registry_commands, run_registry_command, run_registry_scan_command
from asset_studio.cli.release_commands import register_release_commands, run_release_command
from asset_studio.cli.repair_commands import register_repair_commands, run_repair_command
from asset_studio.cli.sdk_commands import register_sdk_commands, run_sdk_command
from asset_studio.cli.validate_commands import register_validate_commands, run_validate_command
from asset_studio.workspace.workspace_manager import WorkspaceManager


def register_subcommands(subparsers: argparse._SubParsersAction[argparse.ArgumentParser]) -> None:
    generate = subparsers.add_parser("generate", help="Generate content")
    register_generate_commands(generate)

    validate = subparsers.add_parser("validate", help="Validate generated assets")
    register_validate_commands(validate)

    project = subparsers.add_parser("project", help="Workspace/project operations")
    register_project_commands(project)

    build = subparsers.add_parser("build", help="Build helper commands")
    build.add_argument("target", choices=["assets", "resourcepack", "datapack"])

    registry = subparsers.add_parser("registry", help="Registry scan/diff/history tools")
    register_registry_commands(registry)

    scan_alias = subparsers.add_parser("scan-registry", help="Legacy alias for registry scan")
    scan_alias.add_argument("--source", default="src/main/java")
    scan_alias.add_argument("--output", default=None, help="Snapshot output path (default: workspace/registry_snapshot.json)")
    scan_alias.add_argument("--save-history", default=None)

    sdk = subparsers.add_parser("sdk", help="ExtremeCraft SDK commands")
    register_sdk_commands(sdk)

    compile_cmd = subparsers.add_parser("compile", help="Compile addon expansions")
    register_compile_commands(compile_cmd)

    repair = subparsers.add_parser("repair", help="Auto repair missing assets")
    register_repair_commands(repair)

    release = subparsers.add_parser("release", help="Build and publish releases")
    register_release_commands(release)

    modpack = subparsers.add_parser("modpack", help="Build modpack archives")
    register_modpack_commands(modpack)

    export_cmd = subparsers.add_parser("export", help="Export operations")
    export_sub = export_cmd.add_subparsers(dest="export_target", required=True)

    bb_export = export_sub.add_parser("blockbench", help="Export model as .bbmodel")
    bb_export.add_argument("model_id")
    bb_export.add_argument("--kind", default="block", choices=["block", "item"])

    pack_export = export_sub.add_parser("pack", help="Export resourcepack/datapack bundles")
    pack_export.add_argument("target", choices=["resourcepack", "datapack", "forge", "fabric"])

    import_cmd = subparsers.add_parser("import", help="Import operations")
    import_sub = import_cmd.add_subparsers(dest="import_target", required=True)
    bb_import = import_sub.add_parser("blockbench", help="Import .bbmodel file")
    bb_import.add_argument("bbmodel_path")


def run_cli(args: argparse.Namespace, workspace_path: Path) -> int:
    manager = WorkspaceManager(workspace_root=workspace_path, repo_root=Path.cwd())
    context = manager.load_context()

    if args.command == "generate":
        return run_generate_command(args, context)

    if args.command == "validate":
        return run_validate_command(args, context)

    if args.command == "project":
        return run_project_command(args, context)

    if args.command == "build":
        return build_command(context, args.target)

    if args.command == "registry":
        return run_registry_command(args, context)

    if args.command == "scan-registry":
        return run_registry_scan_command(args, context)

    if args.command == "sdk":
        return run_sdk_command(args, context)

    if args.command == "compile":
        return run_compile_command(args, context)

    if args.command == "repair":
        return run_repair_command(args, context)

    if args.command == "release":
        return run_release_command(args, context)

    if args.command == "modpack":
        return run_modpack_command(args, context)

    if args.command == "export" and args.export_target == "blockbench":
        path = export_bbmodel(args.model_id, context=context, kind=args.kind)
        print(f"Exported Blockbench model: {path}")
        return 0

    if args.command == "export" and args.export_target == "pack":
        return export_pack_command(context, args.target)

    if args.command == "import" and args.import_target == "blockbench":
        model_id = import_bbmodel(Path(args.bbmodel_path), context=context)
        print(f"Imported Blockbench model: {model_id}")
        return 0

    raise ValueError(f"Unsupported command: {args.command}")
