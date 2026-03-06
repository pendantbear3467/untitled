from __future__ import annotations

import argparse

from asset_studio.validation.validator import ValidationReport, run_validation_pipeline
from asset_studio.workspace.workspace_manager import AssetStudioContext


def register_validate_commands(parser: argparse.ArgumentParser) -> None:
    parser.add_argument("--strict", action="store_true", help="Return non-zero exit code on any issue")


def run_validate_command(args: argparse.Namespace, context: AssetStudioContext) -> int:
    report = run_validation_pipeline(context)
    _print_report(report)
    if args.strict and report.total_issues > 0:
        return 1
    return 0


def _print_report(report: ValidationReport) -> None:
    print("Validation Report")
    print(f"- errors: {report.errors}")
    print(f"- warnings: {report.warnings}")
    print(f"- total: {report.total_issues}")
    for issue in report.issues:
        print(f"[{issue.severity}] {issue.category} :: {issue.path} :: {issue.message}")
