from __future__ import annotations

from asset_studio.gui_studio.models import GuiDocument


def build_preview_payload(document: GuiDocument) -> dict:
    widgets = []
    for widget in sorted(document.widgets.values(), key=lambda item: item.id):
        widgets.append(
            {
                "id": widget.id,
                "type": widget.widget_type,
                "label": widget.label,
                "x": widget.bounds.x,
                "y": widget.bounds.y,
                "width": widget.bounds.width,
                "height": widget.bounds.height,
                "visible": widget.visible,
                "properties": dict(widget.properties),
                "tags": list(widget.tags),
            }
        )
    return {
        "document": document.name,
        "screenType": document.screen_type,
        "canvas": {"width": document.width, "height": document.height},
        "widgets": widgets,
    }
