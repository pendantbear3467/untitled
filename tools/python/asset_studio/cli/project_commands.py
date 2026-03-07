from __future__ import annotations

import argparse
from pathlib import Path

from asset_studio.workspace.project_manager import ProjectManager
from asset_studio.workspace.workspace_manager import AssetStudioContext


def register_project_commands(parser: argparse.ArgumentParser) -> None:
    sub = parser.add_subparsers(dest="project_action", required=True)

    init_cmd = sub.add_parser("init", help="Initialize project workspace")
    init_cmd.add_argument("--name", default="extremecraft-project")

    open_cmd = sub.add_parser("open", help="Open or create workspace path")
    open_cmd.add_argument("--path", default="workspace")

    build_cmd = sub.add_parser("build", help="Build workspace outputs")
    build_cmd.add_argument("--target", choices=["assets", "resourcepack", "datapack"], default="assets")


def run_project_command(args: argparse.Namespace, context: AssetStudioContext) -> int:
    manager = ProjectManager(context)

    if args.project_action == "init":
        manager.init_project(args.name)
        print(f"Project initialized: {args.name}")
        return 0

    if args.project_action == "open":
        opened = manager.open_project(Path(args.path))
        print(f"Project opened: {opened}")
        return 0

    if args.project_action == "build":
        out = manager.build_project(args.target)
        print(f"Project build complete: {out}")
        return 0

    raise ValueError(f"Unsupported project action: {args.project_action}")
