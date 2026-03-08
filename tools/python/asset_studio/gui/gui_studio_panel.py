from __future__ import annotations

import json
from pathlib import Path

from PyQt6.QtCore import Qt, pyqtSignal
from PyQt6.QtGui import QColor, QBrush, QPen
from PyQt6.QtWidgets import (
    QCheckBox,
    QComboBox,
    QFormLayout,
    QGraphicsRectItem,
    QGraphicsScene,
    QGraphicsSimpleTextItem,
    QGraphicsView,
    QGroupBox,
    QHBoxLayout,
    QInputDialog,
    QLabel,
    QListWidget,
    QListWidgetItem,
    QMessageBox,
    QPlainTextEdit,
    QPushButton,
    QSpinBox,
    QSplitter,
    QTabWidget,
    QVBoxLayout,
    QWidget,
)

from asset_studio.gui_studio.models import GuiBinding, GuiDocument, GuiWidget


_WIDGET_COLORS = {
    "panel": QColor("#5a6b8c"),
    "label": QColor("#5886b2"),
    "button": QColor("#4d8b5a"),
    "image": QColor("#8d6e4f"),
    "progress": QColor("#b3834e"),
    "inventory_slot": QColor("#7a5c8d"),
    "machine_slot": QColor("#8d5c5c"),
    "player_inventory_grid": QColor("#7c6cb5"),
    "hotbar": QColor("#577ea6"),
    "armor_slots": QColor("#8c5fa1"),
    "offhand_slot": QColor("#96724a"),
}


class GuiCanvasItem(QGraphicsRectItem):
    def __init__(self, widget: GuiWidget) -> None:
        super().__init__()
        self.widget_id = widget.id
        self.widget_type = widget.widget_type
        self.caption = QGraphicsSimpleTextItem(self)
        self.caption.setBrush(QBrush(QColor("#f5f7ff")))
        self.setFlag(QGraphicsRectItem.GraphicsItemFlag.ItemIsMovable, True)
        self.setFlag(QGraphicsRectItem.GraphicsItemFlag.ItemIsSelectable, True)
        self.setFlag(QGraphicsRectItem.GraphicsItemFlag.ItemSendsGeometryChanges, True)
        self.sync_from_widget(widget)

    def sync_from_widget(self, widget: GuiWidget) -> None:
        self.widget_id = widget.id
        self.widget_type = widget.widget_type
        self.setRect(0, 0, widget.bounds.width, widget.bounds.height)
        self.setPos(widget.bounds.x, widget.bounds.y)
        color = _WIDGET_COLORS.get(widget.widget_type, QColor("#5a6b8c"))
        self.setBrush(QBrush(color.lighter(120)))
        self.setPen(QPen(QColor("#dce6ff") if self.isSelected() else QColor("#233047"), 1.4))
        self.caption.setText(f"{widget.label or widget.id}\n{widget.widget_type}")
        self.caption.setPos(4, 2)

    def paint(self, painter, option, widget=None) -> None:  # noqa: A003
        super().paint(painter, option, widget)
        painter.setPen(QPen(QColor("#233047"), 1))
        if self.widget_type in {"player_inventory_grid", "hotbar", "armor_slots"}:
            rows = 1
            columns = 1
            if self.widget_type == "player_inventory_grid":
                rows, columns = 3, 9
            elif self.widget_type == "hotbar":
                rows, columns = 1, 9
            elif self.widget_type == "armor_slots":
                rows, columns = 4, 1
            slot_size = 18
            for row in range(rows):
                for column in range(columns):
                    painter.drawRect(2 + column * slot_size, 18 + row * slot_size, 16, 16)
        elif self.widget_type in {"inventory_slot", "machine_slot", "offhand_slot"}:
            painter.drawRect(1, 18, 16, 16)
        elif self.widget_type == "progress":
            painter.drawRect(4, 20, max(8, int(self.rect().width()) - 8), 6)


class GuiCanvasView(QGraphicsView):
    selection_ids_changed = pyqtSignal(list)
    positions_changed = pyqtSignal(dict)
    delete_requested = pyqtSignal()
    duplicate_requested = pyqtSignal()

    def __init__(self) -> None:
        self.scene = QGraphicsScene()
        super().__init__(self.scene)
        self.setRenderHint(self.renderHints())
        self.setDragMode(QGraphicsView.DragMode.RubberBandDrag)
        self.scene.selectionChanged.connect(self._emit_selection)
        self.item_map: dict[str, GuiCanvasItem] = {}

    def load_document(self, document: GuiDocument) -> None:
        self.scene.clear()
        self.item_map.clear()
        self.scene.setSceneRect(0, 0, document.width, document.height)
        for gui_widget in sorted(document.widgets.values(), key=lambda item: (item.z_index, item.id)):
            item = GuiCanvasItem(gui_widget)
            self.scene.addItem(item)
            item.setZValue(gui_widget.z_index)
            self.item_map[gui_widget.id] = item

    def sync_widget(self, widget: GuiWidget) -> None:
        item = self.item_map.get(widget.id)
        if item is None:
            item = GuiCanvasItem(widget)
            self.scene.addItem(item)
            self.item_map[widget.id] = item
        item.setZValue(widget.z_index)
        item.sync_from_widget(widget)

    def remove_widget(self, widget_id: str) -> None:
        item = self.item_map.pop(widget_id, None)
        if item is not None:
            self.scene.removeItem(item)

    def selected_ids(self) -> list[str]:
        ids: list[str] = []
        for item in self.scene.selectedItems():
            if isinstance(item, GuiCanvasItem):
                ids.append(item.widget_id)
        return ids

    def select_widget(self, widget_id: str) -> None:
        for item in self.item_map.values():
            item.setSelected(item.widget_id == widget_id)
        self._emit_selection()

    def mouseReleaseEvent(self, event) -> None:  # noqa: N802
        super().mouseReleaseEvent(event)
        positions = {
            widget_id: (int(item.pos().x()), int(item.pos().y()))
            for widget_id, item in self.item_map.items()
        }
        self.positions_changed.emit(positions)

    def keyPressEvent(self, event) -> None:  # noqa: N802
        if event.key() == Qt.Key.Key_Delete:
            self.delete_requested.emit()
            event.accept()
            return
        if event.key() == Qt.Key.Key_D and event.modifiers() & Qt.KeyboardModifier.ControlModifier:
            self.duplicate_requested.emit()
            event.accept()
            return
        super().keyPressEvent(event)

    def _emit_selection(self) -> None:
        self.selection_ids_changed.emit(self.selected_ids())


class GuiStudioPanel(QWidget):
    def __init__(self, session) -> None:
        super().__init__()
        self.session = session
        self.engine = session.gui_studio_engine
        self.current_document: GuiDocument | None = None
        self.selected_widget_ids: list[str] = []

        root = QVBoxLayout(self)
        toolbar = QHBoxLayout()

        self.document_name = QLabel("No document")
        self.screen_type = QComboBox()
        self.screen_type.addItems(["generic", "machine", "overlay", "menu", "player"])
        self.screen_type.currentTextChanged.connect(self._screen_type_changed)

        for title, handler in [
            ("New", self.new_document),
            ("Load", self.load_selected_document),
            ("Save", self.save_document),
            ("Export Runtime", self.export_runtime),
            ("Validate", self.validate_document),
        ]:
            button = QPushButton(title)
            button.clicked.connect(handler)
            toolbar.addWidget(button)
        toolbar.addWidget(QLabel("Screen Type"))
        toolbar.addWidget(self.screen_type)
        toolbar.addWidget(self.document_name)
        toolbar.addStretch(1)
        root.addLayout(toolbar)

        split = QSplitter(Qt.Orientation.Horizontal)
        split.addWidget(self._build_left_panel())
        split.addWidget(self._build_center_panel())
        split.addWidget(self._build_right_panel())
        split.setSizes([260, 760, 360])
        root.addWidget(split)

        self.refresh_document_list()

    def _build_left_panel(self) -> QWidget:
        holder = QWidget()
        layout = QVBoxLayout(holder)

        docs = QGroupBox("GUI Documents")
        docs_layout = QVBoxLayout(docs)
        self.documents = QListWidget()
        self.documents.itemDoubleClicked.connect(lambda _: self.load_selected_document())
        docs_layout.addWidget(self.documents)

        palette = QGroupBox("Palette")
        palette_layout = QVBoxLayout(palette)
        self.palette = QListWidget()
        for widget_type in [
            "panel",
            "label",
            "button",
            "image",
            "progress",
            "inventory_slot",
            "machine_slot",
            "player_inventory_grid",
            "hotbar",
            "armor_slots",
            "offhand_slot",
        ]:
            item = QListWidgetItem(widget_type)
            item.setData(Qt.ItemDataRole.UserRole, widget_type)
            self.palette.addItem(item)
        self.palette.itemDoubleClicked.connect(lambda _: self.add_widget_from_palette())
        add_button = QPushButton("Add Selected")
        add_button.clicked.connect(self.add_widget_from_palette)
        palette_layout.addWidget(self.palette)
        palette_layout.addWidget(add_button)

        hierarchy = QGroupBox("Hierarchy")
        hierarchy_layout = QVBoxLayout(hierarchy)
        self.hierarchy = QListWidget()
        self.hierarchy.itemClicked.connect(self._hierarchy_selected)
        hierarchy_layout.addWidget(self.hierarchy)

        layout.addWidget(docs)
        layout.addWidget(palette)
        layout.addWidget(hierarchy)
        return holder

    def _build_center_panel(self) -> QWidget:
        holder = QWidget()
        layout = QVBoxLayout(holder)
        actions = QHBoxLayout()
        for title, handler in [
            ("Duplicate", self.duplicate_selected),
            ("Delete", self.delete_selected),
            ("Align Left", lambda: self.align_selected("left")),
            ("Align Top", lambda: self.align_selected("top")),
            ("Center H", lambda: self.align_selected("center_h")),
            ("Distribute H", lambda: self.distribute_selected("horizontal")),
            ("Distribute V", lambda: self.distribute_selected("vertical")),
        ]:
            button = QPushButton(title)
            button.clicked.connect(handler)
            actions.addWidget(button)
        layout.addLayout(actions)

        self.canvas = GuiCanvasView()
        self.canvas.selection_ids_changed.connect(self._canvas_selection_changed)
        self.canvas.positions_changed.connect(self._canvas_positions_changed)
        self.canvas.delete_requested.connect(self.delete_selected)
        self.canvas.duplicate_requested.connect(self.duplicate_selected)
        layout.addWidget(self.canvas)
        return holder

    def _build_right_panel(self) -> QWidget:
        tabs = QTabWidget()

        inspector = QWidget()
        form = QFormLayout(inspector)
        self.selection_label = QLabel("No selection")
        self.widget_id = QLineEdit()
        self.widget_label = QLineEdit()
        self.widget_visible = QCheckBox()
        self.widget_x = QSpinBox(); self.widget_x.setRange(-9999, 9999)
        self.widget_y = QSpinBox(); self.widget_y.setRange(-9999, 9999)
        self.widget_w = QSpinBox(); self.widget_w.setRange(1, 4096)
        self.widget_h = QSpinBox(); self.widget_h.setRange(1, 4096)
        self.properties_editor = QPlainTextEdit()
        self.binding_editor = QPlainTextEdit()
        apply_button = QPushButton("Apply Inspector")
        apply_button.clicked.connect(self.apply_inspector)
        form.addRow("Selection", self.selection_label)
        form.addRow("Widget ID", self.widget_id)
        form.addRow("Label", self.widget_label)
        form.addRow("Visible", self.widget_visible)
        form.addRow("X", self.widget_x)
        form.addRow("Y", self.widget_y)
        form.addRow("Width", self.widget_w)
        form.addRow("Height", self.widget_h)
        form.addRow("Properties JSON", self.properties_editor)
        form.addRow("Binding JSON", self.binding_editor)
        form.addRow(apply_button)

        preview = QWidget()
        preview_layout = QVBoxLayout(preview)
        self.preview_text = QPlainTextEdit()
        self.preview_text.setReadOnly(True)
        self.validation_list = QListWidget()
        preview_layout.addWidget(QLabel("Preview / Runtime Payload"))
        preview_layout.addWidget(self.preview_text)
        preview_layout.addWidget(QLabel("Validation"))
        preview_layout.addWidget(self.validation_list)

        tabs.addTab(inspector, "Inspector")
        tabs.addTab(preview, "Preview")
        return tabs

    def refresh_document_list(self) -> None:
        self.documents.clear()
        for name in self.engine.list_documents():
            self.documents.addItem(name)

    def new_document(self) -> None:
        name, ok = QInputDialog.getText(self, "New GUI Document", "Document name")
        if not ok or not name.strip():
            return
        self.current_document = self.engine.create_document(name.strip(), screen_type=self.screen_type.currentText())
        self._refresh_all_views()
        self.session.notification_service.publish("info", "gui", f"Created GUI document {name.strip()}")

    def load_selected_document(self) -> None:
        item = self.documents.currentItem()
        if item is None:
            return
        result = self.engine.load_document(item.text())
        if result.errors:
            QMessageBox.warning(self, "Load Issues", "\n".join(result.errors))
        self.current_document = result.document
        self._refresh_all_views()
        self.session.notification_service.publish("info", "gui", f"Loaded GUI document {result.document.name}")

    def save_document(self) -> None:
        if self.current_document is None:
            return
        self.current_document.screen_type = self.screen_type.currentText()
        path = self.engine.save_document(self.current_document)
        self.refresh_document_list()
        self._refresh_preview()
        self.session.notification_service.publish("info", "gui", f"Saved GUI document to {path}")

    def export_runtime(self) -> None:
        if self.current_document is None:
            return
        path = self.engine.export_runtime_document(self.current_document)
        self._refresh_preview()
        self.session.notification_service.publish("info", "gui", f"Exported GUI runtime definition to {path}")

    def validate_document(self) -> None:
        self._refresh_preview()
        if self.current_document is not None:
            self.session.notification_service.publish("info", "gui", f"Validated GUI document {self.current_document.name}")

    def add_widget_from_palette(self) -> None:
        if self.current_document is None:
            return
        item = self.palette.currentItem()
        if item is None:
            return
        widget_type = str(item.data(Qt.ItemDataRole.UserRole))
        offset = 10 + len(self.current_document.widgets) * 8
        widget = self.engine.create_widget(widget_type, x=offset, y=offset)
        self.engine.add_widget(self.current_document, widget)
        self.canvas.sync_widget(widget)
        self._refresh_hierarchy()
        self._refresh_preview()
        self.session.notification_service.publish("info", "gui", f"Added widget {widget.id}")

    def duplicate_selected(self) -> None:
        if self.current_document is None:
            return
        created: list[str] = []
        for widget_id in list(self.selected_widget_ids):
            duplicate = self.engine.duplicate_widget(self.current_document, widget_id)
            self.canvas.sync_widget(duplicate)
            created.append(duplicate.id)
        if created:
            self._refresh_hierarchy()
            self._refresh_preview()
            self.session.notification_service.publish("info", "gui", f"Duplicated {len(created)} widget(s)")

    def delete_selected(self) -> None:
        if self.current_document is None:
            return
        deleted = 0
        for widget_id in list(self.selected_widget_ids):
            if widget_id == "root_panel":
                continue
            self.engine.remove_widget(self.current_document, widget_id)
            self.canvas.remove_widget(widget_id)
            deleted += 1
        self.selected_widget_ids = []
        self._refresh_hierarchy()
        self._refresh_preview()
        self._load_widget_into_inspector(None)
        if deleted:
            self.session.notification_service.publish("info", "gui", f"Deleted {deleted} widget(s)")

    def align_selected(self, mode: str) -> None:
        if self.current_document is None or len(self.selected_widget_ids) < 2:
            return
        self.engine.align_widgets(self.current_document, self.selected_widget_ids, mode)
        self._refresh_canvas_items()
        self._refresh_preview()

    def distribute_selected(self, axis: str) -> None:
        if self.current_document is None or len(self.selected_widget_ids) < 3:
            return
        self.engine.distribute_widgets(self.current_document, self.selected_widget_ids, axis)
        self._refresh_canvas_items()
        self._refresh_preview()

    def apply_inspector(self) -> None:
        if self.current_document is None or not self.selected_widget_ids:
            return
        widget = self.current_document.widgets[self.selected_widget_ids[0]]
        new_id = self.widget_id.text().strip() or widget.id
        if new_id != widget.id and new_id in self.current_document.widgets:
            QMessageBox.warning(self, "Duplicate ID", f"Widget id '{new_id}' already exists")
            return
        properties = self._parse_json(self.properties_editor.toPlainText(), default={})
        binding_payload = self._parse_json(self.binding_editor.toPlainText(), default=None)
        if properties is None:
            return
        if binding_payload is None and self.binding_editor.toPlainText().strip():
            return

        if new_id != widget.id:
            self.current_document.widgets.pop(widget.id)
            for other in self.current_document.widgets.values():
                other.children = [new_id if child == widget.id else child for child in other.children]
            self.current_document.root_widgets = [new_id if item == widget.id else item for item in self.current_document.root_widgets]
            widget.id = new_id
            self.current_document.widgets[new_id] = widget
            self.selected_widget_ids = [new_id]

        widget.label = self.widget_label.text().strip()
        widget.visible = self.widget_visible.isChecked()
        widget.bounds.x = self.widget_x.value()
        widget.bounds.y = self.widget_y.value()
        widget.bounds.width = self.widget_w.value()
        widget.bounds.height = self.widget_h.value()
        widget.properties = properties or {}
        if isinstance(binding_payload, dict) and binding_payload:
            widget.binding = GuiBinding(
                kind=str(binding_payload.get("kind", "")),
                source=str(binding_payload.get("source", "")),
                slot_id=str(binding_payload.get("slotId", binding_payload.get("slot_id", ""))),
                role=str(binding_payload.get("role", "")),
                rows=int(binding_payload.get("rows", 0) or 0),
                columns=int(binding_payload.get("columns", 0) or 0),
                metadata=dict(binding_payload.get("metadata") or {}),
            )
        else:
            widget.binding = None
        self._refresh_canvas_items()
        self._refresh_hierarchy()
        self._refresh_preview()
        self.session.notification_service.publish("info", "gui", f"Updated widget {widget.id}")

    def _hierarchy_selected(self, item: QListWidgetItem) -> None:
        widget_id = item.data(Qt.ItemDataRole.UserRole)
        if widget_id:
            self.canvas.select_widget(str(widget_id))

    def _canvas_selection_changed(self, widget_ids: list[str]) -> None:
        self.selected_widget_ids = [str(widget_id) for widget_id in widget_ids]
        self._sync_hierarchy_selection()
        widget = None
        if self.current_document is not None and self.selected_widget_ids:
            widget = self.current_document.widgets.get(self.selected_widget_ids[0])
        self._load_widget_into_inspector(widget)

    def _canvas_positions_changed(self, positions: dict) -> None:
        if self.current_document is None:
            return
        for widget_id, (x, y) in positions.items():
            widget = self.current_document.widgets.get(widget_id)
            if widget is not None:
                widget.bounds.x = x
                widget.bounds.y = y
        self._load_widget_into_inspector(self.current_document.widgets.get(self.selected_widget_ids[0]) if self.selected_widget_ids else None)
        self._refresh_hierarchy()
        self._refresh_preview()

    def _load_widget_into_inspector(self, widget: GuiWidget | None) -> None:
        if widget is None:
            self.selection_label.setText("No selection")
            self.widget_id.setText("")
            self.widget_label.setText("")
            self.widget_visible.setChecked(False)
            self.widget_x.setValue(0)
            self.widget_y.setValue(0)
            self.widget_w.setValue(1)
            self.widget_h.setValue(1)
            self.properties_editor.setPlainText("{}")
            self.binding_editor.setPlainText("")
            return
        self.selection_label.setText(f"{widget.id} ({widget.widget_type})")
        self.widget_id.setText(widget.id)
        self.widget_label.setText(widget.label)
        self.widget_visible.setChecked(widget.visible)
        self.widget_x.setValue(widget.bounds.x)
        self.widget_y.setValue(widget.bounds.y)
        self.widget_w.setValue(widget.bounds.width)
        self.widget_h.setValue(widget.bounds.height)
        self.properties_editor.setPlainText(json.dumps(widget.properties, indent=2))
        binding_payload = None
        if widget.binding is not None:
            binding_payload = {
                "kind": widget.binding.kind,
                "source": widget.binding.source,
                "slotId": widget.binding.slot_id,
                "role": widget.binding.role,
                "rows": widget.binding.rows,
                "columns": widget.binding.columns,
                "metadata": dict(widget.binding.metadata),
            }
        self.binding_editor.setPlainText(json.dumps(binding_payload, indent=2) if binding_payload else "")

    def _refresh_hierarchy(self) -> None:
        self.hierarchy.clear()
        if self.current_document is None:
            return
        for widget in sorted(self.current_document.widgets.values(), key=lambda item: (item.z_index, item.id)):
            item = QListWidgetItem(f"{widget.id} [{widget.widget_type}]")
            item.setData(Qt.ItemDataRole.UserRole, widget.id)
            self.hierarchy.addItem(item)
        self._sync_hierarchy_selection()

    def _sync_hierarchy_selection(self) -> None:
        for index in range(self.hierarchy.count()):
            item = self.hierarchy.item(index)
            widget_id = str(item.data(Qt.ItemDataRole.UserRole))
            item.setSelected(widget_id in self.selected_widget_ids)

    def _screen_type_changed(self, value: str) -> None:
        if self.current_document is not None:
            self.current_document.screen_type = value
            self._refresh_preview()

    def _refresh_canvas_items(self) -> None:
        if self.current_document is None:
            return
        self.canvas.load_document(self.current_document)
        for widget_id in self.selected_widget_ids:
            if widget_id in self.canvas.item_map:
                self.canvas.item_map[widget_id].setSelected(True)

    def _refresh_preview(self) -> None:
        if self.current_document is None:
            self.preview_text.setPlainText("")
            self.validation_list.clear()
            return
        preview = self.engine.preview_payload(self.current_document)
        runtime_payload = self.engine.build_runtime_definition(self.current_document)
        self.preview_text.setPlainText(json.dumps({"preview": preview, "runtime": runtime_payload}, indent=2))
        report = self.engine.validate_document(self.current_document)
        self.validation_list.clear()
        if not report.issues:
            self.validation_list.addItem("No validation issues")
        for issue in report.issues:
            self.validation_list.addItem(f"[{issue.severity}] {issue.widget_id or 'document'} :: {issue.message}")

    def _refresh_all_views(self) -> None:
        if self.current_document is None:
            return
        self.document_name.setText(self.current_document.name)
        self.screen_type.setCurrentText(self.current_document.screen_type)
        self.canvas.load_document(self.current_document)
        self._refresh_hierarchy()
        self._refresh_preview()
        self._load_widget_into_inspector(self.current_document.widgets.get("root_panel"))
        self.refresh_document_list()

    def _parse_json(self, text: str, *, default):
        if not text.strip():
            return default
        try:
            return json.loads(text)
        except json.JSONDecodeError as exc:
            QMessageBox.warning(self, "Invalid JSON", f"Could not parse JSON:\n{exc}")
            return None
