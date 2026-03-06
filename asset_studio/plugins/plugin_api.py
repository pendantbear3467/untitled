from __future__ import annotations

from dataclasses import dataclass, field


@dataclass
class PluginAPI:
    generators: dict[str, object] = field(default_factory=dict)
    validators: dict[str, object] = field(default_factory=dict)
    texture_styles: dict[str, object] = field(default_factory=dict)

    def register_generator(self, name: str, handler: object) -> None:
        self.generators[name] = handler

    def register_validator(self, name: str, handler: object) -> None:
        self.validators[name] = handler

    def register_texture_style(self, name: str, style: object) -> None:
        self.texture_styles[name] = style
