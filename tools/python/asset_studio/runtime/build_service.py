from __future__ import annotations

from pathlib import Path
from typing import Any

from asset_studio.cli.pack_commands import export_pack_command
from asset_studio.compiler.module_builder import ModuleBuilder
from asset_studio.modpack.modpack_builder import ModpackBuilder
from asset_studio.release.release_manager import ReleaseManager
from asset_studio.runtime.task_results import (
    StudioTaskResult,
    TaskArtifact,
    TaskIssue,
    TaskReport,
    ValidationTaskResult,
    utc_now,
)
from asset_studio.sdk_support import OptionalDependencyUnavailableError, load_sdk_class
from asset_studio.validation.validator import run_validation_pipeline
from asset_studio.workspace.project_manager import ProjectManager
from asset_studio.workspace.workspace_manager import AssetStudioContext


class BuildService:
    def __init__(self, context: AssetStudioContext) -> None:
        self.context = context
        self.project_manager = ProjectManager(context)

    def validate_workspace(self, *, task_context=None) -> ValidationTaskResult:
        started_at = utc_now()
        if task_context is not None:
            task_context.report_progress("validate", "Running validation pipeline", 0.1)
            task_context.raise_if_cancelled()
        try:
            report = run_validation_pipeline(self.context)
            issues = [
                TaskIssue(severity=issue.severity, code=issue.category, message=issue.message, path=Path(issue.path))
                for issue in report.issues
            ]
            if task_context is not None:
                task_context.report_progress("validate", "Validation pipeline complete", 1.0)
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
                report=TaskReport(
                    operation="validate_workspace",
                    category="validation",
                    summary=f"Validation completed with {report.total_issues} issues",
                    issues=issues,
                ),
                warnings=[issue.message for issue in report.issues if issue.severity == "warning"],
                errors=[issue.message for issue in report.issues if issue.severity == "error"],
            )
        except Exception as exc:  # noqa: BLE001
            return self._failure_result("Validate Workspace", started_at, str(exc), operation="validate_workspace", category="validation", code="validation_failed")

    def build_workspace(self, target: str, *, task_context=None) -> StudioTaskResult:
        started_at = utc_now()
        if task_context is not None:
            task_context.report_progress("build", f"Building workspace target '{target}'", 0.1)
            task_context.raise_if_cancelled()
        try:
            output_path = self.project_manager.build_project(target)
            if task_context is not None:
                task_context.report_progress("build", f"Built workspace target '{target}'", 1.0)
            return StudioTaskResult(
                task_id=f"build-{target}",
                name=f"Build {target}",
                success=True,
                started_at=started_at,
                finished_at=utc_now(),
                message=f"Project build complete: {output_path}",
                data={"target": target, "output": str(output_path)},
                report=TaskReport(
                    operation=f"build:{target}",
                    category="build",
                    summary=f"Project build complete: {output_path}",
                    artifacts=[TaskArtifact(kind="build_output", path=output_path, label=target, status="created")],
                ),
            )
        except Exception as exc:  # noqa: BLE001
            return self._failure_result(f"Build {target}", started_at, str(exc), operation=f"build:{target}", category="build", code="build_failed")

    def export_pack(self, target: str, *, task_context=None) -> StudioTaskResult:
        started_at = utc_now()
        if task_context is not None:
            task_context.report_progress("export", f"Exporting {target}", 0.1)
            task_context.raise_if_cancelled()
        try:
            export_pack_command(self.context, target)
            destination = self._export_destination(target)
            if task_context is not None:
                task_context.report_progress("export", f"Exported {target}", 1.0)
            return StudioTaskResult(
                task_id=f"export-{target}",
                name=f"Export {target}",
                success=True,
                started_at=started_at,
                finished_at=utc_now(),
                message=f"Exported {target}: {destination}",
                data={"target": target, "output": str(destination)},
                report=TaskReport(
                    operation=f"export:{target}",
                    category="export",
                    summary=f"Exported {target}: {destination}",
                    artifacts=[TaskArtifact(kind="export_output", path=destination, label=target, status="created")],
                ),
            )
        except Exception as exc:  # noqa: BLE001
            return self._failure_result(f"Export {target}", started_at, str(exc), operation=f"export:{target}", category="export", code="export_failed")

    def compile_expansion(self, name: str, *, task_context=None) -> StudioTaskResult:
        started_at = utc_now()
        if task_context is not None:
            task_context.report_progress("compile", f"Preparing expansion '{name}'", 0.05)
            task_context.raise_if_cancelled()
        try:
            sdk_class = load_sdk_class("compile expansion")
            sdk = sdk_class(
                addons_root=self.context.workspace_root / "addons",
                context=self.context,
                plugin_api=self.context.plugins,
            )
            if task_context is not None:
                task_context.report_progress("compile", f"Compiling expansion '{name}'", 0.4)
                task_context.raise_if_cancelled()
            result = ModuleBuilder(context=self.context, sdk=sdk).build_expansion(name)
            warnings = [f"[{conflict.kind}] {conflict.identifier}: {conflict.message}" for conflict in result.conflicts]
            message = f"Compiled expansion '{name}' -> {result.jar_path}"
            if warnings:
                message += f" ({len(warnings)} conflict warning(s))"
            if task_context is not None:
                task_context.report_progress("compile", f"Compiled expansion '{name}'", 1.0)
            return StudioTaskResult(
                task_id=f"compile-expansion-{name}",
                name=f"Compile Expansion {name}",
                success=True,
                started_at=started_at,
                finished_at=utc_now(),
                message=message,
                data={
                    "addon": name,
                    "jar": str(result.jar_path),
                    "outputRoot": str(result.output_root),
                    "documentation": [str(path) for path in result.documentation_paths],
                },
                warnings=warnings,
                report=TaskReport(
                    operation=f"compile:{name}",
                    category="compile",
                    summary=message,
                    issues=[
                        TaskIssue(
                            severity="warning",
                            code=conflict.kind,
                            message=conflict.message,
                        )
                        for conflict in result.conflicts
                    ],
                    artifacts=[
                        TaskArtifact(kind="module_output", path=result.output_root, label=name, status="created"),
                        TaskArtifact(kind="jar", path=result.jar_path, label=name, status="created"),
                        TaskArtifact(kind="java_source", path=result.java_source, label="generated registries", status="created"),
                    ] + [TaskArtifact(kind="documentation", path=path, label=path.name, status="created") for path in result.documentation_paths],
                ),
            )
        except OptionalDependencyUnavailableError as exc:
            return self._failure_result(
                f"Compile Expansion {name}",
                started_at,
                str(exc),
                operation=f"compile:{name}",
                category="compile",
                code="sdk_unavailable",
            )
        except Exception as exc:  # noqa: BLE001
            return self._failure_result(f"Compile Expansion {name}", started_at, str(exc), operation=f"compile:{name}", category="compile", code="compile_failed")

    def build_release(self, release_name: str | None = None, *, task_context=None) -> StudioTaskResult:
        started_at = utc_now()
        if task_context is not None:
            task_context.report_progress("release", "Building release bundle", 0.1)
            task_context.raise_if_cancelled()
        try:
            result = ReleaseManager(self.context).build(release_name)
            if task_context is not None:
                task_context.report_progress("release", "Release bundle built", 1.0)
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
                report=TaskReport(
                    operation="release:build",
                    category="release",
                    summary=f"Release built: {result.artifact}",
                    artifacts=[
                        TaskArtifact(kind="release_artifact", path=result.artifact, label=result.release_name, status="created"),
                        TaskArtifact(kind="changelog", path=result.changelog, label="changelog", status="created"),
                    ],
                ),
            )
        except Exception as exc:  # noqa: BLE001
            return self._failure_result("Build Release", started_at, str(exc), operation="release:build", category="release", code="release_failed")

    def build_modpack(self, modpack_name: str, *, task_context=None) -> StudioTaskResult:
        started_at = utc_now()
        if task_context is not None:
            task_context.report_progress("modpack", f"Building modpack '{modpack_name}'", 0.1)
            task_context.raise_if_cancelled()
        try:
            result = ModpackBuilder(self.context).build(modpack_name)
            if task_context is not None:
                task_context.report_progress("modpack", f"Built modpack '{modpack_name}'", 1.0)
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
                report=TaskReport(
                    operation=f"modpack:{modpack_name}",
                    category="modpack",
                    summary=f"Modpack archive created: {result.archive_path}",
                    artifacts=[
                        TaskArtifact(kind="modpack_manifest", path=result.manifest_path, label=modpack_name, status="created"),
                        TaskArtifact(kind="modpack_archive", path=result.archive_path, label=modpack_name, status="created"),
                    ],
                ),
            )
        except Exception as exc:  # noqa: BLE001
            return self._failure_result(f"Build Modpack {modpack_name}", started_at, str(exc), operation=f"modpack:{modpack_name}", category="modpack", code="modpack_failed")

    def _export_destination(self, target: str) -> Path:
        exports = self.context.workspace_root / "exports"
        if target in {"resourcepack", "forge", "fabric"}:
            return exports / f"{target}_assets"
        if target == "datapack":
            return exports / "datapack"
        return exports / target

    def _failure_result(
        self,
        name: str,
        started_at: str,
        message: str,
        *,
        operation: str,
        category: str,
        code: str,
        data: Any = None,
    ) -> StudioTaskResult:
        return StudioTaskResult(
            task_id=operation,
            name=name,
            success=False,
            started_at=started_at,
            finished_at=utc_now(),
            message=message,
            data=data,
            errors=[message],
            report=TaskReport(
                operation=operation,
                category=category,
                summary=message,
                issues=[TaskIssue(severity="error", code=code, message=message)],
            ),
        )
