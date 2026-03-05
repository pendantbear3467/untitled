#!/usr/bin/env python3
"""ExtremeCraft Skill Tree Editor.

Visual editor for creating RPG skill trees and exporting them as JSON for the
ExtremeCraft Forge mod.
"""

from __future__ import annotations

import json
import math
import sys
from dataclasses import dataclass, field
from pathlib import Path
from typing import Iterable

from PyQt6.QtCore import QPoint, QPointF, QRectF, Qt, pyqtSignal
from PyQt6.QtGui import QAction, QBrush, QColor, QPainter, QPainterPath, QPen, QPixmap, QTransform
from PyQt6.QtWidgets import (
    QApplication,
    QComboBox,
    QDoubleSpinBox,
    QFormLayout,
    QGraphicsItem,
    QGraphicsLineItem,
    QGraphicsObject,
    QGraphicsScene,
    QGraphicsSimpleTextItem,
    QGraphicsView,
    QHBoxLayout,
    QLabel,
    QLineEdit,
    QListWidget,
    QMainWindow,
    QMessageBox,
    QPushButton,
    QSpinBox,
    QSplitter,
    QToolBar,
    QVBoxLayout,
    QWidget,
    QFileDialog,
)


CATEGORY_COLORS: dict[str, QColor] = {
    "combat": QColor(196, 64, 64),
    "survival": QColor(64, 164, 85),
    "arcane": QColor(150, 88, 196),
    "exploration": QColor(216, 177, 68),
    "explorer": QColor(216, 177, 68),
    "technology": QColor(82, 192, 208),
}

DEFAULT_TREES = ["combat", "survival", "arcane", "explorer", "technology"]
SNAP_X = 60
SNAP_Y = 40
NODE_RADIUS = 26
CANVAS_EXTENT = 10000

REPO_ROOT = Path(__file__).resolve().parents[1]
ICON_ROOT = REPO_ROOT / "src" / "main" / "resources" / "assets" / "extremecraft"
DEFAULT_EXPORT_ROOT = REPO_ROOT / "src" / "main" / "resources" / "data" / "extremecraft" / "skill_trees"


@dataclass(slots=True)
class Modifier:
    type: str
    value: float


@dataclass(slots=True)
class SkillNodeData:
    id: str
    display_name: str = ""
    category: str = "combat"
    cost: int = 1
    required_nodes: list[str] = field(default_factory=list)
    modifiers: list[Modifier] = field(default_factory=list)
    required_level: int = 1
    required_class: str = ""
    x: float = 0.0
    y: float = 0.0

    def to_export(self) -> dict:
        return {
            "id": self.id,
            "displayName": self.display_name,
            "x": int(self.x),
            "y": int(self.y),
            "cost": self.cost,
            "category": self.category,
            "requiredLevel": self.required_level,
            "requiredClass": self.required_class,
            "requires": list(self.required_nodes),
            "modifiers": [{"type": m.type, "value": m.value} for m in self.modifiers],
        }

    @classmethod
    def from_json(cls, payload: dict) -> "SkillNodeData":
        mods = []
        for raw in payload.get("modifiers", []):
            mods.append(Modifier(type=str(raw.get("type", "")), value=float(raw.get("value", 0.0))))
        return cls(
            id=str(payload.get("id", "")).strip(),
            display_name=str(payload.get("displayName", "")).strip(),
            category=str(payload.get("category", "combat")).strip().lower() or "combat",
            cost=max(1, int(payload.get("cost", 1))),
            required_nodes=[str(v).strip() for v in payload.get("requires", []) if str(v).strip()],
            modifiers=mods,
            required_level=max(1, int(payload.get("requiredLevel", 1))),
            required_class=str(payload.get("requiredClass", "")).strip(),
            x=float(payload.get("x", 0.0)),
            y=float(payload.get("y", 0.0)),
        )


@dataclass(slots=True)
class SkillTreeData:
    tree_name: str
    nodes: dict[str, SkillNodeData] = field(default_factory=dict)

    def to_export(self) -> dict:
        return {
            "tree": self.tree_name,
            "nodes": [node.to_export() for node in self.nodes.values()],
        }


class ModifierEditor(QWidget):
    changed = pyqtSignal()

    def __init__(self, parent: QWidget | None = None) -> None:
        super().__init__(parent)
        self.list_widget = QListWidget()
        self.type_input = QLineEdit()
        self.value_input = QDoubleSpinBox()
        self.value_input.setDecimals(4)
        self.value_input.setRange(-9999.0, 9999.0)
        self.value_input.setSingleStep(0.01)

        add_btn = QPushButton("Add")
        remove_btn = QPushButton("Remove")

        row = QHBoxLayout()
        row.addWidget(self.type_input)
        row.addWidget(self.value_input)
        row.addWidget(add_btn)
        row.addWidget(remove_btn)

        layout = QVBoxLayout(self)
        layout.addWidget(QLabel("Modifiers (type:value)"))
        layout.addWidget(self.list_widget)
        layout.addLayout(row)
        layout.setContentsMargins(0, 0, 0, 0)

        add_btn.clicked.connect(self._add_modifier)
        remove_btn.clicked.connect(self._remove_modifier)

    def set_modifiers(self, modifiers: Iterable[Modifier]) -> None:
        self.list_widget.clear()
        for modifier in modifiers:
            self.list_widget.addItem(f"{modifier.type}:{modifier.value}")

    def modifiers(self) -> list[Modifier]:
        parsed: list[Modifier] = []
        for idx in range(self.list_widget.count()):
            raw = self.list_widget.item(idx).text()
            if ":" not in raw:
                continue
            mod_type, mod_value = raw.split(":", 1)
            mod_type = mod_type.strip()
            if not mod_type:
                continue
            try:
                value = float(mod_value.strip())
            except ValueError:
                value = 0.0
            parsed.append(Modifier(type=mod_type, value=value))
        return parsed

    def _add_modifier(self) -> None:
        mod_type = self.type_input.text().strip()
        if not mod_type:
            return
        value = self.value_input.value()
        self.list_widget.addItem(f"{mod_type}:{value}")
        self.type_input.clear()
        self.value_input.setValue(0.0)
        self.changed.emit()

    def _remove_modifier(self) -> None:
        row = self.list_widget.currentRow()
        if row >= 0:
            self.list_widget.takeItem(row)
            self.changed.emit()


class SkillNodeItem(QGraphicsObject):
    moved = pyqtSignal(str)

    def __init__(self, data: SkillNodeData) -> None:
        super().__init__()
        self.data = data
        self.setFlags(
            QGraphicsItem.GraphicsItemFlag.ItemIsMovable
            | QGraphicsItem.GraphicsItemFlag.ItemIsSelectable
            | QGraphicsItem.GraphicsItemFlag.ItemSendsGeometryChanges
        )
        self.setCacheMode(QGraphicsItem.CacheMode.DeviceCoordinateCache)
        self.label = QGraphicsSimpleTextItem(self._display_label(), self)
        self.label.setBrush(QBrush(QColor(235, 235, 235)))
        self.label.setPos(-NODE_RADIUS, NODE_RADIUS + 6)
        self._icon_pixmap: QPixmap | None = self._load_icon_pixmap()
        self.setPos(self.data.x, self.data.y)

    def _display_label(self) -> str:
        return self.data.display_name or self.data.id

    def _load_icon_pixmap(self) -> QPixmap | None:
        icon_path = ICON_ROOT / "textures" / "gui" / "skills" / f"{self.data.id}.png"
        if not icon_path.exists():
            return None
        pixmap = QPixmap(str(icon_path))
        if pixmap.isNull():
            return None
        return pixmap.scaled(
            NODE_RADIUS * 2 - 6,
            NODE_RADIUS * 2 - 6,
            Qt.AspectRatioMode.KeepAspectRatio,
            Qt.TransformationMode.SmoothTransformation,
        )

    def refresh_visuals(self) -> None:
        self.label.setText(self._display_label())
        self._icon_pixmap = self._load_icon_pixmap()
        self.update()

    def category_color(self) -> QColor:
        return CATEGORY_COLORS.get(self.data.category, QColor(120, 120, 120))

    def boundingRect(self) -> QRectF:
        return QRectF(-NODE_RADIUS - 4, -NODE_RADIUS - 4, NODE_RADIUS * 2 + 8, NODE_RADIUS * 2 + 36)

    def paint(self, painter: QPainter, option, widget=None) -> None:
        base = self.category_color()
        ring = QColor(max(0, base.red() - 40), max(0, base.green() - 40), max(0, base.blue() - 40))

        painter.setRenderHint(QPainter.RenderHint.Antialiasing, True)
        painter.setPen(QPen(ring, 2.0))
        painter.setBrush(QBrush(base))
        painter.drawEllipse(QPointF(0, 0), NODE_RADIUS, NODE_RADIUS)

        inner = QColor(min(255, base.red() + 28), min(255, base.green() + 28), min(255, base.blue() + 28), 170)
        painter.setPen(Qt.PenStyle.NoPen)
        painter.setBrush(QBrush(inner))
        painter.drawEllipse(QPointF(-6, -7), NODE_RADIUS - 10, NODE_RADIUS - 12)

        if self._icon_pixmap is not None:
            path = QPainterPath()
            path.addEllipse(QPointF(0, 0), NODE_RADIUS - 3, NODE_RADIUS - 3)
            painter.setClipPath(path)
            painter.drawPixmap(-self._icon_pixmap.width() / 2, -self._icon_pixmap.height() / 2, self._icon_pixmap)
            painter.setClipping(False)
        else:
            painter.setPen(QPen(QColor(245, 245, 245), 1.2))
            short_label = (self.data.id[:6] + "...") if len(self.data.id) > 9 else self.data.id
            painter.drawText(QRectF(-NODE_RADIUS + 5, -8, NODE_RADIUS * 2 - 10, 16), Qt.AlignmentFlag.AlignCenter, short_label)

        if self.isSelected():
            painter.setBrush(Qt.BrushStyle.NoBrush)
            painter.setPen(QPen(QColor(255, 255, 255, 200), 2.0, Qt.PenStyle.DashLine))
            painter.drawEllipse(QPointF(0, 0), NODE_RADIUS + 3, NODE_RADIUS + 3)

    def itemChange(self, change, value):
        if change == QGraphicsItem.GraphicsItemChange.ItemPositionChange:
            p = value
            snapped = QPointF(round(p.x() / SNAP_X) * SNAP_X, round(p.y() / SNAP_Y) * SNAP_Y)
            return snapped
        if change == QGraphicsItem.GraphicsItemChange.ItemPositionHasChanged:
            self.data.x = self.pos().x()
            self.data.y = self.pos().y()
            self.moved.emit(self.data.id)
        return super().itemChange(change, value)


class LinkItem(QGraphicsLineItem):
    def __init__(self, source: SkillNodeItem, target: SkillNodeItem) -> None:
        super().__init__()
        self.source = source
        self.target = target
        self.setZValue(-1)
        self.setPen(QPen(QColor(170, 190, 220), 2.0))
        self.update_line()

    def update_line(self) -> None:
        self.setLine(self.source.pos().x(), self.source.pos().y(), self.target.pos().x(), self.target.pos().y())


class SkillTreeScene(QGraphicsScene):
    node_selected = pyqtSignal(object)
    links_changed = pyqtSignal()

    def __init__(self, parent: QWidget | None = None) -> None:
        super().__init__(parent)
        self.setSceneRect(-CANVAS_EXTENT, -CANVAS_EXTENT, CANVAS_EXTENT * 2, CANVAS_EXTENT * 2)
        self.node_items: dict[str, SkillNodeItem] = {}
        self.links: dict[tuple[str, str], LinkItem] = {}
        self.connect_mode = False
        self._drag_line: QGraphicsLineItem | None = None
        self._connect_start: SkillNodeItem | None = None

        self.selectionChanged.connect(self._on_selection_changed)

    def _on_selection_changed(self) -> None:
        selected = self.selectedItems()
        if selected and isinstance(selected[0], SkillNodeItem):
            self.node_selected.emit(selected[0])
        else:
            self.node_selected.emit(None)

    def clear_tree(self) -> None:
        self.clear()
        self.node_items.clear()
        self.links.clear()
        self._drag_line = None
        self._connect_start = None

    def add_node_item(self, data: SkillNodeData) -> SkillNodeItem:
        item = SkillNodeItem(data)
        item.moved.connect(self._on_node_moved)
        self.addItem(item)
        self.node_items[data.id] = item
        return item

    def remove_node_item(self, node_id: str) -> None:
        item = self.node_items.get(node_id)
        if item is None:
            return
        for key in list(self.links.keys()):
            if node_id in key:
                self.remove_link(*key)
        self.removeItem(item)
        del self.node_items[node_id]

    def add_link(self, source_id: str, target_id: str) -> bool:
        key = (source_id, target_id)
        if source_id == target_id or key in self.links:
            return False
        source = self.node_items.get(source_id)
        target = self.node_items.get(target_id)
        if not source or not target:
            return False
        link = LinkItem(source, target)
        self.links[key] = link
        self.addItem(link)
        self.links_changed.emit()
        return True

    def remove_link(self, source_id: str, target_id: str) -> None:
        key = (source_id, target_id)
        link = self.links.get(key)
        if not link:
            return
        self.removeItem(link)
        del self.links[key]
        self.links_changed.emit()

    def set_connect_mode(self, enabled: bool) -> None:
        self.connect_mode = enabled

    def _on_node_moved(self, node_id: str) -> None:
        for (source_id, target_id), link in self.links.items():
            if source_id == node_id or target_id == node_id:
                link.update_line()

    def mousePressEvent(self, event) -> None:
        if self.connect_mode:
            view_transform = self.views()[0].transform() if self.views() else QTransform()
            item = self.itemAt(event.scenePos(), view_transform)
            if isinstance(item, SkillNodeItem):
                self._connect_start = item
                self._drag_line = QGraphicsLineItem(item.pos().x(), item.pos().y(), event.scenePos().x(), event.scenePos().y())
                self._drag_line.setPen(QPen(QColor(220, 220, 255, 150), 1.5, Qt.PenStyle.DashLine))
                self.addItem(self._drag_line)
                event.accept()
                return
        super().mousePressEvent(event)

    def mouseMoveEvent(self, event) -> None:
        if self.connect_mode and self._drag_line is not None and self._connect_start is not None:
            self._drag_line.setLine(
                self._connect_start.pos().x(),
                self._connect_start.pos().y(),
                event.scenePos().x(),
                event.scenePos().y(),
            )
            event.accept()
            return
        super().mouseMoveEvent(event)

    def mouseReleaseEvent(self, event) -> None:
        if self.connect_mode and self._connect_start is not None:
            view_transform = self.views()[0].transform() if self.views() else QTransform()
            end_item = self.itemAt(event.scenePos(), view_transform)
            if isinstance(end_item, SkillNodeItem) and end_item is not self._connect_start:
                self.add_link(self._connect_start.data.id, end_item.data.id)
            if self._drag_line is not None:
                self.removeItem(self._drag_line)
            self._drag_line = None
            self._connect_start = None
            event.accept()
            return
        super().mouseReleaseEvent(event)


class SkillTreeView(QGraphicsView):
    def __init__(self, scene: SkillTreeScene, parent: QWidget | None = None) -> None:
        super().__init__(scene, parent)
        self.setRenderHint(QPainter.RenderHint.Antialiasing, True)
        self.setRenderHint(QPainter.RenderHint.TextAntialiasing, True)
        self.setDragMode(QGraphicsView.DragMode.RubberBandDrag)
        self.setTransformationAnchor(QGraphicsView.ViewportAnchor.AnchorUnderMouse)
        self.setResizeAnchor(QGraphicsView.ViewportAnchor.AnchorUnderMouse)
        self.setViewportUpdateMode(QGraphicsView.ViewportUpdateMode.BoundingRectViewportUpdate)
        self._panning = False
        self._pan_start = QPoint()

    def wheelEvent(self, event) -> None:
        zoom_in = 1.12
        zoom_out = 1 / zoom_in
        factor = zoom_in if event.angleDelta().y() > 0 else zoom_out
        self.scale(factor, factor)

    def mousePressEvent(self, event) -> None:
        if event.button() == Qt.MouseButton.MiddleButton:
            self._panning = True
            self._pan_start = event.pos()
            self.setCursor(Qt.CursorShape.ClosedHandCursor)
            event.accept()
            return
        super().mousePressEvent(event)

    def mouseMoveEvent(self, event) -> None:
        if self._panning:
            delta = event.pos() - self._pan_start
            self._pan_start = event.pos()
            self.horizontalScrollBar().setValue(self.horizontalScrollBar().value() - delta.x())
            self.verticalScrollBar().setValue(self.verticalScrollBar().value() - delta.y())
            event.accept()
            return
        super().mouseMoveEvent(event)

    def mouseReleaseEvent(self, event) -> None:
        if event.button() == Qt.MouseButton.MiddleButton and self._panning:
            self._panning = False
            self.setCursor(Qt.CursorShape.ArrowCursor)
            event.accept()
            return
        super().mouseReleaseEvent(event)

    def drawBackground(self, painter: QPainter, rect: QRectF) -> None:
        super().drawBackground(painter, rect)

        left = int(math.floor(rect.left() / SNAP_X) * SNAP_X)
        right = int(math.ceil(rect.right() / SNAP_X) * SNAP_X)
        top = int(math.floor(rect.top() / SNAP_Y) * SNAP_Y)
        bottom = int(math.ceil(rect.bottom() / SNAP_Y) * SNAP_Y)

        minor_pen = QPen(QColor(54, 54, 62), 1)
        major_pen = QPen(QColor(68, 68, 80), 1)

        for x in range(left, right + 1, SNAP_X):
            painter.setPen(major_pen if x % (SNAP_X * 5) == 0 else minor_pen)
            painter.drawLine(x, top, x, bottom)

        for y in range(top, bottom + 1, SNAP_Y):
            painter.setPen(major_pen if y % (SNAP_Y * 5) == 0 else minor_pen)
            painter.drawLine(left, y, right, y)

        painter.setPen(QPen(QColor(100, 100, 120), 1.2))
        painter.drawLine(left, 0, right, 0)
        painter.drawLine(0, top, 0, bottom)


class PropertyPanel(QWidget):
    node_updated = pyqtSignal()
    node_id_renamed = pyqtSignal(str, str)
    required_nodes_changed = pyqtSignal(str, list)

    def __init__(self, parent: QWidget | None = None) -> None:
        super().__init__(parent)
        self._node_item: SkillNodeItem | None = None

        self.id_input = QLineEdit()
        self.display_name_input = QLineEdit()
        self.category_input = QComboBox()
        self.category_input.setEditable(True)
        self.category_input.addItems(["combat", "survival", "arcane", "exploration", "technology"])

        self.cost_input = QSpinBox()
        self.cost_input.setRange(1, 999)

        self.required_level_input = QSpinBox()
        self.required_level_input.setRange(1, 999)

        self.required_class_input = QLineEdit()
        self.required_nodes_input = QLineEdit()
        self.required_nodes_input.setPlaceholderText("comma-separated IDs")

        self.modifier_editor = ModifierEditor()

        form = QFormLayout(self)
        form.addRow("id", self.id_input)
        form.addRow("displayName", self.display_name_input)
        form.addRow("category", self.category_input)
        form.addRow("cost", self.cost_input)
        form.addRow("requiredLevel", self.required_level_input)
        form.addRow("requiredClass", self.required_class_input)
        form.addRow("requiredNodes", self.required_nodes_input)
        form.addRow(self.modifier_editor)

        for widget in [
            self.id_input,
            self.display_name_input,
            self.required_class_input,
            self.required_nodes_input,
        ]:
            widget.editingFinished.connect(self._apply_changes)

        self.category_input.currentTextChanged.connect(self._apply_changes)
        self.cost_input.valueChanged.connect(self._apply_changes)
        self.required_level_input.valueChanged.connect(self._apply_changes)
        self.modifier_editor.changed.connect(self._apply_changes)

        self.setEnabled(False)

    def set_node(self, node_item: SkillNodeItem | None) -> None:
        self._node_item = node_item
        self.setEnabled(node_item is not None)
        if node_item is None:
            self.id_input.clear()
            self.display_name_input.clear()
            self.required_class_input.clear()
            self.required_nodes_input.clear()
            self.modifier_editor.set_modifiers([])
            return

        node = node_item.data
        self.id_input.setText(node.id)
        self.display_name_input.setText(node.display_name)
        self.category_input.setCurrentText(node.category)
        self.cost_input.setValue(node.cost)
        self.required_level_input.setValue(node.required_level)
        self.required_class_input.setText(node.required_class)
        self.required_nodes_input.setText(",".join(node.required_nodes))
        self.modifier_editor.set_modifiers(node.modifiers)

    def _apply_changes(self) -> None:
        if self._node_item is None:
            return

        node = self._node_item.data
        old_id = node.id
        requested_id = self.id_input.text().strip() or node.id
        requested_required = [v.strip() for v in self.required_nodes_input.text().split(",") if v.strip()]

        if requested_id != old_id:
            node.id = requested_id
            self.node_id_renamed.emit(old_id, requested_id)

        node.display_name = self.display_name_input.text().strip()
        node.category = self.category_input.currentText().strip().lower() or "combat"
        node.cost = int(self.cost_input.value())
        node.required_level = int(self.required_level_input.value())
        node.required_class = self.required_class_input.text().strip()
        node.required_nodes = requested_required
        node.modifiers = self.modifier_editor.modifiers()

        self.required_nodes_changed.emit(node.id, node.required_nodes)
        self._node_item.refresh_visuals()
        self.node_updated.emit()


class SkillTreeEditorWindow(QMainWindow):
    def __init__(self) -> None:
        super().__init__()
        self.setWindowTitle("ExtremeCraft Skill Tree Editor")
        self.resize(1480, 920)

        self.scene = SkillTreeScene()
        self.scene.node_selected.connect(self._on_node_selected)
        self.scene.links_changed.connect(self._sync_required_nodes_from_links)
        self.view = SkillTreeView(self.scene)

        self.tree_selector = QComboBox()
        self.tree_selector.addItems(DEFAULT_TREES)

        self.property_panel = PropertyPanel()
        self.property_panel.node_id_renamed.connect(self._rename_node)
        self.property_panel.required_nodes_changed.connect(self._apply_required_nodes_links)

        self.current_tree_name = self.tree_selector.currentText()
        self.tree_data: dict[str, SkillTreeData] = {name: SkillTreeData(name) for name in DEFAULT_TREES}

        self._build_ui()
        self._load_tree(self.current_tree_name)

    def _build_ui(self) -> None:
        toolbar = QToolBar("Main Toolbar")
        toolbar.setMovable(False)
        self.addToolBar(toolbar)

        new_node_action = QAction("New Node", self)
        new_node_action.triggered.connect(self._create_node)
        toolbar.addAction(new_node_action)

        delete_node_action = QAction("Delete Node", self)
        delete_node_action.triggered.connect(self._delete_selected_node)
        toolbar.addAction(delete_node_action)

        self.connect_action = QAction("Connect Nodes", self)
        self.connect_action.setCheckable(True)
        self.connect_action.toggled.connect(self.scene.set_connect_mode)
        toolbar.addAction(self.connect_action)

        export_action = QAction("Export JSON", self)
        export_action.triggered.connect(self._export_current_tree)
        toolbar.addAction(export_action)

        load_action = QAction("Load JSON", self)
        load_action.triggered.connect(self._load_json_dialog)
        toolbar.addAction(load_action)

        top_panel = QWidget()
        top_layout = QHBoxLayout(top_panel)
        top_layout.addWidget(QLabel("Tree"))
        top_layout.addWidget(self.tree_selector)
        top_layout.addStretch(1)
        self.tree_selector.currentTextChanged.connect(self._on_tree_changed)

        canvas_wrapper = QWidget()
        canvas_layout = QVBoxLayout(canvas_wrapper)
        canvas_layout.addWidget(top_panel)
        canvas_layout.addWidget(self.view)

        splitter = QSplitter()
        splitter.addWidget(canvas_wrapper)
        splitter.addWidget(self.property_panel)
        splitter.setSizes([1080, 340])

        container = QWidget()
        layout = QVBoxLayout(container)
        layout.addWidget(splitter)
        self.setCentralWidget(container)

    def _active_tree_data(self) -> SkillTreeData:
        return self.tree_data[self.current_tree_name]

    def _on_tree_changed(self, tree_name: str) -> None:
        if not tree_name:
            return
        self._persist_scene_to_tree(self.current_tree_name)
        self.current_tree_name = tree_name
        if tree_name not in self.tree_data:
            self.tree_data[tree_name] = SkillTreeData(tree_name)
        self._load_tree(tree_name)

    def _on_node_selected(self, node_item: SkillNodeItem | None) -> None:
        self.property_panel.set_node(node_item)

    def _load_tree(self, tree_name: str) -> None:
        self.scene.clear_tree()
        tree = self.tree_data[tree_name]
        for node in tree.nodes.values():
            self.scene.add_node_item(node)

        for node in tree.nodes.values():
            for req in node.required_nodes:
                if req in tree.nodes:
                    self.scene.add_link(req, node.id)

    def _persist_scene_to_tree(self, tree_name: str) -> None:
        tree = self.tree_data.get(tree_name)
        if tree is None:
            return

        tree.nodes = {node_id: item.data for node_id, item in self.scene.node_items.items()}
        self._sync_required_nodes_from_links(persist_only=True)

    def _rename_node(self, old_id: str, new_id: str) -> None:
        if old_id == new_id:
            return
        if not new_id:
            return
        if new_id in self.scene.node_items:
            QMessageBox.warning(self, "Duplicate Node ID", f"Node id '{new_id}' already exists.")
            selected = self.scene.selectedItems()
            if selected and isinstance(selected[0], SkillNodeItem):
                selected[0].data.id = old_id
                self.property_panel.set_node(selected[0])
            return

        node_item = self.scene.node_items.get(old_id)
        if node_item is None:
            return

        del self.scene.node_items[old_id]
        self.scene.node_items[new_id] = node_item

        tree = self._active_tree_data()
        if old_id in tree.nodes:
            tree.nodes[new_id] = tree.nodes.pop(old_id)

        updated_links: dict[tuple[str, str], LinkItem] = {}
        for (source_id, target_id), link in list(self.scene.links.items()):
            source_new = new_id if source_id == old_id else source_id
            target_new = new_id if target_id == old_id else target_id
            updated_links[(source_new, target_new)] = link
        self.scene.links = updated_links

        for item in self.scene.node_items.values():
            item.data.required_nodes = [new_id if req == old_id else req for req in item.data.required_nodes]

        self._sync_required_nodes_from_links()

    def _apply_required_nodes_links(self, node_id: str, required_nodes: list[str]) -> None:
        if node_id not in self.scene.node_items:
            return

        current_required = {source for source, target in self.scene.links.keys() if target == node_id}
        desired_required = {req for req in required_nodes if req in self.scene.node_items and req != node_id}

        for source in sorted(desired_required - current_required):
            self.scene.add_link(source, node_id)

        for source in sorted(current_required - desired_required):
            self.scene.remove_link(source, node_id)

        self._sync_required_nodes_from_links()

    def _sync_required_nodes_from_links(self, persist_only: bool = False) -> None:
        incoming: dict[str, list[str]] = {node_id: [] for node_id in self.scene.node_items}
        for source_id, target_id in self.scene.links.keys():
            if target_id in incoming:
                incoming[target_id].append(source_id)

        for node_id, node_item in self.scene.node_items.items():
            # Auto-populate requiredNodes from link graph for consistency.
            node_item.data.required_nodes = sorted(set(incoming[node_id]))

        if not persist_only:
            selected = self.scene.selectedItems()
            if selected and isinstance(selected[0], SkillNodeItem):
                self.property_panel.set_node(selected[0])

    def _create_node(self) -> None:
        tree = self._active_tree_data()
        idx = 1
        while True:
            node_id = f"{self.current_tree_name}_node_{idx}"
            if node_id not in tree.nodes and node_id not in self.scene.node_items:
                break
            idx += 1

        node = SkillNodeData(
            id=node_id,
            display_name=node_id,
            category=self.current_tree_name if self.current_tree_name in CATEGORY_COLORS else "combat",
            cost=1,
            required_level=1,
            x=round(self.view.mapToScene(self.view.viewport().rect().center()).x() / SNAP_X) * SNAP_X,
            y=round(self.view.mapToScene(self.view.viewport().rect().center()).y() / SNAP_Y) * SNAP_Y,
        )
        tree.nodes[node.id] = node
        item = self.scene.add_node_item(node)
        item.setSelected(True)

    def _delete_selected_node(self) -> None:
        selected = self.scene.selectedItems()
        if not selected or not isinstance(selected[0], SkillNodeItem):
            return

        node_id = selected[0].data.id
        self.scene.remove_node_item(node_id)
        tree = self._active_tree_data()
        if node_id in tree.nodes:
            del tree.nodes[node_id]

        self._sync_required_nodes_from_links()
        self.property_panel.set_node(None)

    def _load_json_dialog(self) -> None:
        start = DEFAULT_EXPORT_ROOT
        start.mkdir(parents=True, exist_ok=True)
        file_path, _ = QFileDialog.getOpenFileName(self, "Load Skill Tree JSON", str(start), "JSON Files (*.json)")
        if not file_path:
            return

        try:
            payload = json.loads(Path(file_path).read_text(encoding="utf-8"))
            tree_name = str(payload.get("tree", "")).strip() or self.current_tree_name
            nodes = payload.get("nodes", [])
            parsed = SkillTreeData(tree_name=tree_name)
            for raw_node in nodes:
                node = SkillNodeData.from_json(raw_node)
                if node.id:
                    parsed.nodes[node.id] = node

            if self.tree_selector.findText(tree_name) < 0:
                self.tree_selector.addItem(tree_name)
            self.tree_data[tree_name] = parsed
            self.tree_selector.setCurrentText(tree_name)
            self._load_tree(tree_name)
        except Exception as exc:  # noqa: BLE001
            QMessageBox.critical(self, "Load Failed", f"Could not load JSON:\n{exc}")

    def _export_current_tree(self) -> None:
        self._persist_scene_to_tree(self.current_tree_name)
        tree = self._active_tree_data()

        export_dir = DEFAULT_EXPORT_ROOT
        export_dir.mkdir(parents=True, exist_ok=True)
        default_path = export_dir / f"{self.current_tree_name}.json"

        file_path, _ = QFileDialog.getSaveFileName(
            self,
            "Export Skill Tree JSON",
            str(default_path),
            "JSON Files (*.json)",
        )
        if not file_path:
            file_path = str(default_path)

        payload = tree.to_export()
        out_path = Path(file_path)
        out_path.parent.mkdir(parents=True, exist_ok=True)
        out_path.write_text(json.dumps(payload, indent=2) + "\n", encoding="utf-8")

        QMessageBox.information(
            self,
            "Export Complete",
            f"Exported {len(tree.nodes)} nodes to:\n{out_path}",
        )


def main() -> None:
    app = QApplication(sys.argv)
    app.setApplicationName("ExtremeCraft Skill Tree Editor")
    window = SkillTreeEditorWindow()
    window.show()
    sys.exit(app.exec())


if __name__ == "__main__":
    main()
