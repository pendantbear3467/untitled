from __future__ import annotations

import json

from asset_studio.validation.issue_types import ValidationIssue
from asset_studio.workspace.workspace_manager import AssetStudioContext


def validate_assets(context: AssetStudioContext) -> list[ValidationIssue]:
    issues: list[ValidationIssue] = []
    for root in [context.workspace_root / "assets", context.workspace_root / "data"]:
        for file in root.rglob("*.json"):
            try:
                json.loads(file.read_text(encoding="utf-8"))
            except json.JSONDecodeError as exc:
                issues.append(ValidationIssue("error", "json", str(file), f"Invalid JSON: {exc}"))
    return issues
