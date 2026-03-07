from __future__ import annotations

import argparse
from pathlib import Path

from asset_studio.skilltree.skilltree_engine import SkillTreeEngine
from asset_studio.workspace.workspace_manager import AssetStudioContext


def register_skilltree_commands(parser: argparse.ArgumentParser) -> None:
    sub = parser.add_subparsers(dest="skilltree_action", required=True)

    new_cmd = sub.add_parser("new", help="Create a new skill tree")
    new_cmd.add_argument("name")
    new_cmd.add_argument("--class-id", default=None)

    validate_cmd = sub.add_parser("validate", help="Validate a skill tree")
    validate_cmd.add_argument("name")

    export_cmd = sub.add_parser("export", help="Export a skill tree to file")
    export_cmd.add_argument("name")
    export_cmd.add_argument("--out", required=True)

    import_cmd = sub.add_parser("import", help="Import a skill tree file")
    import_cmd.add_argument("path")

    sub.add_parser("list", help="List workspace skill trees")


def run_skilltree_command(args: argparse.Namespace, context: AssetStudioContext) -> int:
    engine = SkillTreeEngine(context.workspace_root / "skilltrees")
    profile = context.get_user_profile()

    if args.skilltree_action == "new":
        class_id = args.class_id or profile.preferred_class
        tree = engine.create_tree(name=args.name, owner=profile.username, class_id=class_id)
        print(f"Created skill tree '{tree.name}' for {profile.username}")
        return 0

    if args.skilltree_action == "validate":
        tree = engine.load_tree(args.name)
        report = engine.validate(tree)
        print(f"Skill Tree Validation: {args.name}")
        print(f"- errors: {len(report.errors)}")
        print(f"- warnings: {len(report.warnings)}")
        for message in report.errors:
            print(f"[error] {message}")
        for message in report.warnings:
            print(f"[warning] {message}")
        return 0 if not report.errors else 1

    if args.skilltree_action == "export":
        output = engine.export_tree(args.name, Path(args.out))
        print(f"Exported skill tree to {output}")
        return 0

    if args.skilltree_action == "import":
        tree = engine.import_tree(Path(args.path))
        print(f"Imported skill tree '{tree.name}'")
        return 0

    if args.skilltree_action == "list":
        trees = engine.list_trees()
        if not trees:
            print("No skill trees found")
            return 0
        for name in trees:
            print(name)
        return 0

    raise ValueError(f"Unsupported skilltree action: {args.skilltree_action}")
