from __future__ import annotations

import argparse
import json
from pathlib import Path

from asset_studio.skilltree.engine import SkillTreeEngine
from asset_studio.skilltree.serializer import (
    LEGACY_PROJECT_FORMAT,
    RUNTIME_EXPORT_FORMAT,
    STUDIO_EXPORT_FORMAT,
    STUDIO_PROJECT_FORMAT,
    WORKSPACE_EXPORT_FORMAT,
)
from asset_studio.skilltree.simulator import SimulationRequest
from asset_studio.workspace.workspace_manager import AssetStudioContext


def register_skilltree_commands(parser: argparse.ArgumentParser) -> None:
    sub = parser.add_subparsers(dest="skilltree_action", required=True)

    new_cmd = sub.add_parser("new", help="Create a new skill tree")
    new_cmd.add_argument("name")
    new_cmd.add_argument("--class-id", default=None)

    validate_cmd = sub.add_parser("validate", help="Validate a skill tree")
    validate_cmd.add_argument("name")
    validate_cmd.add_argument("--report-out", default=None)

    export_cmd = sub.add_parser("export", help="Export a skill tree to file")
    export_cmd.add_argument("name")
    export_cmd.add_argument("--out", required=True)
    export_cmd.add_argument("--format", default=WORKSPACE_EXPORT_FORMAT, choices=[WORKSPACE_EXPORT_FORMAT, RUNTIME_EXPORT_FORMAT, STUDIO_EXPORT_FORMAT])

    import_cmd = sub.add_parser("import", help="Import a skill tree file")
    import_cmd.add_argument("path")

    simulate_cmd = sub.add_parser("simulate", help="Simulate a skill tree unlock path")
    simulate_cmd.add_argument("name")
    simulate_cmd.add_argument("--level", type=int, default=1)
    simulate_cmd.add_argument("--points", type=int, default=0)
    simulate_cmd.add_argument("--class-id", default="adventurer")
    simulate_cmd.add_argument("--unlock", action="append", default=[])

    analyze_cmd = sub.add_parser("analyze", help="Run balance analysis for a skill tree")
    analyze_cmd.add_argument("name")
    analyze_cmd.add_argument("--out", default=None)

    project_cmd = sub.add_parser("export-project", help="Export all or selected skill trees as a project file")
    project_cmd.add_argument("--out", required=True)
    project_cmd.add_argument("--format", default=STUDIO_PROJECT_FORMAT, choices=[STUDIO_PROJECT_FORMAT, LEGACY_PROJECT_FORMAT])
    project_cmd.add_argument("--active-tree", default="")
    project_cmd.add_argument("names", nargs="*")

    import_project_cmd = sub.add_parser("import-project", help="Import a legacy or versioned skill tree project file")
    import_project_cmd.add_argument("path")

    sub.add_parser("list", help="List workspace skill trees")


def run_skilltree_command(args: argparse.Namespace, context: AssetStudioContext) -> int:
    engine = SkillTreeEngine(context.workspace_root / "skilltrees")
    profile = context.get_user_profile()

    if args.skilltree_action == "new":
        class_id = args.class_id or profile.preferred_class
        tree = engine.create_tree(name=args.name, owner=profile.username, class_id=class_id)
        profile.skilltree_preferences.last_tree = tree.name
        context.save_user_profile(profile)
        print(f"Created skill tree '{tree.name}' for {profile.username}")
        return 0

    if args.skilltree_action == "validate":
        tree = engine.load_tree(args.name)
        report = engine.validate(tree)
        print(f"Skill Tree Validation: {args.name}")
        print(f"- errors: {len(report.errors)}")
        print(f"- warnings: {len(report.warnings)}")
        for issue in report.issues:
            print(f"[{issue.severity}] {issue.code}: {issue.message}")
        if args.report_out:
            engine.export_validation_report(tree, Path(args.report_out))
            print(f"Validation report written to {args.report_out}")
        return 0 if not report.has_errors else 1

    if args.skilltree_action == "export":
        output = engine.export_tree(args.name, Path(args.out), format=args.format)
        print(f"Exported skill tree to {output}")
        return 0

    if args.skilltree_action == "import":
        result = engine.safe_import_tree(Path(args.path))
        engine.save_tree(result.document)
        print(f"Imported skill tree '{result.document.name}'")
        for issue in result.report.issues:
            print(f"[{issue.severity}] {issue.code}: {issue.message}")
        return 0 if not result.report.has_errors else 1

    if args.skilltree_action == "simulate":
        tree = engine.load_tree(args.name)
        result = engine.simulate(
            tree,
            SimulationRequest(
                player_level=args.level,
                skill_points=args.points,
                selected_class=args.class_id,
                requested_unlocks=list(args.unlock),
            ),
        )
        print(json.dumps(result.to_dict(), indent=2))
        return 0

    if args.skilltree_action == "analyze":
        tree = engine.load_tree(args.name)
        report = engine.analyze_balance(tree)
        payload = report.to_dict()
        if args.out:
            Path(args.out).write_text(json.dumps(payload, indent=2) + "\n", encoding="utf-8")
            print(f"Balance analysis written to {args.out}")
        else:
            print(json.dumps(payload, indent=2))
        return 0

    if args.skilltree_action == "export-project":
        output = engine.export_project(
            Path(args.out),
            tree_names=list(args.names),
            format=args.format,
            active_tree_name=args.active_tree,
        )
        print(f"Exported skill tree project to {output}")
        return 0

    if args.skilltree_action == "import-project":
        result = engine.import_project(Path(args.path))
        print(f"Imported {len(result.documents)} skill tree(s) from project")
        if result.active_tree_name:
            print(f"Active tree: {result.active_tree_name}")
        for issue in result.report.issues:
            print(f"[{issue.severity}] {issue.code}: {issue.message}")
        return 0 if not result.report.has_errors else 1

    if args.skilltree_action == "list":
        trees = engine.list_trees()
        if not trees:
            print("No skill trees found")
            return 0
        for name in trees:
            print(name)
        return 0

    raise ValueError(f"Unsupported skilltree action: {args.skilltree_action}")
