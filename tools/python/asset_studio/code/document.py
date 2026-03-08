from __future__ import annotations

import uuid
from dataclasses import dataclass, field
from datetime import datetime, timezone
from pathlib import Path

from asset_studio.code.language_modes import detect_syntax_mode


@dataclass(frozen=True)
class BufferRevision:
    revision: int
    text: str
    created_at: str = field(default_factory=lambda: datetime.now(timezone.utc).isoformat(timespec="seconds").replace("+00:00", "Z"))


@dataclass
class TextDocument:
    document_id: str
    path: Path | None = None
    content: str = ""
    encoding: str = "utf-8"
    syntax_mode: str = "text"
    dirty: bool = False
    version: int = 0
    history: list[BufferRevision] = field(default_factory=list)

    @classmethod
    def create(cls, path: Path | None = None, content: str = "", encoding: str = "utf-8") -> "TextDocument":
        return cls(
            document_id=uuid.uuid4().hex,
            path=path,
            content=content,
            encoding=encoding,
            syntax_mode=detect_syntax_mode(path),
            dirty=bool(content and path is None),
        )

    @classmethod
    def load(cls, path: Path, encoding: str = "utf-8") -> "TextDocument":
        content = path.read_text(encoding=encoding)
        return cls.create(path=path, content=content, encoding=encoding)

    @property
    def name(self) -> str:
        if self.path is not None:
            return self.path.name
        return f"untitled-{self.document_id[:8]}"

    def set_content(self, content: str) -> None:
        self.history.append(BufferRevision(revision=self.version, text=self.content))
        self.content = content
        self.version += 1
        self.dirty = True

    def save(self, path: Path | None = None) -> Path:
        target = path or self.path
        if target is None:
            raise ValueError("Cannot save a document without a path")
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(self.content, encoding=self.encoding)
        self.path = target
        self.syntax_mode = detect_syntax_mode(target)
        self.dirty = False
        return target


