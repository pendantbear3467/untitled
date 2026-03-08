from __future__ import annotations

import json
import shutil
import uuid
from dataclasses import dataclass
from datetime import datetime, timezone
from pathlib import Path
from typing import Any


@dataclass(frozen=True)
class RecoverySnapshot:
    snapshot_id: str
    document_id: str
    document_type: str
    created_at: str
    snapshot_path: Path
    source_path: Path | None = None
    metadata: dict[str, Any] | None = None


class RecoveryService:
    def __init__(self, workspace_root: Path) -> None:
        self.workspace_root = workspace_root
        self.studio_root = workspace_root / ".studio"
        self.autosave_root = self.studio_root / "autosave"
        self.recovery_root = self.studio_root / "recovery"
        self.crash_root = self.recovery_root / "crashes"
        self.session_root = self.recovery_root / "sessions"
        for directory in [self.studio_root, self.autosave_root, self.recovery_root, self.crash_root, self.session_root]:
            directory.mkdir(parents=True, exist_ok=True)
        self._session_id: str | None = None

    @property
    def session_id(self) -> str | None:
        return self._session_id

    def start_session(self, app_name: str = "extremecraft-studio") -> str:
        self._session_id = uuid.uuid4().hex
        payload = {
            "sessionId": self._session_id,
            "app": app_name,
            "workspace": str(self.workspace_root),
            "startedAt": datetime.now(timezone.utc).isoformat(timespec="seconds").replace("+00:00", "Z"),
            "openedDocuments": [],
            "lastCommand": None,
        }
        self._session_file().write_text(json.dumps(payload, indent=2) + "\n", encoding="utf-8")
        return self._session_id

    def update_session(self, **values: Any) -> None:
        path = self._session_file()
        if not path.exists():
            self.start_session()
        payload = json.loads(path.read_text(encoding="utf-8"))
        payload.update(values)
        path.write_text(json.dumps(payload, indent=2) + "\n", encoding="utf-8")

    def list_sessions(self) -> list[Path]:
        return sorted(self.session_root.glob("*.json"), key=lambda item: item.stat().st_mtime, reverse=True)

    def latest_session_payload(self, *, exclude_session_id: str | None = None) -> dict[str, Any] | None:
        for path in self.list_sessions():
            try:
                payload = json.loads(path.read_text(encoding="utf-8"))
            except json.JSONDecodeError:
                continue
            if str(payload.get("workspace", "")) != str(self.workspace_root):
                continue
            if exclude_session_id and str(payload.get("sessionId", "")) == exclude_session_id:
                continue
            return payload
        return None

    def record_snapshot(
        self,
        document_id: str,
        document_type: str,
        payload: Any,
        *,
        source_path: Path | None = None,
        metadata: dict[str, Any] | None = None,
    ) -> RecoverySnapshot:
        snapshot_id = uuid.uuid4().hex
        created_at = datetime.now(timezone.utc).isoformat(timespec="seconds").replace("+00:00", "Z")
        snapshot_path = self.autosave_root / f"{document_type}-{document_id}-{snapshot_id}.json"
        wrapper = {
            "snapshotId": snapshot_id,
            "documentId": document_id,
            "documentType": document_type,
            "createdAt": created_at,
            "sourcePath": str(source_path) if source_path else None,
            "metadata": metadata or {},
            "payload": payload,
        }
        snapshot_path.write_text(json.dumps(wrapper, indent=2) + "\n", encoding="utf-8")
        return RecoverySnapshot(
            snapshot_id=snapshot_id,
            document_id=document_id,
            document_type=document_type,
            created_at=created_at,
            snapshot_path=snapshot_path,
            source_path=source_path,
            metadata=metadata or {},
        )

    def list_snapshots(self, document_id: str | None = None, document_type: str | None = None) -> list[RecoverySnapshot]:
        snapshots: list[RecoverySnapshot] = []
        for path in sorted(self.autosave_root.glob("*.json")):
            try:
                payload = json.loads(path.read_text(encoding="utf-8"))
            except json.JSONDecodeError:
                continue
            if document_id and payload.get("documentId") != document_id:
                continue
            if document_type and payload.get("documentType") != document_type:
                continue
            source_path = payload.get("sourcePath")
            snapshots.append(
                RecoverySnapshot(
                    snapshot_id=str(payload.get("snapshotId", path.stem)),
                    document_id=str(payload.get("documentId", "unknown")),
                    document_type=str(payload.get("documentType", "unknown")),
                    created_at=str(payload.get("createdAt", "")),
                    snapshot_path=path,
                    source_path=Path(source_path) if source_path else None,
                    metadata=payload.get("metadata") or {},
                )
            )
        return snapshots

    def latest_snapshot(
        self,
        *,
        document_type: str | None = None,
        document_id: str | None = None,
        source_path: Path | None = None,
    ) -> RecoverySnapshot | None:
        normalized_source = source_path.resolve(strict=False) if source_path is not None else None
        candidates = self.list_snapshots(document_id=document_id, document_type=document_type)
        filtered: list[RecoverySnapshot] = []
        for snapshot in candidates:
            if normalized_source is not None:
                if snapshot.source_path is None or snapshot.source_path.resolve(strict=False) != normalized_source:
                    continue
            filtered.append(snapshot)
        if not filtered:
            return None
        return max(filtered, key=lambda item: item.created_at)

    def restore_snapshot(self, snapshot_id: str) -> Any:
        for snapshot in self.list_snapshots():
            if snapshot.snapshot_id == snapshot_id:
                payload = json.loads(snapshot.snapshot_path.read_text(encoding="utf-8"))
                return payload.get("payload")
        raise FileNotFoundError(f"Recovery snapshot not found: {snapshot_id}")

    def record_crash(self, source: str, message: str, traceback_text: str, context: dict[str, Any] | None = None) -> Path:
        crash_id = uuid.uuid4().hex
        payload = {
            "crashId": crash_id,
            "sessionId": self._session_id,
            "source": source,
            "message": message,
            "traceback": traceback_text,
            "context": context or {},
            "createdAt": datetime.now(timezone.utc).isoformat(timespec="seconds").replace("+00:00", "Z"),
        }
        path = self.crash_root / f"{crash_id}.json"
        path.write_text(json.dumps(payload, indent=2) + "\n", encoding="utf-8")
        return path

    def safe_load_json(self, path: Path) -> tuple[dict[str, Any] | None, list[str]]:
        if not path.exists():
            return None, [f"File not found: {path}"]
        try:
            return json.loads(path.read_text(encoding="utf-8")), []
        except json.JSONDecodeError as exc:
            backup = self.recovery_root / f"corrupt-{path.name}-{uuid.uuid4().hex}.json"
            shutil.copy2(path, backup)
            return None, [f"Corrupted JSON recovered to {backup}: {exc}"]

    def _session_file(self) -> Path:
        session_id = self._session_id or "pending"
        return self.session_root / f"{session_id}.json"
