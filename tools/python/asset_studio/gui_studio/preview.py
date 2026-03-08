from __future__ import annotations

from asset_studio.gui_studio.models import GuiDocument


def build_preview_payload(document: GuiDocument) -> dict:
    widgets = []
    inventory_bindings = []
    for widget in sorted(document.widgets.values(), key=lambda item: (item.z_index, item.id)):
        payload = {
            "id": widget.id,
            "type": widget.widget_type,
            "label": widget.label,
            "x": widget.bounds.x,
            "y": widget.bounds.y,
            "width": widget.bounds.width,
            "height": widget.bounds.height,
            "visible": widget.visible,
            "zIndex": widget.z_index,
            "properties": dict(widget.properties),
            "tags": list(widget.tags),
        }
        if widget.binding is not None:
            payload["binding"] = {
                "kind": widget.binding.kind,
                "source": widget.binding.source,
                "slotId": widget.binding.slot_id,
                "role": widget.binding.role,
                "rows": widget.binding.rows,
                "columns": widget.binding.columns,
                "metadata": dict(widget.binding.metadata),
            }
            inventory_bindings.append({
                "widgetId": widget.id,
                "kind": widget.binding.kind,
                "source": widget.binding.source,
                "slotId": widget.binding.slot_id,
            })
        widgets.append(payload)
    return {
        "document": document.name,
        "namespace": document.namespace,
        "screenType": document.screen_type,
        "canvas": {"width": document.width, "height": document.height},
        "widgets": widgets,
        "inventoryBindings": inventory_bindings,
    }
