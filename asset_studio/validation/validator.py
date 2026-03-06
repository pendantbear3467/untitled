from __future__ import annotations

from dataclasses import dataclass

from asset_studio.validation.asset_validator import validate_assets
from asset_studio.validation.datapack_validator import validate_datapack
from asset_studio.validation.issue_types import ValidationIssue
from asset_studio.validation.model_validator import validate_models
from asset_studio.validation.texture_validator import validate_textures
from asset_studio.workspace.workspace_manager import AssetStudioContext


@dataclass
class ValidationReport:
    issues: list[ValidationIssue]

    @property
    def errors(self) -> int:
        return sum(1 for issue in self.issues if issue.severity == "error")

    @property
    def warnings(self) -> int:
        return sum(1 for issue in self.issues if issue.severity == "warning")

    @property
    def total_issues(self) -> int:
        return len(self.issues)


def run_validation_pipeline(context: AssetStudioContext) -> ValidationReport:
    issues: list[ValidationIssue] = []
    issues.extend(validate_assets(context))
    issues.extend(validate_models(context))
    issues.extend(validate_textures(context))
    issues.extend(validate_datapack(context))

    for validator in context.plugins.validators.values():
        if callable(validator):
            plugin_issues = validator(context)
            if isinstance(plugin_issues, list):
                issues.extend(issue for issue in plugin_issues if isinstance(issue, ValidationIssue))

    return ValidationReport(issues=issues)
