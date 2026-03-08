from __future__ import annotations

from dataclasses import dataclass, field
from typing import Any, Callable

from asset_studio.core.crash_guard import CrashGuard
from asset_studio.core.help_registry import HelpEntry, HelpRegistry
from asset_studio.core.notification_service import NotificationService


@dataclass
class CommandExecution:
    command_id: str
    success: bool
    message: str = ""
    value: Any = None
    errors: list[str] = field(default_factory=list)


@dataclass
class StudioCommand:
    id: str
    label: str
    handler: Callable[..., Any]
    category: str = "general"
    short_tooltip: str = ""
    long_description: str = ""
    docs_ref: str | None = None
    example: str | None = None
    keywords: tuple[str, ...] = ()


class CommandRegistry:
    def __init__(
        self,
        *,
        help_registry: HelpRegistry | None = None,
        crash_guard: CrashGuard | None = None,
        notification_service: NotificationService | None = None,
    ) -> None:
        self.help_registry = help_registry
        self.crash_guard = crash_guard
        self.notification_service = notification_service
        self._commands: dict[str, StudioCommand] = {}

    def register(self, command: StudioCommand) -> None:
        self._commands[command.id] = command
        if self.help_registry is not None:
            self.help_registry.register(
                HelpEntry(
                    id=command.id,
                    label=command.label,
                    short_tooltip=command.short_tooltip or command.label,
                    long_description=command.long_description or command.short_tooltip or command.label,
                    category=command.category,
                    docs_ref=command.docs_ref,
                    example=command.example,
                    keywords=command.keywords,
                )
            )

    def get(self, command_id: str) -> StudioCommand | None:
        return self._commands.get(command_id)

    def all(self) -> list[StudioCommand]:
        return sorted(self._commands.values(), key=lambda command: (command.category, command.label.lower(), command.id))

    def dispatch(self, command_id: str, *args: Any, **kwargs: Any) -> CommandExecution:
        command = self._commands.get(command_id)
        if command is None:
            return CommandExecution(command_id=command_id, success=False, message=f"Unknown command: {command_id}", errors=[command_id])

        try:
            value = command.handler(*args, **kwargs)
            if isinstance(value, str):
                message = value
            elif hasattr(value, "message"):
                message = str(value.message)
            else:
                message = f"Executed {command.label}"
            if self.notification_service is not None:
                self.notification_service.publish("info", command_id, message)
            return CommandExecution(command_id=command_id, success=True, message=message, value=value)
        except Exception as exc:  # noqa: BLE001
            if self.crash_guard is not None:
                self.crash_guard.capture_exception(command_id, exc)
            if self.notification_service is not None:
                self.notification_service.publish("error", command_id, str(exc))
            return CommandExecution(command_id=command_id, success=False, message=str(exc), errors=[str(exc)])
