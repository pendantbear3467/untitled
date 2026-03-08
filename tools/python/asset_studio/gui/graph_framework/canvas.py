from __future__ import annotations

from PyQt6.QtCore import QPoint, QPointF, QRectF, Qt, pyqtSignal
from PyQt6.QtGui import QColor, QPainter, QPen, QTransform
from PyQt6.QtWidgets import QGraphicsItem, QGraphicsLineItem, QGraphicsScene, QGraphicsView

from asset_studio.gui.graph_framework.items import ProgressionLinkItem, ProgressionNodeItem, SNAP_X, SNAP_Y
from asset_studio.skilltree.models import ProgressionDocument


CANVAS_EXTENT = 12000


class ProgressionScene(QGraphicsScene):
    selection_changed = pyqtSignal(list, list)
    link_requested = pyqtSignal(str, str)
    node_activated = pyqtSignal(str)
    node_moved = pyqtSignal(str, QPointF, QPointF)

    def __init__(self, parent=None) -> None:  # noqa: ANN001
        super().__init__(parent)
        self.setSceneRect(-CANVAS_EXTENT, -CANVAS_EXTENT, CANVAS_EXTENT * 2, CANVAS_EXTENT * 2)
        self.node_items: dict[str, ProgressionNodeItem] = {}
        self.link_items: dict[tuple[str, str], ProgressionLinkItem] = {}
        self.connect_mode = False
        self._connect_start: str | None = None
        self._drag_line: QGraphicsLineItem | None = None
        self.selectionChanged.connect(self._emit_selection)

    def load_document(self, document: ProgressionDocument) -> None:
        self.clear()
        self.node_items.clear()
        self.link_items.clear()
        for node_id in document.deterministic_node_ids():
            item = ProgressionNodeItem(document.nodes[node_id])
            item.activated.connect(self.node_activated.emit)
            item.moved_finished.connect(self.node_moved.emit)
            self.node_items[node_id] = item
            self.addItem(item)
        for link in document.normalized_links():
            source_item = self.node_items.get(link.source)
            target_item = self.node_items.get(link.target)
            if not source_item or not target_item:
                continue
            link_item = ProgressionLinkItem(source_item, target_item)
            self.link_items[(link.source, link.target)] = link_item
            self.addItem(link_item)

    def refresh_links_for_node(self, node_id: str) -> None:
        for (source_id, target_id), item in self.link_items.items():
            if node_id in {source_id, target_id}:
                item.update_line()

    def selected_node_ids(self) -> list[str]:
        selected: list[str] = []
        for item in self.selectedItems():
            if isinstance(item, ProgressionNodeItem):
                selected.append(item.node.id)
        return sorted(selected)

    def selected_links(self) -> list[tuple[str, str]]:
        selected: list[tuple[str, str]] = []
        for item in self.selectedItems():
            if isinstance(item, ProgressionLinkItem):
                selected.append((item.source_item.node.id, item.target_item.node.id))
        return sorted(selected)

    def set_connect_mode(self, enabled: bool) -> None:
        self.connect_mode = enabled
        if not enabled and self._drag_line is not None:
            self.removeItem(self._drag_line)
            self._drag_line = None
            self._connect_start = None

    def set_simulation_overlay(self, unlocked: set[str], available: set[str]) -> None:
        for node_id, item in self.node_items.items():
            item.set_simulation_state(unlocked=node_id in unlocked, available=node_id in available)

    def set_heatmap_overlay(self, values: dict[str, float]) -> None:
        for node_id, item in self.node_items.items():
            item.set_heat_value(values.get(node_id, 0.0))

    def mousePressEvent(self, event) -> None:  # noqa: ANN001
        if self.connect_mode and event.button() == Qt.MouseButton.LeftButton:
            item = self.itemAt(event.scenePos(), self.views()[0].transform() if self.views() else QTransform())
            if isinstance(item, ProgressionNodeItem):
                self._connect_start = item.node.id
                self._drag_line = QGraphicsLineItem(item.pos().x(), item.pos().y(), event.scenePos().x(), event.scenePos().y())
                self._drag_line.setPen(QPen(QColor(220, 220, 255, 170), 1.5, Qt.PenStyle.DashLine))
                self.addItem(self._drag_line)
                event.accept()
                return
        super().mousePressEvent(event)

    def mouseMoveEvent(self, event) -> None:  # noqa: ANN001
        if self.connect_mode and self._drag_line is not None and self._connect_start is not None:
            start_item = self.node_items.get(self._connect_start)
            if start_item is not None:
                self._drag_line.setLine(start_item.pos().x(), start_item.pos().y(), event.scenePos().x(), event.scenePos().y())
                event.accept()
                return
        super().mouseMoveEvent(event)

    def mouseReleaseEvent(self, event) -> None:  # noqa: ANN001
        if self.connect_mode and self._connect_start is not None:
            end_item = self.itemAt(event.scenePos(), self.views()[0].transform() if self.views() else QTransform())
            if isinstance(end_item, ProgressionNodeItem) and end_item.node.id != self._connect_start:
                self.link_requested.emit(self._connect_start, end_item.node.id)
            if self._drag_line is not None:
                self.removeItem(self._drag_line)
            self._drag_line = None
            self._connect_start = None
            event.accept()
            return
        super().mouseReleaseEvent(event)

    def _emit_selection(self) -> None:
        self.selection_changed.emit(self.selected_node_ids(), self.selected_links())


class ProgressionView(QGraphicsView):
    def __init__(self, scene: ProgressionScene, parent=None) -> None:  # noqa: ANN001
        super().__init__(scene, parent)
        self.setRenderHint(QPainter.RenderHint.Antialiasing, True)
        self.setRenderHint(QPainter.RenderHint.TextAntialiasing, True)
        self.setDragMode(QGraphicsView.DragMode.RubberBandDrag)
        self.setTransformationAnchor(QGraphicsView.ViewportAnchor.AnchorUnderMouse)
        self.setResizeAnchor(QGraphicsView.ViewportAnchor.AnchorUnderMouse)
        self._panning = False
        self._pan_start = QPoint()

    def wheelEvent(self, event) -> None:  # noqa: ANN001
        factor = 1.12 if event.angleDelta().y() > 0 else (1.0 / 1.12)
        self.scale(factor, factor)

    def mousePressEvent(self, event) -> None:  # noqa: ANN001
        if event.button() == Qt.MouseButton.MiddleButton:
            self._panning = True
            self._pan_start = event.pos()
            self.setCursor(Qt.CursorShape.ClosedHandCursor)
            event.accept()
            return
        super().mousePressEvent(event)

    def mouseMoveEvent(self, event) -> None:  # noqa: ANN001
        if self._panning:
            delta = event.pos() - self._pan_start
            self._pan_start = event.pos()
            self.horizontalScrollBar().setValue(self.horizontalScrollBar().value() - delta.x())
            self.verticalScrollBar().setValue(self.verticalScrollBar().value() - delta.y())
            event.accept()
            return
        super().mouseMoveEvent(event)

    def mouseReleaseEvent(self, event) -> None:  # noqa: ANN001
        if event.button() == Qt.MouseButton.MiddleButton and self._panning:
            self._panning = False
            self.setCursor(Qt.CursorShape.ArrowCursor)
            event.accept()
            return
        super().mouseReleaseEvent(event)

    def drawBackground(self, painter: QPainter, rect: QRectF) -> None:
        super().drawBackground(painter, rect)
        left = int(rect.left() // SNAP_X * SNAP_X)
        right = int(rect.right() // SNAP_X * SNAP_X + SNAP_X)
        top = int(rect.top() // SNAP_Y * SNAP_Y)
        bottom = int(rect.bottom() // SNAP_Y * SNAP_Y + SNAP_Y)

        minor_pen = QPen(QColor(52, 52, 60), 1)
        major_pen = QPen(QColor(70, 70, 84), 1)
        for x in range(left, right + 1, SNAP_X):
            painter.setPen(major_pen if x % (SNAP_X * 5) == 0 else minor_pen)
            painter.drawLine(x, top, x, bottom)
        for y in range(top, bottom + 1, SNAP_Y):
            painter.setPen(major_pen if y % (SNAP_Y * 5) == 0 else minor_pen)
            painter.drawLine(left, y, right, y)
