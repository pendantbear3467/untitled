from __future__ import annotations

from dataclasses import dataclass, field
from datetime import datetime, timezone
from threading import Lock


@dataclass(frozen=True)
class LogEntry:
    source: str
    level: str
    message: str
    stream: str = "stdout"
    created_at: str = field(default_factory=lambda: datetime.now(timezone.utc).isoformat(timespec="seconds").replace("+00:00", "Z"))


class LogStreamModel:
    def __init__(self, max_entries: int = 2000) -> None:
        self.max_entries = max_entries
        self._entries: list[LogEntry] = []
        self._lock = Lock()

    def append(self, source: str, level: str, message: str, stream: str = "stdout") -> LogEntry:
        entry = LogEntry(source=source, level=level, message=message, stream=stream)
        with self._lock:
            self._entries.append(entry)
            if len(self._entries) > self.max_entries:
                self._entries = self._entries[-self.max_entries :]
        return entry

    def append_lines(self, source: str, text: str, level: str = "info", stream: str = "stdout") -> None:
        for line in text.splitlines():
            if line.strip():
                self.append(source=source, level=level, message=line, stream=stream)

    def clear(self) -> None:
        with self._lock:
            self._entries.clear()

    def entries(self) -> list[LogEntry]:
        with self._lock:
            return list(self._entries)

    def tail(self, count: int = 100) -> list[LogEntry]:
        with self._lock:
            return list(self._entries[-count:])

    def to_text(self, count: int | None = None) -> str:
        items = self.tail(count) if count else self.entries()
        return "\n".join(f"[{entry.level}] {entry.source}: {entry.message}" for entry in items)


