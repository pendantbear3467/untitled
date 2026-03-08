from __future__ import annotations

import json
from pathlib import Path

from PyQt6.QtCore import QTimer, pyqtSignal
from PyQt6.QtWidgets import (
    QCheckBox,
    QComboBox,
    QFileDialog,
    QFormLayout,
    QGroupBox,
    QHBoxLayout,
    QLabel,
    QLineEdit,
    QListWidget,
    QListWidgetItem,
    QMessageBox,
    QPushButton,
    QPlainTextEdit,
    QSplitter,
    QSpinBox,
    QVBoxLayout,
    QWidget,
)

from asset_studio.gui.graph_framework import ProgressionScene, ProgressionView
from asset_studio.skilltree.engine import SkillTreeEngine
from asset_studio.skilltree.history import DocumentHistory
from asset_studio.skilltree.models import DEFAULT_CATEGORIES, GraphBookmark, ProgressionDocument, ProgressionNode, SimulationRequest
from asset_studio.workspace.workspace_manager import AssetStudioContext


class SkillTreeDesigner(QWidget):
    log_requested = pyqtSignal(str)

    def __init__(self, context: AssetStudioContext) -> None:
        super().__init__()
        self.context = context
        self.engine = SkillTreeEngine(context.workspace_root / "skilltrees")
        self.current_tree: ProgressionDocument | None = None
        self.selected_nodes: list[str] = []
        self.selected_links: list[tuple[str, str]] = []
        self.clipboard_payload: dict | None = None
        self.history = DocumentHistory(max_entries=100)
        self._dirty = False

        self.autosave_timer = QTimer(self)
        self.autosave_timer.setSingleShot(True)
        self.autosave_timer.timeout.connect(self._autosave)

        root = QVBoxLayout(self)
        toolbar = self._build_toolbar()
        split = QSplitter()
        split.addWidget(self._build_left_panel())
        split.addWidget(self._build_center_panel())
        split.addWidget(self._build_right_panel())
        split.setSizes([320, 900, 380])
        root.addLayout(toolbar)
        root.addWidget(split)

        self._load_preferences()
        self._refresh_tree_list()
        self._refresh_palette()
        self._refresh_template_list()
        self._refresh_node_browser()

    def set_context(self, context: AssetStudioContext) -> None:
        self.context = context
        self.engine = SkillTreeEngine(context.workspace_root / "skilltrees")
        self.current_tree = None
        self.selected_nodes = []
        self.selected_links = []
        self.clipboard_payload = None
        self.history.clear()
        self._dirty = False
        self._load_preferences()
        self._refresh_tree_list()
        self._refresh_palette()
        self._refresh_template_list()
        self._refresh_node_browser()
        self.scene.load_document(ProgressionDocument(name="empty"))
        self.report_view.clear()

    def _build_toolbar(self) -> QHBoxLayout:
        layout = QHBoxLayout()
        self.new_btn = QPushButton("New")
        self.load_btn = QPushButton("Load")
        self.save_btn = QPushButton("Save")
        self.validate_btn = QPushButton("Validate")
        self.simulate_btn = QPushButton("Simulate")
        self.analyze_btn = QPushButton("Analyze")
        self.auto_btn = QPushButton("Auto Arrange")
        self.connect_btn = QPushButton("Link Mode")
        self.connect_btn.setCheckable(True)
        self.undo_btn = QPushButton("Undo")
        self.redo_btn = QPushButton("Redo")
        self.duplicate_btn = QPushButton("Duplicate")
        self.delete_btn = QPushButton("Delete")
        self.copy_btn = QPushButton("Copy")
        self.paste_btn = QPushButton("Paste")
        self.export_btn = QPushButton("Export")
        self.import_btn = QPushButton("Import")

        for widget in [
            self.new_btn,
            self.load_btn,
            self.save_btn,
            self.validate_btn,
            self.simulate_btn,
            self.analyze_btn,
            self.auto_btn,
            self.connect_btn,
            self.undo_btn,
            self.redo_btn,
            self.duplicate_btn,
            self.delete_btn,
            self.copy_btn,
            self.paste_btn,
            self.export_btn,
            self.import_btn,
        ]:
            layout.addWidget(widget)

        self.new_btn.clicked.connect(self._create_tree)
        self.load_btn.clicked.connect(self._load_selected_tree)
        self.save_btn.clicked.connect(self._save_tree)
        self.validate_btn.clicked.connect(self._validate_tree)
        self.simulate_btn.clicked.connect(self._simulate_tree)
        self.analyze_btn.clicked.connect(self._analyze_tree)
        self.auto_btn.clicked.connect(self._auto_arrange)
        self.connect_btn.toggled.connect(self.scene_set_connect_mode)
        self.undo_btn.clicked.connect(self._undo)
        self.redo_btn.clicked.connect(self._redo)
        self.duplicate_btn.clicked.connect(self._duplicate_selection)
        self.delete_btn.clicked.connect(self._delete_selection)
        self.copy_btn.clicked.connect(self._copy_selection)
        self.paste_btn.clicked.connect(self._paste_selection)
        self.export_btn.clicked.connect(self._export_tree)
        self.import_btn.clicked.connect(self._import_tree)
        return layout

    def _build_left_panel(self) -> QWidget:
        panel = QWidget()
        layout = QVBoxLayout(panel)

        tree_box = QGroupBox("Workspace Trees")
        tree_layout = QVBoxLayout(tree_box)
        self.tree_name = QLineEdit("combat_tree")
        self.tree_class = QLineEdit("adventurer")
        self.tree_list = QListWidget()
        tree_layout.addWidget(QLabel("Tree Name"))
        tree_layout.addWidget(self.tree_name)
        tree_layout.addWidget(QLabel("Default Class"))
        tree_layout.addWidget(self.tree_class)
        tree_layout.addWidget(self.tree_list)
        self.tree_list.itemDoubleClicked.connect(self._tree_item_activated)

        search_box = QGroupBox("Search / Filter")
        search_layout = QFormLayout(search_box)
        self.search_input = QLineEdit()
        self.search_category = QComboBox()
        self.search_category.addItem("all")
        self.search_category.addItems(list(DEFAULT_CATEGORIES))
        self.node_browser = QListWidget()
        search_layout.addRow("Text", self.search_input)
        search_layout.addRow("Category", self.search_category)
        search_layout.addRow(self.node_browser)
        self.search_input.textChanged.connect(self._refresh_node_browser)
        self.search_category.currentTextChanged.connect(self._refresh_node_browser)
        self.node_browser.itemSelectionChanged.connect(self._browser_selection_changed)

        palette_box = QGroupBox("Node Palette")
        palette_layout = QVBoxLayout(palette_box)
        self.palette_list = QListWidget()
        add_palette_btn = QPushButton("Add Palette Node")
        add_palette_btn.clicked.connect(self._add_palette_node)
        palette_layout.addWidget(self.palette_list)
        palette_layout.addWidget(add_palette_btn)
        self.palette_list.itemDoubleClicked.connect(lambda *_: self._add_palette_node())

        template_box = QGroupBox("Templates")
        template_layout = QVBoxLayout(template_box)
        self.template_list = QListWidget()
        apply_template_btn = QPushButton("Apply Template")
        apply_template_btn.clicked.connect(self._apply_template)
        template_layout.addWidget(self.template_list)
        template_layout.addWidget(apply_template_btn)

        layout.addWidget(tree_box)
        layout.addWidget(search_box)
        layout.addWidget(palette_box)
        layout.addWidget(template_box)
        return panel

    def _build_center_panel(self) -> QWidget:
        panel = QWidget()
        layout = QVBoxLayout(panel)
        self.scene = ProgressionScene(self)
        self.view = ProgressionView(self.scene)
        self.scene.selection_changed.connect(self._on_scene_selection_changed)
        self.scene.link_requested.connect(self._link_requested)
        self.scene.node_activated.connect(self._focus_node)
        self.scene.node_moved.connect(self._node_moved)
        layout.addWidget(self.view)
        return panel

    def _build_right_panel(self) -> QWidget:
        panel = QWidget()
        layout = QVBoxLayout(panel)

        inspector_box = QGroupBox("Inspector")
        inspector_layout = QFormLayout(inspector_box)
        self.selection_label = QLabel("No selection")
        self.node_id_input = QLineEdit()
        self.node_display_input = QLineEdit()
        self.node_category_input = QComboBox()
        self.node_category_input.setEditable(True)
        self.node_category_input.addItems(list(DEFAULT_CATEGORIES))
        self.node_cost_input = QSpinBox()
        self.node_cost_input.setRange(1, 999)
        self.node_level_input = QSpinBox()
        self.node_level_input.setRange(1, 999)
        self.node_class_input = QLineEdit()
        self.node_tags_input = QLineEdit()
        self.node_requires_input = QLineEdit()
        self.bulk_mode = QCheckBox("Apply to all selected nodes")
        apply_btn = QPushButton("Apply Inspector")
        bookmark_btn = QPushButton("Bookmark Selection")
        apply_btn.clicked.connect(self._apply_inspector)
        bookmark_btn.clicked.connect(self._bookmark_selection)
        inspector_layout.addRow("Selection", self.selection_label)
        inspector_layout.addRow("Node ID", self.node_id_input)
        inspector_layout.addRow("Display Name", self.node_display_input)
        inspector_layout.addRow("Category", self.node_category_input)
        inspector_layout.addRow("Cost", self.node_cost_input)
        inspector_layout.addRow("Required Level", self.node_level_input)
        inspector_layout.addRow("Required Class", self.node_class_input)
        inspector_layout.addRow("Tags", self.node_tags_input)
        inspector_layout.addRow("Requires", self.node_requires_input)
        inspector_layout.addRow(self.bulk_mode)
        inspector_layout.addRow(apply_btn)
        inspector_layout.addRow(bookmark_btn)

        simulation_box = QGroupBox("Simulation")
        simulation_layout = QFormLayout(simulation_box)
        self.sim_level = QSpinBox()
        self.sim_level.setRange(1, 999)
        self.sim_level.setValue(1)
        self.sim_points = QSpinBox()
        self.sim_points.setRange(0, 9999)
        self.sim_class = QLineEdit("adventurer")
        simulation_layout.addRow("Player Level", self.sim_level)
        simulation_layout.addRow("Skill Points", self.sim_points)
        simulation_layout.addRow("Class", self.sim_class)

        analysis_box = QGroupBox("Reports")
        analysis_layout = QVBoxLayout(analysis_box)
        self.report_view = QPlainTextEdit()
        self.report_view.setReadOnly(True)
        analysis_layout.addWidget(self.report_view)

        layout.addWidget(inspector_box)
        layout.addWidget(simulation_box)
        layout.addWidget(analysis_box)
        return panel

    def scene_set_connect_mode(self, enabled: bool) -> None:
        self.scene.set_connect_mode(enabled)

    def _load_preferences(self) -> None:
        profile = self.context.get_user_profile()
        self.tree_class.setText(profile.preferred_class)
        self.sim_class.setText(profile.preferred_class)
        self.search_input.setText(profile.skilltree_preferences.last_search)
        self.autosave_timer.setInterval(max(5, profile.skilltree_preferences.autosave_interval_seconds) * 1000)
        if profile.skilltree_preferences.last_tree and (self.engine.root / f"{profile.skilltree_preferences.last_tree}.json").exists():
            self._load_tree_by_name(profile.skilltree_preferences.last_tree)

    def _save_preferences(self) -> None:
        profile = self.context.get_user_profile()
        profile.preferred_class = self.tree_class.text().strip() or profile.preferred_class
        profile.skilltree_preferences.last_tree = self.current_tree.name if self.current_tree else ""
        profile.skilltree_preferences.last_search = self.search_input.text().strip()
        self.context.save_user_profile(profile)

    def _refresh_tree_list(self) -> None:
        current_name = self.current_tree.name if self.current_tree else ""
        self.tree_list.clear()
        for name in self.engine.list_trees():
            item = QListWidgetItem(name)
            self.tree_list.addItem(item)
            if name == current_name:
                item.setSelected(True)

    def _refresh_palette(self) -> None:
        self.palette_list.clear()
        for entry in self.engine.palette():
            item = QListWidgetItem(f"{entry.label} [{entry.category}]")
            item.setData(256, entry.defaults)
            self.palette_list.addItem(item)

    def _refresh_template_list(self) -> None:
        self.template_list.clear()
        for path in self.engine.list_templates():
            item = QListWidgetItem(path.name)
            item.setData(256, str(path))
            self.template_list.addItem(item)

    def _refresh_node_browser(self) -> None:
        self.node_browser.clear()
        if self.current_tree is None:
            return
        category = self.search_category.currentText()
        results = self.engine.search_nodes(
            self.current_tree,
            text=self.search_input.text(),
            category="" if category == "all" else category,
        )
        for node in results:
            item = QListWidgetItem(f"{node.id} :: {node.display_name or node.id}")
            item.setData(256, node.id)
            self.node_browser.addItem(item)

    def _tree_item_activated(self, item: QListWidgetItem) -> None:
        self._load_tree_by_name(item.text())

    def _load_selected_tree(self) -> None:
        item = self.tree_list.currentItem()
        if item is None:
            name = self.tree_name.text().strip()
            if not name:
                QMessageBox.warning(self, "Skill Tree", "Select a tree or enter a tree name.")
                return
            self._load_tree_by_name(name)
            return
        self._load_tree_by_name(item.text())

    def _load_tree_by_name(self, name: str) -> None:
        try:
            result = self.engine.load_tree_with_report(name)
        except FileNotFoundError:
            return
        self.current_tree = result.document
        self.tree_name.setText(self.current_tree.name)
        self.tree_class.setText(self.current_tree.class_id)
        self.scene.load_document(self.current_tree)
        self._refresh_tree_list()
        self._refresh_node_browser()
        self._update_report("Loaded", result.report.to_dict())
        self._save_preferences()
        self.log_requested.emit(f"Loaded skill tree: {name}")

    def _create_tree(self) -> None:
        profile = self.context.get_user_profile()
        tree = self.engine.create_tree(
            name=self.tree_name.text().strip(),
            owner=profile.username,
            class_id=self.tree_class.text().strip() or profile.preferred_class,
        )
        self.current_tree = tree
        self.history.clear()
        self.scene.load_document(tree)
        self._refresh_tree_list()
        self._refresh_node_browser()
        self._update_report("Created", {"tree": tree.name})
        self._save_preferences()
        self.log_requested.emit(f"Created skill tree: {tree.name}")

    def _save_tree(self) -> None:
        if self.current_tree is None:
            QMessageBox.warning(self, "Skill Tree", "No tree loaded")
            return
        self.current_tree.name = self.tree_name.text().strip() or self.current_tree.name
        self.current_tree.class_id = self.tree_class.text().strip() or "adventurer"
        path = self.engine.save_tree(self.current_tree)
        self._dirty = False
        self._refresh_tree_list()
        self._save_preferences()
        self.log_requested.emit(f"Saved skill tree: {path}")

    def _autosave(self) -> None:
        if self.current_tree is None:
            return
        profile = self.context.get_user_profile()
        if not profile.skilltree_preferences.autosave_enabled:
            return
        path = self.engine.autosave_tree(self.current_tree)
        self.log_requested.emit(f"Autosaved skill tree: {path.name}")

    def _export_tree(self) -> None:
        if self.current_tree is None:
            return
        target, _ = QFileDialog.getSaveFileName(self, "Export Skill Tree", f"{self.current_tree.name}.json", "JSON (*.json)")
        if not target:
            return
        path = self.engine.export_tree(self.current_tree.name, Path(target))
        self.log_requested.emit(f"Exported skill tree: {path}")

    def _import_tree(self) -> None:
        selected, _ = QFileDialog.getOpenFileName(self, "Import Skill Tree", "", "JSON (*.json)")
        if not selected:
            return
        result = self.engine.safe_import_tree(Path(selected))
        self.engine.save_tree(result.document)
        self.current_tree = result.document
        self.scene.load_document(self.current_tree)
        self.tree_name.setText(self.current_tree.name)
        self.tree_class.setText(self.current_tree.class_id)
        self._refresh_tree_list()
        self._refresh_node_browser()
        self._update_report("Imported", result.report.to_dict())
        self.log_requested.emit(f"Imported skill tree: {self.current_tree.name}")

    def _validate_tree(self) -> None:
        if self.current_tree is None:
            return
        report = self.engine.validate(self.current_tree)
        self._update_report("Validation", report.to_dict())
        self.log_requested.emit(f"Validated skill tree: {self.current_tree.name}")

    def _simulate_tree(self) -> None:
        if self.current_tree is None:
            return
        request = SimulationRequest(
            player_level=self.sim_level.value(),
            skill_points=self.sim_points.value(),
            selected_class=self.sim_class.text().strip() or self.tree_class.text().strip() or "adventurer",
            requested_unlocks=list(self.selected_nodes),
        )
        result = self.engine.simulate(self.current_tree, request)
        self.scene.set_simulation_overlay(set(result.unlocked_nodes), set(result.available_nodes))
        self._update_report("Simulation", result.to_dict())
        self.log_requested.emit(f"Simulated skill tree: {self.current_tree.name}")

    def _analyze_tree(self) -> None:
        if self.current_tree is None:
            return
        report = self.engine.analyze_balance(self.current_tree)
        self.scene.set_heatmap_overlay({node_id: entry.value for node_id, entry in report.heatmap.items()})
        self._update_report("Balance Analysis", report.to_dict())
        self.log_requested.emit(f"Analyzed skill tree: {self.current_tree.name}")

    def _auto_arrange(self) -> None:
        if self.current_tree is None:
            return
        self._record_history("Auto arrange")
        self.engine.auto_arrange(self.current_tree)
        self.scene.load_document(self.current_tree)
        self._mark_dirty()

    def _add_palette_node(self) -> None:
        if self.current_tree is None:
            self._create_tree()
        if self.current_tree is None:
            return
        item = self.palette_list.currentItem()
        if item is None:
            return
        defaults = dict(item.data(256) or {})
        self._record_history("Add node")
        node = ProgressionNode(
            id=self.current_tree.next_node_id(defaults.get("display_name", "node").lower().replace(" ", "_")),
            display_name=str(defaults.get("display_name", "New Node")),
            category=str(defaults.get("category", "combat")),
            x=float(len(self.current_tree.nodes) * 60),
            y=float(len(self.current_tree.nodes) * 40),
            cost=int(defaults.get("cost", 1)),
            required_level=int(defaults.get("required_level", 1)),
            required_class=str(defaults.get("required_class", "")),
            tags=[str(value) for value in defaults.get("tags", [])],
        )
        self.engine.add_node(self.current_tree, node)
        self.scene.load_document(self.current_tree)
        self._refresh_node_browser()
        self._mark_dirty()

    def _apply_template(self) -> None:
        if self.current_tree is None:
            return
        item = self.template_list.currentItem()
        if item is None:
            return
        path = Path(str(item.data(256)))
        try:
            payload = json.loads(path.read_text(encoding="utf-8"))
        except Exception as exc:  # noqa: BLE001
            QMessageBox.warning(self, "Template", f"Failed to load template: {exc}")
            return
        if "trees" in payload:
            QMessageBox.information(self, "Template", "Project templates are not pasted into an open tree.")
            return
        if "nodes" not in payload:
            QMessageBox.information(self, "Template", "Template does not contain skill tree nodes.")
            return
        self._record_history("Apply template")
        self.engine.paste_selection(self.current_tree, payload)
        self.scene.load_document(self.current_tree)
        self._refresh_node_browser()
        self._mark_dirty()

    def _on_scene_selection_changed(self, node_ids: list[str], links: list[tuple[str, str]]) -> None:
        self.selected_nodes = node_ids
        self.selected_links = links
        self._populate_inspector()

    def _populate_inspector(self) -> None:
        if self.current_tree is None or not self.selected_nodes:
            self.selection_label.setText("No selection")
            return
        first = self.current_tree.nodes[self.selected_nodes[0]]
        self.selection_label.setText(
            f"{len(self.selected_nodes)} node(s) selected" if len(self.selected_nodes) > 1 else first.id
        )
        self.node_id_input.setText(first.id)
        self.node_display_input.setText(first.display_name)
        self.node_category_input.setCurrentText(first.category)
        self.node_cost_input.setValue(first.cost)
        self.node_level_input.setValue(first.required_level)
        self.node_class_input.setText(first.required_class)
        self.node_tags_input.setText(", ".join(first.normalized_tags()))
        self.node_requires_input.setText(", ".join(first.normalized_requires()))

    def _apply_inspector(self) -> None:
        if self.current_tree is None or not self.selected_nodes:
            return
        target_nodes = self.selected_nodes if self.bulk_mode.isChecked() else [self.selected_nodes[0]]
        rename_allowed = len(target_nodes) == 1
        self._record_history("Inspector edit")
        for node_id in target_nodes:
            node = self.current_tree.nodes[node_id]
            if rename_allowed:
                new_id = self.node_id_input.text().strip() or node.id
                if new_id != node.id and new_id not in self.current_tree.nodes:
                    self.current_tree.nodes[new_id] = self.current_tree.nodes.pop(node.id)
                    node = self.current_tree.nodes[new_id]
                    node.id = new_id
                    for other in self.current_tree.nodes.values():
                        other.requires = [new_id if req_id == node_id else req_id for req_id in other.requires]
                    node_id = new_id
            node.display_name = self.node_display_input.text().strip()
            node.category = self.node_category_input.currentText().strip() or node.category
            node.cost = self.node_cost_input.value()
            node.required_level = self.node_level_input.value()
            node.required_class = self.node_class_input.text().strip()
            node.tags = [tag.strip() for tag in self.node_tags_input.text().split(",") if tag.strip()]
            if not self.bulk_mode.isChecked() or len(target_nodes) == 1:
                node.requires = [req_id.strip() for req_id in self.node_requires_input.text().split(",") if req_id.strip()]
        self.current_tree.sync_links_and_requires(prefer="requires")
        self.scene.load_document(self.current_tree)
        self._refresh_node_browser()
        self._mark_dirty()

    def _bookmark_selection(self) -> None:
        if self.current_tree is None or not self.selected_nodes:
            return
        bookmark_index = len(self.current_tree.bookmarks) + 1
        self.current_tree.bookmarks.append(
            GraphBookmark(
                id=f"bookmark_{bookmark_index}",
                label=f"Selection {bookmark_index}",
                node_ids=list(self.selected_nodes),
            )
        )
        self._mark_dirty()

    def _browser_selection_changed(self) -> None:
        if self.current_tree is None:
            return
        node_ids = {str(item.data(256)) for item in self.node_browser.selectedItems()}
        self.scene.clearSelection()
        for node_id in node_ids:
            item = self.scene.node_items.get(node_id)
            if item is not None:
                item.setSelected(True)

    def _focus_node(self, node_id: str) -> None:
        item = self.scene.node_items.get(node_id)
        if item is None:
            return
        self.scene.clearSelection()
        item.setSelected(True)
        self.view.centerOn(item)

    def _node_moved(self, node_id, old_pos, new_pos) -> None:  # noqa: ANN001
        if self.current_tree is None:
            return
        self.scene.refresh_links_for_node(node_id)
        self._mark_dirty()

    def _link_requested(self, source_id: str, target_id: str) -> None:
        if self.current_tree is None:
            return
        self._record_history("Create link")
        report = self.engine.link_nodes(self.current_tree, source_id, target_id)
        self.scene.load_document(self.current_tree)
        self._update_report("Link", report.to_dict())
        self._mark_dirty()

    def _delete_selection(self) -> None:
        if self.current_tree is None:
            return
        if not self.selected_nodes and not self.selected_links:
            return
        self._record_history("Delete selection")
        for source_id, target_id in self.selected_links:
            self.engine.unlink_nodes(self.current_tree, source_id, target_id)
        if self.selected_nodes:
            self.engine.delete_nodes(self.current_tree, self.selected_nodes)
        self.scene.load_document(self.current_tree)
        self._refresh_node_browser()
        self._mark_dirty()

    def _copy_selection(self) -> None:
        if self.current_tree is None or not self.selected_nodes:
            return
        self.clipboard_payload = self.engine.copy_selection(self.current_tree, self.selected_nodes)
        self.log_requested.emit(f"Copied {len(self.selected_nodes)} node(s)")

    def _paste_selection(self) -> None:
        if self.current_tree is None or self.clipboard_payload is None:
            return
        self._record_history("Paste selection")
        self.engine.paste_selection(self.current_tree, self.clipboard_payload)
        self.scene.load_document(self.current_tree)
        self._refresh_node_browser()
        self._mark_dirty()

    def _duplicate_selection(self) -> None:
        if self.current_tree is None or not self.selected_nodes:
            return
        self._copy_selection()
        self._paste_selection()

    def _undo(self) -> None:
        if self.current_tree is None:
            return
        restored = self.history.undo(self.current_tree)
        if restored is None:
            return
        self.current_tree = restored
        self.scene.load_document(self.current_tree)
        self._refresh_node_browser()
        self._mark_dirty(schedule_autosave=False)

    def _redo(self) -> None:
        if self.current_tree is None:
            return
        restored = self.history.redo(self.current_tree)
        if restored is None:
            return
        self.current_tree = restored
        self.scene.load_document(self.current_tree)
        self._refresh_node_browser()
        self._mark_dirty(schedule_autosave=False)

    def _record_history(self, label: str) -> None:
        if self.current_tree is None:
            return
        self.history.push(label, self.current_tree)

    def _mark_dirty(self, *, schedule_autosave: bool = True) -> None:
        self._dirty = True
        self._save_preferences()
        if schedule_autosave:
            self.autosave_timer.start()

    def _update_report(self, title: str, payload: dict) -> None:
        self.report_view.setPlainText(f"{title}\n\n{json.dumps(payload, indent=2)}")

