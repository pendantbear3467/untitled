from __future__ import annotations

import shutil
from pathlib import Path

from PyQt6.QtCore import Qt, QUrl, pyqtSignal
from PyQt6.QtGui import QBrush, QColor, QDesktopServices
from PyQt6.QtWidgets import (
    QHBoxLayout,
    QInputDialog,
    QLabel,
    QLineEdit,
    QMenu,
    QMessageBox,
    QPushButton,
    QSplitter,
    QTreeWidget,
    QTreeWidgetItem,
    QVBoxLayout,
    QWidget,
)

from asset_studio.workspace.index_service import WorkspaceEntry, WorkspaceIndex, WorkspaceIndexService


class ProjectBrowser(QWidget):
    file_open_requested = pyqtSignal(Path)
    notifications = pyqtSignal(str)

    def __init__(self) -> None:
        super().__init__()
        self.workspace_root: Path | None = None
        self.index_service: WorkspaceIndexService | None = None
        self.relationship_service = None
        self._index_snapshot: WorkspaceIndex | None = None
        self._expanded_paths: set[Path] = set()
        self._current_file: Path | None = None
        self._path_to_item: dict[Path, QTreeWidgetItem] = {}

        layout = QVBoxLayout(self)
        layout.setContentsMargins(0, 0, 0, 0)

        search_row = QHBoxLayout()
        self.search_input = QLineEdit()
        self.search_input.setPlaceholderText("Filter workspace")
        self.search_input.textChanged.connect(self.refresh_view)
        self.refresh_button = QPushButton("Refresh")
        self.refresh_button.clicked.connect(self.refresh_view)
        search_row.addWidget(self.search_input)
        search_row.addWidget(self.refresh_button)
        layout.addLayout(search_row)

        split = QSplitter(Qt.Orientation.Horizontal)
        self.tree = QTreeWidget()
        self.tree.setColumnCount(3)
        self.tree.setHeaderLabels(["Workspace", "Kind", "State"])
        self.tree.itemDoubleClicked.connect(self._item_activated)
        self.tree.itemExpanded.connect(self._remember_expanded)
        self.tree.itemCollapsed.connect(self._forget_expanded)
        self.tree.itemSelectionChanged.connect(self._sync_inspector)
        self.tree.setContextMenuPolicy(Qt.ContextMenuPolicy.CustomContextMenu)
        self.tree.customContextMenuRequested.connect(self._open_context_menu)
        split.addWidget(self.tree)

        inspector = QWidget()
        inspector_layout = QVBoxLayout(inspector)
        inspector_layout.addWidget(QLabel("Details"))
        self.path_label = QLabel("No selection")
        self.path_label.setWordWrap(True)
        inspector_layout.addWidget(self.path_label)
        self.kind_label = QLabel("")
        inspector_layout.addWidget(self.kind_label)
        self.resource_label = QLabel("")
        self.resource_label.setWordWrap(True)
        inspector_layout.addWidget(self.resource_label)

        actions = QHBoxLayout()
        self.open_source_button = QPushButton("Open Source")
        self.open_source_button.clicked.connect(lambda: self._open_relation("source_document"))
        self.open_runtime_button = QPushButton("Open Runtime")
        self.open_runtime_button.clicked.connect(lambda: self._open_relation("runtime_export"))
        self.open_java_button = QPushButton("Open Java")
        self.open_java_button.clicked.connect(lambda: self._open_relation("java_target"))
        self.open_asset_button = QPushButton("Open Asset")
        self.open_asset_button.clicked.connect(self._open_linked_asset)
        self.open_linked_button = QPushButton("Open Linked")
        self.open_linked_button.clicked.connect(lambda: self._open_relation(None))
        for button in [self.open_source_button, self.open_runtime_button, self.open_java_button, self.open_asset_button, self.open_linked_button]:
            actions.addWidget(button)
        inspector_layout.addLayout(actions)

        inspector_layout.addWidget(QLabel("Issues / Relationship Summary"))
        self.detail_text = QLabel("Select a file to inspect links, warnings, and workspace issues.")
        self.detail_text.setWordWrap(True)
        self.detail_text.setAlignment(Qt.AlignmentFlag.AlignTop | Qt.AlignmentFlag.AlignLeft)
        inspector_layout.addWidget(self.detail_text, 1)
        split.addWidget(inspector)
        split.setSizes([760, 340])
        layout.addWidget(split)

    def bind_session(self, session) -> None:
        self.index_service = getattr(session, "workspace_index_service", None)
        self.relationship_service = getattr(session, "relationship_service", None)
        if self.workspace_root is not None:
            self.refresh_view()

    def load_workspace(self, workspace_root: Path) -> None:
        self.workspace_root = workspace_root.resolve(strict=False)
        self.refresh_view()

    def set_current_file(self, path: Path | None) -> None:
        self._current_file = path.resolve(strict=False) if path is not None else None
        self._apply_current_selection()

    def refresh_view(self) -> None:
        self.tree.clear()
        self._path_to_item.clear()
        self._index_snapshot = None
        if self.workspace_root is None or self.index_service is None:
            return
        try:
            self._index_snapshot = self.index_service.refresh()
            if self.relationship_service is not None:
                self.relationship_service.refresh()
        except Exception as exc:  # noqa: BLE001
            self.notifications.emit(f"Project Browser refresh failed: {exc}")
            return
        root_entry = self._index_snapshot.entry(self.workspace_root)
        if root_entry is None:
            return
        root_item = self._build_item(root_entry)
        if root_item is None:
            return
        self.tree.addTopLevelItem(root_item)
        root_item.setExpanded(True)
        self._apply_current_selection()
        self._sync_inspector()

    def _current_index(self) -> WorkspaceIndex | None:
        return self._index_snapshot

    def _entry_for_path(self, path: Path | None) -> WorkspaceEntry | None:
        if path is None or self._index_snapshot is None:
            return None
        return self._index_snapshot.entry(path)

    def _relationship_record(self, path: Path | None):
        if path is None or self.relationship_service is None:
            return None
        return self.relationship_service.resolve_path(path)

    def _build_item(self, entry: WorkspaceEntry) -> QTreeWidgetItem | None:
        if not self._subtree_matches_filter(entry):
            return None
        label = entry.path.name if entry.path != self.workspace_root else (entry.path.name or str(entry.path))
        badge_text = ", ".join(sorted(entry.badges))
        item = QTreeWidgetItem([label, entry.kind, badge_text])
        item.setData(0, Qt.ItemDataRole.UserRole, str(entry.path))
        tooltip = self._tooltip(entry)
        for column in range(3):
            item.setToolTip(column, tooltip)
        self._apply_visual_state(item, entry)
        self._path_to_item[entry.path] = item
        if entry.is_dir:
            for child in self._children(entry.path):
                child_item = self._build_item(child)
                if child_item is not None:
                    item.addChild(child_item)
            item.setExpanded(entry.path in self._expanded_paths or entry.path == self.workspace_root)
        return item

    def _children(self, path: Path) -> list[WorkspaceEntry]:
        index = self._current_index()
        if index is None or self.workspace_root is None:
            return []
        items: list[WorkspaceEntry] = []
        for child in index.children_of(path):
            if child.path.parent != path:
                continue
            if not child.path.is_relative_to(self.workspace_root):
                continue
            items.append(child)
        return items

    def _matches_filter(self, entry: WorkspaceEntry) -> bool:
        query = self.search_input.text().strip().lower()
        if not query:
            return True
        relationship = self._relationship_record(entry.path)
        relation_text = ""
        if relationship is not None:
            relation_text = " ".join(target.label for target in relationship.targets)
        return query in f"{entry.search_text} {relation_text}".lower()

    def _subtree_matches_filter(self, entry: WorkspaceEntry) -> bool:
        if self._matches_filter(entry):
            return True
        if not entry.is_dir:
            return False
        return any(self._subtree_matches_filter(child) for child in self._children(entry.path))

    def _tooltip(self, entry: WorkspaceEntry) -> str:
        lines = [str(entry.path), f"kind: {entry.kind}"]
        if entry.resource_id:
            lines.append(f"resource: {entry.resource_id}")
        for relation, target in sorted(entry.links.items()):
            lines.append(f"{relation}: {target}")
        relationship = self._relationship_record(entry.path)
        if relationship is not None:
            for target in relationship.targets[:8]:
                lines.append(f"{target.relation}: {target.path}")
        for issue in entry.issues:
            lines.append(f"[{issue.severity}] {issue.code}: {issue.message}")
        return "\n".join(lines)

    def _apply_visual_state(self, item: QTreeWidgetItem, entry: WorkspaceEntry) -> None:
        for column in range(3):
            item.setForeground(column, QBrush())
            item.setBackground(column, QBrush())
        if "stale" in entry.badges:
            item.setForeground(2, QBrush(QColor("#f6c177")))
        if "invalid" in entry.badges:
            item.setForeground(0, QBrush(QColor("#ff8e8e")))
        if "generated" in entry.badges:
            item.setForeground(2, QBrush(QColor("#7dd3fc")))
        if "linked" in entry.badges:
            item.setForeground(1, QBrush(QColor("#8bd5ca")))
        if "missing target" in entry.badges:
            item.setForeground(2, QBrush(QColor("#ffb86c")))
        if self._current_file is not None and entry.path == self._current_file:
            highlight = QBrush(QColor("#31548a"))
            for column in range(3):
                item.setBackground(column, highlight)

    def _apply_current_selection(self) -> None:
        for path, item in self._path_to_item.items():
            item.setSelected(False)
            entry = self._entry_for_path(path)
            if entry is not None:
                self._apply_visual_state(item, entry)
        if self._current_file is None:
            return
        item = self._path_to_item.get(self._current_file)
        if item is None:
            return
        self.tree.setCurrentItem(item)
        item.setSelected(True)

    def _remember_expanded(self, item: QTreeWidgetItem) -> None:
        path = self._item_path(item)
        if path is not None:
            self._expanded_paths.add(path)

    def _forget_expanded(self, item: QTreeWidgetItem) -> None:
        path = self._item_path(item)
        if path is not None:
            self._expanded_paths.discard(path)

    def _item_path(self, item: QTreeWidgetItem | None) -> Path | None:
        if item is None:
            return None
        value = item.data(0, Qt.ItemDataRole.UserRole)
        if not value:
            return None
        return Path(str(value)).resolve(strict=False)

    def _selected_path(self) -> Path | None:
        return self._item_path(self.tree.currentItem())

    def _item_activated(self, item: QTreeWidgetItem) -> None:
        path = self._item_path(item)
        if path is not None and path.exists() and path.is_file():
            self.file_open_requested.emit(path)

    def _sync_inspector(self) -> None:
        path = self._selected_path()
        entry = self._entry_for_path(path)
        record = self._relationship_record(path)
        if entry is None:
            self.path_label.setText("No selection")
            self.kind_label.setText("")
            self.resource_label.setText("")
            self.detail_text.setText("Select a file to inspect links, warnings, and workspace issues.")
            for button in [self.open_source_button, self.open_runtime_button, self.open_java_button, self.open_linked_button]:
                button.setEnabled(False)
            return
        self.path_label.setText(str(entry.path))
        self.kind_label.setText(f"Kind: {entry.kind}")
        self.resource_label.setText(f"Resource: {entry.resource_id or 'n/a'}")
        details: list[str] = []
        if entry.issues:
            details.append("Issues")
            details.extend(f"- [{issue.severity}] {issue.code}: {issue.message}" for issue in entry.issues)
        if record is not None:
            details.append("")
            details.append("Relationships")
            details.extend(f"- {target.relation} -> {target.path.name} ({target.kind})" for target in record.targets[:12])
            if record.warnings:
                details.append("")
                details.append("Relationship warnings")
                details.extend(f"- {warning}" for warning in record.warnings[:8])
        self.detail_text.setText("\n".join(details).strip() or "No issues or relationships available.")
        self.open_source_button.setEnabled(self._relation_target(path, "source_document") is not None)
        self.open_runtime_button.setEnabled(self._relation_target(path, "runtime_export") is not None)
        self.open_java_button.setEnabled(self._relation_target(path, "java_target") is not None)
        self.open_asset_button.setEnabled(self._asset_relation_target(path) is not None)
        self.open_linked_button.setEnabled(record is not None and bool(record.targets))

    def _open_context_menu(self, position) -> None:
        item = self.tree.itemAt(position)
        path = self._item_path(item)
        entry = self._entry_for_path(path)
        menu = QMenu(self)
        new_file_action = menu.addAction("New File")
        new_folder_action = menu.addAction("New Folder")
        rename_action = menu.addAction("Rename")
        duplicate_action = menu.addAction("Duplicate")
        delete_action = menu.addAction("Delete")
        menu.addSeparator()
        reveal_action = menu.addAction("Reveal in Explorer")
        open_action = menu.addAction("Open in Editor")
        source_action = menu.addAction("Open Source Definition")
        runtime_action = menu.addAction("Open Runtime Export")
        java_action = menu.addAction("Open Java Target")
        asset_action = menu.addAction("Open Linked Asset")
        menu.addSeparator()
        refresh_action = menu.addAction("Refresh")

        if path is None:
            rename_action.setEnabled(False)
            duplicate_action.setEnabled(False)
            delete_action.setEnabled(False)
            reveal_action.setEnabled(False)
            open_action.setEnabled(False)
            source_action.setEnabled(False)
            runtime_action.setEnabled(False)
            java_action.setEnabled(False)
        else:
            open_action.setEnabled(path.exists() and path.is_file())
            source_action.setEnabled(self._relation_target(path, "source_document") is not None)
            runtime_action.setEnabled(self._relation_target(path, "runtime_export") is not None)
            java_action.setEnabled(self._relation_target(path, "java_target") is not None)
            asset_action.setEnabled(self._asset_relation_target(path) is not None)

        chosen = menu.exec(self.tree.viewport().mapToGlobal(position))
        if chosen is None:
            return
        try:
            if chosen == new_file_action:
                self._create_path(path, is_dir=False)
            elif chosen == new_folder_action:
                self._create_path(path, is_dir=True)
            elif chosen == rename_action and path is not None:
                self._rename_path(path)
            elif chosen == duplicate_action and path is not None:
                self._duplicate_path(path)
            elif chosen == delete_action and path is not None:
                self._delete_path(path)
            elif chosen == reveal_action and path is not None:
                target = path if path.exists() and path.is_dir() else path.parent
                QDesktopServices.openUrl(QUrl.fromLocalFile(str(target)))
            elif chosen == open_action and path is not None and path.exists() and path.is_file():
                self.file_open_requested.emit(path)
            elif chosen == source_action and path is not None:
                self._open_relation("source_document", path)
            elif chosen == runtime_action and path is not None:
                self._open_relation("runtime_export", path)
            elif chosen == java_action and path is not None:
                self._open_relation("java_target", path)
            elif chosen == asset_action and path is not None:
                self._open_linked_asset(path)
            elif chosen == refresh_action:
                self.refresh_view()
        except Exception as exc:  # noqa: BLE001
            self.notifications.emit(f"Project Browser action failed: {exc}")
            self.refresh_view()

    def _relation_target(self, path: Path | None, relation: str | None) -> Path | None:
        if path is None:
            return None
        if relation is None:
            record = self._relationship_record(path)
            if record is None or not record.targets:
                return None
            return record.targets[0].path
        if self.relationship_service is not None:
            target = self.relationship_service.first_related_path(path, relation)
            if target is not None:
                return target
        if self.index_service is not None:
            return self.index_service.related_path(path, relation)
        return None

    def _open_relation(self, relation: str | None, path: Path | None = None) -> None:
        target = self._relation_target(path or self._selected_path(), relation)
        if target is None or not target.exists() or not target.is_file():
            label = "linked file" if relation is None else relation.replace("_", " ")
            self.notifications.emit(f"No {label} available for the current selection")
            return
        self.file_open_requested.emit(target)

    def _asset_relation_target(self, path: Path | None) -> Path | None:
        if path is None:
            return None
        record = self._relationship_record(path)
        if record is None:
            return None
        asset_kinds = {"texture_asset", "item_model", "block_model", "model_runtime", "json"}
        for target in record.targets:
            if target.kind in asset_kinds:
                return target.path
        return None

    def _open_linked_asset(self, path: Path | None = None) -> None:
        target = self._asset_relation_target(path or self._selected_path())
        if target is None or not target.exists() or not target.is_file():
            self.notifications.emit("No linked asset available for the current selection")
            return
        self.file_open_requested.emit(target)

    def _create_path(self, base_path: Path | None, *, is_dir: bool) -> None:
        if self.workspace_root is None:
            return
        target_dir = self.workspace_root if base_path is None else (base_path if base_path.is_dir() else base_path.parent)
        label = "folder" if is_dir else "file"
        name, accepted = QInputDialog.getText(self, f"New {label.title()}", f"{label.title()} name:")
        if not accepted or not name.strip():
            return
        target = target_dir / name.strip()
        if target.exists():
            self.notifications.emit(f"Path already exists: {target}")
            return
        if is_dir:
            target.mkdir(parents=True, exist_ok=True)
        else:
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_text("", encoding="utf-8")
            self.file_open_requested.emit(target)
        self.refresh_view()

    def _rename_path(self, path: Path) -> None:
        name, accepted = QInputDialog.getText(self, "Rename", "New name:", text=path.name)
        if not accepted or not name.strip() or name.strip() == path.name:
            return
        path.rename(path.with_name(name.strip()))
        self.refresh_view()

    def _duplicate_path(self, path: Path) -> None:
        target = self._unique_copy_path(path)
        if path.is_dir():
            shutil.copytree(path, target)
        else:
            target.write_bytes(path.read_bytes())
            self.file_open_requested.emit(target)
        self.refresh_view()

    def _delete_path(self, path: Path) -> None:
        result = QMessageBox.question(self, "Delete", f"Delete {path.name}? This removes it from disk.", QMessageBox.StandardButton.Yes | QMessageBox.StandardButton.No)
        if result != QMessageBox.StandardButton.Yes:
            return
        if path.is_dir():
            shutil.rmtree(path)
        else:
            path.unlink(missing_ok=True)
        self.refresh_view()

    def _unique_copy_path(self, path: Path) -> Path:
        stem = path.stem if path.is_file() else path.name
        suffix = path.suffix if path.is_file() else ""
        parent = path.parent
        index = 1
        candidate = parent / f"{stem}_copy{suffix}"
        while candidate.exists():
            index += 1
            candidate = parent / f"{stem}_copy_{index}{suffix}"
        return candidate
