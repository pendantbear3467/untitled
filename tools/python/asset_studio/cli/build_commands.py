from __future__ import annotations

from asset_studio.runtime.build_service import BuildService
from asset_studio.runtime.task_results import StudioTaskResult
from asset_studio.validation.validator import run_validation_pipeline
from asset_studio.workspace.workspace_manager import AssetStudioContext


def validate_command(context: AssetStudioContext, strict: bool = False) -> int:
    report = run_validation_pipeline(context)
    if report.total_issues == 0:
        print("Validation passed: no issues found")
        return 0

    for issue in report.issues:
        print(f"[{issue.severity}] {issue.category} :: {issue.path} :: {issue.message}")

    if strict:
        return 1
    return 0


def build_command(context: AssetStudioContext, target: str, name: str | None = None) -> int:
    service = BuildService(context)
    if target == "assets":
        result = service.build_workspace("assets")
    elif target == "resourcepack":
        result = service.build_workspace("resourcepack")
    elif target == "datapack":
        result = service.build_workspace("datapack")
    elif target == "expansion":
        if not name:
            print("Expansion name is required: assetstudio build expansion <name>")
            return 1
        result = service.compile_expansion(name)
    else:
        print(f"Unsupported build target: {target}")
        return 1

    return _print_task_result(result)


def _print_task_result(result: StudioTaskResult) -> int:
    print(result.message)
    if result.report is not None:
        for artifact in result.report.artifacts:
            if artifact.path is not None:
                print(f"- {artifact.kind}: {artifact.path}")
        for issue in result.report.issues:
            print(f"[{issue.severity}] {issue.code}: {issue.message}")
    return 0 if result.success else 1
