from __future__ import annotations

import concurrent.futures
import threading
import uuid
from dataclasses import dataclass, field
from typing import Any, Callable

from asset_studio.core.crash_guard import CrashGuard
from asset_studio.core.notification_service import NotificationService
from asset_studio.runtime.task_results import StudioTaskResult, TaskProgressEvent, utc_now


@dataclass
class TaskExecutionContext:
    task_id: str
    cancel_event: threading.Event
    progress_events: list[TaskProgressEvent]

    def report_progress(self, step: str, message: str, percent: float | None = None) -> TaskProgressEvent:
        event = TaskProgressEvent(task_id=self.task_id, step=step, message=message, percent=percent)
        self.progress_events.append(event)
        return event

    @property
    def cancelled(self) -> bool:
        return self.cancel_event.is_set()


@dataclass
class TaskHandle:
    task_id: str
    name: str
    future: concurrent.futures.Future[StudioTaskResult]
    cancel_event: threading.Event = field(default_factory=threading.Event)
    progress_events: list[TaskProgressEvent] = field(default_factory=list)

    def cancel(self) -> bool:
        self.cancel_event.set()
        return self.future.cancel()

    def done(self) -> bool:
        return self.future.done()

    def result(self, timeout: float | None = None) -> StudioTaskResult:
        return self.future.result(timeout=timeout)


class TaskService:
    def __init__(
        self,
        *,
        max_workers: int = 4,
        crash_guard: CrashGuard | None = None,
        notification_service: NotificationService | None = None,
    ) -> None:
        self.crash_guard = crash_guard
        self.notification_service = notification_service
        self._executor = concurrent.futures.ThreadPoolExecutor(max_workers=max_workers, thread_name_prefix="studio-task")
        self._tasks: dict[str, TaskHandle] = {}

    def submit(self, name: str, handler: Callable[..., Any], *args: Any, **kwargs: Any) -> TaskHandle:
        task_id = uuid.uuid4().hex
        cancel_event = threading.Event()
        progress_events: list[TaskProgressEvent] = []
        task_context = TaskExecutionContext(task_id=task_id, cancel_event=cancel_event, progress_events=progress_events)
        started_at = utc_now()
        expects_task_context = hasattr(handler, "__code__") and "task_context" in handler.__code__.co_varnames

        def runner() -> StudioTaskResult:
            try:
                if expects_task_context:
                    value = handler(*args, task_context=task_context, **kwargs)
                else:
                    value = handler(*args, **kwargs)
                if isinstance(value, StudioTaskResult):
                    return value
                return StudioTaskResult(
                    task_id=task_id,
                    name=name,
                    success=True,
                    started_at=started_at,
                    finished_at=utc_now(),
                    message=f"Task completed: {name}",
                    data=value,
                )
            except Exception as exc:  # noqa: BLE001
                if self.crash_guard is not None:
                    self.crash_guard.capture_exception(name, exc, context={"taskId": task_id})
                if self.notification_service is not None:
                    self.notification_service.publish("error", name, str(exc), task_id=task_id)
                return StudioTaskResult(
                    task_id=task_id,
                    name=name,
                    success=False,
                    started_at=started_at,
                    finished_at=utc_now(),
                    message=str(exc),
                    errors=[str(exc)],
                )

        future = self._executor.submit(runner)
        handle = TaskHandle(task_id=task_id, name=name, future=future, cancel_event=cancel_event, progress_events=progress_events)
        self._tasks[task_id] = handle
        return handle

    def get(self, task_id: str) -> TaskHandle | None:
        return self._tasks.get(task_id)

    def shutdown(self, wait: bool = False) -> None:
        self._executor.shutdown(wait=wait, cancel_futures=False)
