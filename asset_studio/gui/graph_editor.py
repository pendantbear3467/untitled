from __future__ import annotations

from PyQt6.QtCore import Qt, pyqtSignal
from PyQt6.QtGui import QBrush, QColor, QDrag, QDragEnterEvent, QDropEvent, QPainter, QPen
from PyQt6.QtWidgets import (
    QAbstractItemView,
    QCheckBox,
    QComboBox,
    QDoubleSpinBox,
    QFormLayout,
    QGraphicsItem,
    QGraphicsLineItem,
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
    QSpinBox,
    QSplitter,
    QTreeWidget,
    QTreeWidgetItem,
    QVBoxLayout,
    QWidget,
)

from asset_studio.graph.graph_engine import GraphEngine
from asset_studio.graph.graph_node_registry import NodeParameterSchema, get_registry
from asset_studio.graph.graph_nodes import BaseGraphNode


class GraphNodeItem(QGraphicsRectItem):
    def __init__(self, node_id: str, title: str, x: float, y: float) -> None:
        super().__init__(0, 0, 190, 78)
        self.node_id = node_id
        self.setFlags(
            QGraphicsItem.GraphicsItemFlag.ItemIsMovable
            | QGraphicsItem.GraphicsItemFlag.ItemIsSelectable
            | QGraphicsItem.GraphicsItemFlag.ItemSendsGeometryChanges
        )

        self._text = QGraphicsSimpleTextItem("", self)
        self._text.setBrush(QBrush(QColor(230, 238, 248)))
        self._text.setPos(8, 8)

        self._pens = {
            "idle": QPen(QColor(122, 132, 150), 2),
            "executing": QPen(QColor(72, 146, 255), 2),
            "success": QPen(QColor(66, 188, 120), 2),
            "error": QPen(QColor(225, 82, 82), 2),
            "warning": QPen(QColor(238, 183, 75), 2),
            "ok": QPen(QColor(122, 132, 150), 2),
        }
        self._brushes = {
            "idle": QBrush(QColor(56, 62, 76)),
            "executing": QBrush(QColor(44, 82, 136)),
            "success": QBrush(QColor(40, 92, 64)),
            "error": QBrush(QColor(111, 44, 44)),
            "warning": QBrush(QColor(104, 84, 42)),
            "ok": QBrush(QColor(56, 62, 76)),
        }

        self.setPos(x, y)
        self.set_display_text(title)
        self.set_status("idle")

    def set_display_text(self, title: str) -> None:
        self._text.setText(f"{title}\n{self.node_id}")

    def set_status(self, status: str) -> None:
        self.setPen(self._pens.get(status, self._pens["idle"]))
        self.setBrush(self._brushes.get(status, self._brushes["idle"]))


class GraphLinkItem(QGraphicsLineItem):
    def __init__(self, src_node_id: str, dst_node_id: str, src_port: str, dst_port: str) -> None:
        super().__init__()
        self.src_node_id = src_node_id
        self.dst_node_id = dst_node_id
        self.src_port = src_port
        self.dst_port = dst_port
        self._normal_pen = QPen(QColor(144, 188, 248), 2)
        self._error_pen = QPen(QColor(225, 82, 82), 3)
        self._ok_pen = QPen(QColor(72, 192, 120), 2)
        self.set_status("idle")

    def set_status(self, status: str) -> None:
        if status == "error":
            self.setPen(self._error_pen)
        elif status in {"ok", "success"}:
            self.setPen(self._ok_pen)
        else:
            self.setPen(self._normal_pen)


class GraphCanvasView(QGraphicsView):
    node_drop_requested = pyqtSignal(str, float, float)

    def __init__(self, scene: QGraphicsScene, parent: QWidget | None = None) -> None:
        super().__init__(scene, parent)
        self.setAcceptDrops(True)

    def dragEnterEvent(self, event: QDragEnterEvent | None) -> None:  # noqa: N802
        mime = event.mimeData() if event is not None else None
        if mime is not None and mime.hasText() and event is not None:
            event.acceptProposedAction()
            return
        super().dragEnterEvent(event)

    def dropEvent(self, event: QDropEvent | None) -> None:  # noqa: N802
        mime = event.mimeData() if event is not None else None
        if mime is not None and mime.hasText() and event is not None:
            node_type = mime.text().strip()
            point = self.mapToScene(event.position().toPoint())
            self.node_drop_requested.emit(node_type, float(point.x()), float(point.y()))
            event.acceptProposedAction()
            return
        super().dropEvent(event)


class NodeLibraryTree(QTreeWidget):
    def __init__(self) -> None:
        super().__init__()
        self.setHeaderHidden(True)
        self.setDragEnabled(True)
        self.setSelectionMode(QAbstractItemView.SelectionMode.SingleSelection)

    def startDrag(self, supportedActions):  # noqa: N802
        item = self.currentItem()
        if item is None:
            return
        node_type = item.data(0, Qt.ItemDataRole.UserRole)
        if not node_type:
            return

        model = self.model()
        if model is None:
            return
        mime = model.mimeData(self.selectedIndexes())
        if mime is None:
            return
        mime.setText(str(node_type))

        drag = QDrag(self)
        drag.setMimeData(mime)
        drag.exec(supportedActions)


class GraphEditor(QWidget):
    graph_log = pyqtSignal(str)

    def __init__(self, context) -> None:
        super().__init__()
        self.context = context
        plugin_nodes = getattr(getattr(context, "plugins", None), "graph_nodes", {})
        self.engine = GraphEngine(context.workspace_root, plugin_api_nodes=plugin_nodes)

        self.node_items: dict[str, GraphNodeItem] = {}
        self.link_items: list[GraphLinkItem] = []
        self.param_editors: dict[str, QWidget] = {}

        root = QVBoxLayout(self)
        split = QSplitter(Qt.Orientation.Horizontal)

        self.left_panel = self._build_left_panel()
        self.center_panel = self._build_center_panel()
        self.right_panel = self._build_right_panel()

        split.addWidget(self.left_panel)
        split.addWidget(self.center_panel)
        split.addWidget(self.right_panel)
        split.setSizes([320, 780, 360])
        root.addWidget(split)

        self._refresh_library()
        self._refresh_preview()
        self._on_selection_changed()

    def set_context(self, context) -> None:
        self.context = context
        plugin_nodes = getattr(getattr(context, "plugins", None), "graph_nodes", {})
        self.engine = GraphEngine(context.workspace_root, plugin_api_nodes=plugin_nodes)
        self.node_items.clear()
        self.link_items.clear()
        self.scene.clear()
        self.graph_name.setText(self.engine.name)
        self._refresh_library()
        self._refresh_preview()
        self._on_selection_changed()

    def _build_left_panel(self) -> QWidget:
        panel = QWidget()
        layout = QVBoxLayout(panel)

        self.graph_name = QLineEdit(self.engine.name)
        self.search_box = QLineEdit()
        self.search_box.setPlaceholderText("Search nodes (e.g. tool, machine, skill)")
        self.search_box.textChanged.connect(self._filter_library)

        self.library = NodeLibraryTree()
        self.library.itemDoubleClicked.connect(lambda *_: self._add_selected_library_node())

        self.selected_node_type = QComboBox()

        self.from_port = QComboBox()
        self.to_port = QComboBox()

        self.layout_combo = QComboBox()
        self.layout_combo.addItems(["layered", "dag", "force"])

        add_btn = QPushButton("Add Selected Node")
        add_btn.clicked.connect(self._add_selected_library_node)

        link_btn = QPushButton("Link Selected Nodes")
        link_btn.clicked.connect(self._link_selected)

        auto_btn = QPushButton("Auto Arrange Graph")
        auto_btn.clicked.connect(self._auto_arrange)

        validate_btn = QPushButton("Validate Graph")
        validate_btn.clicked.connect(self._validate)

        exec_btn = QPushButton("Execute Graph")
        exec_btn.clicked.connect(self._execute)

        new_btn = QPushButton("New Graph")
        new_btn.clicked.connect(self._new_graph)

        load_btn = QPushButton("Load Graph")
        load_btn.clicked.connect(self._load_graph)

        save_btn = QPushButton("Save Graph")
        save_btn.clicked.connect(self._save)

        export_tpl_btn = QPushButton("Export Template")
        export_tpl_btn.clicked.connect(self._export_template)

        import_tpl_btn = QPushButton("Import Template")
        import_tpl_btn.clicked.connect(self._import_template)

        form = QFormLayout()
        form.addRow("Graph Name", self.graph_name)
        form.addRow("Quick Add", self.selected_node_type)
        form.addRow("From Port", self.from_port)
        form.addRow("To Port", self.to_port)
        form.addRow("Layout", self.layout_combo)

        layout.addWidget(QLabel("Node Library"))
        layout.addWidget(self.search_box)
        layout.addWidget(self.library)
        layout.addLayout(form)
        layout.addWidget(add_btn)
        layout.addWidget(link_btn)
        layout.addWidget(auto_btn)
        layout.addWidget(validate_btn)
        layout.addWidget(exec_btn)
        layout.addWidget(new_btn)
        layout.addWidget(load_btn)
        layout.addWidget(save_btn)
        layout.addWidget(export_tpl_btn)
        layout.addWidget(import_tpl_btn)
        layout.addStretch(1)
        return panel

    def _build_center_panel(self) -> QWidget:
        panel = QWidget()
        layout = QVBoxLayout(panel)

        self.scene = QGraphicsScene()
        self.scene.setSceneRect(0, 0, 4200, 2600)
        self.scene.selectionChanged.connect(self._on_selection_changed)

        self.view = GraphCanvasView(self.scene)
        self.view.setRenderHint(QPainter.RenderHint.Antialiasing)
        self.view.setDragMode(QGraphicsView.DragMode.RubberBandDrag)
        self.view.node_drop_requested.connect(self._add_node_at)

        layout.addWidget(self.view)
        return panel

    def _build_right_panel(self) -> QWidget:
        panel = QWidget()
        layout = QVBoxLayout(panel)

        self.selected_label = QLabel("Selected: none")

        self.param_panel = QWidget()
        self.param_form = QFormLayout(self.param_panel)

        self.debug_timeline = QListWidget()
        self.debug_console = QListWidget()

        self.preview_list = QListWidget()

        layout.addWidget(QLabel("Parameter Inspector"))
        layout.addWidget(self.selected_label)
        layout.addWidget(self.param_panel)
        layout.addWidget(QLabel("Preview Renderer"))
        layout.addWidget(self.preview_list)
        layout.addWidget(QLabel("Execution Timeline"))
        layout.addWidget(self.debug_timeline)
        layout.addWidget(QLabel("Debug Console"))
        layout.addWidget(self.debug_console)
        return panel

    def _refresh_library(self) -> None:
        registry = get_registry()
        categories = registry.categories()

        self.library.clear()
        self.selected_node_type.clear()

        for category in [
            "Materials",
            "Items",
            "Tools",
            "Blocks",
            "Recipes",
            "Machines",
            "Magic",
            "Skills",
            "Worldgen",
            "Utility",
        ]:
            parent = QTreeWidgetItem([category])
            parent.setData(0, Qt.ItemDataRole.UserRole, None)
            parent.setFlags(parent.flags() & ~Qt.ItemFlag.ItemIsDragEnabled)
            self.library.addTopLevelItem(parent)

            for node_type in categories.get(category, []):
                child = QTreeWidgetItem([node_type])
                child.setData(0, Qt.ItemDataRole.UserRole, node_type)
                parent.addChild(child)
                self.selected_node_type.addItem(node_type)

            parent.setExpanded(True)

    def _filter_library(self, text: str) -> None:
        needle = text.strip().lower()
        for i in range(self.library.topLevelItemCount()):
            category_item = self.library.topLevelItem(i)
            if category_item is None:
                continue
            visible_child_count = 0
            for j in range(category_item.childCount()):
                child = category_item.child(j)
                if child is None:
                    continue
                child_visible = needle in child.text(0).lower() if needle else True
                child.setHidden(not child_visible)
                if child_visible:
                    visible_child_count += 1
            category_item.setHidden(visible_child_count == 0)

    def _add_selected_library_node(self) -> None:
        node_type = self.selected_node_type.currentText().strip()
        if not node_type:
            item = self.library.currentItem()
            if item is not None:
                data = item.data(0, Qt.ItemDataRole.UserRole)
                node_type = str(data or "").strip()
        if not node_type:
            QMessageBox.information(self, "Graph", "Select a node type first.")
            return
        x = 100 + (len(self.node_items) % 6) * 230
        y = 100 + (len(self.node_items) // 6) * 130
        self._add_node_at(node_type, float(x), float(y))

    def _add_node_at(self, node_type: str, x: float, y: float) -> None:
        try:
            node = self.engine.add_node(node_type=node_type, parameters={}, x=x, y=y)
        except Exception as exc:  # noqa: BLE001
            QMessageBox.warning(self, "Graph", str(exc))
            self.graph_log.emit(f"Node add failed: {exc}")
            return

        item = GraphNodeItem(node.node_id, node.node_type, x, y)
        self.scene.addItem(item)
        self.node_items[node.node_id] = item
        self.graph_log.emit(f"Graph node added: {node.node_id}")
        self._update_link_port_selectors()
        self._validate()

    def _link_selected(self) -> None:
        selected = [item for item in self.scene.selectedItems() if isinstance(item, GraphNodeItem)]
        if len(selected) != 2:
            QMessageBox.information(self, "Graph", "Select exactly two nodes to link.")
            return

        src, dst = selected
        src_port = self.from_port.currentText().strip() or None
        dst_port = self.to_port.currentText().strip() or None
        try:
            self.engine.add_link(src.node_id, dst.node_id, src_port=src_port, dst_port=dst_port)
        except Exception as exc:  # noqa: BLE001
            QMessageBox.warning(self, "Graph Link", str(exc))
            self.graph_log.emit(f"Link rejected: {exc}")
            return

        self._redraw_links()
        self._validate()
        self.graph_log.emit(f"Linked {src.node_id}.{src_port or '*'} -> {dst.node_id}.{dst_port or '*'}")

    def _sync_positions(self) -> None:
        for node in self.engine.nodes:
            item = self.node_items.get(node.node_id)
            if item is None:
                continue
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
        plugin_nodes = getattr(getattr(self.context, "plugins", None), "graph_nodes", {})
        self.engine = GraphEngine(self.context.workspace_root, plugin_api_nodes=plugin_nodes)
        self.node_items.clear()
        self.link_items.clear()
        self.scene.clear()
        self.graph_name.setText(self.engine.name)
        self.debug_timeline.clear()
        self.debug_console.clear()
        self._refresh_preview()
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
        self._update_link_port_selectors()
        self._validate()
        self._refresh_preview()
        self.graph_log.emit(f"Graph loaded: {graph_name}")

    def _auto_arrange(self) -> None:
        algorithm = self.layout_combo.currentText().strip() or "layered"
        self.engine.auto_arrange(algorithm)
        for node in self.engine.nodes:
            item = self.node_items.get(node.node_id)
            if item is not None:
                item.setPos(node.x, node.y)
        self._redraw_links()
        self._validate()
        self.graph_log.emit(f"Graph auto arranged using {algorithm}")

    def _validate(self) -> None:
        self._sync_positions()
        report = self.engine.validate()
        self._redraw_links()

        error_nodes = self._collect_node_ids(report.errors)
        warning_nodes = self._collect_node_ids(report.warnings)

        for node in self.engine.nodes:
            item = self.node_items.get(node.node_id)
            if item is None:
                continue
            if node.node_id in error_nodes:
                item.set_status("error")
            elif node.node_id in warning_nodes:
                item.set_status("warning")
            else:
                item.set_status(node.execution_state if node.execution_state else "idle")

        self._apply_link_validation_status()

        self.debug_console.addItem(
            f"validate: errors={len(report.errors)} warnings={len(report.warnings)}"
        )
        for msg in report.errors[:8]:
            self.debug_console.addItem(f"ERROR {msg}")
        for msg in report.warnings[:8]:
            self.debug_console.addItem(f"WARN {msg}")

        self.graph_log.emit(
            f"Graph validation: errors={len(report.errors)} warnings={len(report.warnings)}"
        )

    def _execute(self) -> None:
        self._sync_positions()
        self.debug_timeline.clear()

        for node in self.engine.nodes:
            node.execution_state = "idle"
            node.last_error = None

        try:
            generated, debug = self.engine.execute_with_debug(self.context)
        except Exception as exc:  # noqa: BLE001
            QMessageBox.critical(self, "Graph Execute", str(exc))
            self.debug_console.addItem(f"execute failed: {exc}")
            self._apply_execution_states()
            self._validate()
            self.graph_log.emit(f"Graph execute failed: {exc}")
            return

        for line in debug.timeline_lines():
            self.debug_timeline.addItem(line)

        self._apply_execution_states()
        self._refresh_preview()
        self._validate()
        self.graph_log.emit(f"Graph executed ({len(generated)} outputs)")

    def _apply_execution_states(self) -> None:
        for node in self.engine.nodes:
            item = self.node_items.get(node.node_id)
            if item is None:
                continue
            item.set_status(node.execution_state)

    def _on_selection_changed(self) -> None:
        node = self._selected_node()
        self._update_link_port_selectors()

        while self.param_form.rowCount() > 0:
            self.param_form.removeRow(0)
        self.param_editors.clear()

        if node is None:
            self.selected_label.setText("Selected: none")
            return

        self.selected_label.setText(f"Selected: {node.node_id} [{node.node_type}]")
        definition = get_registry().get(node.node_type)
        if definition is None:
            self.param_form.addRow(QLabel("No schema available"))
            return

        for param_name, schema in definition.parameters.items():
            widget = self._create_param_editor(node, param_name, schema)
            self.param_editors[param_name] = widget
            self.param_form.addRow(param_name, widget)

        for extra_key in sorted(node.parameters.keys()):
            if extra_key in definition.parameters:
                continue
            extra = QLineEdit(str(node.parameters.get(extra_key, "")))
            extra.textChanged.connect(lambda value, key=extra_key: self._set_node_param(node, key, value))
            self.param_form.addRow(extra_key, extra)

    def _create_param_editor(self, node: BaseGraphNode, key: str, schema: NodeParameterSchema) -> QWidget:
        raw_value = node.parameters.get(key, schema.default)

        if schema.value_type == "int":
            editor = QSpinBox()
            editor.setRange(-1_000_000, 1_000_000)
            editor.setValue(self._coerce_int(raw_value, 0))
            editor.valueChanged.connect(lambda value, k=key: self._set_node_param(node, k, int(value)))
            return editor

        if schema.value_type == "float":
            editor = QDoubleSpinBox()
            editor.setDecimals(3)
            editor.setRange(-1_000_000.0, 1_000_000.0)
            editor.setValue(self._coerce_float(raw_value, 0.0))
            editor.valueChanged.connect(lambda value, k=key: self._set_node_param(node, k, float(value)))
            return editor

        if schema.value_type == "boolean":
            editor = QCheckBox()
            editor.setChecked(bool(raw_value))
            editor.stateChanged.connect(lambda _, k=key, e=editor: self._set_node_param(node, k, e.isChecked()))
            return editor

        if schema.value_type == "enum":
            editor = QComboBox()
            for value in schema.enum_values:
                editor.addItem(value)
            if raw_value in schema.enum_values:
                editor.setCurrentText(str(raw_value))
            editor.currentTextChanged.connect(lambda value, k=key: self._set_node_param(node, k, value))
            return editor

        if schema.value_type == "list":
            editor = QLineEdit(
                ",".join(str(v) for v in raw_value) if isinstance(raw_value, list) else str(raw_value or "")
            )
            editor.textChanged.connect(
                lambda value, k=key: self._set_node_param(
                    node,
                    k,
                    [part.strip() for part in value.split(",") if part.strip()],
                )
            )
            return editor

        editor = QLineEdit(str(raw_value if raw_value is not None else ""))
        editor.textChanged.connect(lambda value, k=key: self._set_node_param(node, k, value))
        return editor

    def _set_node_param(self, node: BaseGraphNode, key: str, value: object) -> None:
        node.parameters[key] = value
        item = self.node_items.get(node.node_id)
        if item is not None:
            item.set_display_text(f"{node.node_type} ({len(node.parameters)} params)")
        self._validate()

    def _selected_node(self) -> BaseGraphNode | None:
        selected = [item for item in self.scene.selectedItems() if isinstance(item, GraphNodeItem)]
        if len(selected) != 1:
            return None
        node_id = selected[0].node_id
        for node in self.engine.nodes:
            if node.node_id == node_id:
                return node
        return None

    def _update_link_port_selectors(self) -> None:
        self.from_port.clear()
        self.to_port.clear()
        selected = [item for item in self.scene.selectedItems() if isinstance(item, GraphNodeItem)]
        if len(selected) != 2:
            return

        src, dst = selected
        src_node = next((n for n in self.engine.nodes if n.node_id == src.node_id), None)
        dst_node = next((n for n in self.engine.nodes if n.node_id == dst.node_id), None)
        if src_node is None or dst_node is None:
            return

        for port in src_node.outputs:
            self.from_port.addItem(port.name)
        for port in dst_node.inputs:
            self.to_port.addItem(port.name)

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

    def _collect_node_ids(self, messages: list[str]) -> set[str]:
        ids: set[str] = set()
        for message in messages:
            for node in self.engine.nodes:
                if node.node_id in message:
                    ids.add(node.node_id)
        return ids

    def _refresh_preview(self) -> None:
        self.preview_list.clear()
        preview = self.engine.preview()
        self.preview_list.addItem(f"item preview: {len(preview.items)}")
        self.preview_list.addItem(f"texture preview: {len(preview.textures)}")
        self.preview_list.addItem(f"recipe preview: {len(preview.recipes)}")
        self.preview_list.addItem(f"skill tree preview: {len(preview.skill_trees)}")
        self.preview_list.addItem(f"machine preview: {len(preview.machines)}")

    def _export_template(self) -> None:
        template_name = self.graph_name.text().strip() or self.engine.name
        try:
            path = self.engine.export_template(template_name)
        except Exception as exc:  # noqa: BLE001
            QMessageBox.warning(self, "Template", str(exc))
            return
        self.debug_console.addItem(f"template exported: {path}")

    def _import_template(self) -> None:
        template_name = self.graph_name.text().strip() or self.engine.name
        try:
            self.engine.import_template(template_name)
        except FileNotFoundError:
            QMessageBox.warning(self, "Template", f"Template not found: {template_name}")
            return
        except Exception as exc:  # noqa: BLE001
            QMessageBox.warning(self, "Template", str(exc))
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
        self.debug_console.addItem(f"template imported: {template_name}")

    @staticmethod
    def _coerce_int(value: object, default: int) -> int:
        try:
            return int(str(value)) if value is not None else default
        except (TypeError, ValueError):
            return default

    @staticmethod
    def _coerce_float(value: object, default: float) -> float:
        try:
            return float(str(value)) if value is not None else default
        except (TypeError, ValueError):
            return default
