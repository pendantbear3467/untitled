from __future__ import annotations

from dataclasses import dataclass, field
from typing import Any


@dataclass
class GuiBounds:
    x: int = 0
    y: int = 0
    width: int = 176
    height: int = 166


@dataclass
class GuiAnchor:
    left: int | None = None
    top: int | None = None
    right: int | None = None
    bottom: int | None = None
    center_x: int | None = None
    center_y: int | None = None


@dataclass(frozen=True)
class GuiPropertySchema:
    name: str
    property_type: str
    required: bool = False
    default: Any = None
    description: str = ""
    options: tuple[str, ...] = ()


@dataclass
class GuiWidget:
    id: str
    widget_type: str
    label: str = ""
    bounds: GuiBounds = field(default_factory=GuiBounds)
    anchor: GuiAnchor = field(default_factory=GuiAnchor)
    properties: dict[str, Any] = field(default_factory=dict)
    children: list[str] = field(default_factory=list)
    tags: list[str] = field(default_factory=list)
    visible: bool = True


@dataclass
class GuiDocument:
    name: str
    screen_type: str = "generic"
    schema_version: int = 1
    width: int = 176
    height: int = 166
    widgets: dict[str, GuiWidget] = field(default_factory=dict)
    root_widgets: list[str] = field(default_factory=list)
    metadata: dict[str, Any] = field(default_factory=dict)

    def add_widget(self, widget: GuiWidget) -> None:
        self.widgets[widget.id] = widget
        if widget.id not in self.root_widgets:
            self.root_widgets.append(widget.id)
