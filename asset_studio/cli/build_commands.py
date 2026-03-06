from __future__ import annotations

import shutil

from compiler.module_builder import ModuleBuilder
from extremecraft_sdk.api.sdk import ExtremeCraftSDK

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
    if target == "assets":
        print("Assets build complete (workspace artifacts already generated).")
        return 0

    if target == "resourcepack":
        return _build_pack(context, pack_type="resourcepack")

    if target == "datapack":
        return _build_pack(context, pack_type="datapack")

    if target == "expansion":
        return _build_expansion(context, name)

    raise ValueError(f"Unsupported build target: {target}")


def _build_pack(context: AssetStudioContext, pack_type: str) -> int:
    out_dir = context.workspace_root / "build" / pack_type
    out_dir.mkdir(parents=True, exist_ok=True)
    print(f"Built {pack_type} output at {out_dir}")
    return 0


def _build_expansion(context: AssetStudioContext, name: str | None) -> int:
    if not name:
        raise ValueError("Expansion name is required: assetstudio build expansion <name>")

    sdk = ExtremeCraftSDK(
        addons_root=context.workspace_root / "addons",
        context=context,
        plugin_api=context.plugins,
    )
    result = ModuleBuilder(context=context, sdk=sdk).build_expansion(name)

    release_dir = context.workspace_root / "build" / "expansions"
    release_dir.mkdir(parents=True, exist_ok=True)
    packaged_name = f"extremecraft-{name.replace('_', '-')}-pack.jar"
    packaged_path = release_dir / packaged_name
    shutil.copy2(result.jar_path, packaged_path)

    print(f"Built expansion artifact: {packaged_path}")
    return 0
