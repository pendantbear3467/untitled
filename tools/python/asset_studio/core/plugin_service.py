from __future__ import annotations

from typing import Any, Callable

from asset_studio.core.crash_guard import CrashGuard
from asset_studio.plugins.plugin_api import PluginMetadata


class PluginService:
    def __init__(self, plugin_api: Any, *, crash_guard: CrashGuard | None = None) -> None:
        self.plugin_api = plugin_api
        self.crash_guard = crash_guard

    def metadata(self) -> list[PluginMetadata]:
        return sorted(self.plugin_api.metadata.values(), key=lambda item: item.name)

    def safe_call(self, name: str, handler: Callable[..., Any], *args: Any, **kwargs: Any) -> Any:
        if self.crash_guard is None:
            return handler(*args, **kwargs)
        return self.crash_guard.run(f"plugin:{name}", handler, *args, **kwargs)
