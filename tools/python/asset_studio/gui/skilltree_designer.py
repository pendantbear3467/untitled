from __future__ import annotations

import json
import subprocess
import sys
from pathlib import Path

from PyQt6.QtCore import pyqtSignal
from PyQt6.QtWidgets import (
    QComboBox,
    QFileDialog,
    QFormLayout,
    QGroupBox,
    QHBoxLayout,
    QLineEdit,
    QListWidget,
    QMessageBox,
    QPushButton,
    QSpinBox,
    QVBoxLayout,
    QWidget,
)

from asset_studio.skilltree.models import SkillNode, SkillTree
from asset_studio.skilltree.skilltree_engine import SkillTreeEngine
from asset_studio.workspace.workspace_manager import AssetStudioContext


class SkillTreeDesigner(QWidget):
    log_requested = pyqtSignal(str)

    def __init__(self, context: AssetStudioContext) -> None:
        super().__init__()
        self.context = context
        self.engine = SkillTreeEngine(context.workspace_root / "skilltrees")
        self.current_tree: SkillTree | None = None

        layout = QVBoxLayout(self)

        tree_box = QGroupBox("Skill Tree")
        tree_form = QFormLayout(tree_box)
        self.tree_name = QLineEdit("combat_tree")
        self.tree_class = QLineEdit("adventurer")
        tree_form.addRow("Tree Name", self.tree_name)
        tree_form.addRow("Class", self.tree_class)

        tree_actions = QHBoxLayout()
        create_btn = QPushButton("Create")
        load_btn = QPushButton("Load")
        save_btn = QPushButton("Save")
        validate_btn = QPushButton("Validate")
        export_btn = QPushButton("Export")
        import_btn = QPushButton("Import")
        studio_btn = QPushButton("Open Progression Studio")
        tree_actions.addWidget(create_btn)
        tree_actions.addWidget(load_btn)
        tree_actions.addWidget(save_btn)
        tree_actions.addWidget(validate_btn)
        tree_actions.addWidget(export_btn)
        tree_actions.addWidget(import_btn)
        tree_actions.addWidget(studio_btn)

        node_box = QGroupBox("Node Editor")
        node_form = QFormLayout(node_box)
        self.node_id = QLineEdit("new_node")
        self.node_display = QLineEdit("New Node")
        self.node_category = QComboBox()
        self.node_category.addItems(["combat", "survival", "arcane", "exploration", "technology"])
        self.node_cost = QSpinBox()
        self.node_cost.setRange(1, 999)
        self.node_cost.setValue(1)
        node_form.addRow("Node ID", self.node_id)
        node_form.addRow("Display Name", self.node_display)
        node_form.addRow("Category", self.node_category)
        node_form.addRow("Cost", self.node_cost)

        node_actions = QHBoxLayout()
        add_node_btn = QPushButton("Add Node")
        del_node_btn = QPushButton("Delete Node")
        node_actions.addWidget(add_node_btn)
        node_actions.addWidget(del_node_btn)

        self.node_list = QListWidget()

        layout.addWidget(tree_box)
        layout.addLayout(tree_actions)
        layout.addWidget(node_box)
        layout.addLayout(node_actions)
        layout.addWidget(self.node_list)

        create_btn.clicked.connect(self._create_tree)
        load_btn.clicked.connect(self._load_tree)
        save_btn.clicked.connect(self._save_tree)
        validate_btn.clicked.connect(self._validate_tree)
        export_btn.clicked.connect(self._export_tree)
        import_btn.clicked.connect(self._import_tree)
        studio_btn.clicked.connect(self._open_progression_studio)
        add_node_btn.clicked.connect(self._add_node)
        del_node_btn.clicked.connect(self._delete_node)

    def set_context(self, context: AssetStudioContext) -> None:
        self.context = context
        self.engine = SkillTreeEngine(context.workspace_root / "skilltrees")
        self.current_tree = None
        self.node_list.clear()

    def _create_tree(self) -> None:
        profile = self.context.get_user_profile()
        tree = self.engine.create_tree(
            name=self.tree_name.text().strip(),
            owner=profile.username,
            class_id=self.tree_class.text().strip() or profile.preferred_class,
        )
        self.current_tree = tree
        self._refresh_node_list()
        self.log_requested.emit(f"Created skill tree: {tree.name}")

    def _load_tree(self) -> None:
        name = self.tree_name.text().strip()
        try:
            self.current_tree = self.engine.load_tree(name)
            self.tree_class.setText(self.current_tree.class_id)
            self._refresh_node_list()
            self.log_requested.emit(f"Loaded skill tree: {name}")
        except FileNotFoundError:
            QMessageBox.warning(self, "Skill Tree", f"Skill tree not found: {name}")

    def _save_tree(self) -> None:
        if self.current_tree is None:
            QMessageBox.warning(self, "Skill Tree", "No tree loaded")
            return
        self.current_tree.class_id = self.tree_class.text().strip() or "adventurer"
        path = self.engine.save_tree(self.current_tree)
        self.log_requested.emit(f"Saved skill tree: {path}")

    def _validate_tree(self) -> None:
        if self.current_tree is None:
            QMessageBox.warning(self, "Skill Tree", "No tree loaded")
            return
        report = self.engine.validate(self.current_tree)
        message = f"Errors: {len(report.errors)}\nWarnings: {len(report.warnings)}"
        if report.errors:
            message += "\n\n" + "\n".join(report.errors)
        QMessageBox.information(self, "Validation", message)
        self.log_requested.emit(f"Validated skill tree: {self.current_tree.name}")

    def _export_tree(self) -> None:
        if self.current_tree is None:
            QMessageBox.warning(self, "Skill Tree", "No tree loaded")
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
        tree = self.engine.import_tree(Path(selected))
        self.current_tree = tree
        self.tree_name.setText(tree.name)
        self.tree_class.setText(tree.class_id)
        self._refresh_node_list()
        self.log_requested.emit(f"Imported skill tree: {tree.name}")

    def _add_node(self) -> None:
        if self.current_tree is None:
            self._create_tree()
        if self.current_tree is None:
            return

        node = SkillNode(
            id=self.node_id.text().strip(),
            display_name=self.node_display.text().strip(),
            category=self.node_category.currentText(),
            cost=self.node_cost.value(),
            required_level=1,
            modifiers=[],
        )
        if not node.id:
            QMessageBox.warning(self, "Skill Tree", "Node ID is required")
            return
        self.current_tree.nodes[node.id] = node
        self._refresh_node_list()
        self.log_requested.emit(f"Added node: {node.id}")

    def _delete_node(self) -> None:
        if self.current_tree is None:
            return
        current = self.node_list.currentItem()
        if current is None:
            return
        node_id = current.text().split(" :: ", 1)[0]
        if node_id in self.current_tree.nodes:
            del self.current_tree.nodes[node_id]
            self._refresh_node_list()
            self.log_requested.emit(f"Deleted node: {node_id}")

    def _refresh_node_list(self) -> None:
        self.node_list.clear()
        if self.current_tree is None:
            return
        for node in self.current_tree.nodes.values():
            self.node_list.addItem(f"{node.id} :: {node.display_name} [{node.category}] (cost={node.cost})")

    def _open_progression_studio(self) -> None:
        editor_path = Path(__file__).resolve().parents[3] / "extremecraft_skill_tree_editor.py"
        if not editor_path.exists():
            QMessageBox.warning(self, "Progression Studio", f"Editor not found: {editor_path}")
            return
        try:
            subprocess.Popen([sys.executable, str(editor_path)], cwd=str(self.context.workspace_root))
            self.log_requested.emit("Opened Progression Studio window")
        except Exception as exc:  # noqa: BLE001
            QMessageBox.warning(self, "Progression Studio", f"Could not launch editor:\n{exc}")
