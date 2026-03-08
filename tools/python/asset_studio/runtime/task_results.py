from __future__ import annotations

from dataclasses import dataclass, field
from datetime import datetime, timezone
from pathlib import Path
from typing import Any


def utc_now() -> str:
    return datetime.now(timezone.utc).isoformat(timespec="seconds").replace("+00:00", "Z")


@dataclass
class TaskProgressEvent:
    task_id: str
    step: str
    message: str
    percent: float | None = None
    created_at: str = field(default_factory=utc_now)


@dataclass
class StudioTaskResult:
    task_id: str
    name: str
    success: bool
    started_at: str
    finished_at: str
    message: str = ""
    data: Any = None
    errors: list[str] = field(default_factory=list)
    warnings: list[str] = field(default_factory=list)


@dataclass
class ProcessTaskResult(StudioTaskResult):
    command: list[str] = field(default_factory=list)
    exit_code: int | None = None
    stdout: str = ""
    stderr: str = ""
    log_path: Path | None = None
    cancelled: bool = False


@dataclass
class ValidationTaskResult(StudioTaskResult):
    error_count: int = 0
    warning_count: int = 0
    issue_count: int = 0


