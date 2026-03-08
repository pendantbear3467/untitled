from __future__ import annotations

import copy
import json
from pathlib import Path

from PyQt6.QtCore import QPoint, QRect, QRectF, QSize, Qt, QTimer, pyqtSignal
from PyQt6.QtGui import QColor, QPainter, QPen
from PyQt6.QtWidgets import (
    QCheckBox,
    QComboBox,
    QFileDialog,
    QFormLayout,
    QFrame,
    QGridLayout,
    QGroupBox,
    QHBoxLayout,
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

from asset_studio.gui_studio.engine import GuiStudioEngine
from asset_studio.gui_studio.models import GuiAnchor, GuiBinding, GuiBounds, GuiDocument, GuiWidget
from asset_studio.model_studio.engine import ModelStudioEngine
from asset_studio.model_studio.models import FaceMapping, ModelBone, ModelCube, ModelDocument, Vec3


def _safe_int(text: str, fallback: int = 0) -> int:
    try:
        return int(text)
    except (TypeError, ValueError):
        return fallback


def _safe_json(text: str) -> dict | None:
    stripped = text.strip()
    if not stripped:
        return {}
    try:
        payload = json.loads(stripped)
    except json.JSONDecodeError:
        return None
    return payload if isinstance(payload, dict) else None


class _PanelHeader(QFrame):
    def __init__(self, title: str, help_text: str) -> None:
        super().__init__()
        row = QHBoxLayout(self)
        row.setContentsMargins(8, 6, 8, 6)
        label = QLabel(title)
        label.setObjectName("panelHeaderTitle")
        label.setToolTip(help_text)
        row.addWidget(label)
        hint = QLabel("What does this do?")
        hint.setObjectName("panelHelpHint")
        hint.setToolTip(help_text)
        row.addWidget(hint)
        row.addStretch(1)


def _button(label: str, callback, help_text: str) -> QPushButton:
    button = QPushButton(label)
    button.setToolTip(help_text)
    button.setStatusTip(help_text)
    button.clicked.connect(callback)
    return button


class _GuiCanvas(QWidget):
    place_requested = pyqtSignal(str, int, int)
    widget_selected = pyqtSignal(str)
    widget_moved = pyqtSignal(str, int, int)

    def __init__(self, *, interactive: bool = True) -> None:
        super().__init__()
        self.interactive = interactive
        self.document: GuiDocument | None = None
        self.selected_widget_id: str | None = None
        self.pending_widget_type: str | None = None
        self.zoom = 2.0
        self.theme = "Industrial Slate"
        self._dragging = False
        self._drag_offset = QPoint(0, 0)
        self.setMinimumSize(QSize(420, 320))
        self.setMouseTracking(interactive)
        self.setToolTip("Canvas for click-to-place and drag-to-move GUI widgets. Inventory widgets render with slot previews.")

    def set_document(self, document: GuiDocument | None) -> None:
        self.document = document
        self.update()

    def set_selected(self, widget_id: str | None) -> None:
        self.selected_widget_id = widget_id
        self.update()

    def set_pending_widget(self, widget_type: str | None) -> None:
        self.pending_widget_type = widget_type
        self.update()

    def set_zoom(self, zoom: float) -> None:
        self.zoom = max(0.5, min(zoom, 6.0))
        self.update()

    def set_theme(self, theme: str) -> None:
        self.theme = theme
        self.update()

    def paintEvent(self, event) -> None:  # noqa: N802
        painter = QPainter(self)
        painter.fillRect(self.rect(), QColor("#111723"))
        canvas = self._canvas_rect()
        painter.fillRect(canvas, self._theme_background())
        painter.setPen(QPen(QColor("#40516c"), 1))
        painter.drawRect(canvas)
        self._paint_grid(painter, canvas)

        if self.document is None:
            painter.setPen(QColor("#c7d5ef"))
            painter.drawText(self.rect(), Qt.AlignmentFlag.AlignCenter, "Create or open a GUI document, then choose a widget from the palette.")
            return

        for widget in sorted(self.document.widgets.values(), key=lambda item: (item.id != "root_panel", item.id)):
            self._paint_widget(painter, canvas, widget)

        if self.pending_widget_type and self.interactive:
            painter.setPen(QColor("#9edbff"))
            painter.drawText(canvas.adjusted(8, 8, -8, -8), Qt.AlignmentFlag.AlignTop | Qt.AlignmentFlag.AlignLeft, f"Click canvas to place: {self.pending_widget_type}")

    def mousePressEvent(self, event) -> None:  # noqa: N802
        if not self.interactive or self.document is None:
            return
        canvas = self._canvas_rect()
        if self.pending_widget_type and canvas.contains(event.position().toPoint()):
            x = int((event.position().x() - canvas.x()) / self.zoom)
            y = int((event.position().y() - canvas.y()) / self.zoom)
            self.place_requested.emit(self.pending_widget_type, max(0, x), max(0, y))
            return
        widget_id = self._widget_at(event.position().toPoint())
        if widget_id is not None:
            self.selected_widget_id = widget_id
            self.widget_selected.emit(widget_id)
            widget = self.document.widgets[widget_id]
            widget_rect = self._widget_rect(canvas, widget)
            self._dragging = True
            self._drag_offset = event.position().toPoint() - widget_rect.topLeft()
            self.update()

    def mouseMoveEvent(self, event) -> None:  # noqa: N802
        if not self.interactive or not self._dragging or self.document is None or self.selected_widget_id is None:
            return
        canvas = self._canvas_rect()
        x = int((event.position().x() - canvas.x() - self._drag_offset.x()) / self.zoom)
        y = int((event.position().y() - canvas.y() - self._drag_offset.y()) / self.zoom)
        self.widget_moved.emit(self.selected_widget_id, max(0, x), max(0, y))

    def mouseReleaseEvent(self, event) -> None:  # noqa: N802
        self._dragging = False
        super().mouseReleaseEvent(event)

    def _canvas_rect(self) -> QRect:
        if self.document is None:
            return self.rect().adjusted(32, 32, -32, -32)
        width = int(self.document.width * self.zoom)
        height = int(self.document.height * self.zoom)
        left = max(24, (self.width() - width) // 2)
        top = max(24, (self.height() - height) // 2)
        return QRect(left, top, width, height)

    def _paint_grid(self, painter: QPainter, canvas: QRect) -> None:
        painter.setPen(QPen(QColor("#213046"), 1))
        step = max(8, int(16 * self.zoom))
        for x in range(canvas.left(), canvas.right(), step):
            painter.drawLine(x, canvas.top(), x, canvas.bottom())
        for y in range(canvas.top(), canvas.bottom(), step):
            painter.drawLine(canvas.left(), y, canvas.right(), y)

    def _paint_widget(self, painter: QPainter, canvas: QRect, widget: GuiWidget) -> None:
        rect = self._widget_rect(canvas, widget)
        selected = widget.id == self.selected_widget_id
        painter.save()
        painter.setPen(QPen(QColor("#c9dbff") if selected else QColor("#8ca6d6"), 2 if selected else 1))
        painter.setBrush(self._widget_fill(widget.widget_type, selected))
        painter.drawRect(rect)
        painter.setPen(QColor("#ffffff"))
        painter.drawText(rect.adjusted(4, 4, -4, -4), Qt.AlignmentFlag.AlignTop | Qt.AlignmentFlag.AlignLeft, widget.label or widget.id)
        if widget.widget_type in {"player_inventory_grid", "hotbar", "armor_slots", "offhand_slot", "inventory_slot", "machine_slot", "slot"}:
            self._paint_slot_widget(painter, rect, widget)
        elif widget.widget_type == "progress":
            self._paint_progress_widget(painter, rect, widget)
        painter.restore()

    def _paint_slot_widget(self, painter: QPainter, rect: QRect, widget: GuiWidget) -> None:
        if widget.widget_type == "player_inventory_grid":
            rows = int(widget.properties.get("rows", 3))
            columns = int(widget.properties.get("columns", 9))
        elif widget.widget_type == "hotbar":
            rows = 1
            columns = int(widget.properties.get("columns", 9))
        elif widget.widget_type == "armor_slots":
            rows = int(widget.properties.get("rows", 4))
            columns = 1
        else:
            rows = 1
            columns = 1
        slot_size = max(10, min(rect.width() // max(columns, 1), rect.height() // max(rows, 1)) - 2)
        origin_x = rect.x() + 6
        origin_y = rect.y() + 18
        painter.setPen(QPen(QColor("#f0d593"), 1))
        for row in range(rows):
            for col in range(columns):
                slot_rect = QRect(origin_x + col * (slot_size + 2), origin_y + row * (slot_size + 2), slot_size, slot_size)
                if slot_rect.right() < rect.right() - 4 and slot_rect.bottom() < rect.bottom() - 4:
                    painter.drawRect(slot_rect)

    def _paint_progress_widget(self, painter: QPainter, rect: QRect, widget: GuiWidget) -> None:
        value = float(widget.properties.get("value", 0))
        maximum = max(float(widget.properties.get("max", 100)), 1.0)
        fill = QRect(rect.x() + 4, rect.bottom() - 12, int((rect.width() - 8) * min(value / maximum, 1.0)), 8)
        painter.fillRect(fill, QColor("#8bd97b"))

    def _widget_rect(self, canvas: QRect, widget: GuiWidget) -> QRect:
        return QRect(
            int(canvas.x() + widget.bounds.x * self.zoom),
            int(canvas.y() + widget.bounds.y * self.zoom),
            max(12, int(widget.bounds.width * self.zoom)),
            max(12, int(widget.bounds.height * self.zoom)),
        )

    def _widget_at(self, point: QPoint) -> str | None:
        if self.document is None:
            return None
        canvas = self._canvas_rect()
        for widget in sorted(self.document.widgets.values(), key=lambda item: (item.id == "root_panel", item.bounds.width * item.bounds.height)):
            if self._widget_rect(canvas, widget).contains(point):
                return widget.id
        return None

    def _widget_fill(self, widget_type: str, selected: bool) -> QColor:
        if widget_type == "panel":
            return QColor("#304155" if selected else "#243345")
        if widget_type in {"player_inventory_grid", "hotbar", "armor_slots", "offhand_slot", "inventory_slot", "machine_slot", "slot"}:
            return QColor("#4e4030" if selected else "#392d21")
        if widget_type == "button":
            return QColor("#415b82")
        if widget_type == "label":
            return QColor("#2f2f48")
        if widget_type == "progress":
            return QColor("#2c4f34")
        return QColor("#2c3648")

    def _theme_background(self) -> QColor:
        return {
            "Extreme Dark": QColor("#182230"),
            "Arcane Brass": QColor("#2f271d"),
            "Industrial Slate": QColor("#1f2836"),
        }.get(self.theme, QColor("#1f2836"))


class GuiStudioPanel(QWidget):
    status_message = pyqtSignal(str)
    notifications = pyqtSignal(str)

    def __init__(self, engine: GuiStudioEngine) -> None:
        super().__init__()
        self.engine = engine
        self.current_document: GuiDocument | None = None
        self.current_path: Path | None = None
        self.selected_widget_id: str | None = None
        self.pending_widget_type: str | None = None
        self._updating = False
        self._dirty = False

        root = QVBoxLayout(self)
        root.setContentsMargins(0, 0, 0, 0)
        root.addWidget(_PanelHeader("GUI Studio", "Build mod-loadable GUI definitions with palette placement, hierarchy editing, property inspection, validation, and preview."))
        root.addWidget(self._build_top_actions())

        split = QSplitter(Qt.Orientation.Horizontal)
        split.addWidget(self._build_left_panel())
        split.addWidget(self._build_center_panel())
        split.addWidget(self._build_right_panel())
        split.setSizes([310, 760, 380])
        root.addWidget(split)
        self._refresh_document_list()
        self._refresh_surface()

    def set_engine(self, engine: GuiStudioEngine) -> None:
        self.engine = engine
        self.current_document = None
        self.current_path = None
        self.selected_widget_id = None
        self._dirty = False
        self._refresh_document_list()
        self._refresh_surface()

    def _build_top_actions(self) -> QWidget:
        host = QWidget()
        row = QHBoxLayout(host)
        row.setContentsMargins(8, 0, 8, 0)
        self.document_selector = QComboBox()
        self.document_selector.setToolTip("Saved GUI documents in the current workspace.")
        self.document_selector.currentTextChanged.connect(self._open_selected_document)
        row.addWidget(QLabel("Document"))
        row.addWidget(self.document_selector)
        row.addWidget(_button("New Document", self.new_document, "Create a new GUI document with a root panel."))
        row.addWidget(_button("Open", self.open_document_dialog, "Open a saved GUI document from disk."))
        row.addWidget(_button("Save", self.save_current, "Save the current GUI document."))
        row.addWidget(_button("Export Current", self.export_current, "Export the current GUI document to a chosen file path."))
        row.addWidget(_button("Export Runtime", self.export_runtime_current, "Export the current GUI document into the deterministic mod-facing runtime path."))
        row.addWidget(_button("Validate Current", self.validate_current, "Validate the current GUI definition against the authoritative backend validator."))
        row.addWidget(_button("Preview Current", self.preview_current, "Refresh the preview payload and selection summary."))
        row.addStretch(1)
        return host

    def _build_left_panel(self) -> QWidget:
        box = QWidget()
        layout = QVBoxLayout(box)

        palette = QGroupBox("Component Palette")
        palette.setToolTip("Choose a widget type, then click the canvas to place it. These widgets export into mod-loadable GUI definitions.")
        palette_layout = QGridLayout(palette)
        palette_buttons = [
            ("Panel", "panel"),
            ("Label", "label"),
            ("Button", "button"),
            ("Image", "image"),
            ("Progress", "progress"),
            ("Machine Slot", "machine_slot"),
        ]
        for index, (label, widget_type) in enumerate(palette_buttons):
            palette_layout.addWidget(_button(label, lambda checked=False, kind=widget_type: self.prepare_widget(kind), f"Prepare a {label} widget for placement on the canvas."), index // 2, index % 2)

        inventory = QGroupBox("Inventory Widgets")
        inventory.setToolTip("Visual authoring helpers for player inventory, hotbar, armor, offhand, and slot definitions.")
        inventory_layout = QGridLayout(inventory)
        inventory_buttons = [
            ("Player Grid", "player_inventory_grid"),
            ("Hotbar", "hotbar"),
            ("Armor Slots", "armor_slots"),
            ("Offhand", "offhand_slot"),
            ("Inventory Slot", "inventory_slot"),
            ("Add Inventory Section", "inventory_section"),
        ]
        for index, (label, widget_type) in enumerate(inventory_buttons):
            if widget_type == "inventory_section":
                callback = self.add_inventory_section
                help_text = "Place a full player inventory section with inventory grid, hotbar, armor slots, and offhand widget set."
            else:
                callback = lambda checked=False, kind=widget_type: self.prepare_widget(kind)
                help_text = f"Prepare a {label} widget for placement."
            inventory_layout.addWidget(_button(label, callback, help_text), index // 2, index % 2)

        hierarchy = QGroupBox("Hierarchy / Outliner")
        hierarchy_layout = QVBoxLayout(hierarchy)
        helper = QLabel("Selected widget state stays synchronized between the canvas, hierarchy, and inspector.")
        helper.setWordWrap(True)
        helper.setObjectName("panelHelpHint")
        hierarchy_layout.addWidget(helper)
        self.hierarchy = QListWidget()
        self.hierarchy.setToolTip("Widget hierarchy for the current GUI document. Double-click to select a widget.")
        self.hierarchy.itemDoubleClicked.connect(self._select_hierarchy_item)
        hierarchy_layout.addWidget(self.hierarchy)
        actions = QGridLayout()
        actions.addWidget(_button("Duplicate", self.duplicate_selected, "Duplicate the selected widget and offset it slightly."), 0, 0)
        actions.addWidget(_button("Delete", self.delete_selected, "Delete the selected widget after confirmation."), 0, 1)
        actions.addWidget(_button("Add Slot", self.add_slot, "Add a single inventory or machine slot near the current selection."), 1, 0)
        actions.addWidget(_button("Machine Slot", self.add_machine_slot, "Add a machine slot widget with export-friendly defaults."), 1, 1)
        hierarchy_layout.addLayout(actions)

        layout.addWidget(palette)
        layout.addWidget(inventory)
        layout.addWidget(hierarchy)
        return box

    def _build_center_panel(self) -> QWidget:
        box = QWidget()
        layout = QVBoxLayout(box)

        controls = QHBoxLayout()
        self.screen_selector = QComboBox()
        self.screen_selector.addItems(["generic", "machine", "overlay", "menu", "player"])
        self.screen_selector.setToolTip("Screen type stored in the document contract and used by validation/export.")
        self.screen_selector.currentTextChanged.connect(self._update_document_shape)
        self.theme_selector = QComboBox()
        self.theme_selector.addItems(["Industrial Slate", "Extreme Dark", "Arcane Brass"])
        self.theme_selector.setToolTip("Theme tint for the live canvas and preview surface.")
        self.theme_selector.currentTextChanged.connect(self._update_theme)
        self.resolution_selector = QComboBox()
        self.resolution_selector.addItems(["176x166", "256x180", "320x240", "384x256"])
        self.resolution_selector.setToolTip("Canvas resolution stored in the GUI document and reflected in preview payloads.")
        self.resolution_selector.currentTextChanged.connect(self._update_document_shape)
        self.zoom_selector = QComboBox()
        self.zoom_selector.addItems(["100%", "150%", "200%", "250%", "300%"])
        self.zoom_selector.setCurrentText("200%")
        self.zoom_selector.setToolTip("Canvas zoom for authoring. Zoom only changes the editor view, not exported bounds.")
        self.zoom_selector.currentTextChanged.connect(self._update_zoom)
        for label, widget in [
            ("Screen", self.screen_selector),
            ("Theme", self.theme_selector),
            ("Resolution", self.resolution_selector),
            ("Zoom", self.zoom_selector),
        ]:
            controls.addWidget(QLabel(label))
            controls.addWidget(widget)
        controls.addStretch(1)
        layout.addLayout(controls)

        helper = QLabel("Click a palette button, then click the canvas to place a widget. Drag widgets to reposition them. Inventory widgets show their slot structure visually and export as GUI definitions.")
        helper.setWordWrap(True)
        helper.setObjectName("panelHelpHint")
        layout.addWidget(helper)

        self.canvas = _GuiCanvas(interactive=True)
        self.canvas.place_requested.connect(self._handle_place_request)
        self.canvas.widget_selected.connect(self.select_widget)
        self.canvas.widget_moved.connect(self._move_widget)
        layout.addWidget(self.canvas, 1)

        align = QGridLayout()
        align.addWidget(_button("Align Left", lambda: self.align_selected("left"), "Align the selected widget to the left edge of the canvas."), 0, 0)
        align.addWidget(_button("Align Right", lambda: self.align_selected("right"), "Align the selected widget to the right edge of the canvas."), 0, 1)
        align.addWidget(_button("Align Top", lambda: self.align_selected("top"), "Align the selected widget to the top of the canvas."), 0, 2)
        align.addWidget(_button("Align Bottom", lambda: self.align_selected("bottom"), "Align the selected widget to the bottom of the canvas."), 1, 0)
        align.addWidget(_button("Center", lambda: self.align_selected("center"), "Center the selected widget horizontally and vertically."), 1, 1)
        align.addWidget(_button("Center X", lambda: self.align_selected("center_x"), "Center the selected widget horizontally."), 1, 2)
        align.addWidget(_button("Distribute H", lambda: self.distribute_widgets("horizontal"), "Distribute root widgets horizontally across the canvas."), 2, 0)
        align.addWidget(_button("Distribute V", lambda: self.distribute_widgets("vertical"), "Distribute root widgets vertically across the canvas."), 2, 1)
        align.addWidget(_button("Zoom to Fit", self.zoom_to_fit, "Fit the canvas to a readable zoom level."), 2, 2)
        align.addWidget(_button("Center Selection", self.center_selection, "Center the selected widget within the canvas."), 3, 0)
        align.addWidget(_button("Reset View", self.reset_view, "Reset zoom and clear pending placement state."), 3, 1)
        layout.addLayout(align)
        return box

    def _build_right_panel(self) -> QWidget:
        panel = QTabWidget()

        inspector = QWidget()
        form = QFormLayout(inspector)
        self.widget_id = QLabel("None")
        self.widget_type = QLabel("None")
        self.widget_label = QPlainTextEdit()
        self.widget_label.setFixedHeight(50)
        self.widget_label.setToolTip("Editable widget label shown in previews and exported definitions.")
        self.widget_label.textChanged.connect(self._apply_inspector_changes)
        self.x_spin = self._spin(self._apply_inspector_changes, "Left position in GUI pixels.")
        self.y_spin = self._spin(self._apply_inspector_changes, "Top position in GUI pixels.")
        self.width_spin = self._spin(self._apply_inspector_changes, "Widget width in GUI pixels.")
        self.height_spin = self._spin(self._apply_inspector_changes, "Widget height in GUI pixels.")
        self.visible_check = QCheckBox("Visible")
        self.visible_check.setToolTip("Hide or show the selected widget in preview/export payloads.")
        self.visible_check.toggled.connect(self._apply_inspector_changes)
        self.anchor_json = QPlainTextEdit()
        self.anchor_json.setFixedHeight(80)
        self.anchor_json.setToolTip("Anchor/layout metadata as JSON. Invalid JSON is ignored instead of crashing the page.")
        self.anchor_json.textChanged.connect(self._apply_inspector_changes)
        self.binding_json = QPlainTextEdit()
        self.binding_json.setFixedHeight(96)
        self.binding_json.setToolTip("Inventory/runtime binding metadata as JSON. Invalid JSON is ignored instead of crashing the page.")
        self.binding_json.textChanged.connect(self._apply_inspector_changes)
        self.properties = QPlainTextEdit()
        self.properties.setToolTip("Widget properties as JSON. Invalid JSON is ignored instead of crashing the page.")
        self.properties.textChanged.connect(self._apply_inspector_changes)
        self.properties.setFixedHeight(120)
        form.addRow("Widget ID", self.widget_id)
        form.addRow("Widget Type", self.widget_type)
        form.addRow("Label", self.widget_label)
        form.addRow("X", self.x_spin)
        form.addRow("Y", self.y_spin)
        form.addRow("Width", self.width_spin)
        form.addRow("Height", self.height_spin)
        form.addRow("Visibility", self.visible_check)
        form.addRow("Anchors", self.anchor_json)
        form.addRow("Binding", self.binding_json)
        form.addRow("Properties", self.properties)

        preview = QWidget()
        preview_layout = QVBoxLayout(preview)
        preview_layout.addWidget(QLabel("Live Preview"))
        preview_note = QLabel("The preview reflects the current selection and exports through the same backend document contract used by GUI Studio.")
        preview_note.setWordWrap(True)
        preview_note.setObjectName("panelHelpHint")
        preview_layout.addWidget(preview_note)
        self.preview_canvas = _GuiCanvas(interactive=False)
        preview_layout.addWidget(self.preview_canvas, 1)
        self.preview_summary = QPlainTextEdit()
        self.preview_summary.setReadOnly(True)
        self.preview_summary.setToolTip("Preview payload summary for the current GUI document.")
        preview_layout.addWidget(self.preview_summary)

        reports = QWidget()
        reports_layout = QVBoxLayout(reports)
        reports_layout.addWidget(QLabel("Validation / Export Report"))
        self.report = QPlainTextEdit()
        self.report.setReadOnly(True)
        self.report.setToolTip("Validation results, export targets, and other safe workflow messages for GUI Studio.")
        reports_layout.addWidget(self.report)

        panel.addTab(inspector, "Inspector")
        panel.addTab(preview, "Preview")
        panel.addTab(reports, "Report")
        return panel

    def _spin(self, callback, help_text: str) -> QSpinBox:
        spin = QSpinBox()
        spin.setRange(0, 4096)
        spin.setToolTip(help_text)
        spin.valueChanged.connect(callback)
        return spin

    def new_document(self) -> None:
        name = f"screen_{len(self.engine.list_documents()) + 1}"
        self.current_document = self.engine.create_document(name, screen_type=self.screen_selector.currentText() or "generic")
        self.current_path = None
        self.selected_widget_id = "root_panel"
        self._dirty = True
        self._refresh_document_list(select=name)
        self._refresh_surface()
        self.notifications.emit(f"Created GUI document {name}")
        self.status_message.emit(f"GUI document ready: {name}")

    def open_document_dialog(self) -> None:
        selected, _ = QFileDialog.getOpenFileName(self, "Open GUI document", str(self.engine.root), "GUI Documents (*.gui.json)")
        if not selected:
            self.notifications.emit("Open GUI document cancelled")
            return
        self.open_document(Path(selected))

    def open_document(self, path_or_name: str | Path) -> None:
        loaded = self.engine.load_document(path_or_name)
        self.current_document = loaded.document
        self.current_path = Path(path_or_name) if isinstance(path_or_name, Path) else self.engine.document_path(str(path_or_name))
        self.selected_widget_id = next(iter(self.current_document.widgets), None)
        self._dirty = False
        self._refresh_document_list(select=self.current_document.name)
        self._refresh_surface()
        for warning in loaded.warnings:
            self.notifications.emit(f"GUI load warning: {warning}")
        for error in loaded.errors:
            self.notifications.emit(f"GUI load error: {error}")
        self.status_message.emit(f"Opened GUI document: {self.current_document.name}")

    def save_current(self) -> bool:
        if self.current_document is None:
            self.notifications.emit("Create or open a GUI document before saving")
            return False
        saved_path = self.engine.save_document(self.current_document)
        self.current_path = saved_path
        self._dirty = False
        self._refresh_document_list(select=self.current_document.name)
        self.report.setPlainText(f"Saved GUI document to {saved_path}")
        self.notifications.emit(f"Saved GUI document {self.current_document.name}")
        return True

    def save_all(self) -> int:
        return 1 if self.save_current() else 0

    def export_current(self) -> None:
        if self.current_document is None:
            self.notifications.emit("No GUI document to export")
            return
        selected, _ = QFileDialog.getSaveFileName(self, "Export GUI document", str(self.engine.document_path(self.current_document.name)), "GUI Documents (*.gui.json)")
        if not selected:
            self.notifications.emit("Export cancelled")
            return
        target = self.engine.export_document(self.current_document, Path(selected))
        self.report.setPlainText(f"Exported GUI document to {target}")
        self.notifications.emit(f"Exported GUI document to {target.name}")

    def export_runtime_current(self) -> None:
        if self.current_document is None:
            self.notifications.emit("No GUI document to export")
            return
        target = self.engine.export_runtime_document(self.current_document)
        runtime = self.engine.build_runtime_definition(self.current_document)
        self.report.setPlainText(
            f"Exported runtime GUI to {target}\nResource ID: {runtime['resourceId']}\nInventory bindings: {len(runtime['inventoryBindings'])}"
        )
        self.notifications.emit(f"Exported runtime GUI definition to {target.name}")

    def validate_current(self) -> None:
        if self.current_document is None:
            self.notifications.emit("No GUI document to validate")
            return
        report = self.engine.validate_document(self.current_document)
        if report.issues:
            text = "\n".join(f"[{issue.severity.upper()}] {issue.widget_id or 'document'}: {issue.message}" for issue in report.issues)
        else:
            text = "No validation issues detected. This GUI document is ready for export through the backend contract."
        self.report.setPlainText(text)
        self.notifications.emit("Validated current GUI document")

    def preview_current(self) -> None:
        if self.current_document is None:
            self.notifications.emit("No GUI document to preview")
            return
        payload = self.engine.preview_payload(self.current_document)
        runtime = self.engine.build_runtime_definition(self.current_document)
        selected = self.current_document.widgets.get(self.selected_widget_id) if self.selected_widget_id else None
        self.preview_summary.setPlainText(
            json.dumps(
                {
                    "document": payload["document"],
                    "screenType": payload["screenType"],
                    "canvas": payload["canvas"],
                    "selected": selected.id if selected else None,
                    "widgetCount": len(payload["widgets"]),
                    "inventoryBindings": len(runtime["inventoryBindings"]),
                    "runtimeResourceId": runtime["resourceId"],
                    "runtimePath": str(self.engine.runtime_export_path(self.current_document)),
                },
                indent=2,
            )
        )
        self.notifications.emit("Preview refreshed for current GUI document")

    def prepare_widget(self, widget_type: str) -> None:
        self.pending_widget_type = widget_type
        self.canvas.set_pending_widget(widget_type)
        self.status_message.emit(f"Click the canvas to place: {widget_type}")

    def add_inventory_section(self) -> None:
        if self.current_document is None:
            self.new_document()
        base_x = 8
        base_y = 18
        for widget_type, dx, dy in [
            ("player_inventory_grid", 0, 0),
            ("hotbar", 0, 58),
            ("armor_slots", 150, 0),
            ("offhand_slot", 150, 82),
        ]:
            self._add_widget(widget_type, base_x + dx, base_y + dy)
        self.notifications.emit("Added full inventory section")

    def add_slot(self) -> None:
        self._add_widget("inventory_slot", 16, 16)

    def add_machine_slot(self) -> None:
        self._add_widget("machine_slot", 16, 16)

    def duplicate_selected(self) -> None:
        widget = self._selected_widget()
        if widget is None or self.current_document is None:
            self.notifications.emit("Select a widget before duplicating")
            return
        if widget.id == "root_panel":
            self.notifications.emit("The root panel cannot be duplicated")
            return
        duplicate = self.engine.duplicate_widget(self.current_document, widget.id)
        self.select_widget(duplicate.id)
        self._mark_dirty("Duplicated selected widget")

    def delete_selected(self) -> None:
        if self.current_document is None or self.selected_widget_id is None:
            self.notifications.emit("Select a widget before deleting")
            return
        if self.selected_widget_id == "root_panel":
            self.notifications.emit("The root panel cannot be deleted")
            return
        result = QMessageBox.question(
            self,
            "Delete Widget",
            f"Delete widget {self.selected_widget_id}?",
            QMessageBox.StandardButton.Yes | QMessageBox.StandardButton.No,
        )
        if result != QMessageBox.StandardButton.Yes:
            return
        self.engine.remove_widget(self.current_document, self.selected_widget_id)
        self.selected_widget_id = "root_panel" if "root_panel" in self.current_document.widgets else None
        self._mark_dirty("Deleted selected widget")

    def align_selected(self, mode: str) -> None:
        widget = self._selected_widget()
        doc = self.current_document
        if widget is None or doc is None:
            self.notifications.emit("Select a widget before aligning")
            return
        if mode == "left":
            widget.bounds.x = 0
        elif mode == "right":
            widget.bounds.x = max(0, doc.width - widget.bounds.width)
        elif mode == "top":
            widget.bounds.y = 0
        elif mode == "bottom":
            widget.bounds.y = max(0, doc.height - widget.bounds.height)
        elif mode == "center":
            widget.bounds.x = max(0, (doc.width - widget.bounds.width) // 2)
            widget.bounds.y = max(0, (doc.height - widget.bounds.height) // 2)
        elif mode == "center_x":
            widget.bounds.x = max(0, (doc.width - widget.bounds.width) // 2)
        self._mark_dirty(f"Aligned {widget.id}")

    def distribute_widgets(self, axis: str) -> None:
        if self.current_document is None:
            self.notifications.emit("No GUI document to distribute")
            return
        widget_ids = [widget.id for widget in self.current_document.widgets.values() if widget.id != "root_panel"]
        if len(widget_ids) < 3:
            self.notifications.emit("Add at least three widgets before distributing")
            return
        self.engine.distribute_widgets(self.current_document, widget_ids, axis)
        self._mark_dirty(f"Distributed widgets {axis}")

    def zoom_to_fit(self) -> None:
        self.zoom_selector.setCurrentText("150%")

    def center_selection(self) -> None:
        self.align_selected("center")

    def reset_view(self) -> None:
        self.pending_widget_type = None
        self.canvas.set_pending_widget(None)
        self.zoom_selector.setCurrentText("200%")
        self.notifications.emit("Reset GUI canvas view")

    def select_widget(self, widget_id: str) -> None:
        self.selected_widget_id = widget_id
        self._refresh_surface()

    def _handle_place_request(self, widget_type: str, x: int, y: int) -> None:
        self._add_widget(widget_type, x, y)
        self.pending_widget_type = None
        self.canvas.set_pending_widget(None)

    def _add_widget(self, widget_type: str, x: int, y: int) -> None:
        if self.current_document is None:
            self.new_document()
        assert self.current_document is not None
        widget = self.engine.create_widget(widget_type, widget_id=self._unique_widget_id(widget_type), x=x, y=y)
        self.engine.add_widget(self.current_document, widget, parent_id="root_panel")
        self.select_widget(widget.id)
        self._mark_dirty(f"Added {widget_type}")

    def _widget_dimensions(self, widget_type: str) -> tuple[int, int]:
        return {
            "panel": (120, 60),
            "label": (80, 20),
            "button": (90, 20),
            "image": (32, 32),
            "progress": (120, 20),
            "player_inventory_grid": (144, 54),
            "hotbar": (144, 18),
            "armor_slots": (18, 72),
            "offhand_slot": (18, 18),
            "inventory_slot": (18, 18),
            "machine_slot": (18, 18),
        }.get(widget_type, (32, 18))

    def _default_properties(self, widget_type: str) -> dict:
        properties: dict[str, object] = {}
        for schema in self.engine.property_schemas.get(widget_type, []):
            properties[schema.name] = schema.default
        if widget_type == "progress":
            properties.update({"value": 40, "max": 100})
        return properties

    def _move_widget(self, widget_id: str, x: int, y: int) -> None:
        if self.current_document is None or widget_id not in self.current_document.widgets:
            return
        widget = self.current_document.widgets[widget_id]
        widget.bounds.x = x
        widget.bounds.y = y
        self._mark_dirty(f"Moved {widget_id}")

    def _selected_widget(self) -> GuiWidget | None:
        if self.current_document is None or self.selected_widget_id is None:
            return None
        return self.current_document.widgets.get(self.selected_widget_id)

    def _select_hierarchy_item(self, item: QListWidgetItem) -> None:
        widget_id = item.data(Qt.ItemDataRole.UserRole)
        if isinstance(widget_id, str):
            self.select_widget(widget_id)

    def _apply_inspector_changes(self) -> None:
        if self._updating:
            return
        widget = self._selected_widget()
        if widget is None:
            return
        widget.label = self.widget_label.toPlainText().strip()
        widget.bounds.x = self.x_spin.value()
        widget.bounds.y = self.y_spin.value()
        widget.bounds.width = max(1, self.width_spin.value())
        widget.bounds.height = max(1, self.height_spin.value())
        widget.visible = self.visible_check.isChecked()

        anchor_payload = _safe_json(self.anchor_json.toPlainText())
        if anchor_payload is not None:
            widget.anchor = GuiAnchor(
                left=anchor_payload.get("left"),
                top=anchor_payload.get("top"),
                right=anchor_payload.get("right"),
                bottom=anchor_payload.get("bottom"),
                center_x=anchor_payload.get("centerX"),
                center_y=anchor_payload.get("centerY"),
            )

        binding_payload = _safe_json(self.binding_json.toPlainText())
        if binding_payload is not None:
            if binding_payload:
                widget.binding = GuiBinding(
                    kind=str(binding_payload.get("kind", widget.widget_type)),
                    source=str(binding_payload.get("source", "")),
                    slot_id=str(binding_payload.get("slotId", "")),
                    role=str(binding_payload.get("role", "")),
                    rows=int(binding_payload.get("rows", 0) or 0),
                    columns=int(binding_payload.get("columns", 0) or 0),
                    metadata=dict(binding_payload.get("metadata") or {}),
                )
            else:
                widget.binding = None

        properties = _safe_json(self.properties.toPlainText())
        if properties is not None:
            widget.properties = properties
        self._mark_dirty(f"Updated {widget.id}")

    def _update_document_shape(self) -> None:
        if self.current_document is None:
            return
        self.current_document.screen_type = self.screen_selector.currentText() or self.current_document.screen_type
        resolution = self.resolution_selector.currentText()
        if "x" in resolution:
            width, height = resolution.split("x", maxsplit=1)
            self.current_document.width = _safe_int(width, self.current_document.width)
            self.current_document.height = _safe_int(height, self.current_document.height)
            root = self.current_document.widgets.get("root_panel")
            if root is not None:
                root.bounds.width = self.current_document.width
                root.bounds.height = self.current_document.height
        self._mark_dirty("Updated GUI resolution or screen type")

    def _update_theme(self) -> None:
        self.canvas.set_theme(self.theme_selector.currentText())
        self.preview_canvas.set_theme(self.theme_selector.currentText())
        if self.current_document is not None:
            self.current_document.metadata["theme"] = self.theme_selector.currentText()
        self.preview_current()

    def _update_zoom(self) -> None:
        zoom = _safe_int(self.zoom_selector.currentText().replace("%", ""), 200) / 100.0
        self.canvas.set_zoom(zoom)

    def _refresh_document_list(self, select: str | None = None) -> None:
        self.document_selector.blockSignals(True)
        current = select or (self.current_document.name if self.current_document is not None else "")
        self.document_selector.clear()
        self.document_selector.addItem("Open saved document...")
        for name in self.engine.list_documents():
            self.document_selector.addItem(name)
        if current:
            index = self.document_selector.findText(current)
            if index >= 0:
                self.document_selector.setCurrentIndex(index)
        self.document_selector.blockSignals(False)

    def _open_selected_document(self, name: str) -> None:
        if not name or name == "Open saved document...":
            return
        if self.current_document is not None and self.current_document.name == name:
            return
        self.open_document(name)

    def _refresh_surface(self) -> None:
        self.canvas.set_document(self.current_document)
        self.preview_canvas.set_document(self.current_document)
        self.canvas.set_selected(self.selected_widget_id)
        self.preview_canvas.set_selected(self.selected_widget_id)
        self._refresh_hierarchy()
        self._refresh_inspector()
        if self.current_document is None:
            self.preview_summary.setPlainText("Create or open a GUI document to preview exportable widget payloads.")
            self.report.setPlainText("Validation and export reports will appear here.")
        else:
            self.preview_current()

    def _refresh_hierarchy(self) -> None:
        self.hierarchy.clear()
        if self.current_document is None:
            self.hierarchy.addItem("Create a document to see widget hierarchy")
            return
        for widget in sorted(self.current_document.widgets.values(), key=lambda item: (item.id != "root_panel", item.id)):
            text = f"{widget.id} [{widget.widget_type}]"
            item = QListWidgetItem(text)
            item.setData(Qt.ItemDataRole.UserRole, widget.id)
            if widget.id == self.selected_widget_id:
                item.setSelected(True)
            self.hierarchy.addItem(item)

    def _refresh_inspector(self) -> None:
        self._updating = True
        widget = self._selected_widget()
        if widget is None:
            self.widget_id.setText("None")
            self.widget_type.setText("None")
            self.widget_label.setPlainText("")
            self.x_spin.setValue(0)
            self.y_spin.setValue(0)
            self.width_spin.setValue(0)
            self.height_spin.setValue(0)
            self.visible_check.setChecked(False)
            self.anchor_json.setPlainText("{}")
            self.binding_json.setPlainText("{}")
            self.properties.setPlainText("{}")
        else:
            self.widget_id.setText(widget.id)
            self.widget_type.setText(widget.widget_type)
            self.widget_label.setPlainText(widget.label)
            self.x_spin.setValue(widget.bounds.x)
            self.y_spin.setValue(widget.bounds.y)
            self.width_spin.setValue(widget.bounds.width)
            self.height_spin.setValue(widget.bounds.height)
            self.visible_check.setChecked(widget.visible)
            self.anchor_json.setPlainText(json.dumps({
                "left": widget.anchor.left,
                "top": widget.anchor.top,
                "right": widget.anchor.right,
                "bottom": widget.anchor.bottom,
                "centerX": widget.anchor.center_x,
                "centerY": widget.anchor.center_y,
            }, indent=2))
            binding_payload = {}
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
            self.binding_json.setPlainText(json.dumps(binding_payload, indent=2))
            self.properties.setPlainText(json.dumps(widget.properties, indent=2))
        self._updating = False

    def _unique_widget_id(self, prefix: str) -> str:
        assert self.current_document is not None
        base = prefix.lower()
        index = 1
        candidate = base
        while candidate in self.current_document.widgets:
            index += 1
            candidate = f"{base}_{index}"
        return candidate

    def _mark_dirty(self, message: str) -> None:
        self._dirty = True
        self._refresh_surface()
        self.notifications.emit(message)


class _ModelViewport(QWidget):
    cube_selected = pyqtSignal(str)
    cube_moved = pyqtSignal(str, float, float)

    def __init__(self, *, interactive: bool = True) -> None:
        super().__init__()
        self.interactive = interactive
        self.document: ModelDocument | None = None
        self.selected_cube_id: str | None = None
        self.zoom = 12.0
        self.camera_mode = "front"
        self._dragging = False
        self._drag_offset = QPoint(0, 0)
        self.setMinimumSize(QSize(420, 320))
        self.setToolTip("Viewport for model cubes. Click to select a cube and drag to adjust its X/Y origin in the simplified authoring view.")

    def set_document(self, document: ModelDocument | None) -> None:
        self.document = document
        self.update()

    def set_selected_cube(self, cube_id: str | None) -> None:
        self.selected_cube_id = cube_id
        self.update()

    def set_zoom(self, zoom: float) -> None:
        self.zoom = max(4.0, min(zoom, 32.0))
        self.update()

    def set_camera_mode(self, camera_mode: str) -> None:
        self.camera_mode = camera_mode
        self.update()

    def paintEvent(self, event) -> None:  # noqa: N802
        painter = QPainter(self)
        painter.fillRect(self.rect(), QColor("#101824"))
        frame = self.rect().adjusted(24, 24, -24, -24)
        painter.setPen(QPen(QColor("#3b4e6e"), 1))
        painter.drawRect(frame)
        painter.drawText(frame.adjusted(8, 8, -8, -8), Qt.AlignmentFlag.AlignTop | Qt.AlignmentFlag.AlignLeft, f"Camera: {self.camera_mode.title()}")
        if self.document is None:
            painter.setPen(QColor("#d8e5ff"))
            painter.drawText(frame, Qt.AlignmentFlag.AlignCenter, "Create or open a model document, then add a cube or part.")
            return
        for cube in self.document.cubes.values():
            rect = self._cube_rect(frame, cube)
            selected = cube.id == self.selected_cube_id
            painter.setPen(QPen(QColor("#d5e5ff") if selected else QColor("#8ba6d4"), 2 if selected else 1))
            painter.setBrush(QColor("#44638a") if selected else QColor("#30475e"))
            painter.drawRect(rect)
            painter.drawText(rect.adjusted(4, 4, -4, -4), Qt.AlignmentFlag.AlignTop | Qt.AlignmentFlag.AlignLeft, cube.id)

    def mousePressEvent(self, event) -> None:  # noqa: N802
        if not self.interactive or self.document is None:
            return
        cube_id = self._cube_at(event.position().toPoint())
        if cube_id is None:
            return
        self.selected_cube_id = cube_id
        self.cube_selected.emit(cube_id)
        rect = self._cube_rect(self.rect().adjusted(24, 24, -24, -24), self.document.cubes[cube_id])
        self._dragging = True
        self._drag_offset = event.position().toPoint() - rect.topLeft()
        self.update()

    def mouseMoveEvent(self, event) -> None:  # noqa: N802
        if not self.interactive or not self._dragging or self.document is None or self.selected_cube_id is None:
            return
        frame = self.rect().adjusted(24, 24, -24, -24)
        x = (event.position().x() - frame.x() - self._drag_offset.x()) / self.zoom
        y = (event.position().y() - frame.y() - self._drag_offset.y()) / self.zoom
        self.cube_moved.emit(self.selected_cube_id, x, y)

    def mouseReleaseEvent(self, event) -> None:  # noqa: N802
        self._dragging = False
        super().mouseReleaseEvent(event)

    def _cube_rect(self, frame: QRect, cube: ModelCube) -> QRect:
        if self.camera_mode == "top":
            origin_x, origin_y = cube.from_pos.x, cube.from_pos.z
            width, height = cube.to_pos.x - cube.from_pos.x, cube.to_pos.z - cube.from_pos.z
        elif self.camera_mode == "side":
            origin_x, origin_y = cube.from_pos.z, cube.from_pos.y
            width, height = cube.to_pos.z - cube.from_pos.z, cube.to_pos.y - cube.from_pos.y
        else:
            origin_x, origin_y = cube.from_pos.x, cube.from_pos.y
            width, height = cube.to_pos.x - cube.from_pos.x, cube.to_pos.y - cube.from_pos.y
        return QRect(
            int(frame.x() + origin_x * self.zoom),
            int(frame.y() + origin_y * self.zoom),
            max(10, int(width * self.zoom)),
            max(10, int(height * self.zoom)),
        )

    def _cube_at(self, point: QPoint) -> str | None:
        if self.document is None:
            return None
        frame = self.rect().adjusted(24, 24, -24, -24)
        for cube in self.document.cubes.values():
            if self._cube_rect(frame, cube).contains(point):
                return cube.id
        return None


class ModelStudioPanel(QWidget):
    status_message = pyqtSignal(str)
    notifications = pyqtSignal(str)

    def __init__(self, engine: ModelStudioEngine) -> None:
        super().__init__()
        self.engine = engine
        self.current_document: ModelDocument | None = None
        self.current_path: Path | None = None
        self.selected_cube_id: str | None = None
        self.selected_bone_id: str | None = "root"
        self._updating = False

        root = QVBoxLayout(self)
        root.setContentsMargins(0, 0, 0, 0)
        root.addWidget(_PanelHeader("Model Studio", "Author cube-based models with part hierarchy, transforms, texture state, validation, and export preview."))
        root.addWidget(self._build_top_actions())

        split = QSplitter(Qt.Orientation.Horizontal)
        split.addWidget(self._build_left_panel())
        split.addWidget(self._build_center_panel())
        split.addWidget(self._build_right_panel())
        split.setSizes([300, 760, 380])
        root.addWidget(split)
        self._refresh_document_list()
        self._refresh_surface()

    def set_engine(self, engine: ModelStudioEngine) -> None:
        self.engine = engine
        self.current_document = None
        self.current_path = None
        self.selected_cube_id = None
        self.selected_bone_id = "root"
        self._refresh_document_list()
        self._refresh_surface()

    def _build_top_actions(self) -> QWidget:
        host = QWidget()
        row = QHBoxLayout(host)
        row.setContentsMargins(8, 0, 8, 0)
        self.document_selector = QComboBox()
        self.document_selector.setToolTip("Saved model documents in the current workspace.")
        self.document_selector.currentTextChanged.connect(self._open_selected_document)
        row.addWidget(QLabel("Document"))
        row.addWidget(self.document_selector)
        row.addWidget(_button("New Document", self.new_document, "Create a new model document with a root bone and starter cube."))
        row.addWidget(_button("Open", self.open_document_dialog, "Open a saved model document from disk."))
        row.addWidget(_button("Save", self.save_current, "Save the current model document."))
        row.addWidget(_button("Import", self.open_document_dialog, "Import an existing .model.json document into the workbench."))
        row.addWidget(_button("Export Current", self.export_current, "Export the current model document to a chosen file path."))
        row.addWidget(_button("Export Runtime", self.export_runtime_current, "Export the current model document into the deterministic mod-facing runtime path."))
        row.addWidget(_button("Validate Current", self.validate_current, "Validate the current model document using the authoritative model validator."))
        row.addWidget(_button("Preview Current", self.preview_current, "Refresh the viewport summary and preview payload."))
        row.addStretch(1)
        return host

    def _build_left_panel(self) -> QWidget:
        outliner = QGroupBox("Outliner / Part List")
        layout = QVBoxLayout(outliner)
        helper = QLabel("Use Add Part for bones and Add Cube for visible geometry. Selection stays synchronized between the outliner, inspector, and viewport.")
        helper.setWordWrap(True)
        helper.setObjectName("panelHelpHint")
        layout.addWidget(helper)
        self.outliner = QListWidget()
        self.outliner.setToolTip("Model bones and cubes. Double-click to focus a part or cube.")
        self.outliner.itemDoubleClicked.connect(self._select_outliner_item)
        layout.addWidget(self.outliner)
        actions = QGridLayout()
        actions.addWidget(_button("Add Part", self.add_part, "Add a new child bone under the current bone or root."), 0, 0)
        actions.addWidget(_button("Add Cube", self.add_cube, "Add a cube to the current bone."), 0, 1)
        actions.addWidget(_button("Duplicate Part", self.duplicate_part, "Duplicate the selected cube or bone."), 1, 0)
        actions.addWidget(_button("Delete", self.delete_selected, "Delete the selected cube or non-root bone after confirmation."), 1, 1)
        layout.addLayout(actions)
        wrapper = QWidget()
        outer = QVBoxLayout(wrapper)
        outer.addWidget(outliner)
        return wrapper

    def _build_center_panel(self) -> QWidget:
        center = QWidget()
        layout = QVBoxLayout(center)
        controls = QHBoxLayout()
        self.camera_selector = QComboBox()
        self.camera_selector.addItems(["front", "side", "top"])
        self.camera_selector.setToolTip("Viewport camera mode for this simplified model workbench.")
        self.camera_selector.currentTextChanged.connect(self._update_camera)
        self.zoom_selector = QComboBox()
        self.zoom_selector.addItems(["8x", "10x", "12x", "16x", "20x"])
        self.zoom_selector.setCurrentText("12x")
        self.zoom_selector.setToolTip("Viewport zoom factor. This affects authoring only, not exported geometry.")
        self.zoom_selector.currentTextChanged.connect(self._update_viewport_zoom)
        self.model_type_selector = QComboBox()
        self.model_type_selector.addItems(["block", "item", "entity"])
        self.model_type_selector.setToolTip("Model type stored in the authoritative model document contract.")
        self.model_type_selector.currentTextChanged.connect(self._update_model_type)
        for label, widget in [("Camera", self.camera_selector), ("Zoom", self.zoom_selector), ("Model Type", self.model_type_selector)]:
            controls.addWidget(QLabel(label))
            controls.addWidget(widget)
        controls.addStretch(1)
        layout.addLayout(controls)
        note = QLabel("Drag a selected cube in the viewport to adjust its simplified X/Y placement. Use the inspector for exact transforms and texture state.")
        note.setWordWrap(True)
        note.setObjectName("panelHelpHint")
        layout.addWidget(note)

        self.viewport = _ModelViewport(interactive=True)
        self.viewport.cube_selected.connect(self.select_cube)
        self.viewport.cube_moved.connect(self._move_cube)
        layout.addWidget(self.viewport, 1)

        bottom = QGridLayout()
        bottom.addWidget(_button("Center Selection", self.center_selection, "Center the selected cube in the current document bounds."), 0, 0)
        bottom.addWidget(_button("Reset View", self.reset_view, "Reset viewport zoom and camera."), 0, 1)
        bottom.addWidget(_button("Zoom to Fit", self.zoom_to_fit, "Set a practical overview zoom for the current model."), 0, 2)
        bottom.addWidget(_button("Export Current", self.export_current, "Export the current model document."), 1, 0)
        bottom.addWidget(_button("Validate Current", self.validate_current, "Run model validation checks."), 1, 1)
        bottom.addWidget(_button("Preview Current", self.preview_current, "Refresh preview payload and viewport summary."), 1, 2)
        layout.addLayout(bottom)
        return center

    def _build_right_panel(self) -> QWidget:
        tabs = QTabWidget()

        inspector = QWidget()
        form = QFormLayout(inspector)
        self.selection_label = QLabel("None")
        self.selection_kind = QLabel("None")
        self.bone_selector = QComboBox()
        self.bone_selector.setToolTip("Assign the selected cube to a bone/group in the model hierarchy.")
        self.bone_selector.currentTextChanged.connect(self._assign_selected_cube_to_bone)
        self.pos_x = self._model_spin(self._apply_inspector_changes, "Start/position X value for the current selection.")
        self.pos_y = self._model_spin(self._apply_inspector_changes, "Start/position Y value for the current selection.")
        self.pos_z = self._model_spin(self._apply_inspector_changes, "Start/position Z value for the current selection.")
        self.size_x = self._model_spin(self._apply_inspector_changes, "Cube width or bone pivot X." )
        self.size_y = self._model_spin(self._apply_inspector_changes, "Cube height or bone pivot Y." )
        self.size_z = self._model_spin(self._apply_inspector_changes, "Cube depth or bone pivot Z." )
        self.rot_x = self._model_spin(self._apply_inspector_changes, "Rotation X for the current cube or bone.")
        self.rot_y = self._model_spin(self._apply_inspector_changes, "Rotation Y for the current cube or bone.")
        self.rot_z = self._model_spin(self._apply_inspector_changes, "Rotation Z for the current cube or bone.")
        self.texture_path = QPlainTextEdit()
        self.texture_path.setFixedHeight(54)
        self.texture_path.setToolTip("Texture path for the selected cube. This is part of the exported model contract.")
        self.texture_path.textChanged.connect(self._apply_inspector_changes)
        form.addRow("Selection", self.selection_label)
        form.addRow("Type", self.selection_kind)
        form.addRow("Bone", self.bone_selector)
        form.addRow("X", self.pos_x)
        form.addRow("Y", self.pos_y)
        form.addRow("Z", self.pos_z)
        form.addRow("Size / Pivot X", self.size_x)
        form.addRow("Size / Pivot Y", self.size_y)
        form.addRow("Size / Pivot Z", self.size_z)
        form.addRow("Rotation X", self.rot_x)
        form.addRow("Rotation Y", self.rot_y)
        form.addRow("Rotation Z", self.rot_z)
        form.addRow("Texture", self.texture_path)

        uv = QWidget()
        uv_layout = QVBoxLayout(uv)
        uv_note = QLabel("Texture / UV state stays visible here even though the viewport is simplified. Update texture size or texture path before export.")
        uv_note.setWordWrap(True)
        uv_note.setObjectName("panelHelpHint")
        uv_layout.addWidget(uv_note)
        texture_size_row = QHBoxLayout()
        self.texture_width = self._size_spin(self._apply_texture_state, "Model texture width in pixels.")
        self.texture_height = self._size_spin(self._apply_texture_state, "Model texture height in pixels.")
        texture_size_row.addWidget(QLabel("Texture Width"))
        texture_size_row.addWidget(self.texture_width)
        texture_size_row.addWidget(QLabel("Texture Height"))
        texture_size_row.addWidget(self.texture_height)
        uv_layout.addLayout(texture_size_row)
        self.face_summary = QPlainTextEdit()
        self.face_summary.setReadOnly(False)
        self.face_summary.setToolTip("Editable face mapping state for the current cube as JSON.")
        uv_layout.addWidget(self.face_summary)
        uv_layout.addWidget(_button("Apply Face Mapping", self._apply_face_mappings, "Apply the editable face mapping JSON to the selected cube."))

        report_tab = QWidget()
        report_layout = QVBoxLayout(report_tab)
        self.preview_summary = QPlainTextEdit()
        self.preview_summary.setReadOnly(True)
        self.preview_summary.setToolTip("Preview payload summary for the current model document.")
        report_layout.addWidget(self.preview_summary)
        self.report = QPlainTextEdit()
        self.report.setReadOnly(True)
        self.report.setToolTip("Validation and export messages for Model Studio.")
        report_layout.addWidget(self.report)

        tabs.addTab(inspector, "Inspector")
        tabs.addTab(uv, "Texture / UV")
        tabs.addTab(report_tab, "Report")
        return tabs

    def _model_spin(self, callback, help_text: str) -> QSpinBox:
        spin = QSpinBox()
        spin.setRange(-256, 256)
        spin.setToolTip(help_text)
        spin.valueChanged.connect(callback)
        return spin

    def _size_spin(self, callback, help_text: str) -> QSpinBox:
        spin = QSpinBox()
        spin.setRange(1, 512)
        spin.setToolTip(help_text)
        spin.valueChanged.connect(callback)
        return spin

    def new_document(self) -> None:
        name = f"model_{len(self.engine.list_documents()) + 1}"
        self.current_document = self.engine.create_document(name, model_type=self.model_type_selector.currentText() or "block")
        self.current_path = None
        self.selected_cube_id = next(iter(self.current_document.cubes), None)
        self.selected_bone_id = "root"
        self._refresh_document_list(select=name)
        self._refresh_surface()
        self.notifications.emit(f"Created model document {name}")

    def open_document_dialog(self) -> None:
        selected, _ = QFileDialog.getOpenFileName(self, "Open model document", str(self.engine.root), "Model Documents (*.model.json)")
        if not selected:
            self.notifications.emit("Open model document cancelled")
            return
        self.open_document(Path(selected))

    def open_document(self, path_or_name: str | Path) -> None:
        loaded = self.engine.load_document(path_or_name)
        self.current_document = loaded.document
        self.current_path = Path(path_or_name) if isinstance(path_or_name, Path) else self.engine.document_path(str(path_or_name))
        self.selected_cube_id = next(iter(self.current_document.cubes), None)
        self.selected_bone_id = "root"
        self._refresh_document_list(select=self.current_document.name)
        self._refresh_surface()
        for warning in loaded.warnings:
            self.notifications.emit(f"Model load warning: {warning}")
        for error in loaded.errors:
            self.notifications.emit(f"Model load error: {error}")

    def save_current(self) -> bool:
        if self.current_document is None:
            self.notifications.emit("Create or open a model document before saving")
            return False
        saved_path = self.engine.save_document(self.current_document)
        self.current_path = saved_path
        self.report.setPlainText(f"Saved model document to {saved_path}\nRuntime export path: {self.engine.runtime_export_path(self.current_document)}")
        self.notifications.emit(f"Saved model document {self.current_document.name}")
        return True

    def save_all(self) -> int:
        return 1 if self.save_current() else 0

    def export_current(self) -> None:
        if self.current_document is None:
            self.notifications.emit("No model document to export")
            return
        selected, _ = QFileDialog.getSaveFileName(self, "Export model document", str(self.engine.document_path(self.current_document.name)), "Model Documents (*.model.json)")
        if not selected:
            self.notifications.emit("Model export cancelled")
            return
        target = self.engine.export_document(self.current_document, Path(selected))
        self.report.setPlainText(f"Exported model document to {target}")
        self.notifications.emit(f"Exported model document to {target.name}")

    def export_runtime_current(self) -> None:
        if self.current_document is None:
            self.notifications.emit("No model document to export")
            return
        target = self.engine.export_runtime_document(self.current_document)
        runtime = self.engine.build_runtime_definition(self.current_document)
        self.report.setPlainText(
            f"Exported runtime model to {target}\nResource ID: {runtime['resourceId']}\nCube count: {len(runtime['cubes'])}\nBone count: {len(runtime['bones'])}"
        )
        self.notifications.emit(f"Exported runtime model definition to {target.name}")

    def validate_current(self) -> None:
        if self.current_document is None:
            self.notifications.emit("No model document to validate")
            return
        report = self.engine.validate_document(self.current_document)
        if report.issues:
            text = "\n".join(f"[{issue.severity.upper()}] {issue.target_id or 'document'}: {issue.message}" for issue in report.issues)
        else:
            text = "No validation issues detected. The model document is internally consistent."
        self.report.setPlainText(text)
        self.notifications.emit("Validated current model document")

    def preview_current(self) -> None:
        if self.current_document is None:
            self.notifications.emit("No model document to preview")
            return
        payload = self.engine.preview_payload(self.current_document)
        runtime = self.engine.build_runtime_definition(self.current_document)
        self.preview_summary.setPlainText(json.dumps({"document": payload["document"], "modelType": payload["modelType"], "textureSize": payload["textureSize"], "cubeCount": len(payload["cubes"]), "boneCount": len(payload["bones"]), "selectedCube": self.selected_cube_id, "runtimeResourceId": runtime["resourceId"], "runtimePath": str(self.engine.runtime_export_path(self.current_document))}, indent=2))
        cube = self._selected_cube()
        if cube is not None:
            face_state = {face: {"texture": mapping.texture, "uv": list(mapping.uv), "rotation": mapping.rotation} for face, mapping in cube.faces.items()}
            self.face_summary.setPlainText(json.dumps(face_state, indent=2))
        else:
            self.face_summary.setPlainText("Select a cube to inspect face mapping and UV state.")

    def add_part(self) -> None:
        if self.current_document is None:
            self.new_document()
        assert self.current_document is not None
        parent_id = self.selected_bone_id if self.selected_bone_id in self.current_document.bones else "root"
        bone_id = self._unique_bone_id("part")
        self.engine.add_bone(self.current_document, ModelBone(id=bone_id), parent_id=parent_id)
        self.selected_bone_id = bone_id
        self._refresh_surface()
        self.notifications.emit(f"Added part {bone_id}")

    def add_cube(self) -> None:
        if self.current_document is None:
            self.new_document()
        assert self.current_document is not None
        cube_id = self._unique_cube_id("cube")
        cube = self.engine.create_cube(cube_id=cube_id, width=8.0, height=8.0, depth=8.0)
        self.engine.add_cube(self.current_document, cube, bone_id=self.selected_bone_id or "root")
        self.select_cube(cube_id)
        self.notifications.emit(f"Added cube {cube_id}")

    def duplicate_part(self) -> None:
        if self.current_document is None:
            self.notifications.emit("Select a bone or cube before duplicating")
            return
        if self.selected_cube_id and self.selected_cube_id in self.current_document.cubes:
            cube = self.engine.duplicate_cube(self.current_document, self.selected_cube_id)
            self.select_cube(cube.id)
            self.notifications.emit(f"Duplicated cube {cube.id}")
            return
        if self.selected_bone_id and self.selected_bone_id in self.current_document.bones and self.selected_bone_id != "root":
            bone = copy.deepcopy(self.current_document.bones[self.selected_bone_id])
            bone.id = self._unique_bone_id(f"{bone.id}_copy")
            bone.children = []
            bone.cubes = []
            self.engine.add_bone(self.current_document, bone, parent_id=bone.parent or "root")
            self.selected_bone_id = bone.id
            self._refresh_surface()
            self.notifications.emit(f"Duplicated part {bone.id}")

    def delete_selected(self) -> None:
        if self.current_document is None:
            self.notifications.emit("Nothing to delete")
            return
        if self.selected_cube_id and self.selected_cube_id in self.current_document.cubes:
            result = QMessageBox.question(self, "Delete Cube", f"Delete cube {self.selected_cube_id}?", QMessageBox.StandardButton.Yes | QMessageBox.StandardButton.No)
            if result != QMessageBox.StandardButton.Yes:
                return
            cube_id = self.selected_cube_id
            self.engine.remove_cube(self.current_document, cube_id)
            self.selected_cube_id = next(iter(self.current_document.cubes), None)
            self._refresh_surface()
            self.notifications.emit(f"Deleted cube {cube_id}")
            return
        if self.selected_bone_id and self.selected_bone_id in self.current_document.bones and self.selected_bone_id != "root":
            result = QMessageBox.question(self, "Delete Part", f"Delete part {self.selected_bone_id}?", QMessageBox.StandardButton.Yes | QMessageBox.StandardButton.No)
            if result != QMessageBox.StandardButton.Yes:
                return
            bone_id = self.selected_bone_id
            self.engine.remove_bone(self.current_document, bone_id)
            self.selected_bone_id = "root"
            self._refresh_surface()
            self.notifications.emit(f"Deleted part {bone_id}")

    def center_selection(self) -> None:
        cube = self._selected_cube()
        if cube is None or self.current_document is None:
            self.notifications.emit("Select a cube before centering")
            return
        width = cube.to_pos.x - cube.from_pos.x
        height = cube.to_pos.y - cube.from_pos.y
        cube.from_pos = Vec3((16.0 - width) / 2.0, (16.0 - height) / 2.0, cube.from_pos.z)
        cube.to_pos = Vec3(cube.from_pos.x + width, cube.from_pos.y + height, cube.to_pos.z)
        self._refresh_surface()
        self.notifications.emit(f"Centered cube {cube.id}")

    def reset_view(self) -> None:
        self.camera_selector.setCurrentText("front")
        self.zoom_selector.setCurrentText("12x")
        self.notifications.emit("Reset model viewport")

    def zoom_to_fit(self) -> None:
        self.zoom_selector.setCurrentText("10x")

    def select_cube(self, cube_id: str) -> None:
        self.selected_cube_id = cube_id
        if self.current_document is not None:
            for bone_id, bone in self.current_document.bones.items():
                if cube_id in bone.cubes:
                    self.selected_bone_id = bone_id
                    break
        self._refresh_surface()

    def _move_cube(self, cube_id: str, x: float, y: float) -> None:
        if self.current_document is None or cube_id not in self.current_document.cubes:
            return
        cube = self.current_document.cubes[cube_id]
        width = cube.to_pos.x - cube.from_pos.x
        height = cube.to_pos.y - cube.from_pos.y
        cube.from_pos = Vec3(float(x), float(y), cube.from_pos.z)
        cube.to_pos = Vec3(float(x) + width, float(y) + height, cube.to_pos.z)
        self._refresh_surface()

    def _update_camera(self) -> None:
        self.viewport.set_camera_mode(self.camera_selector.currentText())

    def _update_viewport_zoom(self) -> None:
        zoom = _safe_int(self.zoom_selector.currentText().replace("x", ""), 12)
        self.viewport.set_zoom(float(zoom))

    def _update_model_type(self) -> None:
        if self.current_document is not None:
            self.current_document.model_type = self.model_type_selector.currentText()
            self.preview_current()

    def _apply_texture_state(self) -> None:
        if self.current_document is None:
            return
        self.current_document.texture_width = self.texture_width.value()
        self.current_document.texture_height = self.texture_height.value()
        self.preview_current()

    def _assign_selected_cube_to_bone(self, bone_id: str) -> None:
        if self._updating or self.current_document is None or self.selected_cube_id is None:
            return
        if not bone_id or bone_id not in self.current_document.bones:
            return
        self.engine.assign_cube_to_bone(self.current_document, self.selected_cube_id, bone_id)
        self.selected_bone_id = bone_id
        self._refresh_surface()

    def _apply_face_mappings(self) -> None:
        cube = self._selected_cube()
        if cube is None:
            self.notifications.emit("Select a cube before editing face mappings")
            return
        payload = _safe_json(self.face_summary.toPlainText())
        if payload is None:
            self.notifications.emit("Face mapping JSON is invalid")
            return
        updated_faces: dict[str, FaceMapping] = {}
        for face, mapping in cube.faces.items():
            source = payload.get(face) if isinstance(payload, dict) else None
            if not isinstance(source, dict):
                updated_faces[face] = mapping
                continue
            uv = source.get("uv", list(mapping.uv))
            if not isinstance(uv, (list, tuple)) or len(uv) != 4:
                uv = list(mapping.uv)
            updated_faces[face] = FaceMapping(
                texture=str(source.get("texture", mapping.texture)),
                uv=tuple(float(value) for value in uv),
                rotation=int(source.get("rotation", mapping.rotation) or 0),
            )
        cube.faces = updated_faces
        self.notifications.emit(f"Updated face mapping for {cube.id}")
        self.preview_current()


    def _select_outliner_item(self, item: QListWidgetItem) -> None:
        payload = item.data(Qt.ItemDataRole.UserRole)
        if not isinstance(payload, tuple) or len(payload) != 2:
            return
        kind, identifier = payload
        if kind == "cube":
            self.select_cube(identifier)
        elif kind == "bone":
            self.selected_bone_id = identifier
            self.selected_cube_id = None
            self._refresh_surface()

    def _apply_inspector_changes(self) -> None:
        if self._updating or self.current_document is None:
            return
        cube = self._selected_cube()
        if cube is not None:
            cube.from_pos = Vec3(float(self.pos_x.value()), float(self.pos_y.value()), float(self.pos_z.value()))
            cube.to_pos = Vec3(cube.from_pos.x + float(self.size_x.value()), cube.from_pos.y + float(self.size_y.value()), cube.from_pos.z + float(self.size_z.value()))
            cube.rotation = Vec3(float(self.rot_x.value()), float(self.rot_y.value()), float(self.rot_z.value()))
            cube.texture = self.texture_path.toPlainText().strip()
        elif self.selected_bone_id and self.selected_bone_id in self.current_document.bones:
            bone = self.current_document.bones[self.selected_bone_id]
            bone.pivot = Vec3(float(self.size_x.value()), float(self.size_y.value()), float(self.size_z.value()))
            bone.rotation = Vec3(float(self.rot_x.value()), float(self.rot_y.value()), float(self.rot_z.value()))
        self._refresh_surface()

    def _refresh_document_list(self, select: str | None = None) -> None:
        self.document_selector.blockSignals(True)
        current = select or (self.current_document.name if self.current_document is not None else "")
        self.document_selector.clear()
        self.document_selector.addItem("Open saved document...")
        for name in self.engine.list_documents():
            self.document_selector.addItem(name)
        if current:
            index = self.document_selector.findText(current)
            if index >= 0:
                self.document_selector.setCurrentIndex(index)
        self.document_selector.blockSignals(False)

    def _open_selected_document(self, name: str) -> None:
        if not name or name == "Open saved document...":
            return
        if self.current_document is not None and self.current_document.name == name:
            return
        self.open_document(name)

    def _refresh_surface(self) -> None:
        self.viewport.set_document(self.current_document)
        self.viewport.set_selected_cube(self.selected_cube_id)
        self._update_camera()
        self._update_viewport_zoom()
        self._refresh_outliner()
        self._refresh_bone_selector()
        self._refresh_inspector()
        if self.current_document is None:
            self.preview_summary.setPlainText("Create or open a model document to preview cube and bone payloads.")
            self.report.setPlainText("Validation and export messages will appear here.")
            self.face_summary.setPlainText("Select a cube to inspect face mapping and UV state.")
        else:
            self.texture_width.blockSignals(True)
            self.texture_height.blockSignals(True)
            self.texture_width.setValue(self.current_document.texture_width)
            self.texture_height.setValue(self.current_document.texture_height)
            self.texture_width.blockSignals(False)
            self.texture_height.blockSignals(False)
            self.model_type_selector.blockSignals(True)
            self.model_type_selector.setCurrentText(self.current_document.model_type)
            self.model_type_selector.blockSignals(False)
            self.preview_current()

    def _refresh_outliner(self) -> None:
        self.outliner.clear()
        if self.current_document is None:
            self.outliner.addItem("Create a model document to see bones and cubes")
            return
        for bone_id, bone in sorted(self.current_document.bones.items()):
            bone_item = QListWidgetItem(f"Part: {bone_id}")
            bone_item.setData(Qt.ItemDataRole.UserRole, ("bone", bone_id))
            self.outliner.addItem(bone_item)
            for cube_id in bone.cubes:
                cube_item = QListWidgetItem(f"  Cube: {cube_id}")
                cube_item.setData(Qt.ItemDataRole.UserRole, ("cube", cube_id))
                self.outliner.addItem(cube_item)

    def _refresh_inspector(self) -> None:
        self._updating = True
        cube = self._selected_cube()
        if cube is not None:
            self.selection_label.setText(cube.id)
            self.selection_kind.setText("Cube")
            self.pos_x.setValue(int(cube.from_pos.x))
            self.pos_y.setValue(int(cube.from_pos.y))
            self.pos_z.setValue(int(cube.from_pos.z))
            self.size_x.setValue(int(cube.to_pos.x - cube.from_pos.x))
            self.size_y.setValue(int(cube.to_pos.y - cube.from_pos.y))
            self.size_z.setValue(int(cube.to_pos.z - cube.from_pos.z))
            self.rot_x.setValue(int(cube.rotation.x))
            self.rot_y.setValue(int(cube.rotation.y))
            self.rot_z.setValue(int(cube.rotation.z))
            self.texture_path.setPlainText(cube.texture)
        elif self.current_document is not None and self.selected_bone_id in self.current_document.bones:
            bone = self.current_document.bones[self.selected_bone_id]
            self.selection_label.setText(bone.id)
            self.selection_kind.setText("Part")
            self.pos_x.setValue(0)
            self.pos_y.setValue(0)
            self.pos_z.setValue(0)
            self.size_x.setValue(int(bone.pivot.x))
            self.size_y.setValue(int(bone.pivot.y))
            self.size_z.setValue(int(bone.pivot.z))
            self.rot_x.setValue(int(bone.rotation.x))
            self.rot_y.setValue(int(bone.rotation.y))
            self.rot_z.setValue(int(bone.rotation.z))
            self.texture_path.setPlainText("")
        else:
            self.selection_label.setText("None")
            self.selection_kind.setText("None")
            for spin in [self.pos_x, self.pos_y, self.pos_z, self.size_x, self.size_y, self.size_z, self.rot_x, self.rot_y, self.rot_z]:
                spin.setValue(0)
            self.texture_path.setPlainText("")
        self._updating = False

    def _refresh_bone_selector(self) -> None:
        self._updating = True
        current = self.selected_bone_id or "root"
        self.bone_selector.blockSignals(True)
        self.bone_selector.clear()
        if self.current_document is not None:
            for bone_id in sorted(self.current_document.bones):
                self.bone_selector.addItem(bone_id)
            index = self.bone_selector.findText(current)
            if index >= 0:
                self.bone_selector.setCurrentIndex(index)
        self.bone_selector.setEnabled(self.selected_cube_id is not None)
        self.bone_selector.blockSignals(False)
        self._updating = False

    def _selected_cube(self) -> ModelCube | None:
        if self.current_document is None or self.selected_cube_id is None:
            return None
        return self.current_document.cubes.get(self.selected_cube_id)

    def _unique_cube_id(self, prefix: str) -> str:
        assert self.current_document is not None
        index = 1
        candidate = prefix
        while candidate in self.current_document.cubes:
            index += 1
            candidate = f"{prefix}_{index}"
        return candidate

    def _unique_bone_id(self, prefix: str) -> str:
        assert self.current_document is not None
        index = 1
        candidate = prefix
        while candidate in self.current_document.bones:
            index += 1
            candidate = f"{prefix}_{index}"
        return candidate


class BuildRunPanel(QWidget):
    def __init__(self, session, callbacks: dict[str, object]) -> None:
        super().__init__()
        self.session = session
        self.callbacks = callbacks

        root = QVBoxLayout(self)
        root.addWidget(_PanelHeader("Build/Run Studio", "Run validation, compile/export pipelines, run client/server configurations, and inspect runtime logs without leaving the shell."))

        actions = QGridLayout()
        entries = [
            ("Validate", "validate_assets", "Run the current workspace validation pipeline."),
            ("Build Assets", "compile_assets", "Build workspace asset outputs through the backend build service."),
            ("Compile Expansion", "compile_expansion", "Compile the first detected addon expansion with safe backend feedback."),
            ("Run Client", "run_client", "Run the configured client through the session run service."),
            ("Run Server", "run_server", "Run the configured server through the session run service."),
            ("Build Release", "release_build", "Build the release artifact bundle."),
            ("Build Modpack", "modpack_build", "Build the modpack distribution archive."),
            ("Export Datapack", "export_datapack", "Export datapack outputs to the workspace export folder."),
            ("Export Resourcepack", "export_resourcepack", "Export resourcepack outputs to the workspace export folder."),
            ("Latest Log", "latest_log", "Open the latest runtime log resolved by the session services."),
            ("Clear Logs", "clear_logs", "Clear in-memory logs and notifications without deleting files."),
        ]
        for index, (title, key, help_text) in enumerate(entries):
            button = QPushButton(title)
            button.setToolTip(help_text)
            button.setStatusTip(help_text)
            callback = self.callbacks.get(key)
            if callable(callback):
                button.clicked.connect(callback)
            else:
                button.setEnabled(False)
            actions.addWidget(button, index // 3, index % 3)
        root.addLayout(actions)

        info_row = QHBoxLayout()
        self.run_configs = QLabel("Run configs: none")
        self.log_path = QLabel("Latest log: unavailable")
        info_row.addWidget(self.run_configs)
        info_row.addWidget(self.log_path)
        root.addLayout(info_row)

        self.task_status = QPlainTextEdit()
        self.task_status.setReadOnly(True)
        self.task_status.setToolTip("Live task, process, and notification output routed through session services.")
        root.addWidget(self.task_status)

        self._timer = QTimer(self)
        self._timer.setInterval(800)
        self._timer.timeout.connect(self.refresh)
        self._timer.start()
        self.refresh()

    def set_session(self, session) -> None:
        self.session = session
        self.refresh()

    def refresh(self) -> None:
        configurations = self.session.run_service.list_configurations()
        self.run_configs.setText("Run configs: " + (", ".join(config.name for config in configurations) if configurations else "none"))
        latest = self.session.latest_log_path()
        self.log_path.setText(f"Latest log: {latest}" if latest else "Latest log: unavailable")

        lines: list[str] = []
        for entry in self.session.log_model.tail(20):
            lines.append(f"[{entry.level}] {entry.source}: {entry.message}")
        notifications = self.session.notification_service.recent(10)
        if notifications:
            lines.append("")
            lines.append("Notifications")
            for notification in notifications:
                lines.append(f"[{notification.severity}] {notification.source}: {notification.message}")
        self.task_status.setPlainText("\n".join(lines) if lines else "No task output yet.")

