from __future__ import annotations

from PyQt6.QtCore import QPointF, Qt, pyqtSignal
from PyQt6.QtGui import QBrush, QColor, QPainter, QPen
from PyQt6.QtWidgets import (
    QComboBox,
    QFormLayout,
    QGraphicsLineItem,
    QGraphicsItem,
    QGraphicsRectItem,
    QGraphicsScene,
    QGraphicsSimpleTextItem,
    QGraphicsView,
    QHBoxLayout,
    QLabel,
    QLineEdit,
    QListWidget,
    QMessageBox,
    QPushButton,
    QSplitter,
    QVBoxLayout,
    QWidget,
)

from asset_studio.graph.graph_engine import GraphEngine
from asset_studio.graph.graph_nodes import NODE_TYPES, NodePort


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
        self._normal_pen = QPen(QColor(110, 188, 255), 2)
        self._error_pen = QPen(QColor(220, 91, 91), 2)
        self._warning_pen = QPen(QColor(240, 186, 76), 2)
        self.setPen(self._normal_pen)
        self.setPos(x, y)

        self._text = QGraphicsSimpleTextItem("", self)
        self._text.setBrush(QBrush(QColor(225, 233, 245)))
        self._text.setPos(8, 8)
        self.set_display_text(title)

    def set_display_text(self, title: str) -> None:
        self._text.setText(f"{title}\n{self.node_id}")

    def set_status(self, status: str) -> None:
        if status == "error":
            self.setPen(self._error_pen)
        elif status == "warning":
            self.setPen(self._warning_pen)
        else:
            self.setPen(self._normal_pen)


class GraphLinkItem(QGraphicsLineItem):
    def __init__(self, src_node_id: str, dst_node_id: str, src_port: str, dst_port: str) -> None:
        super().__init__()
        self.src_node_id = src_node_id
        self.dst_node_id = dst_node_id
        self.src_port = src_port
        self.dst_port = dst_port
        self._normal_pen = QPen(QColor(140, 180, 250), 2)
        self._error_pen = QPen(QColor(220, 91, 91), 3)
        self.setPen(self._normal_pen)

    def set_status(self, status: str) -> None:
        if status == "error":
            self.setPen(self._error_pen)
        else:
            self.setPen(self._normal_pen)


class GraphEditor(QWidget):
    graph_log = pyqtSignal(str)

    def __init__(self, context) -> None:
        super().__init__()
        self.context = context
        self.engine = GraphEngine(context.workspace_root)
        self.node_items: dict[str, GraphNodeItem] = {}
        self.link_items: list[GraphLinkItem] = []

        root = QVBoxLayout(self)
        split = QSplitter(Qt.Orientation.Horizontal)

        left = QWidget()
        left_layout = QVBoxLayout(left)
        self.palette = QListWidget()
        self._refresh_palette()

        self.graph_name = QLineEdit(self.engine.name)
        self.node_type = QComboBox()
        self.node_type.addItems(list(NODE_TYPES.keys()))
        add_btn = QPushButton("Add Node")
        link_btn = QPushButton("Link Selected")
        validate_btn = QPushButton("Validate Graph")
        exec_btn = QPushButton("Execute Graph")
        new_btn = QPushButton("New Graph")
        load_btn = QPushButton("Load Graph")
        save_btn = QPushButton("Save Graph")

        param_form = QFormLayout()
        param_form.addRow("Graph Name", self.graph_name)
        param_form.addRow("Node Type", self.node_type)
        left_layout.addWidget(self.palette)
        left_layout.addLayout(param_form)
        left_layout.addWidget(add_btn)
        left_layout.addWidget(link_btn)
        left_layout.addWidget(validate_btn)
        left_layout.addWidget(exec_btn)
        left_layout.addWidget(new_btn)
        left_layout.addWidget(load_btn)
        left_layout.addWidget(save_btn)
        left_layout.addStretch(1)

        inspector = QWidget()
        inspector_layout = QVBoxLayout(inspector)
        self.selected_label = QLabel("Selected: none")
        self.param_list = QListWidget()
        self.param_key = QLineEdit()
        self.param_value = QLineEdit()
        set_param_btn = QPushButton("Set Parameter")
        remove_param_btn = QPushButton("Remove Parameter")

        inspector_form = QFormLayout()
        inspector_form.addRow("Key", self.param_key)
        inspector_form.addRow("Value", self.param_value)

        inspector_layout.addWidget(self.selected_label)
        inspector_layout.addWidget(self.param_list)
        inspector_layout.addLayout(inspector_form)
        inspector_layout.addWidget(set_param_btn)
        inspector_layout.addWidget(remove_param_btn)
        inspector_layout.addStretch(1)

        self.scene = QGraphicsScene()
        self.scene.setSceneRect(0, 0, 3200, 2000)
        self.view = QGraphicsView(self.scene)
        self.view.setRenderHint(QPainter.RenderHint.Antialiasing)
        self.view.setDragMode(QGraphicsView.DragMode.RubberBandDrag)
        self.scene.selectionChanged.connect(self._on_selection_changed)

        split.addWidget(left)
        split.addWidget(self.view)
        split.addWidget(inspector)
        split.setSizes([260, 760, 280])
        root.addWidget(split)

        add_btn.clicked.connect(self._add_node)
        link_btn.clicked.connect(self._link_selected)
        validate_btn.clicked.connect(self._validate)
        exec_btn.clicked.connect(self._execute)
        new_btn.clicked.connect(self._new_graph)
        load_btn.clicked.connect(self._load_graph)
        save_btn.clicked.connect(self._save)
        set_param_btn.clicked.connect(self._set_parameter)
        remove_param_btn.clicked.connect(self._remove_parameter)

        self._on_selection_changed()

    def set_context(self, context) -> None:
        self.context = context
        self.engine = GraphEngine(context.workspace_root)
        self.node_items.clear()
        self.link_items.clear()
        self.scene.clear()
        self._refresh_palette()
        self.graph_name.setText(self.engine.name)
        self._on_selection_changed()

    def _refresh_palette(self) -> None:
        if hasattr(self, "palette"):
            self.palette.clear()
        if hasattr(self, "node_type"):
            self.node_type.clear()

        for node_name in list(NODE_TYPES.keys()):
            if hasattr(self, "palette"):
                self.palette.addItem(node_name)

        plugins = getattr(getattr(self, "context", None), "plugins", None)
        if plugins is not None:
            for node_name in plugins.graph_nodes.keys():
                if node_name not in NODE_TYPES:
                    NODE_TYPES[node_name] = {
                        "inputs": [NodePort("in", "any")],
                        "outputs": [NodePort("out", "any")],
                    }
                if hasattr(self, "palette"):
                    self.palette.addItem(node_name)

        if hasattr(self, "node_type"):
            self.node_type.addItems(list(NODE_TYPES.keys()))

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
        self._validate()

    def _link_selected(self) -> None:
        selected = [item for item in self.scene.selectedItems() if isinstance(item, GraphNodeItem)]
        if len(selected) != 2:
            QMessageBox.information(self, "Graph", "Select exactly two nodes to link.")
            return
        src, dst = selected
        try:
            self.engine.add_link(src.node_id, dst.node_id)
        except ValueError as exc:
            QMessageBox.warning(self, "Graph Link", str(exc))
            self.graph_log.emit(f"Link rejected: {exc}")
            return
        self.graph_log.emit(f"Linked {src.node_id} -> {dst.node_id}")
        self._redraw_links()
        self._validate()

    def _sync_positions(self) -> None:
        for node in self.engine.nodes:
            item = self.node_items.get(node.node_id)
            if item:
                pos = item.scenePos()
                node.x = float(pos.x())
                node.y = float(pos.y())

    def _save(self) -> None:
        self._sync_positions()
        graph_name = self.graph_name.text().strip() or self.engine.name
        path = self.engine.save(name=graph_name)
        self.graph_name.setText(self.engine.name)
        self.graph_log.emit(f"Graph saved: {path}")

    def _new_graph(self) -> None:
        self.engine = GraphEngine(self.context.workspace_root)
        self.node_items.clear()
        self.link_items.clear()
        self.scene.clear()
        self.graph_name.setText(self.engine.name)
        self._on_selection_changed()
        self.graph_log.emit("Graph reset to untitled_graph")

    def _load_graph(self) -> None:
        graph_name = self.graph_name.text().strip()
        if not graph_name:
            QMessageBox.information(self, "Graph", "Enter a graph name first.")
            return
        try:
            self.engine.load(graph_name)
        except FileNotFoundError:
            QMessageBox.warning(self, "Graph", f"Graph not found: {graph_name}")
            return

        self.node_items.clear()
        self.link_items.clear()
        self.scene.clear()

        for node in self.engine.nodes:
            item = GraphNodeItem(node.node_id, node.node_type, node.x, node.y)
            self.scene.addItem(item)
            self.node_items[node.node_id] = item

        self._redraw_links()
        self._validate()
        self.graph_log.emit(f"Graph loaded: {graph_name}")

    def _validate(self) -> None:
        self._sync_positions()
        report = self.engine.validate()
        error_node_ids = self._collect_error_node_ids(report.errors)

        for node in self.engine.nodes:
            item = self.node_items.get(node.node_id)
            if item is None:
                continue
            if node.node_id in error_node_ids:
                item.set_status("error")
            else:
                item.set_status("ok")

        self._apply_link_validation_status()
        self.graph_log.emit(
            f"Graph validation: errors={len(report.errors)} warnings={len(report.warnings)}"
        )

    def _execute(self) -> None:
        self._sync_positions()
        try:
            generated = self.engine.execute(self.context)
        except Exception as exc:  # noqa: BLE001
            QMessageBox.critical(self, "Graph Execute", str(exc))
            self.graph_log.emit(f"Graph execute failed: {exc}")
            return
        self.graph_log.emit(f"Graph executed ({len(generated)} outputs)")

    def _on_selection_changed(self) -> None:
        node = self._selected_node()
        self.param_list.clear()

        if node is None:
            self.selected_label.setText("Selected: none")
            return

        self.selected_label.setText(f"Selected: {node.node_id} [{node.node_type}]")
        if not node.parameters:
            self.param_list.addItem("<no parameters>")
            return
        for key, value in sorted(node.parameters.items()):
            self.param_list.addItem(f"{key}={value}")

    def _set_parameter(self) -> None:
        node = self._selected_node()
        if node is None:
            QMessageBox.information(self, "Graph", "Select a node first.")
            return

        key = self.param_key.text().strip()
        if not key:
            QMessageBox.information(self, "Graph", "Parameter key cannot be empty.")
            return

        node.parameters[key] = self._coerce_param_value(self.param_value.text().strip())
        item = self.node_items.get(node.node_id)
        if item is not None:
            item.set_display_text(f"{node.node_type} ({len(node.parameters)} params)")

        self._on_selection_changed()
        self._validate()
        self.graph_log.emit(f"Parameter set: {node.node_id}.{key}")

    def _remove_parameter(self) -> None:
        node = self._selected_node()
        if node is None:
            QMessageBox.information(self, "Graph", "Select a node first.")
            return

        key = self.param_key.text().strip()
        if not key:
            QMessageBox.information(self, "Graph", "Enter parameter key to remove.")
            return
        if key not in node.parameters:
            QMessageBox.information(self, "Graph", f"Parameter not found: {key}")
            return

        del node.parameters[key]
        item = self.node_items.get(node.node_id)
        if item is not None:
            item.set_display_text(f"{node.node_type} ({len(node.parameters)} params)")

        self._on_selection_changed()
        self._validate()
        self.graph_log.emit(f"Parameter removed: {node.node_id}.{key}")

    def _selected_node(self):
        selected = [item for item in self.scene.selectedItems() if isinstance(item, GraphNodeItem)]
        if len(selected) != 1:
            return None
        node_id = selected[0].node_id
        for node in self.engine.nodes:
            if node.node_id == node_id:
                return node
        return None

    def _redraw_links(self) -> None:
        for item in self.link_items:
            self.scene.removeItem(item)
        self.link_items.clear()

        for link in self.engine.links:
            src_id = str(link.get("from", ""))
            dst_id = str(link.get("to", ""))
            src_item = self.node_items.get(src_id)
            dst_item = self.node_items.get(dst_id)
            if src_item is None or dst_item is None:
                continue

            line_item = GraphLinkItem(
                src_node_id=src_id,
                dst_node_id=dst_id,
                src_port=str(link.get("from_port", "")),
                dst_port=str(link.get("to_port", "")),
            )

            p1 = src_item.sceneBoundingRect().center()
            p2 = dst_item.sceneBoundingRect().center()
            line_item.setLine(p1.x(), p1.y(), p2.x(), p2.y())
            self.scene.addItem(line_item)
            self.link_items.append(line_item)

    def _apply_link_validation_status(self) -> None:
        node_map = {n.node_id: n for n in self.engine.nodes}
        for link_item in self.link_items:
            src_node = node_map.get(link_item.src_node_id)
            dst_node = node_map.get(link_item.dst_node_id)
            if src_node is None or dst_node is None:
                link_item.set_status("error")
                continue

            errors, _ = self.engine.validator.validate_link(
                src_node,
                dst_node,
                src_port=link_item.src_port,
                dst_port=link_item.dst_port,
            )
            link_item.set_status("error" if errors else "ok")

    @staticmethod
    def _collect_error_node_ids(errors: list[str]) -> set[str]:
        node_ids: set[str] = set()
        for message in errors:
            if ":" in message:
                node_ids.add(message.split(":", 1)[0].strip())
            if " -> " in message:
                left, right = message.split(" -> ", 1)
                node_ids.add(left.split()[-1].strip())
                node_ids.add(right.split()[0].strip())
        return node_ids

    @staticmethod
    def _coerce_param_value(raw: str) -> object:
        lowered = raw.lower()
        if lowered in {"true", "false"}:
            return lowered == "true"
        if lowered in {"none", "null"}:
            return None
        try:
            if "." in raw:
                return float(raw)
            return int(raw)
        except ValueError:
            return raw
