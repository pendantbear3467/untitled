from __future__ import annotations

import subprocess
import threading
import uuid
from dataclasses import dataclass, field
from pathlib import Path

from asset_studio.core.crash_guard import CrashGuard
from asset_studio.runtime.log_model import LogStreamModel
from asset_studio.runtime.task_results import ProcessTaskResult, utc_now


@dataclass
class ManagedProcess:
    task_id: str
    name: str
    command: list[str]
    thread: threading.Thread | None = None
    process: subprocess.Popen[str] | None = None
    result: ProcessTaskResult | None = None
    cancel_event: threading.Event = field(default_factory=threading.Event)

    def cancel(self) -> None:
        self.cancel_event.set()
        if self.process and self.process.poll() is None:
            self.process.terminate()

    def done(self) -> bool:
        return self.result is not None

    def wait(self, timeout: float | None = None) -> ProcessTaskResult | None:
        if self.thread is not None:
            self.thread.join(timeout)
        return self.result


class ProcessService:
    def __init__(self, *, crash_guard: CrashGuard | None = None, log_model: LogStreamModel | None = None) -> None:
        self.crash_guard = crash_guard
        self.log_model = log_model
        self._processes: dict[str, ManagedProcess] = {}

    def run(
        self,
        name: str,
        command: list[str],
        *,
        cwd: Path | None = None,
        env: dict[str, str] | None = None,
        timeout: float | None = None,
        log_model: LogStreamModel | None = None,
    ) -> ProcessTaskResult:
        handle = self.start(name, command, cwd=cwd, env=env, timeout=timeout, log_model=log_model)
        result = handle.wait(timeout if timeout else None)
        if result is None:
            raise TimeoutError(f"Process did not complete: {name}")
        return result

    def start(
        self,
        name: str,
        command: list[str],
        *,
        cwd: Path | None = None,
        env: dict[str, str] | None = None,
        timeout: float | None = None,
        log_model: LogStreamModel | None = None,
    ) -> ManagedProcess:
        task_id = uuid.uuid4().hex
        handle = ManagedProcess(task_id=task_id, name=name, command=list(command))
        self._processes[task_id] = handle

        def runner() -> None:
            started_at = utc_now()
            active_log = log_model or self.log_model
            try:
                process = subprocess.Popen(
                    command,
                    cwd=str(cwd) if cwd else None,
                    env=env,
                    stdout=subprocess.PIPE,
                    stderr=subprocess.PIPE,
                    text=True,
                )
                handle.process = process
                stdout, stderr = process.communicate(timeout=timeout)
                cancelled = handle.cancel_event.is_set()
                success = process.returncode == 0 and not cancelled
                if active_log is not None:
                    active_log.append_lines(name, stdout, level="info", stream="stdout")
                    active_log.append_lines(name, stderr, level="error" if stderr else "info", stream="stderr")
                handle.result = ProcessTaskResult(
                    task_id=task_id,
                    name=name,
                    success=success,
                    started_at=started_at,
                    finished_at=utc_now(),
                    message=(stderr.strip() or stdout.strip() or f"Process finished with code {process.returncode}"),
                    command=list(command),
                    exit_code=process.returncode,
                    stdout=stdout,
                    stderr=stderr,
                    cancelled=cancelled,
                )
            except subprocess.TimeoutExpired:
                if handle.process and handle.process.poll() is None:
                    handle.process.kill()
                handle.result = ProcessTaskResult(
                    task_id=task_id,
                    name=name,
                    success=False,
                    started_at=started_at,
                    finished_at=utc_now(),
                    message=f"Timed out after {timeout} seconds",
                    command=list(command),
                    cancelled=True,
                )
            except Exception as exc:  # noqa: BLE001
                if self.crash_guard is not None:
                    self.crash_guard.capture_exception(name, exc, context={"command": command, "cwd": str(cwd) if cwd else None})
                handle.result = ProcessTaskResult(
                    task_id=task_id,
                    name=name,
                    success=False,
                    started_at=started_at,
                    finished_at=utc_now(),
                    message=str(exc),
                    errors=[str(exc)],
                    command=list(command),
                )

        handle.thread = threading.Thread(target=runner, name=f"process-{name}-{task_id}", daemon=True)
        handle.thread.start()
        return handle

    def cancel(self, task_id: str) -> bool:
        handle = self._processes.get(task_id)
        if handle is None:
            return False
        handle.cancel()
        return True

    def get(self, task_id: str) -> ManagedProcess | None:
        return self._processes.get(task_id)
