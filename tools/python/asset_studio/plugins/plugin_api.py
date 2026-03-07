from __future__ import annotations

from dataclasses import dataclass, field


@dataclass(frozen=True)
class PluginMetadata:
    name: str
    version: str
    dependencies: tuple[str, ...] = ()
    compatible_platform_version: str = "*"
    entrypoint: str = ""


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
    metadata: dict[str, PluginMetadata] = field(default_factory=dict)

    def register_plugin(self, metadata: PluginMetadata) -> None:
        self.metadata[metadata.name] = metadata

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
