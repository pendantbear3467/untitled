from __future__ import annotations

from pathlib import Path

from asset_studio.gui_studio.models import GuiBounds, GuiDocument, GuiPropertySchema, GuiWidget
from asset_studio.gui_studio.preview import build_preview_payload
from asset_studio.gui_studio.serializer import GUI_STUDIO_FORMAT, GuiImportResult, load_document, save_document
from asset_studio.gui_studio.validator import GuiDocumentValidator, GuiValidationReport


class GuiStudioEngine:
    def __init__(self, root: Path) -> None:
        self.root = root
        self.root.mkdir(parents=True, exist_ok=True)
        self.validator = GuiDocumentValidator()
        self.property_schemas: dict[str, list[GuiPropertySchema]] = {
            "panel": [GuiPropertySchema("background", "string", default="textures/gui/panel.png")],
            "label": [GuiPropertySchema("text", "string", required=True)],
            "button": [GuiPropertySchema("text", "string", required=True), GuiPropertySchema("action", "string")],
            "slot": [GuiPropertySchema("slotType", "string", default="input")],
            "image": [GuiPropertySchema("texture", "string", required=True)],
            "progress": [GuiPropertySchema("value", "number", default=0), GuiPropertySchema("max", "number", default=100)],
            "text_input": [GuiPropertySchema("placeholder", "string")],
        }

    def create_document(self, name: str, *, screen_type: str = "generic", width: int = 176, height: int = 166) -> GuiDocument:
        document = GuiDocument(name=name, screen_type=screen_type, width=width, height=height)
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

    def list_documents(self) -> list[str]:
        return sorted(path.stem for path in self.root.glob("*.gui.json"))

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

    def validate_document(self, document: GuiDocument) -> GuiValidationReport:
        return self.validator.validate(document)

    def preview_payload(self, document: GuiDocument) -> dict:
        return build_preview_payload(document)

    def add_widget(self, document: GuiDocument, widget: GuiWidget, *, parent_id: str | None = None) -> None:
        document.widgets[widget.id] = widget
        if parent_id and parent_id in document.widgets:
            document.widgets[parent_id].children.append(widget.id)
        elif widget.id not in document.root_widgets:
            document.root_widgets.append(widget.id)

    def remove_widget(self, document: GuiDocument, widget_id: str) -> None:
        document.widgets.pop(widget_id, None)
        document.root_widgets = [item for item in document.root_widgets if item != widget_id]
        for widget in document.widgets.values():
            widget.children = [item for item in widget.children if item != widget_id]

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
