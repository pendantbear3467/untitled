from __future__ import annotations

from dataclasses import dataclass, field


@dataclass
class PluginAPI:
    generators: dict[str, object] = field(default_factory=dict)
    validators: dict[str, object] = field(default_factory=dict)
    texture_styles: dict[str, object] = field(default_factory=dict)
    templates: dict[str, object] = field(default_factory=dict)
    exporters: dict[str, object] = field(default_factory=dict)
    asset_repairs: dict[str, object] = field(default_factory=dict)
    gui_editors: dict[str, object] = field(default_factory=dict)
    datapack_rules: dict[str, object] = field(default_factory=dict)
    graph_nodes: dict[str, object] = field(default_factory=dict)

    def register_generator(self, name: str, handler: object) -> None:
        self.generators[name] = handler

    def register_validator(self, name: str, handler: object) -> None:
        self.validators[name] = handler

    def register_texture_style(self, name: str, style: object) -> None:
        self.texture_styles[name] = style

    def register_asset_repair(self, name: str, handler: object) -> None:
        self.asset_repairs[name] = handler

    def register_gui_editor(self, name: str, handler: object) -> None:
        self.gui_editors[name] = handler

    def register_datapack_rule(self, name: str, handler: object) -> None:
        self.datapack_rules[name] = handler

    def register_graph_node(self, name: str, handler: object) -> None:
        self.graph_nodes[name] = handler
