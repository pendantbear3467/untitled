from __future__ import annotations

import argparse
from pathlib import Path

from asset_studio.blockbench.bbmodel_exporter import export_bbmodel
from asset_studio.blockbench.bbmodel_importer import import_bbmodel
from asset_studio.cli.build_commands import build_command
from asset_studio.cli.generate_commands import register_generate_commands, run_generate_command
from asset_studio.cli.project_commands import register_project_commands, run_project_command
from asset_studio.cli.registry_commands import register_registry_commands, run_registry_scan_command
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

    scan = subparsers.add_parser("scan-registry", help="Scan Java registries")
    register_registry_commands(scan)

    export_cmd = subparsers.add_parser("export", help="Export operations")
    export_sub = export_cmd.add_subparsers(dest="export_target", required=True)
    bb_export = export_sub.add_parser("blockbench", help="Export model as .bbmodel")
    bb_export.add_argument("model_id")
    bb_export.add_argument("--kind", default="block", choices=["block", "item"])

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

    if args.command == "export" and args.export_target == "blockbench":
        path = export_bbmodel(args.model_id, context=context, kind=args.kind)
        print(f"Exported Blockbench model: {path}")
        return 0

    if args.command == "import" and args.import_target == "blockbench":
        model_id = import_bbmodel(Path(args.bbmodel_path), context=context)
        print(f"Imported Blockbench model: {model_id}")
        return 0

    if args.command == "scan-registry":
        return run_registry_scan_command(args, context)

    raise ValueError(f"Unsupported command: {args.command}")
