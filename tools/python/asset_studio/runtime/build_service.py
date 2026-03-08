from __future__ import annotations

from pathlib import Path

from asset_studio.cli.pack_commands import export_pack_command
from asset_studio.modpack.modpack_builder import ModpackBuilder
from asset_studio.release.release_manager import ReleaseManager
from asset_studio.runtime.task_results import StudioTaskResult, ValidationTaskResult, utc_now
from asset_studio.validation.validator import run_validation_pipeline
from asset_studio.workspace.project_manager import ProjectManager
from asset_studio.workspace.workspace_manager import AssetStudioContext


class BuildService:
    def __init__(self, context: AssetStudioContext) -> None:
        self.context = context
        self.project_manager = ProjectManager(context)

    def validate_workspace(self) -> ValidationTaskResult:
        started_at = utc_now()
        report = run_validation_pipeline(self.context)
        issues = report.issues
        return ValidationTaskResult(
            task_id="validate-workspace",
            name="Validate Workspace",
            success=report.errors == 0,
            started_at=started_at,
            finished_at=utc_now(),
            message=f"Validation completed with {report.total_issues} issues",
            data=report,
            error_count=report.errors,
            warning_count=report.warnings,
            issue_count=report.total_issues,
        )

    def build_workspace(self, target: str) -> StudioTaskResult:
        started_at = utc_now()
        output_path = self.project_manager.build_project(target)
        return StudioTaskResult(
            task_id=f"build-{target}",
            name=f"Build {target}",
            success=True,
            started_at=started_at,
            finished_at=utc_now(),
            message=f"Project build complete: {output_path}",
            data={"target": target, "output": str(output_path)},
        )

    def export_pack(self, target: str) -> StudioTaskResult:
        started_at = utc_now()
        export_pack_command(self.context, target)
        destination = self._export_destination(target)
        return StudioTaskResult(
            task_id=f"export-{target}",
            name=f"Export {target}",
            success=True,
            started_at=started_at,
            finished_at=utc_now(),
            message=f"Exported {target}: {destination}",
            data={"target": target, "output": str(destination)},
        )

    def compile_expansion(self, name: str) -> StudioTaskResult:
        started_at = utc_now()
        from compiler.module_builder import ModuleBuilder
        from extremecraft_sdk.api.sdk import ExtremeCraftSDK

        sdk = ExtremeCraftSDK(
            addons_root=self.context.workspace_root / "addons",
            context=self.context,
            plugin_api=self.context.plugins,
        )
        result = ModuleBuilder(context=self.context, sdk=sdk).build_expansion(name)
        return StudioTaskResult(
            task_id=f"compile-expansion-{name}",
            name=f"Compile Expansion {name}",
            success=True,
            started_at=started_at,
            finished_at=utc_now(),
            message=f"Compiled expansion '{name}' -> {result.jar_path}",
            data={
                "addon": name,
                "jar": str(result.jar_path),
                "outputRoot": str(result.output_root),
                "documentation": [str(path) for path in result.documentation_paths],
            },
        )

    def build_release(self, release_name: str | None = None) -> StudioTaskResult:
        started_at = utc_now()
        result = ReleaseManager(self.context).build(release_name)
        return StudioTaskResult(
            task_id="build-release",
            name="Build Release",
            success=True,
            started_at=started_at,
            finished_at=utc_now(),
            message=f"Release built: {result.artifact}",
            data={
                "release": result.release_name,
                "artifact": str(result.artifact),
                "changelog": str(result.changelog),
            },
        )

    def build_modpack(self, modpack_name: str) -> StudioTaskResult:
        started_at = utc_now()
        result = ModpackBuilder(self.context).build(modpack_name)
        return StudioTaskResult(
            task_id=f"build-modpack-{modpack_name}",
            name=f"Build Modpack {modpack_name}",
            success=True,
            started_at=started_at,
            finished_at=utc_now(),
            message=f"Modpack archive created: {result.archive_path}",
            data={
                "modpack": modpack_name,
                "manifest": str(result.manifest_path),
                "archive": str(result.archive_path),
            },
        )

    def _export_destination(self, target: str) -> Path:
        exports = self.context.workspace_root / "exports"
        if target in {"resourcepack", "forge", "fabric"}:
            return exports / f"{target}_assets"
        if target == "datapack":
            return exports / "datapack"
        return exports / target
