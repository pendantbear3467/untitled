from __future__ import annotations

import argparse

from asset_studio.runtime.build_service import BuildService
from asset_studio.runtime.task_results import StudioTaskResult


def register_compile_commands(parser: argparse.ArgumentParser) -> None:
    sub = parser.add_subparsers(dest="compile_target", required=True)

    expansion = sub.add_parser("expansion", help="Compile addon expansion into module artifact")
    expansion.add_argument("addon_name")


def run_compile_command(args: argparse.Namespace, context) -> int:
    if args.compile_target != "expansion":
        raise ValueError(f"Unsupported compile target: {args.compile_target}")

    result = BuildService(context).compile_expansion(args.addon_name)
    _print_result(result)
    return 0 if result.success else 1


def _print_result(result: StudioTaskResult) -> None:
    print(result.message)
    if result.data and isinstance(result.data, dict):
        for key in ["addon", "jar", "outputRoot"]:
            if key in result.data:
                print(f"{key}: {result.data[key]}")
    if result.report is not None:
        for artifact in result.report.artifacts:
            if artifact.path is not None and artifact.kind not in {"module_output", "jar"}:
                print(f"- {artifact.kind}: {artifact.path}")
        for issue in result.report.issues:
            print(f"[{issue.severity}] {issue.code}: {issue.message}")
