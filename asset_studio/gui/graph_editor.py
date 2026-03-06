from __future__ import annotations

from PyQt6.QtCore import QPointF, Qt, pyqtSignal
from PyQt6.QtGui import QBrush, QColor, QPainter, QPen
from PyQt6.QtWidgets import (
    QComboBox,
    QFormLayout,
    QGraphicsItem,
    QGraphicsRectItem,
    QGraphicsScene,
    QGraphicsSimpleTextItem,
    QGraphicsView,
    QHBoxLayout,
    QListWidget,
    QMessageBox,
    QPushButton,
    QSplitter,
    QVBoxLayout,
    QWidget,
)

from asset_studio.graph.graph_engine import GraphEngine
from asset_studio.graph.graph_nodes import NODE_TYPES


class GraphNodeItem(QGraphicsRectItem):
    def __init__(self, node_id: str, title: str, x: float, y: float) -> None:
        super().__init__(0, 0, 170, 70)
        self.node_id = node_id
        self.setFlags(
            QGraphicsItem.GraphicsItemFlag.ItemIsMovable
            | QGraphicsItem.GraphicsItemFlag.ItemIsSelectable
            | QGraphicsItem.GraphicsItemFlag.ItemSendsGeometryChanges
        )
        self.setBrush(QBrush(QColor(46, 54, 71)))
        self.setPen(QPen(QColor(110, 188, 255), 2))
        self.setPos(x, y)

        text = QGraphicsSimpleTextItem(f"{title}\n{node_id}", self)
        text.setBrush(QBrush(QColor(225, 233, 245)))
        text.setPos(8, 8)


class GraphEditor(QWidget):
    graph_log = pyqtSignal(str)

    def __init__(self, context) -> None:
        super().__init__()
        self.context = context
        self.engine = GraphEngine(context.workspace_root)
        self.node_items: dict[str, GraphNodeItem] = {}

        root = QVBoxLayout(self)
        split = QSplitter(Qt.Orientation.Horizontal)

        left = QWidget()
        left_layout = QVBoxLayout(left)
        self.palette = QListWidget()
        for node_name in NODE_TYPES.keys():
            self.palette.addItem(node_name)

        self.node_type = QComboBox()
        self.node_type.addItems(list(NODE_TYPES.keys()))
        add_btn = QPushButton("Add Node")
        link_btn = QPushButton("Link Selected")
        exec_btn = QPushButton("Execute Graph")
        save_btn = QPushButton("Save Graph")

        param_form = QFormLayout()
        param_form.addRow("Node Type", self.node_type)
        left_layout.addWidget(self.palette)
        left_layout.addLayout(param_form)
        left_layout.addWidget(add_btn)
        left_layout.addWidget(link_btn)
        left_layout.addWidget(exec_btn)
        left_layout.addWidget(save_btn)
        left_layout.addStretch(1)

        self.scene = QGraphicsScene()
        self.scene.setSceneRect(0, 0, 3200, 2000)
        self.view = QGraphicsView(self.scene)
        self.view.setRenderHint(QPainter.RenderHint.Antialiasing)
        self.view.setDragMode(QGraphicsView.DragMode.RubberBandDrag)

        split.addWidget(left)
        split.addWidget(self.view)
        split.setSizes([260, 940])
        root.addWidget(split)

        add_btn.clicked.connect(self._add_node)
        link_btn.clicked.connect(self._link_selected)
        exec_btn.clicked.connect(self._execute)
        save_btn.clicked.connect(self._save)

    def set_context(self, context) -> None:
        self.context = context
        self.engine = GraphEngine(context.workspace_root)
        self.node_items.clear()
        self.scene.clear()

    def _add_node(self) -> None:
        node_type = self.node_type.currentText()
        node = self.engine.add_node(node_type=node_type, parameters={})
        x = 80 + (len(self.node_items) % 5) * 220
        y = 80 + (len(self.node_items) // 5) * 120
        node.x = float(x)
        node.y = float(y)
        item = GraphNodeItem(node.node_id, node.node_type, x, y)
        self.scene.addItem(item)
        self.node_items[node.node_id] = item
        self.graph_log.emit(f"Graph node added: {node.node_id}")

    def _link_selected(self) -> None:
        selected = [item for item in self.scene.selectedItems() if isinstance(item, GraphNodeItem)]
        if len(selected) != 2:
            QMessageBox.information(self, "Graph", "Select exactly two nodes to link.")
            return
        src, dst = selected
        self.engine.add_link(src.node_id, dst.node_id)
        p1 = src.sceneBoundingRect().center()
        p2 = dst.sceneBoundingRect().center()
        self.scene.addLine(p1.x(), p1.y(), p2.x(), p2.y(), QPen(QColor(140, 180, 250), 2))
        self.graph_log.emit(f"Linked {src.node_id} -> {dst.node_id}")

    def _sync_positions(self) -> None:
        for node in self.engine.nodes:
            item = self.node_items.get(node.node_id)
            if item:
                pos = item.scenePos()
                node.x = float(pos.x())
                node.y = float(pos.y())

    def _save(self) -> None:
        self._sync_positions()
        path = self.engine.save()
        self.graph_log.emit(f"Graph saved: {path}")

    def _execute(self) -> None:
        self._sync_positions()
        try:
            generated = self.engine.execute(self.context)
        except Exception as exc:  # noqa: BLE001
            QMessageBox.critical(self, "Graph Execute", str(exc))
            self.graph_log.emit(f"Graph execute failed: {exc}")
            return
        self.graph_log.emit(f"Graph executed ({len(generated)} outputs)")
