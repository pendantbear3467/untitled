#!/usr/bin/env python3
"""ExtremeCraft Skill Tree Editor.

Professional PyQt6 editor for creating, validating, simulating, and exporting
RPG skill trees for the ExtremeCraft Minecraft Forge mod.
"""

from __future__ import annotations

import json
import math
import os
import sys
from dataclasses import dataclass, field
from pathlib import Path
from typing import Callable, Iterable

from PyQt6.QtCore import QPoint, QPointF, QRectF, Qt, pyqtSignal
from PyQt6.QtGui import (
    QAction,
    QBrush,
    QColor,
    QKeySequence,
    QPainter,
    QPainterPath,
    QPen,
    QPixmap,
    QTransform,
    QUndoCommand,
    QUndoStack,
)
from PyQt6.QtGui import QDesktopServices
from PyQt6.QtWidgets import (
    QApplication,
    QComboBox,
    QDialog,
    QDialogButtonBox,
    QDoubleSpinBox,
    QFileDialog,
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
    QMenu,
    QMessageBox,
    QPushButton,
    QSpinBox,
    QSplitter,
    QTextEdit,
    QToolBar,
    QVBoxLayout,
    QWidget,
)
from PyQt6.QtCore import QUrl


CATEGORY_COLORS: dict[str, QColor] = {
    "combat": QColor(196, 64, 64),
    "survival": QColor(64, 164, 85),
    "arcane": QColor(150, 88, 196),
    "exploration": QColor(216, 177, 68),
    "explorer": QColor(216, 177, 68),
    "technology": QColor(82, 192, 208),
}

DEFAULT_TREES = ["combat", "survival", "arcane", "exploration", "technology"]
SNAP_X = 60
SNAP_Y = 40
NODE_RADIUS = 26
CANVAS_EXTENT = 12000
MIN_COST = 1
MAX_COST = 999
MIN_LEVEL = 1
MAX_LEVEL = 999
MIN_MODIFIER_VALUE = -100000.0
MAX_MODIFIER_VALUE = 100000.0

REPO_ROOT = Path(__file__).resolve().parents[1]
ICON_ROOT = REPO_ROOT / "src" / "main" / "resources" / "assets" / "extremecraft"
ICON_FOLDER = ICON_ROOT / "textures" / "gui" / "skills"
DEFAULT_EXPORT_ROOT = REPO_ROOT / "src" / "main" / "resources" / "data" / "extremecraft" / "skill_trees"
DEFAULT_PROJECT_PATH = REPO_ROOT / ".extremecraft_project.json"


def _clamp(value: float, lo: float, hi: float) -> float:
    return max(lo, min(hi, value))


@dataclass(slots=True)
class Modifier:
    type: str
    value: float

    def clone(self) -> "Modifier":
        return Modifier(self.type, self.value)


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

    def clone(self) -> "SkillNodeData":
        return SkillNodeData(
            id=self.id,
            display_name=self.display_name,
            category=self.category,
            cost=self.cost,
            required_nodes=list(self.required_nodes),
            modifiers=[m.clone() for m in self.modifiers],
            required_level=self.required_level,
            required_class=self.required_class,
            x=self.x,
            y=self.y,
        )

    def to_export(self, bounds: QRectF) -> dict:
        x_val = int(_clamp(self.x, bounds.left(), bounds.right()))
        y_val = int(_clamp(self.y, bounds.top(), bounds.bottom()))
        return {
            "id": self.id,
            "displayName": self.display_name,
            "x": x_val,
            "y": y_val,
            "cost": int(_clamp(self.cost, MIN_COST, MAX_COST)),
            "category": self.category,
            "requiredLevel": int(_clamp(self.required_level, MIN_LEVEL, MAX_LEVEL)),
            "requiredClass": self.required_class,
            "requires": list(self.required_nodes),
            "modifiers": [{"type": m.type, "value": m.value} for m in self.modifiers],
        }

    @classmethod
    def from_json(cls, payload: dict) -> "SkillNodeData":
        modifiers: list[Modifier] = []
        for raw in payload.get("modifiers", []):
            modifiers.append(
                Modifier(
                    type=str(raw.get("type", "")).strip(),
                    value=float(raw.get("value", 0.0)),
                )
            )
        return cls(
            id=str(payload.get("id", "")).strip(),
            display_name=str(payload.get("displayName", "")).strip(),
            category=str(payload.get("category", "combat")).strip().lower() or "combat",
            cost=int(payload.get("cost", 1)),
            required_nodes=[str(v).strip() for v in payload.get("requires", []) if str(v).strip()],
            modifiers=modifiers,
            required_level=int(payload.get("requiredLevel", 1)),
            required_class=str(payload.get("requiredClass", "")).strip(),
            x=float(payload.get("x", 0.0)),
            y=float(payload.get("y", 0.0)),
        )


@dataclass(slots=True)
class SkillTreeData:
    tree_name: str
    nodes: dict[str, SkillNodeData] = field(default_factory=dict)

    def clone(self) -> "SkillTreeData":
        return SkillTreeData(
            tree_name=self.tree_name,
            nodes={node_id: node.clone() for node_id, node in self.nodes.items()},
        )


class TreeValidator:
    """Validation helper used on load, edit, and export paths."""

    @staticmethod
    def validate_tree(tree: SkillTreeData, bounds: QRectF) -> list[str]:
        errors: list[str] = []
        ids_seen: set[str] = set()

        for node_id, node in tree.nodes.items():
            if not node.id.strip():
                errors.append(f"Node with empty id detected at internal key '{node_id}'.")
                continue

            if node.id != node_id:
                errors.append(f"Node id mismatch: key '{node_id}' != node.id '{node.id}'.")

            if node.id in ids_seen:
                errors.append(f"Duplicate node id: {node.id}")
            ids_seen.add(node.id)

            if not node.display_name.strip():
                errors.append(f"Node '{node.id}' has empty displayName.")

            if not (MIN_COST <= node.cost <= MAX_COST):
                errors.append(f"Node '{node.id}' cost out of range ({MIN_COST}-{MAX_COST}): {node.cost}")

            if not (MIN_LEVEL <= node.required_level <= MAX_LEVEL):
                errors.append(
                    f"Node '{node.id}' requiredLevel out of range ({MIN_LEVEL}-{MAX_LEVEL}): {node.required_level}"
                )

            if not (bounds.left() <= node.x <= bounds.right() and bounds.top() <= node.y <= bounds.bottom()):
                errors.append(
                    f"Node '{node.id}' position out of canvas bounds: ({int(node.x)}, {int(node.y)})."
                )

            for req in node.required_nodes:
                if req not in tree.nodes:
                    errors.append(f"Node '{node.id}' references missing required node '{req}'.")

            for modifier in node.modifiers:
                if not modifier.type.strip():
                    errors.append(f"Node '{node.id}' has modifier with empty type.")
                if not (MIN_MODIFIER_VALUE <= modifier.value <= MAX_MODIFIER_VALUE):
                    errors.append(
                        f"Node '{node.id}' modifier '{modifier.type}' out of range "
                        f"({MIN_MODIFIER_VALUE}..{MAX_MODIFIER_VALUE}): {modifier.value}"
                    )

        errors.extend(TreeValidator._cycle_errors(tree))
        return errors

    @staticmethod
    def _cycle_errors(tree: SkillTreeData) -> list[str]:
        errors: list[str] = []
        graph = {node_id: list(node.required_nodes) for node_id, node in tree.nodes.items()}
        visited: set[str] = set()
        stack: set[str] = set()

        def dfs(node_id: str) -> None:
            if node_id in stack:
                errors.append(f"Circular dependency detected involving '{node_id}'.")
                return
            if node_id in visited:
                return
            visited.add(node_id)
            stack.add(node_id)
            for req in graph.get(node_id, []):
                if req in graph:
                    dfs(req)
            stack.remove(node_id)

        for node_id in list(graph.keys()):
            dfs(node_id)

        return sorted(set(errors))


class CallbackCommand(QUndoCommand):
    """Generic undo command backed by callbacks."""

    def __init__(self, text: str, redo_cb: Callable[[], None], undo_cb: Callable[[], None]) -> None:
        super().__init__(text)
        self._redo_cb = redo_cb
        self._undo_cb = undo_cb

    def redo(self) -> None:
        self._redo_cb()

    def undo(self) -> None:
        self._undo_cb()


class ModifierEditor(QWidget):
    changed = pyqtSignal()

    def __init__(self, parent: QWidget | None = None) -> None:
        super().__init__(parent)
        self.list_widget = QListWidget()
        self.type_input = QLineEdit()
        self.value_input = QDoubleSpinBox()
        self.value_input.setDecimals(4)
        self.value_input.setRange(MIN_MODIFIER_VALUE, MAX_MODIFIER_VALUE)
        self.value_input.setSingleStep(0.01)

        add_btn = QPushButton("Add")
        remove_btn = QPushButton("Remove")

        row = QHBoxLayout()
        row.addWidget(self.type_input)
        row.addWidget(self.value_input)
        row.addWidget(add_btn)
        row.addWidget(remove_btn)

        layout = QVBoxLayout(self)
        layout.addWidget(QLabel("Modifiers"))
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
        out: list[Modifier] = []
        for idx in range(self.list_widget.count()):
            item = self.list_widget.item(idx)
            if item is None:
                continue
            raw = item.text().strip()
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
            out.append(Modifier(mod_type, value))
        return out

    def _add_modifier(self) -> None:
        mod_type = self.type_input.text().strip()
        if not mod_type:
            return
        self.list_widget.addItem(f"{mod_type}:{self.value_input.value()}")
        self.type_input.clear()
        self.value_input.setValue(0.0)
        self.changed.emit()

    def _remove_modifier(self) -> None:
        row = self.list_widget.currentRow()
        if row >= 0:
            self.list_widget.takeItem(row)
            self.changed.emit()


class NodePropertiesDialog(QDialog):
    """Modal property editor that participates cleanly in undo/redo."""

    def __init__(self, node: SkillNodeData, parent: QWidget | None = None) -> None:
        super().__init__(parent)
        self.setWindowTitle("Edit Node")
        self.resize(480, 480)

        self.id_input = QLineEdit(node.id)
        self.display_name_input = QLineEdit(node.display_name)
        self.category_input = QComboBox()
        self.category_input.setEditable(True)
        self.category_input.addItems(["combat", "survival", "arcane", "exploration", "technology"])
        self.category_input.setCurrentText(node.category)

        self.cost_input = QSpinBox()
        self.cost_input.setRange(MIN_COST, MAX_COST)
        self.cost_input.setValue(node.cost)

        self.level_input = QSpinBox()
        self.level_input.setRange(MIN_LEVEL, MAX_LEVEL)
        self.level_input.setValue(node.required_level)

        self.class_input = QLineEdit(node.required_class)
        self.requires_input = QLineEdit(",".join(node.required_nodes))
        self.modifier_editor = ModifierEditor()
        self.modifier_editor.set_modifiers(node.modifiers)

        form = QFormLayout()
        form.addRow("id", self.id_input)
        form.addRow("displayName", self.display_name_input)
        form.addRow("category", self.category_input)
        form.addRow("cost", self.cost_input)
        form.addRow("requiredLevel", self.level_input)
        form.addRow("requiredClass", self.class_input)
        form.addRow("requiredNodes", self.requires_input)
        form.addRow(self.modifier_editor)

        buttons = QDialogButtonBox(
            QDialogButtonBox.StandardButton.Ok | QDialogButtonBox.StandardButton.Cancel
        )
        buttons.accepted.connect(self.accept)
        buttons.rejected.connect(self.reject)

        layout = QVBoxLayout(self)
        layout.addLayout(form)
        layout.addWidget(buttons)

    def to_data(self, source: SkillNodeData) -> SkillNodeData:
        out = source.clone()
        out.id = self.id_input.text().strip()
        out.display_name = self.display_name_input.text().strip()
        out.category = self.category_input.currentText().strip().lower() or "combat"
        out.cost = self.cost_input.value()
        out.required_level = self.level_input.value()
        out.required_class = self.class_input.text().strip()
        out.required_nodes = [v.strip() for v in self.requires_input.text().split(",") if v.strip()]
        out.modifiers = self.modifier_editor.modifiers()
        return out


class SkillNodeItem(QGraphicsObject):
    moved_finished = pyqtSignal(str, QPointF, QPointF)
    clicked = pyqtSignal(str)

    def __init__(self, data: SkillNodeData) -> None:
        super().__init__()
        self.node_data = data
        self.setFlags(
            QGraphicsItem.GraphicsItemFlag.ItemIsMovable
            | QGraphicsItem.GraphicsItemFlag.ItemIsSelectable
            | QGraphicsItem.GraphicsItemFlag.ItemSendsGeometryChanges
        )
        self.setAcceptHoverEvents(True)
        self.setCacheMode(QGraphicsItem.CacheMode.DeviceCoordinateCache)
        self.label = QGraphicsSimpleTextItem(self._display_label(), self)
        self.label.setBrush(QBrush(QColor(236, 236, 236)))
        self.label.setPos(-NODE_RADIUS, NODE_RADIUS + 6)
        self._icon_pixmap: QPixmap | None = None
        self._missing_icon = False
        self._sim_unlocked = False
        self._sim_available = False
        self._drag_start_pos = QPointF()
        self.reload_icon()
        self.setPos(self.node_data.x, self.node_data.y)
        self.update_tooltip()

    def _display_label(self) -> str:
        return self.node_data.display_name or self.node_data.id

    @property
    def missing_icon(self) -> bool:
        return self._missing_icon

    def set_simulation_state(self, unlocked: bool, available: bool) -> None:
        self._sim_unlocked = unlocked
        self._sim_available = available
        self.update()

    def reload_icon(self) -> None:
        icon_path = ICON_FOLDER / f"{self.node_data.id}.png"
        if icon_path.exists():
            pixmap = QPixmap(str(icon_path))
            if not pixmap.isNull():
                self._icon_pixmap = pixmap.scaled(
                    NODE_RADIUS * 2 - 6,
                    NODE_RADIUS * 2 - 6,
                    Qt.AspectRatioMode.KeepAspectRatio,
                    Qt.TransformationMode.SmoothTransformation,
                )
                self._missing_icon = False
                return
        self._icon_pixmap = None
        self._missing_icon = True

    def refresh_visuals(self) -> None:
        self.label.setText(self._display_label())
        self.reload_icon()
        self.update_tooltip()
        self.update()

    def update_tooltip(self) -> None:
        mods = "\n".join([f"- {m.type}: {m.value}" for m in self.node_data.modifiers]) or "- none"
        reqs = ", ".join(self.node_data.required_nodes) or "none"
        tooltip = (
            f"{self._display_label()}\n"
            f"ID: {self.node_data.id}\n"
            f"Cost: {self.node_data.cost}\n"
            f"Required Level: {self.node_data.required_level}\n"
            f"Required Class: {self.node_data.required_class or 'none'}\n"
            f"Required Nodes: {reqs}\n\n"
            f"Modifiers:\n{mods}"
        )
        self.setToolTip(tooltip)

    def category_color(self) -> QColor:
        return CATEGORY_COLORS.get(self.node_data.category, QColor(120, 120, 120))

    def boundingRect(self) -> QRectF:
        return QRectF(-NODE_RADIUS - 4, -NODE_RADIUS - 8, NODE_RADIUS * 2 + 8, NODE_RADIUS * 2 + 40)

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
            clip_path = QPainterPath()
            clip_path.addEllipse(QPointF(0, 0), NODE_RADIUS - 3, NODE_RADIUS - 3)
            painter.setClipPath(clip_path)
            painter.drawPixmap(
                int(-self._icon_pixmap.width() / 2),
                int(-self._icon_pixmap.height() / 2),
                self._icon_pixmap,
            )
            painter.setClipping(False)
        else:
            painter.setPen(QPen(QColor(244, 244, 244), 1.2))
            short_label = (
                self.node_data.id[:6] + "..."
                if len(self.node_data.id) > 9
                else self.node_data.id
            )
            painter.drawText(QRectF(-NODE_RADIUS + 5, -8, NODE_RADIUS * 2 - 10, 16), Qt.AlignmentFlag.AlignCenter, short_label)

        if self._missing_icon:
            painter.setPen(QPen(QColor(70, 0, 0), 1.0))
            painter.setBrush(QBrush(QColor(225, 62, 62)))
            painter.drawEllipse(QPointF(NODE_RADIUS - 4, -NODE_RADIUS + 4), 4, 4)

        if self._sim_available and not self._sim_unlocked:
            painter.setPen(QPen(QColor(255, 255, 255, 120), 1.5, Qt.PenStyle.DashLine))
            painter.setBrush(Qt.BrushStyle.NoBrush)
            painter.drawEllipse(QPointF(0, 0), NODE_RADIUS + 2, NODE_RADIUS + 2)

        if self._sim_unlocked:
            painter.setPen(QPen(QColor(66, 210, 255, 215), 3.0))
            painter.setBrush(Qt.BrushStyle.NoBrush)
            painter.drawEllipse(QPointF(0, 0), NODE_RADIUS + 3, NODE_RADIUS + 3)

        if self.isSelected():
            painter.setPen(QPen(QColor(255, 255, 255, 220), 2.0, Qt.PenStyle.DashLine))
            painter.setBrush(Qt.BrushStyle.NoBrush)
            painter.drawEllipse(QPointF(0, 0), NODE_RADIUS + 5, NODE_RADIUS + 5)

    def itemChange(self, change, value):
        if change == QGraphicsItem.GraphicsItemChange.ItemPositionChange:
            p = value
            return QPointF(round(p.x() / SNAP_X) * SNAP_X, round(p.y() / SNAP_Y) * SNAP_Y)
        if change == QGraphicsItem.GraphicsItemChange.ItemPositionHasChanged:
            self.node_data.x = self.pos().x()
            self.node_data.y = self.pos().y()
        return super().itemChange(change, value)

    def mousePressEvent(self, event) -> None:
        self._drag_start_pos = self.pos()
        super().mousePressEvent(event)

    def mouseReleaseEvent(self, event) -> None:
        super().mouseReleaseEvent(event)
        self.clicked.emit(self.node_data.id)
        if self._drag_start_pos != self.pos():
            self.moved_finished.emit(self.node_data.id, self._drag_start_pos, self.pos())


class LinkItem(QGraphicsLineItem):
    def __init__(self, source: SkillNodeItem, target: SkillNodeItem) -> None:
        super().__init__()
        self.source = source
        self.target = target
        self.setZValue(-1)
        self.setPen(QPen(QColor(170, 190, 220), 2.0))
        self.setCacheMode(QGraphicsItem.CacheMode.DeviceCoordinateCache)
        self.update_line()

    def update_line(self) -> None:
        self.setLine(self.source.pos().x(), self.source.pos().y(), self.target.pos().x(), self.target.pos().y())


class SkillTreeScene(QGraphicsScene):
    node_selected = pyqtSignal(object)
    link_requested = pyqtSignal(str, str)
    link_removed_requested = pyqtSignal(str, str)
    node_move_finished = pyqtSignal(str, QPointF, QPointF)
    node_clicked = pyqtSignal(str)
    node_context_requested = pyqtSignal(str, QPoint)
    empty_context_requested = pyqtSignal(QPoint)

    def __init__(self, parent: QWidget | None = None) -> None:
        super().__init__(parent)
        self.setSceneRect(-CANVAS_EXTENT, -CANVAS_EXTENT, CANVAS_EXTENT * 2, CANVAS_EXTENT * 2)
        self.node_items: dict[str, SkillNodeItem] = {}
        self.links: dict[tuple[str, str], LinkItem] = {}
        self.connect_mode = False
        self._drag_line: QGraphicsLineItem | None = None
        self._connect_start_id: str | None = None

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
        self._connect_start_id = None

    def add_node_item(self, node: SkillNodeData) -> SkillNodeItem:
        item = SkillNodeItem(node)
        item.moved_finished.connect(self.node_move_finished.emit)
        item.clicked.connect(self.node_clicked.emit)
        self.addItem(item)
        self.node_items[node.id] = item
        return item

    def remove_node_item(self, node_id: str) -> None:
        node_item = self.node_items.get(node_id)
        if node_item is None:
            return
        for source_id, target_id in list(self.links.keys()):
            if node_id in (source_id, target_id):
                self.remove_link_item(source_id, target_id)
        self.removeItem(node_item)
        del self.node_items[node_id]

    def add_link_item(self, source_id: str, target_id: str) -> bool:
        if source_id == target_id:
            return False
        key = (source_id, target_id)
        if key in self.links:
            return False
        source_item = self.node_items.get(source_id)
        target_item = self.node_items.get(target_id)
        if not source_item or not target_item:
            return False
        link_item = LinkItem(source_item, target_item)
        self.links[key] = link_item
        self.addItem(link_item)
        return True

    def remove_link_item(self, source_id: str, target_id: str) -> None:
        key = (source_id, target_id)
        item = self.links.get(key)
        if item is None:
            return
        self.removeItem(item)
        del self.links[key]

    def update_links_for_node(self, node_id: str) -> None:
        for (source_id, target_id), link in self.links.items():
            if source_id == node_id or target_id == node_id:
                link.update_line()

    def set_connect_mode(self, enabled: bool) -> None:
        self.connect_mode = enabled

    def set_highlight_edges(self, edges: set[tuple[str, str]]) -> None:
        for key, link in self.links.items():
            if key in edges:
                link.setPen(QPen(QColor(245, 216, 95), 3.0))
            else:
                link.setPen(QPen(QColor(170, 190, 220), 2.0))

    def mousePressEvent(self, event) -> None:
        if self.connect_mode and event.button() == Qt.MouseButton.LeftButton:
            item = self.itemAt(event.scenePos(), self.views()[0].transform() if self.views() else QTransform())
            if isinstance(item, SkillNodeItem):
                self._connect_start_id = item.node_data.id
                self._drag_line = QGraphicsLineItem(
                    item.pos().x(), item.pos().y(), event.scenePos().x(), event.scenePos().y()
                )
                self._drag_line.setPen(QPen(QColor(220, 220, 255, 170), 1.5, Qt.PenStyle.DashLine))
                self.addItem(self._drag_line)
                event.accept()
                return
        super().mousePressEvent(event)

    def mouseMoveEvent(self, event) -> None:
        if self.connect_mode and self._drag_line is not None and self._connect_start_id is not None:
            start_item = self.node_items.get(self._connect_start_id)
            if start_item is not None:
                self._drag_line.setLine(
                    start_item.pos().x(),
                    start_item.pos().y(),
                    event.scenePos().x(),
                    event.scenePos().y(),
                )
                event.accept()
                return
        super().mouseMoveEvent(event)

    def mouseReleaseEvent(self, event) -> None:
        if self.connect_mode and self._connect_start_id is not None:
            end_item = self.itemAt(event.scenePos(), self.views()[0].transform() if self.views() else QTransform())
            if isinstance(end_item, SkillNodeItem) and end_item.node_data.id != self._connect_start_id:
                self.link_requested.emit(self._connect_start_id, end_item.node_data.id)
            if self._drag_line is not None:
                self.removeItem(self._drag_line)
            self._drag_line = None
            self._connect_start_id = None
            event.accept()
            return
        super().mouseReleaseEvent(event)

    def contextMenuEvent(self, event) -> None:
        item = self.itemAt(event.scenePos(), self.views()[0].transform() if self.views() else QTransform())
        if isinstance(item, SkillNodeItem):
            self.node_context_requested.emit(item.node_data.id, event.screenPos())
        else:
            self.empty_context_requested.emit(event.screenPos())
        event.accept()


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
        factor = 1.12 if event.angleDelta().y() > 0 else (1.0 / 1.12)
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

        minor_pen = QPen(QColor(52, 52, 60), 1)
        major_pen = QPen(QColor(70, 70, 84), 1)

        for x in range(left, right + 1, SNAP_X):
            painter.setPen(major_pen if x % (SNAP_X * 5) == 0 else minor_pen)
            painter.drawLine(x, top, x, bottom)

        for y in range(top, bottom + 1, SNAP_Y):
            painter.setPen(major_pen if y % (SNAP_Y * 5) == 0 else minor_pen)
            painter.drawLine(left, y, right, y)

        painter.setPen(QPen(QColor(110, 110, 126), 1.2))
        painter.drawLine(left, 0, right, 0)
        painter.drawLine(0, top, 0, bottom)


class MinimapWidget(QWidget):
    """Lightweight minimap for large graph navigation."""

    def __init__(self, view: SkillTreeView, scene: SkillTreeScene, parent: QWidget | None = None) -> None:
        super().__init__(parent)
        self.view = view
        self.scene = scene
        self.setMinimumSize(220, 160)

    def paintEvent(self, event) -> None:
        painter = QPainter(self)
        painter.fillRect(self.rect(), QColor(24, 24, 30))

        scene_rect = self.scene.sceneRect()
        if scene_rect.width() <= 0 or scene_rect.height() <= 0:
            return

        sx = self.width() / scene_rect.width()
        sy = self.height() / scene_rect.height()

        def map_point(p: QPointF) -> QPointF:
            return QPointF((p.x() - scene_rect.left()) * sx, (p.y() - scene_rect.top()) * sy)

        painter.setPen(QPen(QColor(68, 68, 78), 1))
        painter.drawRect(self.rect().adjusted(0, 0, -1, -1))

        for item in self.scene.node_items.values():
            p = map_point(item.pos())
            painter.setPen(Qt.PenStyle.NoPen)
            painter.setBrush(item.category_color())
            painter.drawEllipse(p, 2.5, 2.5)

        viewport_scene = self.view.mapToScene(self.view.viewport().rect()).boundingRect()
        tl = map_point(viewport_scene.topLeft())
        br = map_point(viewport_scene.bottomRight())
        painter.setPen(QPen(QColor(220, 220, 240), 1.2))
        painter.setBrush(Qt.BrushStyle.NoBrush)
        painter.drawRect(QRectF(tl, br))

    def mousePressEvent(self, event) -> None:
        scene_rect = self.scene.sceneRect()
        if scene_rect.width() <= 0 or scene_rect.height() <= 0:
            return

        rx = event.position().x() / max(1.0, self.width())
        ry = event.position().y() / max(1.0, self.height())
        sx = scene_rect.left() + rx * scene_rect.width()
        sy = scene_rect.top() + ry * scene_rect.height()
        self.view.centerOn(QPointF(sx, sy))
        self.update()


class SimulationPanel(QWidget):
    def __init__(self, parent: QWidget | None = None) -> None:
        super().__init__(parent)
        layout = QVBoxLayout(self)
        layout.addWidget(QLabel("Simulation Stats"))
        self.summary = QTextEdit()
        self.summary.setReadOnly(True)
        layout.addWidget(self.summary)

    def update_content(self, total_cost: int, modifiers: dict[str, float]) -> None:
        lines = [f"Total Skill Cost: {total_cost}", "", "Stats Gained:"]
        if not modifiers:
            lines.append("(none)")
        else:
            for mod, value in sorted(modifiers.items()):
                sign = "+" if value >= 0 else ""
                lines.append(f"{sign}{value} {mod}")
        self.summary.setPlainText("\n".join(lines))


class SkillTreeEditorWindow(QMainWindow):
    """Main editor window coordinating scene, data model, and tooling."""

    def __init__(self) -> None:
        super().__init__()
        self.setWindowTitle("ExtremeCraft Skill Tree Editor")
        self.resize(1660, 980)

        self.scene = SkillTreeScene()
        self.view = SkillTreeView(self.scene)
        self.minimap = MinimapWidget(self.view, self.scene)
        self.simulation_panel = SimulationPanel()

        self.undo_stack = QUndoStack(self)
        self._suspend_undo = False

        self.project_path: Path = DEFAULT_PROJECT_PATH
        self.trees: dict[str, SkillTreeData] = {name: SkillTreeData(name) for name in DEFAULT_TREES}
        self.current_tree_name = DEFAULT_TREES[0]
        self.sim_unlocked: dict[str, set[str]] = {name: set() for name in DEFAULT_TREES}

        self._build_ui()
        self._wire_scene_signals()
        self._refresh_tree_selector()
        self._load_tree_to_scene(self.current_tree_name)
        self._refresh_icon_warning()

    def _build_ui(self) -> None:
        toolbar = QToolBar("Main")
        toolbar.setMovable(False)
        self.addToolBar(toolbar)

        self.new_node_action = QAction("New Node", self)
        self.new_node_action.setShortcut(QKeySequence("Ctrl+N"))
        self.new_node_action.triggered.connect(self.create_node)
        toolbar.addAction(self.new_node_action)

        self.delete_node_action = QAction("Delete Node", self)
        self.delete_node_action.setShortcut(QKeySequence(Qt.Key.Key_Delete))
        self.delete_node_action.triggered.connect(self.delete_selected_node)
        toolbar.addAction(self.delete_node_action)

        self.duplicate_action = QAction("Duplicate Node", self)
        self.duplicate_action.setShortcut(QKeySequence("Ctrl+D"))
        self.duplicate_action.triggered.connect(self.duplicate_selected_node)
        toolbar.addAction(self.duplicate_action)

        self.connect_action = QAction("Connect Nodes", self)
        self.connect_action.setCheckable(True)
        self.connect_action.triggered.connect(self.scene.set_connect_mode)
        toolbar.addAction(self.connect_action)

        self.undo_action = self.undo_stack.createUndoAction(self, "Undo")
        if self.undo_action is not None:
            self.undo_action.setShortcut(QKeySequence("Ctrl+Z"))
        toolbar.addAction(self.undo_action)

        self.redo_action = self.undo_stack.createRedoAction(self, "Redo")
        if self.redo_action is not None:
            self.redo_action.setShortcut(QKeySequence("Ctrl+Y"))
        toolbar.addAction(self.redo_action)

        self.auto_arrange_action = QAction("Auto Arrange", self)
        self.auto_arrange_action.triggered.connect(self.auto_arrange_tree)
        toolbar.addAction(self.auto_arrange_action)

        self.export_action = QAction("Export Tree", self)
        self.export_action.setShortcut(QKeySequence("Ctrl+S"))
        self.export_action.triggered.connect(self.export_current_tree)
        toolbar.addAction(self.export_action)

        self.export_all_action = QAction("Export All", self)
        self.export_all_action.triggered.connect(self.export_all_trees)
        toolbar.addAction(self.export_all_action)

        self.load_tree_json_action = QAction("Load Tree JSON", self)
        self.load_tree_json_action.triggered.connect(self.load_tree_from_json)
        toolbar.addAction(self.load_tree_json_action)

        self.open_icon_folder_action = QAction("Open Icon Folder", self)
        self.open_icon_folder_action.triggered.connect(self.open_icon_folder)
        toolbar.addAction(self.open_icon_folder_action)

        self.sim_mode_action = QAction("Simulation Mode", self)
        self.sim_mode_action.setCheckable(True)
        self.sim_mode_action.triggered.connect(self._update_simulation_visuals)
        toolbar.addAction(self.sim_mode_action)

        toolbar.addSeparator()

        self.new_project_action = QAction("New Project", self)
        self.new_project_action.triggered.connect(self.new_project)
        toolbar.addAction(self.new_project_action)

        self.open_project_action = QAction("Open Project", self)
        self.open_project_action.triggered.connect(self.open_project)
        toolbar.addAction(self.open_project_action)

        self.save_project_action = QAction("Save Project", self)
        self.save_project_action.triggered.connect(self.save_project)
        toolbar.addAction(self.save_project_action)

        top_bar = QWidget()
        top_layout = QHBoxLayout(top_bar)
        top_layout.addWidget(QLabel("Tree"))

        self.tree_selector = QComboBox()
        self.tree_selector.currentTextChanged.connect(self.switch_tree)
        top_layout.addWidget(self.tree_selector)

        self.icon_warning = QLabel("")
        top_layout.addWidget(self.icon_warning)
        top_layout.addStretch(1)

        right_panel = QWidget()
        right_layout = QVBoxLayout(right_panel)
        right_layout.addWidget(QLabel("Minimap"))
        right_layout.addWidget(self.minimap)
        right_layout.addWidget(self.simulation_panel)

        splitter = QSplitter()

        canvas_panel = QWidget()
        canvas_layout = QVBoxLayout(canvas_panel)
        canvas_layout.addWidget(top_bar)
        canvas_layout.addWidget(self.view)

        splitter.addWidget(canvas_panel)
        splitter.addWidget(right_panel)
        splitter.setSizes([1220, 360])

        container = QWidget()
        root = QVBoxLayout(container)
        root.addWidget(splitter)
        self.setCentralWidget(container)

    def _wire_scene_signals(self) -> None:
        self.scene.node_move_finished.connect(self._on_node_moved)
        self.scene.link_requested.connect(self._request_link)
        self.scene.node_context_requested.connect(self._show_node_context_menu)
        self.scene.node_clicked.connect(self._on_node_clicked)

    def _active_tree(self) -> SkillTreeData:
        return self.trees[self.current_tree_name]

    def _refresh_tree_selector(self) -> None:
        self.tree_selector.blockSignals(True)
        self.tree_selector.clear()
        for tree_name in sorted(self.trees.keys()):
            self.tree_selector.addItem(tree_name)
        self.tree_selector.setCurrentText(self.current_tree_name)
        self.tree_selector.blockSignals(False)

    def _refresh_icon_warning(self) -> None:
        missing = sum(1 for item in self.scene.node_items.values() if item.missing_icon)
        self.icon_warning.setText(f"Missing icons: {missing}")

    def _load_tree_to_scene(self, tree_name: str) -> None:
        self.scene.clear_tree()
        tree = self.trees[tree_name]

        for node in tree.nodes.values():
            self.scene.add_node_item(node)

        for node in tree.nodes.values():
            for req in node.required_nodes:
                if req in tree.nodes:
                    self.scene.add_link_item(req, node.id)

        self._refresh_icon_warning()
        self._update_simulation_visuals()
        self.minimap.update()

    def _next_available_id(self, base: str) -> str:
        tree = self._active_tree()
        candidate = base
        index = 2
        while candidate in tree.nodes:
            candidate = f"{base}_{index}"
            index += 1
        return candidate

    def create_node(self) -> None:
        tree = self._active_tree()
        viewport = self.view.viewport()
        if viewport is None:
            center = QPointF(0, 0)
        else:
            center = self.view.mapToScene(viewport.rect().center())
        node_id = self._next_available_id(f"{self.current_tree_name}_node")
        node = SkillNodeData(
            id=node_id,
            display_name=node_id,
            category=self.current_tree_name if self.current_tree_name in CATEGORY_COLORS else "combat",
            cost=1,
            required_level=1,
            x=round(center.x() / SNAP_X) * SNAP_X,
            y=round(center.y() / SNAP_Y) * SNAP_Y,
        )

        def redo() -> None:
            self._insert_node(node.clone())

        def undo() -> None:
            self._remove_node(node.id)

        self.undo_stack.push(CallbackCommand("Create Node", redo, undo))

    def delete_selected_node(self) -> None:
        selected = self.scene.selectedItems()
        if not selected or not isinstance(selected[0], SkillNodeItem):
            return

        node_id = selected[0].node_data.id
        tree = self._active_tree()
        if node_id not in tree.nodes:
            return

        snapshot_node = tree.nodes[node_id].clone()
        snapshot_links = {(s, t) for (s, t) in self.scene.links.keys() if node_id in (s, t)}
        snapshot_reqs = {nid: list(n.required_nodes) for nid, n in tree.nodes.items()}

        def redo() -> None:
            self._remove_node(node_id)

        def undo() -> None:
            self._insert_node(snapshot_node.clone())
            for source_id, target_id in snapshot_links:
                self._add_link(source_id, target_id)
            for nid, reqs in snapshot_reqs.items():
                if nid in tree.nodes:
                    tree.nodes[nid].required_nodes = list(reqs)
            self._sync_links_from_requirements()

        self.undo_stack.push(CallbackCommand("Delete Node", redo, undo))

    def duplicate_selected_node(self) -> None:
        selected = self.scene.selectedItems()
        if not selected or not isinstance(selected[0], SkillNodeItem):
            return

        source_node = selected[0].node_data
        duplicate = source_node.clone()
        duplicate.id = self._next_available_id(source_node.id)
        duplicate.display_name = f"{source_node.display_name or source_node.id} Copy"
        duplicate.x += SNAP_X
        duplicate.y += SNAP_Y
        duplicate.required_nodes = []

        def redo() -> None:
            self._insert_node(duplicate.clone())

        def undo() -> None:
            self._remove_node(duplicate.id)

        self.undo_stack.push(CallbackCommand("Duplicate Node", redo, undo))

    def _insert_node(self, node: SkillNodeData) -> None:
        tree = self._active_tree()
        if not node.id or node.id in tree.nodes:
            return
        tree.nodes[node.id] = node
        item = self.scene.add_node_item(node)
        item.setSelected(True)
        self._refresh_icon_warning()
        self.minimap.update()
        self._update_simulation_visuals()

    def _remove_node(self, node_id: str) -> None:
        tree = self._active_tree()
        if node_id not in tree.nodes:
            return

        self.scene.remove_node_item(node_id)
        del tree.nodes[node_id]

        for node in tree.nodes.values():
            if node_id in node.required_nodes:
                node.required_nodes = [req for req in node.required_nodes if req != node_id]

        self._sync_links_from_requirements()
        self._refresh_icon_warning()
        self.minimap.update()
        self._update_simulation_visuals()

    def _on_node_moved(self, node_id: str, old_pos: QPointF, new_pos: QPointF) -> None:
        if self._suspend_undo or old_pos == new_pos:
            return

        def redo() -> None:
            self._set_node_position(node_id, new_pos)

        def undo() -> None:
            self._set_node_position(node_id, old_pos)

        self.undo_stack.push(CallbackCommand("Move Node", redo, undo))

    def _set_node_position(self, node_id: str, pos: QPointF) -> None:
        item = self.scene.node_items.get(node_id)
        if item is None:
            return
        self._suspend_undo = True
        item.setPos(pos)
        item.node_data.x = item.pos().x()
        item.node_data.y = item.pos().y()
        self.scene.update_links_for_node(node_id)
        self._suspend_undo = False
        self.minimap.update()

    def _would_create_cycle(self, source_id: str, target_id: str) -> bool:
        tree = self._active_tree()
        graph: dict[str, set[str]] = {node_id: set(node.required_nodes) for node_id, node in tree.nodes.items()}
        graph.setdefault(target_id, set()).add(source_id)

        visited: set[str] = set()
        stack: set[str] = set()

        def dfs(node_id: str) -> bool:
            if node_id in stack:
                return True
            if node_id in visited:
                return False
            visited.add(node_id)
            stack.add(node_id)
            for req in graph.get(node_id, set()):
                if req in graph and dfs(req):
                    return True
            stack.remove(node_id)
            return False

        return any(dfs(node_id) for node_id in graph.keys())

    def _request_link(self, source_id: str, target_id: str) -> None:
        tree = self._active_tree()
        if source_id not in tree.nodes or target_id not in tree.nodes:
            return
        if source_id == target_id:
            return
        if source_id in tree.nodes[target_id].required_nodes:
            return
        if self._would_create_cycle(source_id, target_id):
            self._show_validation_errors([f"Cannot link '{source_id}' -> '{target_id}': circular dependency."])
            return

        def redo() -> None:
            self._add_link(source_id, target_id)

        def undo() -> None:
            self._remove_link(source_id, target_id)

        self.undo_stack.push(CallbackCommand("Create Link", redo, undo))

    def _add_link(self, source_id: str, target_id: str) -> None:
        tree = self._active_tree()
        node = tree.nodes.get(target_id)
        if node is None:
            return
        if source_id not in node.required_nodes:
            node.required_nodes.append(source_id)
            node.required_nodes.sort()
        self.scene.add_link_item(source_id, target_id)
        self._update_simulation_visuals()
        self.minimap.update()

    def _remove_link(self, source_id: str, target_id: str) -> None:
        tree = self._active_tree()
        node = tree.nodes.get(target_id)
        if node is not None and source_id in node.required_nodes:
            node.required_nodes = [req for req in node.required_nodes if req != source_id]
        self.scene.remove_link_item(source_id, target_id)
        self._update_simulation_visuals()
        self.minimap.update()

    def _sync_links_from_requirements(self) -> None:
        for source_id, target_id in list(self.scene.links.keys()):
            self.scene.remove_link_item(source_id, target_id)

        tree = self._active_tree()
        for node in tree.nodes.values():
            for req in node.required_nodes:
                if req in tree.nodes:
                    self.scene.add_link_item(req, node.id)

        self.minimap.update()

    def _show_node_context_menu(self, node_id: str, screen_pos: QPoint) -> None:
        if node_id not in self._active_tree().nodes:
            return

        menu = QMenu(self)
        delete_action = menu.addAction("Delete Node")
        duplicate_action = menu.addAction("Duplicate Node")
        disconnect_action = menu.addAction("Disconnect Links")
        center_action = menu.addAction("Center Camera On Node")
        highlight_action = menu.addAction("Highlight Dependencies")
        edit_action = menu.addAction("Edit Node Properties")

        picked = menu.exec(screen_pos)
        if picked is delete_action:
            self._select_single(node_id)
            self.delete_selected_node()
        elif picked is duplicate_action:
            self._select_single(node_id)
            self.duplicate_selected_node()
        elif picked is disconnect_action:
            self._disconnect_node_links(node_id)
        elif picked is center_action:
            item = self.scene.node_items.get(node_id)
            if item:
                self.view.centerOn(item.pos())
        elif picked is highlight_action:
            self._highlight_dependencies(node_id)
        elif picked is edit_action:
            self.edit_selected_node(node_id)

    def _select_single(self, node_id: str) -> None:
        for item in self.scene.node_items.values():
            item.setSelected(item.node_data.id == node_id)

    def _disconnect_node_links(self, node_id: str) -> None:
        existing = {(s, t) for (s, t) in self.scene.links.keys() if node_id in (s, t)}
        if not existing:
            return

        def redo() -> None:
            for source_id, target_id in existing:
                self._remove_link(source_id, target_id)

        def undo() -> None:
            for source_id, target_id in existing:
                self._add_link(source_id, target_id)

        self.undo_stack.push(CallbackCommand("Disconnect Links", redo, undo))

    def _highlight_dependencies(self, node_id: str) -> None:
        tree = self._active_tree()
        highlight_edges: set[tuple[str, str]] = set()

        def visit(target: str) -> None:
            for req in tree.nodes.get(target, SkillNodeData(id="")).required_nodes:
                edge = (req, target)
                if edge in highlight_edges:
                    continue
                highlight_edges.add(edge)
                visit(req)

        visit(node_id)
        self.scene.set_highlight_edges(highlight_edges)

    def edit_selected_node(self, node_id: str | None = None) -> None:
        target_id = node_id
        if target_id is None:
            selected = self.scene.selectedItems()
            if not selected or not isinstance(selected[0], SkillNodeItem):
                return
            target_id = selected[0].node_data.id

        tree = self._active_tree()
        source_node = tree.nodes.get(target_id)
        if source_node is None:
            return

        dialog = NodePropertiesDialog(source_node, self)
        if dialog.exec() != QDialog.DialogCode.Accepted:
            return

        new_node = dialog.to_data(source_node)
        validation_errors = self._validate_edit_proposal(source_node.id, new_node)
        if validation_errors:
            self._show_validation_errors(validation_errors)
            return

        old_node = source_node.clone()

        def redo() -> None:
            self._apply_node_edit(old_node.id, new_node.clone())

        def undo() -> None:
            self._apply_node_edit(new_node.id, old_node.clone())

        self.undo_stack.push(CallbackCommand("Edit Node", redo, undo))

    def _validate_edit_proposal(self, old_id: str, proposal: SkillNodeData) -> list[str]:
        errors: list[str] = []
        tree = self._active_tree().clone()

        if not proposal.id.strip():
            errors.append("Node id cannot be empty.")

        if proposal.id != old_id and proposal.id in tree.nodes:
            errors.append(f"Duplicate node id: {proposal.id}")

        if proposal.cost < MIN_COST or proposal.cost > MAX_COST:
            errors.append(f"cost must be in range {MIN_COST}-{MAX_COST}.")

        if proposal.required_level < MIN_LEVEL or proposal.required_level > MAX_LEVEL:
            errors.append(f"requiredLevel must be in range {MIN_LEVEL}-{MAX_LEVEL}.")

        for req in proposal.required_nodes:
            if req == proposal.id:
                errors.append("Node cannot require itself.")
            if req not in tree.nodes and req != old_id:
                errors.append(f"Required node does not exist: {req}")

        for modifier in proposal.modifiers:
            if not modifier.type.strip():
                errors.append("Modifier type cannot be empty.")
            if not (MIN_MODIFIER_VALUE <= modifier.value <= MAX_MODIFIER_VALUE):
                errors.append(
                    f"Modifier value for '{modifier.type}' out of range ({MIN_MODIFIER_VALUE}..{MAX_MODIFIER_VALUE})."
                )

        if old_id in tree.nodes:
            del tree.nodes[old_id]
        tree.nodes[proposal.id] = proposal.clone()

        for node in tree.nodes.values():
            node.required_nodes = [proposal.id if req == old_id else req for req in node.required_nodes]

        errors.extend(TreeValidator._cycle_errors(tree))
        return sorted(set(errors))

    def _apply_node_edit(self, source_id: str, new_node: SkillNodeData) -> None:
        tree = self._active_tree()
        if source_id not in tree.nodes and new_node.id not in tree.nodes:
            return

        if source_id in tree.nodes:
            del tree.nodes[source_id]
        tree.nodes[new_node.id] = new_node

        for node in tree.nodes.values():
            node.required_nodes = [new_node.id if req == source_id else req for req in node.required_nodes]

        old_pos: QPointF | None = None
        if source_id in self.scene.node_items:
            old_pos = self.scene.node_items[source_id].pos()
            self.scene.remove_node_item(source_id)

        item = self.scene.add_node_item(new_node)
        if old_pos is not None:
            item.setPos(new_node.x, new_node.y)
            item.node_data.x = item.pos().x()
            item.node_data.y = item.pos().y()

        self._sync_links_from_requirements()
        self._refresh_icon_warning()
        self._update_simulation_visuals()

    def _on_node_clicked(self, node_id: str) -> None:
        if not self.sim_mode_action.isChecked():
            return
        self._toggle_sim_unlock(node_id)

    def _toggle_sim_unlock(self, node_id: str) -> None:
        unlocked = self.sim_unlocked.setdefault(self.current_tree_name, set())
        tree = self._active_tree()
        node = tree.nodes.get(node_id)
        if node is None:
            return

        if node_id in unlocked:
            # Allow lock only if no unlocked dependent nodes remain.
            for other in tree.nodes.values():
                if node_id in other.required_nodes and other.id in unlocked:
                    return
            unlocked.remove(node_id)
        else:
            if all(req in unlocked for req in node.required_nodes):
                unlocked.add(node_id)

        self._update_simulation_visuals()

    def _update_simulation_visuals(self) -> None:
        tree = self._active_tree()
        unlocked = self.sim_unlocked.setdefault(self.current_tree_name, set())
        sim_mode = self.sim_mode_action.isChecked()

        total_cost = 0
        modifiers: dict[str, float] = {}

        for node_id, item in self.scene.node_items.items():
            node = tree.nodes[node_id]
            available = all(req in unlocked for req in node.required_nodes)
            unlocked_state = node_id in unlocked
            item.set_simulation_state(unlocked_state if sim_mode else False, available if sim_mode else False)

            if unlocked_state:
                total_cost += node.cost
                for modifier in node.modifiers:
                    modifiers[modifier.type] = modifiers.get(modifier.type, 0.0) + modifier.value

        self.simulation_panel.update_content(total_cost, modifiers)
        self.minimap.update()

    def _show_validation_errors(self, errors: list[str]) -> None:
        if not errors:
            return
        message = "\n".join(f"- {err}" for err in errors)
        QMessageBox.warning(self, "Validation Errors", message)

    def _validate_current_tree(self) -> list[str]:
        return TreeValidator.validate_tree(self._active_tree(), self.scene.sceneRect())

    def switch_tree(self, tree_name: str) -> None:
        if not tree_name:
            return
        if tree_name not in self.trees:
            self.trees[tree_name] = SkillTreeData(tree_name)
        self.current_tree_name = tree_name
        self._load_tree_to_scene(tree_name)

    def auto_arrange_tree(self) -> None:
        tree = self._active_tree()
        if not tree.nodes:
            return

        indegree: dict[str, int] = {nid: 0 for nid in tree.nodes}
        children: dict[str, list[str]] = {nid: [] for nid in tree.nodes}
        for node in tree.nodes.values():
            for req in node.required_nodes:
                if req in tree.nodes:
                    indegree[node.id] += 1
                    children[req].append(node.id)

        roots = [nid for nid, degree in indegree.items() if degree == 0]
        queue = list(roots)
        depth: dict[str, int] = {nid: 0 for nid in roots}

        while queue:
            nid = queue.pop(0)
            for child in children[nid]:
                indegree[child] -= 1
                depth[child] = max(depth.get(child, 0), depth[nid] + 1)
                if indegree[child] == 0:
                    queue.append(child)

        # Fallback for cycles/unresolved: keep existing depth 0 and rely on validator.
        layers: dict[int, list[str]] = {}
        for nid in tree.nodes:
            layers.setdefault(depth.get(nid, 0), []).append(nid)

        before = {nid: QPointF(tree.nodes[nid].x, tree.nodes[nid].y) for nid in tree.nodes}
        after: dict[str, QPointF] = {}

        y_gap = 130
        x_gap = 170
        top = -((len(layers) - 1) * y_gap) / 2

        for layer, node_ids in sorted(layers.items(), key=lambda entry: entry[0]):
            node_ids.sort()
            row_width = (len(node_ids) - 1) * x_gap
            left = -row_width / 2
            for idx, node_id in enumerate(node_ids):
                x = round((left + idx * x_gap) / SNAP_X) * SNAP_X
                y = round((top + layer * y_gap) / SNAP_Y) * SNAP_Y
                after[node_id] = QPointF(x, y)

        def redo() -> None:
            for node_id, pos in after.items():
                self._set_node_position(node_id, pos)

        def undo() -> None:
            for node_id, pos in before.items():
                self._set_node_position(node_id, pos)

        self.undo_stack.push(CallbackCommand("Auto Arrange", redo, undo))

    def _dependency_order(self, tree: SkillTreeData) -> list[str]:
        indegree = {nid: 0 for nid in tree.nodes}
        outgoing = {nid: [] for nid in tree.nodes}

        for node in tree.nodes.values():
            for req in node.required_nodes:
                if req in tree.nodes:
                    indegree[node.id] += 1
                    outgoing[req].append(node.id)

        queue = sorted([nid for nid, degree in indegree.items() if degree == 0])
        ordered: list[str] = []

        while queue:
            nid = queue.pop(0)
            ordered.append(nid)
            for child in sorted(outgoing[nid]):
                indegree[child] -= 1
                if indegree[child] == 0:
                    queue.append(child)

        # Add unresolved (e.g., cycles) deterministically to preserve export stability.
        for nid in sorted(tree.nodes.keys()):
            if nid not in ordered:
                ordered.append(nid)

        return ordered

    def _export_tree_payload(self, tree: SkillTreeData) -> dict:
        bounds = self.scene.sceneRect()
        ordered_ids = self._dependency_order(tree)
        return {
            "tree": tree.tree_name,
            "nodes": [tree.nodes[node_id].to_export(bounds) for node_id in ordered_ids],
        }

    def export_current_tree(self) -> None:
        self.save_project(auto=True)
        errors = self._validate_current_tree()
        if errors:
            self._show_validation_errors(errors)
            return

        DEFAULT_EXPORT_ROOT.mkdir(parents=True, exist_ok=True)
        default_path = DEFAULT_EXPORT_ROOT / f"{self.current_tree_name}.json"
        file_path, _ = QFileDialog.getSaveFileName(
            self,
            "Export Current Tree",
            str(default_path),
            "JSON Files (*.json)",
        )
        if not file_path:
            file_path = str(default_path)

        payload = self._export_tree_payload(self._active_tree())
        out = Path(file_path)
        out.parent.mkdir(parents=True, exist_ok=True)
        out.write_text(json.dumps(payload, indent=2) + "\n", encoding="utf-8")
        QMessageBox.information(self, "Export Complete", f"Exported current tree to:\n{out}")

    def export_all_trees(self) -> None:
        self.save_project(auto=True)
        all_errors: list[str] = []

        for tree_name, tree in self.trees.items():
            errors = TreeValidator.validate_tree(tree, self.scene.sceneRect())
            all_errors.extend([f"[{tree_name}] {err}" for err in errors])

        if all_errors:
            self._show_validation_errors(all_errors)
            return

        DEFAULT_EXPORT_ROOT.mkdir(parents=True, exist_ok=True)
        for tree_name, tree in self.trees.items():
            path = DEFAULT_EXPORT_ROOT / f"{tree_name}.json"
            path.write_text(json.dumps(self._export_tree_payload(tree), indent=2) + "\n", encoding="utf-8")

        QMessageBox.information(self, "Export Complete", f"Exported {len(self.trees)} trees to:\n{DEFAULT_EXPORT_ROOT}")

    def load_tree_from_json(self) -> None:
        DEFAULT_EXPORT_ROOT.mkdir(parents=True, exist_ok=True)
        file_path, _ = QFileDialog.getOpenFileName(
            self,
            "Load Tree JSON",
            str(DEFAULT_EXPORT_ROOT),
            "JSON Files (*.json)",
        )
        if not file_path:
            return

        try:
            payload = json.loads(Path(file_path).read_text(encoding="utf-8"))
            tree_name = str(payload.get("tree", "")).strip() or self.current_tree_name
            tree = SkillTreeData(tree_name)
            for raw in payload.get("nodes", []):
                node = SkillNodeData.from_json(raw)
                if node.id:
                    if node.id in tree.nodes:
                        raise ValueError(f"Duplicate node id in JSON: {node.id}")
                    tree.nodes[node.id] = node

            errors = TreeValidator.validate_tree(tree, self.scene.sceneRect())
            if errors:
                self._show_validation_errors(errors)
                return

            self.trees[tree_name] = tree
            self.sim_unlocked.setdefault(tree_name, set())
            self.current_tree_name = tree_name
            self._refresh_tree_selector()
            self._load_tree_to_scene(tree_name)
        except Exception as exc:  # noqa: BLE001
            QMessageBox.critical(self, "Load Failed", f"Could not load tree JSON:\n{exc}")

    def new_project(self) -> None:
        self.undo_stack.clear()
        self.trees = {name: SkillTreeData(name) for name in DEFAULT_TREES}
        self.sim_unlocked = {name: set() for name in DEFAULT_TREES}
        self.current_tree_name = DEFAULT_TREES[0]
        self.project_path = DEFAULT_PROJECT_PATH
        self._refresh_tree_selector()
        self._load_tree_to_scene(self.current_tree_name)

    def open_project(self) -> None:
        file_path, _ = QFileDialog.getOpenFileName(
            self,
            "Open Project",
            str(REPO_ROOT),
            "ExtremeCraft Project (*.extremecraft_project.json);;JSON Files (*.json)",
        )
        if not file_path:
            return

        try:
            payload = json.loads(Path(file_path).read_text(encoding="utf-8"))
            loaded_trees: dict[str, SkillTreeData] = {}
            for tree_name, tree_payload in payload.get("trees", {}).items():
                tree = SkillTreeData(str(tree_name))
                for raw in tree_payload.get("nodes", []):
                    node = SkillNodeData.from_json(raw)
                    if node.id:
                        tree.nodes[node.id] = node
                errors = TreeValidator.validate_tree(tree, self.scene.sceneRect())
                if errors:
                    self._show_validation_errors([f"[{tree_name}] {e}" for e in errors])
                    return
                loaded_trees[tree_name] = tree

            if not loaded_trees:
                raise ValueError("Project contains no trees.")

            self.trees = loaded_trees
            self.sim_unlocked = {tree_name: set() for tree_name in self.trees}
            current = payload.get("ui", {}).get("currentTree", "")
            self.current_tree_name = current if current in self.trees else sorted(self.trees.keys())[0]
            self.project_path = Path(file_path)
            self.undo_stack.clear()
            self._refresh_tree_selector()
            self._load_tree_to_scene(self.current_tree_name)
        except Exception as exc:  # noqa: BLE001
            QMessageBox.critical(self, "Open Project Failed", f"Could not load project:\n{exc}")

    def save_project(self, auto: bool = False) -> None:
        path = self.project_path
        if not auto:
            file_path, _ = QFileDialog.getSaveFileName(
                self,
                "Save Project",
                str(path),
                "ExtremeCraft Project (*.extremecraft_project.json)",
            )
            if not file_path:
                return
            path = Path(file_path)
            self.project_path = path

        payload = {
            "version": 1,
            "ui": {
                "currentTree": self.current_tree_name,
                "snapX": SNAP_X,
                "snapY": SNAP_Y,
            },
            "trees": {
                tree_name: self._export_tree_payload(tree)
                for tree_name, tree in sorted(self.trees.items(), key=lambda entry: entry[0])
            },
        }

        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(json.dumps(payload, indent=2) + "\n", encoding="utf-8")
        if not auto:
            QMessageBox.information(self, "Project Saved", f"Saved project:\n{path}")

    def open_icon_folder(self) -> None:
        ICON_FOLDER.mkdir(parents=True, exist_ok=True)
        QDesktopServices.openUrl(QUrl.fromLocalFile(str(ICON_FOLDER)))

    def keyPressEvent(self, event) -> None:
        if event.matches(QKeySequence.StandardKey.Delete):
            self.delete_selected_node()
            return
        if event.matches(QKeySequence("Ctrl+D")):
            self.duplicate_selected_node()
            return
        if event.key() in (Qt.Key.Key_Return, Qt.Key.Key_Enter):
            self.edit_selected_node()
            return
        super().keyPressEvent(event)


def main() -> None:
    app = QApplication(sys.argv)
    app.setApplicationName("ExtremeCraft Skill Tree Editor")
    window = SkillTreeEditorWindow()
    window.show()
    sys.exit(app.exec())


if __name__ == "__main__":
    main()
