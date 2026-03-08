from __future__ import annotations

import traceback
from dataclasses import dataclass
from typing import Any, Callable, TypeVar

from asset_studio.core.notification_service import NotificationService
from asset_studio.core.recovery_service import RecoveryService

T = TypeVar("T")


@dataclass(frozen=True)
class CrashRecord:
    source: str
    message: str
    traceback_text: str
    context: dict[str, Any]
    crash_path: str | None = None


class CrashGuard:
    def __init__(
        self,
        *,
        recovery_service: RecoveryService | None = None,
        notification_service: NotificationService | None = None,
    ) -> None:
        self.recovery_service = recovery_service
        self.notification_service = notification_service

    def capture_exception(self, source: str, exc: BaseException, context: dict[str, Any] | None = None) -> CrashRecord:
        traceback_text = "".join(traceback.format_exception(type(exc), exc, exc.__traceback__))
        crash_path = None
        if self.recovery_service is not None:
            crash_path = str(self.recovery_service.record_crash(source, str(exc), traceback_text, context=context))
        if self.notification_service is not None:
            self.notification_service.publish("error", source, str(exc), crash_path=crash_path, context=context or {})
        return CrashRecord(
            source=source,
            message=str(exc),
            traceback_text=traceback_text,
            context=context or {},
            crash_path=crash_path,
        )

    def run(self, source: str, handler: Callable[..., T], *args: Any, fallback: T | None = None, **kwargs: Any) -> T | None:
        try:
            return handler(*args, **kwargs)
        except Exception as exc:  # noqa: BLE001
            self.capture_exception(source, exc)
            return fallback
