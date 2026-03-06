from __future__ import annotations

import json
from collections import defaultdict
from pathlib import Path

from asset_studio.validation.issue_types import ValidationIssue
from asset_studio.workspace.workspace_manager import AssetStudioContext


def validate_registry_conflicts(context: AssetStudioContext) -> list[ValidationIssue]:
    issues: list[ValidationIssue] = []
    known_ids = _load_known_ids(context.workspace_root / "registry_snapshot.json")
    seen_local: dict[str, list[str]] = defaultdict(list)

    addons_root = context.workspace_root / "addons"
    if not addons_root.exists():
        return issues

    for definition_file in addons_root.rglob("*.json"):
        if definition_file.name == "addon.json":
            continue

        try:
            payload = json.loads(definition_file.read_text(encoding="utf-8"))
        except json.JSONDecodeError:
            continue

        definition_type = str(payload.get("type", definition_file.stem))
        definition_id = str(payload.get("id", definition_file.stem))
        key = f"{definition_type}:{definition_id}"

        if key in known_ids:
            issues.append(
                ValidationIssue(
                    "error",
                    "registry",
                    str(definition_file),
                    f"Definition conflicts with registry snapshot: {key}",
                )
            )

        seen_local[key].append(str(definition_file))

    for key, files in seen_local.items():
        if len(files) <= 1:
            continue
        issues.append(
            ValidationIssue(
                "error",
                "registry",
                files[0],
                f"Duplicate definition key found across addons: {key}",
            )
        )

    return issues


def _load_known_ids(snapshot_path: Path) -> set[str]:
    if not snapshot_path.exists():
        return set()

    payload = json.loads(snapshot_path.read_text(encoding="utf-8"))
    existing: set[str] = set()
    for key, definition_type in [
        ("item_ids", "item"),
        ("block_ids", "block"),
        ("machine_ids", "machine"),
        ("material_ids", "material"),
        ("ore_ids", "worldgen"),
    ]:
        for entry_id in payload.get(key, []):
            existing.add(f"{definition_type}:{entry_id}")

    return existing
