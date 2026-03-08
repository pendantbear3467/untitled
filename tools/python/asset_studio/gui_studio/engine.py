from __future__ import annotations

import json
from pathlib import Path
from uuid import uuid4

from asset_studio.gui_studio.models import GuiBinding, GuiBounds, GuiDocument, GuiPropertySchema, GuiWidget
from asset_studio.gui_studio.preview import build_preview_payload
from asset_studio.gui_studio.serializer import GUI_RUNTIME_FORMAT, GUI_STUDIO_FORMAT, GuiImportResult, document_to_dict, load_document, save_document
from asset_studio.gui_studio.validator import GuiDocumentValidator, GuiValidationReport


class GuiStudioEngine:
    def __init__(self, root: Path) -> None:
        self.root = root
        self.root.mkdir(parents=True, exist_ok=True)
        self.validator = GuiDocumentValidator()
        self.property_schemas: dict[str, list[GuiPropertySchema]] = {
            "panel": [GuiPropertySchema("background", "string", default="textures/gui/default_panel.png", description="Panel background texture")],
            "label": [GuiPropertySchema("text", "string", required=True, default="Label", description="Visible text label")],
            "button": [
                GuiPropertySchema("text", "string", required=True, default="Button"),
                GuiPropertySchema("action", "string", default="noop", description="Action id for runtime button callbacks"),
            ],
            "image": [GuiPropertySchema("texture", "string", required=True, default="textures/gui/placeholder.png")],
            "progress": [
                GuiPropertySchema("value", "number", default=25),
                GuiPropertySchema("max", "number", default=100),
                GuiPropertySchema("direction", "enum", default="right", options=("right", "left", "up", "down")),
            ],
            "inventory_slot": [GuiPropertySchema("slotSize", "number", default=18)],
            "machine_slot": [GuiPropertySchema("slotSize", "number", default=18), GuiPropertySchema("slotRole", "string", default="input")],
            "player_inventory_grid": [GuiPropertySchema("rows", "number", default=3), GuiPropertySchema("columns", "number", default=9), GuiPropertySchema("slotSize", "number", default=18)],
            "hotbar": [GuiPropertySchema("columns", "number", default=9), GuiPropertySchema("slotSize", "number", default=18)],
            "armor_slots": [GuiPropertySchema("count", "number", default=4), GuiPropertySchema("slotSize", "number", default=18)],
            "offhand_slot": [GuiPropertySchema("slotSize", "number", default=18)],
        }

    def create_document(self, name: str, *, screen_type: str = "generic", width: int = 176, height: int = 166) -> GuiDocument:
        document = GuiDocument(
            name=name,
            screen_type=screen_type,
            width=width,
            height=height,
            namespace=self._load_modid(),
            metadata={"runtimeContract": GUI_RUNTIME_FORMAT},
        )
        document.add_widget(
            GuiWidget(
                id="root_panel",
                widget_type="panel",
                label="Root Panel",
                bounds=GuiBounds(x=0, y=0, width=width, height=height),
                properties={"background": "textures/gui/default_panel.png"},
                tags=["root"],
            )
        )
        return document

    def create_widget(self, widget_type: str, *, widget_id: str | None = None, x: int = 8, y: int = 8) -> GuiWidget:
        defaults = self._widget_defaults(widget_type)
        bounds = GuiBounds(x=x, y=y, width=defaults["width"], height=defaults["height"])
        return GuiWidget(
            id=widget_id or f"{widget_type}_{uuid4().hex[:6]}",
            widget_type=widget_type,
            label=defaults["label"],
            bounds=bounds,
            properties=defaults["properties"],
            binding=defaults["binding"],
            tags=defaults["tags"],
            z_index=defaults["z_index"],
        )

    def list_documents(self) -> list[str]:
        return sorted(path.stem.replace(".gui", "") for path in self.root.glob("*.gui.json"))

    def document_path(self, name: str) -> Path:
        return self.root / f"{name}.gui.json"

    def save_document(self, document: GuiDocument) -> Path:
        return save_document(document, self.document_path(document.name))

    def load_document(self, name_or_path: str | Path) -> GuiImportResult:
        path = Path(name_or_path)
        if not path.suffix:
            path = self.document_path(str(name_or_path))
        return load_document(path)

    def import_document(self, path: Path) -> GuiImportResult:
        return load_document(path)

    def export_document(self, document: GuiDocument, path: Path | None = None, *, format: str = GUI_STUDIO_FORMAT) -> Path:
        if format != GUI_STUDIO_FORMAT:
            raise ValueError(f"Unsupported GUI export format: {format}")
        target = path or self.document_path(document.name)
        return save_document(document, target)

    def runtime_export_path(self, document: GuiDocument) -> Path:
        return self.root.parent / "assets" / document.namespace / "studio" / "gui" / f"{document.name}.json"

    def export_runtime_document(self, document: GuiDocument, path: Path | None = None) -> Path:
        target = path or self.runtime_export_path(document)
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(json.dumps(self.build_runtime_definition(document), indent=2) + "\n", encoding="utf-8")
        return target

    def build_runtime_definition(self, document: GuiDocument) -> dict:
        widgets = []
        inventory_bindings = []
        for widget in sorted(document.widgets.values(), key=lambda item: (item.z_index, item.id)):
            slot_bindings = self.expand_inventory_bindings(widget)
            widgets.append(
                {
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
                    "visible": widget.visible,
                    "zIndex": widget.z_index,
                    "properties": dict(widget.properties),
                    "binding": None if widget.binding is None else {
                        "kind": widget.binding.kind,
                        "source": widget.binding.source,
                        "slotId": widget.binding.slot_id,
                        "role": widget.binding.role,
                        "rows": widget.binding.rows,
                        "columns": widget.binding.columns,
                        "metadata": dict(widget.binding.metadata),
                    },
                    "children": list(widget.children),
                    "slotBindings": slot_bindings,
                }
            )
            inventory_bindings.extend(slot_bindings)
        return {
            "schemaVersion": 1,
            "documentType": GUI_RUNTIME_FORMAT,
            "studioFormat": GUI_STUDIO_FORMAT,
            "screenId": document.name,
            "namespace": document.namespace,
            "resourceId": f"{document.namespace}:studio/gui/{document.name}",
            "screenType": document.screen_type,
            "canvas": {"width": document.width, "height": document.height},
            "widgets": widgets,
            "inventoryBindings": inventory_bindings,
            "previewCompatible": True,
            "metadata": dict(document.metadata),
        }

    def validate_document(self, document: GuiDocument) -> GuiValidationReport:
        return self.validator.validate(document)

    def preview_payload(self, document: GuiDocument) -> dict:
        return build_preview_payload(document)

    def add_widget(self, document: GuiDocument, widget: GuiWidget, *, parent_id: str | None = None) -> None:
        document.widgets[widget.id] = widget
        if parent_id and parent_id in document.widgets:
            if widget.id not in document.widgets[parent_id].children:
                document.widgets[parent_id].children.append(widget.id)
        elif widget.id not in document.root_widgets:
            document.root_widgets.append(widget.id)

    def remove_widget(self, document: GuiDocument, widget_id: str) -> None:
        if widget_id == "root_panel":
            raise ValueError("The root_panel widget cannot be removed")
        document.widgets.pop(widget_id, None)
        document.root_widgets = [item for item in document.root_widgets if item != widget_id]
        for widget in document.widgets.values():
            widget.children = [item for item in widget.children if item != widget_id]

    def duplicate_widget(self, document: GuiDocument, widget_id: str, *, offset_x: int = 12, offset_y: int = 12) -> GuiWidget:
        widget = document.widgets[widget_id]
        duplicate = self.create_widget(widget.widget_type, x=widget.bounds.x + offset_x, y=widget.bounds.y + offset_y)
        duplicate.label = f"{widget.label} Copy"
        duplicate.properties = dict(widget.properties)
        duplicate.tags = list(widget.tags)
        duplicate.z_index = widget.z_index
        if widget.binding is not None:
            duplicate.binding = GuiBinding(
                kind=widget.binding.kind,
                source=widget.binding.source,
                slot_id=f"{widget.binding.slot_id}_copy" if widget.binding.slot_id else "",
                role=widget.binding.role,
                rows=widget.binding.rows,
                columns=widget.binding.columns,
                metadata=dict(widget.binding.metadata),
            )
        self.add_widget(document, duplicate)
        return duplicate

    def move_widgets(self, document: GuiDocument, widget_ids: list[str], dx: int, dy: int) -> None:
        for widget_id in widget_ids:
            if widget_id in document.widgets:
                document.widgets[widget_id].bounds.x += dx
                document.widgets[widget_id].bounds.y += dy

    def align_widgets(self, document: GuiDocument, widget_ids: list[str], mode: str) -> None:
        widgets = [document.widgets[widget_id] for widget_id in widget_ids if widget_id in document.widgets]
        if len(widgets) < 2:
            return
        if mode == "left":
            anchor = min(widget.bounds.x for widget in widgets)
            for widget in widgets:
                widget.bounds.x = anchor
        elif mode == "right":
            anchor = max(widget.bounds.x + widget.bounds.width for widget in widgets)
            for widget in widgets:
                widget.bounds.x = anchor - widget.bounds.width
        elif mode == "top":
            anchor = min(widget.bounds.y for widget in widgets)
            for widget in widgets:
                widget.bounds.y = anchor
        elif mode == "bottom":
            anchor = max(widget.bounds.y + widget.bounds.height for widget in widgets)
            for widget in widgets:
                widget.bounds.y = anchor - widget.bounds.height
        elif mode == "center_h":
            center = sum(widget.bounds.x + widget.bounds.width // 2 for widget in widgets) // len(widgets)
            for widget in widgets:
                widget.bounds.x = center - widget.bounds.width // 2
        elif mode == "center_v":
            center = sum(widget.bounds.y + widget.bounds.height // 2 for widget in widgets) // len(widgets)
            for widget in widgets:
                widget.bounds.y = center - widget.bounds.height // 2

    def distribute_widgets(self, document: GuiDocument, widget_ids: list[str], axis: str) -> None:
        widgets = [document.widgets[widget_id] for widget_id in widget_ids if widget_id in document.widgets]
        if len(widgets) < 3:
            return
        if axis == "horizontal":
            widgets.sort(key=lambda widget: widget.bounds.x)
            start = widgets[0].bounds.x
            end = widgets[-1].bounds.x
            step = (end - start) // (len(widgets) - 1) if len(widgets) > 1 else 0
            for index, widget in enumerate(widgets[1:-1], start=1):
                widget.bounds.x = start + step * index
        elif axis == "vertical":
            widgets.sort(key=lambda widget: widget.bounds.y)
            start = widgets[0].bounds.y
            end = widgets[-1].bounds.y
            step = (end - start) // (len(widgets) - 1) if len(widgets) > 1 else 0
            for index, widget in enumerate(widgets[1:-1], start=1):
                widget.bounds.y = start + step * index

    def search_widgets(self, document: GuiDocument, query: str) -> list[GuiWidget]:
        needle = query.strip().lower()
        if not needle:
            return sorted(document.widgets.values(), key=lambda item: item.id)
        return [
            widget
            for widget in sorted(document.widgets.values(), key=lambda item: item.id)
            if needle in widget.id.lower()
            or needle in widget.label.lower()
            or needle in widget.widget_type.lower()
            or any(needle in tag.lower() for tag in widget.tags)
        ]

    def expand_inventory_bindings(self, widget: GuiWidget) -> list[dict]:
        if widget.binding is None:
            return []
        slot_size = int(widget.properties.get("slotSize", 18) or 18)
        bindings: list[dict] = []

        if widget.widget_type in {"inventory_slot", "machine_slot", "offhand_slot"}:
            slot_id = widget.binding.slot_id or self._default_slot_id(widget)
            bindings.append(
                {
                    "widgetId": widget.id,
                    "slotId": slot_id,
                    "source": widget.binding.source,
                    "role": widget.binding.role,
                    "x": widget.bounds.x,
                    "y": widget.bounds.y,
                    "width": widget.bounds.width,
                    "height": widget.bounds.height,
                }
            )
            return bindings

        if widget.widget_type == "player_inventory_grid":
            rows = int(widget.properties.get("rows", widget.binding.rows or 3) or 3)
            columns = int(widget.properties.get("columns", widget.binding.columns or 9) or 9)
            for row in range(rows):
                for column in range(columns):
                    index = row * columns + column
                    bindings.append(
                        {
                            "widgetId": widget.id,
                            "slotId": f"player.inventory.main[{index}]",
                            "source": "player.inventory.main",
                            "role": "inventory",
                            "x": widget.bounds.x + column * slot_size,
                            "y": widget.bounds.y + row * slot_size,
                            "width": slot_size,
                            "height": slot_size,
                        }
                    )
            return bindings

        if widget.widget_type == "hotbar":
            for column in range(9):
                bindings.append(
                    {
                        "widgetId": widget.id,
                        "slotId": f"player.hotbar[{column}]",
                        "source": "player.hotbar",
                        "role": "hotbar",
                        "x": widget.bounds.x + column * slot_size,
                        "y": widget.bounds.y,
                        "width": slot_size,
                        "height": slot_size,
                    }
                )
            return bindings

        if widget.widget_type == "armor_slots":
            armor_roles = ["head", "chest", "legs", "feet"]
            for row, role in enumerate(armor_roles):
                bindings.append(
                    {
                        "widgetId": widget.id,
                        "slotId": f"player.armor.{role}",
                        "source": "player.armor",
                        "role": role,
                        "x": widget.bounds.x,
                        "y": widget.bounds.y + row * slot_size,
                        "width": slot_size,
                        "height": slot_size,
                    }
                )
            return bindings

        return bindings

    def _widget_defaults(self, widget_type: str) -> dict:
        defaults = {
            "panel": {"width": 120, "height": 60, "label": "Panel", "properties": {"background": "textures/gui/default_panel.png"}, "binding": None, "tags": ["panel"], "z_index": 0},
            "label": {"width": 80, "height": 18, "label": "Label", "properties": {"text": "Label"}, "binding": None, "tags": ["text"], "z_index": 10},
            "button": {"width": 90, "height": 20, "label": "Button", "properties": {"text": "Button", "action": "noop"}, "binding": None, "tags": ["interactive"], "z_index": 20},
            "image": {"width": 32, "height": 32, "label": "Image", "properties": {"texture": "textures/gui/placeholder.png"}, "binding": None, "tags": ["visual"], "z_index": 15},
            "progress": {"width": 96, "height": 12, "label": "Progress", "properties": {"value": 25, "max": 100, "direction": "right"}, "binding": None, "tags": ["progress"], "z_index": 15},
            "inventory_slot": {"width": 18, "height": 18, "label": "Inventory Slot", "properties": {"slotSize": 18}, "binding": GuiBinding(kind="inventory_slot", source="player.inventory.custom", slot_id="player.inventory.custom[0]", role="inventory"), "tags": ["inventory", "slot"], "z_index": 25},
            "machine_slot": {"width": 18, "height": 18, "label": "Machine Slot", "properties": {"slotSize": 18, "slotRole": "input"}, "binding": GuiBinding(kind="machine_slot", source="machine.slot", slot_id="machine.slot.input_0", role="input"), "tags": ["inventory", "machine"], "z_index": 25},
            "player_inventory_grid": {"width": 162, "height": 54, "label": "Player Inventory", "properties": {"rows": 3, "columns": 9, "slotSize": 18}, "binding": GuiBinding(kind="player_inventory_grid", source="player.inventory.main", role="inventory", rows=3, columns=9), "tags": ["inventory", "player"], "z_index": 25},
            "hotbar": {"width": 162, "height": 18, "label": "Hotbar", "properties": {"columns": 9, "slotSize": 18}, "binding": GuiBinding(kind="hotbar", source="player.hotbar", role="hotbar", rows=1, columns=9), "tags": ["inventory", "player"], "z_index": 25},
            "armor_slots": {"width": 18, "height": 72, "label": "Armor Slots", "properties": {"count": 4, "slotSize": 18}, "binding": GuiBinding(kind="armor_slots", source="player.armor", role="armor", rows=4, columns=1), "tags": ["inventory", "player"], "z_index": 25},
            "offhand_slot": {"width": 18, "height": 18, "label": "Offhand", "properties": {"slotSize": 18}, "binding": GuiBinding(kind="offhand_slot", source="player.offhand", slot_id="player.offhand", role="offhand"), "tags": ["inventory", "player"], "z_index": 25},
        }
        if widget_type not in defaults:
            raise ValueError(f"Unsupported widget type: {widget_type}")
        payload = defaults[widget_type]
        binding = payload["binding"]
        clone_binding = None
        if binding is not None:
            clone_binding = GuiBinding(
                kind=binding.kind,
                source=binding.source,
                slot_id=binding.slot_id,
                role=binding.role,
                rows=binding.rows,
                columns=binding.columns,
                metadata=dict(binding.metadata),
            )
        return {
            "width": payload["width"],
            "height": payload["height"],
            "label": payload["label"],
            "properties": dict(payload["properties"]),
            "binding": clone_binding,
            "tags": list(payload["tags"]),
            "z_index": payload["z_index"],
        }

    def _default_slot_id(self, widget: GuiWidget) -> str:
        if widget.binding is None:
            return widget.id
        if widget.binding.source:
            return widget.binding.source
        return widget.id

    def _load_modid(self) -> str:
        project_file = self.root.parent / "project.json"
        if not project_file.exists():
            return "extremecraft"
        try:
            payload = json.loads(project_file.read_text(encoding="utf-8"))
        except json.JSONDecodeError:
            return "extremecraft"
        return str(payload.get("modid", "extremecraft"))
