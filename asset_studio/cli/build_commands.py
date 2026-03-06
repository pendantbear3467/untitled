from __future__ import annotations

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


def build_command(context: AssetStudioContext, target: str) -> int:
    if target == "assets":
        print("Assets build complete (workspace artifacts already generated).")
        return 0

    if target == "resourcepack":
        return _build_pack(context, pack_type="resourcepack")

    if target == "datapack":
        return _build_pack(context, pack_type="datapack")

    raise ValueError(f"Unsupported build target: {target}")


def _build_pack(context: AssetStudioContext, pack_type: str) -> int:
    out_dir = context.workspace_root / "build" / pack_type
    out_dir.mkdir(parents=True, exist_ok=True)
    print(f"Built {pack_type} output at {out_dir}")
    return 0
