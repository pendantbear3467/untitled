from __future__ import annotations

import json
import shutil
from dataclasses import dataclass, field
from datetime import datetime, timezone
from pathlib import Path
from typing import Any


CURRENT_SCHEMA = {"gui": 2, "model": 2}
CURRENT_DOCUMENT_TYPE = {"gui": "gui-studio", "model": "cube-model-studio"}


@dataclass
class MigrationPreview:
    document_kind: str
    payload: dict[str, Any]
    applied: bool = False
    source_path: Path | None = None
    from_version: int | None = None
    to_version: int | None = None
    warnings: list[str] = field(default_factory=list)
    errors: list[str] = field(default_factory=list)
    actions: list[str] = field(default_factory=list)
    backup_path: Path | None = None
    raw_text: str = ""


class DocumentMigrationService:
    def __init__(self, workspace_root: Path) -> None:
        self.workspace_root = workspace_root
        self.backup_root = workspace_root / ".studio" / "migrations"
        self.backup_root.mkdir(parents=True, exist_ok=True)

    def load_preview(self, document_kind: str, path: Path) -> MigrationPreview:
        preview = MigrationPreview(document_kind=document_kind, payload={}, source_path=path.resolve(strict=False))
        try:
            preview.raw_text = path.read_text(encoding="utf-8")
        except OSError as exc:
            preview.errors.append(f"Could not read {path}: {exc}")
            preview.payload = self._fallback_payload(document_kind, path, error=str(exc))
            preview.applied = True
            preview.actions.append("opened in diagnostic mode")
            preview.to_version = CURRENT_SCHEMA[document_kind]
            return preview
        try:
            payload = json.loads(preview.raw_text)
        except json.JSONDecodeError as exc:
            preview.errors.append(f"Malformed JSON: {exc}")
            preview.payload = self._fallback_payload(document_kind, path, raw_text=preview.raw_text, error=str(exc))
            preview.applied = True
            preview.actions.append("opened in diagnostic mode")
            preview.to_version = CURRENT_SCHEMA[document_kind]
            return preview
        if not isinstance(payload, dict):
            preview.errors.append("Document root must be a JSON object")
            preview.payload = self._fallback_payload(document_kind, path, raw_text=preview.raw_text, error="Root JSON value was not an object")
            preview.applied = True
            preview.actions.append("opened in diagnostic mode")
            preview.to_version = CURRENT_SCHEMA[document_kind]
            return preview
        preview.payload = self._migrate_payload(document_kind, payload, preview)
        return preview

    def backup_before_write(self, path: Path) -> Path:
        timestamp = datetime.now(timezone.utc).strftime("%Y%m%dT%H%M%SZ")
        target = self.backup_root / f"{timestamp}-{path.name}.bak"
        shutil.copy2(path, target)
        return target

    def prepare_save(self, document_kind: str, path: Path, metadata: dict[str, Any] | None = None) -> MigrationPreview | None:
        if not path.exists():
            return None
        preview = self.load_preview(document_kind, path)
        needs_backup = preview.applied or bool((metadata or {}).get("diagnosticMode"))
        if needs_backup:
            preview.backup_path = self.backup_before_write(path)
        return preview

    def _migrate_payload(self, document_kind: str, payload: dict[str, Any], preview: MigrationPreview) -> dict[str, Any]:
        migrated = dict(payload)
        current_schema = CURRENT_SCHEMA[document_kind]
        preview.from_version = int(migrated.get("schemaVersion", 1) or 1)
        preview.to_version = current_schema

        if migrated.get("documentType") != CURRENT_DOCUMENT_TYPE[document_kind]:
            preview.warnings.append(
                f"Normalized documentType from {migrated.get('documentType')} to {CURRENT_DOCUMENT_TYPE[document_kind]}"
            )
            migrated["documentType"] = CURRENT_DOCUMENT_TYPE[document_kind]
            preview.actions.append("normalized documentType")
            preview.applied = True

        if preview.from_version != current_schema:
            migrated["schemaVersion"] = current_schema
            preview.actions.append(f"upgraded schemaVersion {preview.from_version} -> {current_schema}")
            preview.applied = True

        if document_kind == "gui":
            migrated.setdefault("name", preview.source_path.stem.replace(".gui", "") if preview.source_path else "untitled_gui")
            migrated.setdefault("screenType", "generic")
            migrated.setdefault("width", 176)
            migrated.setdefault("height", 166)
            migrated.setdefault("widgets", [])
            migrated.setdefault("rootWidgets", [])
            migrated.setdefault("metadata", {})
        else:
            migrated.setdefault("name", preview.source_path.stem.replace(".model", "") if preview.source_path else "untitled_model")
            migrated.setdefault("modelType", "block")
            migrated.setdefault("textureSize", [64, 64])
            migrated.setdefault("bones", [])
            migrated.setdefault("cubes", [])
            migrated.setdefault("metadata", {})

        metadata = dict(migrated.get("metadata") or {})
        if preview.applied:
            metadata.setdefault("migrationPreview", {})
            metadata["migrationPreview"] = {
                "documentKind": document_kind,
                "fromVersion": preview.from_version,
                "toVersion": preview.to_version,
                "actions": list(preview.actions),
                "warnings": list(preview.warnings),
                "errors": list(preview.errors),
                "sourcePath": str(preview.source_path) if preview.source_path else "",
            }
            migrated["metadata"] = metadata
        return migrated

    def _fallback_payload(self, document_kind: str, path: Path, *, raw_text: str = "", error: str = "") -> dict[str, Any]:
        metadata = {
            "diagnosticMode": True,
            "rawPath": str(path),
            "loadErrors": [error] if error else [],
            "rawText": raw_text,
        }
        if document_kind == "gui":
            return {
                "schemaVersion": CURRENT_SCHEMA[document_kind],
                "documentType": CURRENT_DOCUMENT_TYPE[document_kind],
                "name": path.stem.replace(".gui", ""),
                "namespace": "extremecraft",
                "screenType": "generic",
                "width": 176,
                "height": 166,
                "rootWidgets": [],
                "widgets": [],
                "metadata": metadata,
            }
        return {
            "schemaVersion": CURRENT_SCHEMA[document_kind],
            "documentType": CURRENT_DOCUMENT_TYPE[document_kind],
            "name": path.stem.replace(".model", ""),
            "namespace": "extremecraft",
            "modelType": "block",
            "textureSize": [64, 64],
            "bones": [],
            "cubes": [],
            "metadata": metadata,
        }
