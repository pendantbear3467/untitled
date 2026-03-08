from __future__ import annotations

from dataclasses import dataclass, field
from datetime import UTC, datetime

from asset_studio.skilltree.models import ProgressionDocument


@dataclass(slots=True)
class HistorySnapshot:
    label: str
    document: ProgressionDocument
    created_at: str = field(default_factory=lambda: datetime.now(UTC).isoformat())


class DocumentHistory:
    def __init__(self, max_entries: int = 100) -> None:
        self.max_entries = max_entries
        self._undo_stack: list[HistorySnapshot] = []
        self._redo_stack: list[HistorySnapshot] = []

    def clear(self) -> None:
        self._undo_stack.clear()
        self._redo_stack.clear()

    def push(self, label: str, document: ProgressionDocument) -> None:
        self._undo_stack.append(HistorySnapshot(label=label, document=document.clone()))
        if len(self._undo_stack) > self.max_entries:
            self._undo_stack.pop(0)
        self._redo_stack.clear()

    def undo(self, current: ProgressionDocument) -> ProgressionDocument | None:
        if not self._undo_stack:
            return None
        snapshot = self._undo_stack.pop()
        self._redo_stack.append(HistorySnapshot(label=snapshot.label, document=current.clone()))
        return snapshot.document.clone()

    def redo(self, current: ProgressionDocument) -> ProgressionDocument | None:
        if not self._redo_stack:
            return None
        snapshot = self._redo_stack.pop()
        self._undo_stack.append(HistorySnapshot(label=snapshot.label, document=current.clone()))
        return snapshot.document.clone()

    def describe(self) -> dict[str, list[str]]:
        return {
            "undo": [snapshot.label for snapshot in self._undo_stack],
            "redo": [snapshot.label for snapshot in self._redo_stack],
        }
