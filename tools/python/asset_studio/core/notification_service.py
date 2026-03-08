from __future__ import annotations

from dataclasses import dataclass, field
from datetime import datetime, timezone
from threading import Lock
from typing import Any


@dataclass(frozen=True)
class Notification:
    severity: str
    source: str
    message: str
    created_at: str = field(default_factory=lambda: datetime.now(timezone.utc).isoformat(timespec="seconds").replace("+00:00", "Z"))
    details: dict[str, Any] = field(default_factory=dict)


class NotificationService:
    def __init__(self, max_items: int = 500) -> None:
        self.max_items = max_items
        self._notifications: list[Notification] = []
        self._lock = Lock()

    def publish(self, severity: str, source: str, message: str, **details: Any) -> Notification:
        notification = Notification(severity=severity, source=source, message=message, details=details)
        with self._lock:
            self._notifications.append(notification)
            if len(self._notifications) > self.max_items:
                self._notifications = self._notifications[-self.max_items :]
        return notification

    def recent(self, limit: int = 50) -> list[Notification]:
        with self._lock:
            return list(self._notifications[-limit:])

    def clear(self) -> None:
        with self._lock:
            self._notifications.clear()


