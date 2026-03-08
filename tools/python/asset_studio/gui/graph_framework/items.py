from __future__ import annotations

from PyQt6.QtCore import QPointF, QRectF, Qt, pyqtSignal
from PyQt6.QtGui import QBrush, QColor, QPainter, QPen
from PyQt6.QtWidgets import QGraphicsItem, QGraphicsLineItem, QGraphicsObject, QGraphicsSimpleTextItem

from asset_studio.skilltree.models import ProgressionNode


CATEGORY_COLORS: dict[str, QColor] = {
    "combat": QColor(196, 64, 64),
    "survival": QColor(64, 164, 85),
    "arcane": QColor(150, 88, 196),
    "exploration": QColor(216, 177, 68),
    "technology": QColor(82, 192, 208),
}
SNAP_X = 60
SNAP_Y = 40
NODE_RADIUS = 28


class ProgressionNodeItem(QGraphicsObject):
    moved_finished = pyqtSignal(str, QPointF, QPointF)
    activated = pyqtSignal(str)

    def __init__(self, node: ProgressionNode) -> None:
        super().__init__()
        self.node = node
        self.setFlags(
            QGraphicsItem.GraphicsItemFlag.ItemIsMovable
            | QGraphicsItem.GraphicsItemFlag.ItemIsSelectable
            | QGraphicsItem.GraphicsItemFlag.ItemSendsGeometryChanges
        )
        self._drag_start = QPointF()
        self._sim_unlocked = False
        self._sim_available = False
        self._heat_value = 0.0
        self.label = QGraphicsSimpleTextItem("", self)
        self.label.setBrush(QBrush(QColor(238, 238, 238)))
        self.label.setPos(-NODE_RADIUS, NODE_RADIUS + 8)
        self.refresh_from_node()
        self.setPos(self.node.x, self.node.y)

    def refresh_from_node(self) -> None:
        label = self.node.display_name or self.node.id
        self.label.setText(label)
        self.setToolTip(
            "\n".join(
                [
                    label,
                    f"ID: {self.node.id}",
                    f"Category: {self.node.category}",
                    f"Cost: {self.node.cost}",
                    f"Level: {self.node.required_level}",
                    f"Class: {self.node.required_class or 'any'}",
                    f"Requires: {', '.join(self.node.normalized_requires()) or 'none'}",
                    f"Tags: {', '.join(self.node.normalized_tags()) or 'none'}",
                ]
            )
        )
        self.update()

    def set_simulation_state(self, *, unlocked: bool, available: bool) -> None:
        self._sim_unlocked = unlocked
        self._sim_available = available
        self.update()

    def set_heat_value(self, value: float) -> None:
        self._heat_value = max(0.0, min(1.0, float(value)))
        self.update()

    def boundingRect(self) -> QRectF:
        return QRectF(-NODE_RADIUS - 8, -NODE_RADIUS - 8, NODE_RADIUS * 2 + 16, NODE_RADIUS * 2 + 44)

    def paint(self, painter: QPainter, option, widget=None) -> None:  # noqa: ANN001, ANN201
        painter.setRenderHint(QPainter.RenderHint.Antialiasing, True)
        base = CATEGORY_COLORS.get(self.node.category, QColor(120, 120, 120))
        ring = QColor(max(base.red() - 32, 0), max(base.green() - 32, 0), max(base.blue() - 32, 0))

        painter.setPen(QPen(ring, 2.0))
        painter.setBrush(QBrush(base))
        painter.drawEllipse(QPointF(0, 0), NODE_RADIUS, NODE_RADIUS)

        glow = QColor(255, 255, 255, 30 + int(self._heat_value * 120))
        painter.setPen(Qt.PenStyle.NoPen)
        painter.setBrush(QBrush(glow))
        painter.drawEllipse(QPointF(-5, -6), NODE_RADIUS - 10, NODE_RADIUS - 12)

        text = self.node.id[:8] if len(self.node.id) <= 8 else f"{self.node.id[:6]}.."
        painter.setPen(QPen(QColor(245, 245, 245), 1.2))
        painter.drawText(QRectF(-NODE_RADIUS + 4, -10, NODE_RADIUS * 2 - 8, 20), Qt.AlignmentFlag.AlignCenter, text)

        if self._sim_available and not self._sim_unlocked:
            painter.setPen(QPen(QColor(255, 255, 255, 110), 1.5, Qt.PenStyle.DashLine))
            painter.setBrush(Qt.BrushStyle.NoBrush)
            painter.drawEllipse(QPointF(0, 0), NODE_RADIUS + 3, NODE_RADIUS + 3)
        if self._sim_unlocked:
            painter.setPen(QPen(QColor(66, 210, 255, 220), 3.0))
            painter.setBrush(Qt.BrushStyle.NoBrush)
            painter.drawEllipse(QPointF(0, 0), NODE_RADIUS + 4, NODE_RADIUS + 4)
        if self.isSelected():
            painter.setPen(QPen(QColor(255, 255, 255, 220), 2.0, Qt.PenStyle.DashLine))
            painter.setBrush(Qt.BrushStyle.NoBrush)
            painter.drawEllipse(QPointF(0, 0), NODE_RADIUS + 7, NODE_RADIUS + 7)

    def itemChange(self, change, value):  # noqa: ANN001, ANN201
        if change == QGraphicsItem.GraphicsItemChange.ItemPositionChange:
            point = value
            return QPointF(round(point.x() / SNAP_X) * SNAP_X, round(point.y() / SNAP_Y) * SNAP_Y)
        if change == QGraphicsItem.GraphicsItemChange.ItemPositionHasChanged:
            self.node.x = self.pos().x()
            self.node.y = self.pos().y()
        return super().itemChange(change, value)

    def mousePressEvent(self, event) -> None:  # noqa: ANN001
        self._drag_start = self.pos()
        super().mousePressEvent(event)

    def mouseDoubleClickEvent(self, event) -> None:  # noqa: ANN001
        self.activated.emit(self.node.id)
        super().mouseDoubleClickEvent(event)

    def mouseReleaseEvent(self, event) -> None:  # noqa: ANN001
        super().mouseReleaseEvent(event)
        if self._drag_start != self.pos():
            self.moved_finished.emit(self.node.id, self._drag_start, self.pos())


class ProgressionLinkItem(QGraphicsLineItem):
    def __init__(self, source_item: ProgressionNodeItem, target_item: ProgressionNodeItem) -> None:
        super().__init__()
        self.source_item = source_item
        self.target_item = target_item
        self.setFlags(QGraphicsItem.GraphicsItemFlag.ItemIsSelectable)
        self.setZValue(-1)
        self._normal_pen = QPen(QColor(170, 190, 220), 2.0)
        self._selected_pen = QPen(QColor(245, 216, 95), 3.0)
        self._active_pen = QPen(QColor(66, 210, 255, 210), 2.5)
        self._active = False
        self.update_line()

    def update_line(self) -> None:
        self.setLine(
            self.source_item.pos().x(),
            self.source_item.pos().y(),
            self.target_item.pos().x(),
            self.target_item.pos().y(),
        )

    def set_active(self, active: bool) -> None:
        self._active = active
        self.update()

    def paint(self, painter: QPainter, option, widget=None) -> None:  # noqa: ANN001, ANN201
        if self.isSelected():
            self.setPen(self._selected_pen)
        elif self._active:
            self.setPen(self._active_pen)
        else:
            self.setPen(self._normal_pen)
        super().paint(painter, option, widget)
