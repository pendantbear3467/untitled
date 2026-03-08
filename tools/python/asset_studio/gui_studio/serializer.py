from __future__ import annotations

import json
from dataclasses import dataclass, field
from pathlib import Path

from asset_studio.gui_studio.models import GuiAnchor, GuiBounds, GuiDocument, GuiWidget

GUI_STUDIO_FORMAT = "gui-studio"


@dataclass
class GuiImportResult:
    document: GuiDocument
    warnings: list[str] = field(default_factory=list)
    errors: list[str] = field(default_factory=list)


def widget_to_dict(widget: GuiWidget) -> dict:
    return {
        "id": widget.id,
        "type": widget.widget_type,
        "label": widget.label,
        "bounds": {
            "x": widget.bounds.x,
            "y": widget.bounds.y,
            "width": widget.bounds.width,
            "height": widget.bounds.height,
        },
        "anchor": {
            "left": widget.anchor.left,
            "top": widget.anchor.top,
            "right": widget.anchor.right,
            "bottom": widget.anchor.bottom,
            "centerX": widget.anchor.center_x,
            "centerY": widget.anchor.center_y,
        },
        "properties": dict(widget.properties),
        "children": list(widget.children),
        "tags": list(widget.tags),
        "visible": widget.visible,
    }


def document_to_dict(document: GuiDocument) -> dict:
    return {
        "schemaVersion": document.schema_version,
        "documentType": GUI_STUDIO_FORMAT,
        "name": document.name,
        "screenType": document.screen_type,
        "width": document.width,
        "height": document.height,
        "rootWidgets": list(document.root_widgets),
        "metadata": dict(document.metadata),
        "widgets": [widget_to_dict(widget) for widget in sorted(document.widgets.values(), key=lambda item: item.id)],
    }


def save_document(document: GuiDocument, path: Path) -> Path:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(document_to_dict(document), indent=2) + "\n", encoding="utf-8")
    return path


def load_document(payload_or_path: dict | Path) -> GuiImportResult:
    warnings: list[str] = []
    errors: list[str] = []
    payload = payload_or_path
    if isinstance(payload_or_path, Path):
        payload = json.loads(payload_or_path.read_text(encoding="utf-8"))
    assert isinstance(payload, dict)

    if payload.get("documentType") not in {None, GUI_STUDIO_FORMAT}:
        warnings.append(f"Unexpected GUI document type: {payload.get('documentType')}")

    widgets: dict[str, GuiWidget] = {}
    for widget_payload in payload.get("widgets", []):
        widget_id = str(widget_payload.get("id", "")).strip()
        if not widget_id:
            errors.append("Widget missing id")
            continue
        if widget_id in widgets:
            errors.append(f"Duplicate widget id: {widget_id}")
            continue
        bounds_payload = widget_payload.get("bounds") or {}
        anchor_payload = widget_payload.get("anchor") or {}
        widgets[widget_id] = GuiWidget(
            id=widget_id,
            widget_type=str(widget_payload.get("type", "panel")),
            label=str(widget_payload.get("label", "")),
            bounds=GuiBounds(
                x=int(bounds_payload.get("x", 0)),
                y=int(bounds_payload.get("y", 0)),
                width=int(bounds_payload.get("width", 0)),
                height=int(bounds_payload.get("height", 0)),
            ),
            anchor=GuiAnchor(
                left=_maybe_int(anchor_payload.get("left")),
                top=_maybe_int(anchor_payload.get("top")),
                right=_maybe_int(anchor_payload.get("right")),
                bottom=_maybe_int(anchor_payload.get("bottom")),
                center_x=_maybe_int(anchor_payload.get("centerX")),
                center_y=_maybe_int(anchor_payload.get("centerY")),
            ),
            properties=dict(widget_payload.get("properties") or {}),
            children=[str(item) for item in widget_payload.get("children", [])],
            tags=[str(item) for item in widget_payload.get("tags", [])],
            visible=bool(widget_payload.get("visible", True)),
        )

    document = GuiDocument(
        name=str(payload.get("name", "untitled_gui")),
        screen_type=str(payload.get("screenType", "generic")),
        schema_version=int(payload.get("schemaVersion", 1)),
        width=int(payload.get("width", 176)),
        height=int(payload.get("height", 166)),
        widgets=widgets,
        root_widgets=[str(item) for item in payload.get("rootWidgets", list(widgets))],
        metadata=dict(payload.get("metadata") or {}),
    )
    return GuiImportResult(document=document, warnings=warnings, errors=errors)


def _maybe_int(value: object) -> int | None:
    if value is None or value == "":
        return None
    return int(value)
