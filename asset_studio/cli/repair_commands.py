from __future__ import annotations

import argparse

from asset_studio.repair.repair_engine import RepairEngine


def register_repair_commands(parser: argparse.ArgumentParser) -> None:
    sub = parser.add_subparsers(dest="repair_command", required=True)
    sub.add_parser("run", help="Run auto asset repair pipeline")


def run_repair_command(args: argparse.Namespace, context) -> int:
    if args.repair_command != "run":
        raise ValueError(f"Unsupported repair command: {args.repair_command}")

    report = RepairEngine(context).repair()
    print(f"Repair actions: {report.total}")
    for action in report.actions:
        print(f"- [{action.category}] {action.path} :: {action.message}")
    return 0
