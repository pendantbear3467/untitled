"""SDK plugin helpers."""

from dataclasses import dataclass, field


@dataclass
class SDKPluginHooks:
    hooks: dict[str, object] = field(default_factory=dict)

    def register(self, name: str, handler: object) -> None:
        self.hooks[name] = handler
