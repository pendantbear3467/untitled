from __future__ import annotations

from dataclasses import dataclass, field
from pathlib import Path
from typing import Any

from asset_studio.code.diagnostics import ProblemsModel
from asset_studio.code.document import TextDocument
from asset_studio.code.language_modes import detect_syntax_mode
from asset_studio.code.search_service import SearchResults, SearchService
from asset_studio.core.recovery_service import RecoveryService


@dataclass
class EditorSessionState:
    open_documents: list[str] = field(default_factory=list)
    recent_files: list[str] = field(default_factory=list)
    split_layouts: dict[str, list[str]] = field(default_factory=dict)
    open_files: list[str] = field(default_factory=list)


class EditorService:
    def __init__(self, *, recovery_service: RecoveryService | None = None) -> None:
        self.recovery_service = recovery_service
        self.documents: dict[str, TextDocument] = {}
        self.problems = ProblemsModel()
        self.search_service = SearchService()
        self.recent_files: list[Path] = []
        self.split_layouts: dict[str, list[str]] = {}

    def new_document(self, path: Path | None = None, content: str = "") -> TextDocument:
        document = TextDocument.create(path=path, content=content)
        self.documents[document.document_id] = document
        if path is not None:
            self._remember_recent(path)
        return document

    def open_document(self, path: Path) -> TextDocument:
        for document in self.documents.values():
            if document.path == path:
                return document
        try:
            document = TextDocument.load(path)
        except Exception:  # noqa: BLE001
            document = self._recover_document(path)
            if document is None:
                raise
        document.syntax_mode = detect_syntax_mode(path)
        self.documents[document.document_id] = document
        self._remember_recent(path)
        return document

    def save_document(self, document_id: str, path: Path | None = None) -> Path:
        document = self.documents[document_id]
        saved_path = document.save(path)
        self._remember_recent(saved_path)
        if self.recovery_service is not None:
            self.recovery_service.record_snapshot(
                document_id=document.document_id,
                document_type="text-document",
                payload={
                    "path": str(saved_path),
                    "content": document.content,
                    "encoding": document.encoding,
                    "syntaxMode": document.syntax_mode,
                },
                source_path=saved_path,
                metadata={"saved": True, "version": document.version},
            )
        return saved_path

    def save_all(self) -> list[Path]:
        paths: list[Path] = []
        for document_id, document in self.documents.items():
            if document.dirty and document.path is not None:
                paths.append(self.save_document(document_id))
        return paths

    def set_document_text(self, document_id: str, text: str) -> None:
        document = self.documents[document_id]
        document.set_content(text)
        if self.recovery_service is not None:
            self.recovery_service.record_snapshot(
                document_id=document.document_id,
                document_type="text-document",
                payload={
                    "path": str(document.path) if document.path else None,
                    "content": document.content,
                    "encoding": document.encoding,
                    "syntaxMode": document.syntax_mode,
                },
                source_path=document.path,
                metadata={"dirty": document.dirty, "version": document.version},
            )

    def close_document(self, document_id: str) -> TextDocument | None:
        self.problems.clear_document(document_id)
        return self.documents.pop(document_id, None)

    def search(self, query: str, document_ids: list[str] | None = None, *, case_sensitive: bool = False) -> SearchResults:
        documents = list(self.documents.values())
        if document_ids is not None:
            documents = [self.documents[document_id] for document_id in document_ids if document_id in self.documents]
        return self.search_service.search_documents(query, documents, case_sensitive=case_sensitive)

    def replace_all(self, document_id: str, needle: str, replacement: str, *, case_sensitive: bool = False) -> int:
        document = self.documents[document_id]
        return self.search_service.replace_all(document, needle, replacement, case_sensitive=case_sensitive)

    def build_session_state(self) -> EditorSessionState:
        open_files = [str(document.path) for document in self.documents.values() if document.path is not None]
        return EditorSessionState(
            open_documents=list(self.documents),
            recent_files=[str(path) for path in self.recent_files],
            split_layouts={key: list(value) for key, value in self.split_layouts.items()},
            open_files=open_files,
        )

    def restore_session_state(self, payload: dict[str, Any]) -> None:
        self.recent_files = [Path(path) for path in payload.get("recent_files", []) if path]
        self.split_layouts = {str(key): list(value) for key, value in (payload.get("split_layouts") or {}).items()}
        open_files = [Path(path) for path in payload.get("open_files", []) if path]
        for path in open_files:
            try:
                self.open_document(path)
            except Exception:  # noqa: BLE001
                continue

    def set_split_layout(self, split_id: str, document_ids: list[str]) -> None:
        self.split_layouts[split_id] = list(document_ids)

    def _remember_recent(self, path: Path) -> None:
        self.recent_files = [entry for entry in self.recent_files if entry != path]
        self.recent_files.insert(0, path)
        self.recent_files = self.recent_files[:25]

    def _recover_document(self, path: Path) -> TextDocument | None:
        if self.recovery_service is None:
            return None
        snapshot = self.recovery_service.latest_snapshot(document_type="text-document", source_path=path)
        if snapshot is None:
            return None
        payload = self.recovery_service.restore_snapshot(snapshot.snapshot_id)
        if not isinstance(payload, dict):
            return None
        content = str(payload.get("content", ""))
        encoding = str(payload.get("encoding", "utf-8"))
        document = TextDocument.create(path=path, content=content, encoding=encoding)
        document.syntax_mode = str(payload.get("syntaxMode", detect_syntax_mode(path)))
        document.dirty = True
        return document
