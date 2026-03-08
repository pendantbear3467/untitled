from __future__ import annotations

import shutil
from pathlib import Path

from PyQt6.QtCore import Qt, QUrl, pyqtSignal
from PyQt6.QtGui import QBrush, QColor, QDesktopServices
from PyQt6.QtWidgets import (
    QHBoxLayout,
    QInputDialog,
    QLineEdit,
    QMenu,
    QMessageBox,
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
        search_row.addWidget(self.search_input)
        layout.addLayout(search_row)

        self.tree = QTreeWidget()
        self.tree.setColumnCount(2)
        self.tree.setHeaderLabels(["Workspace", "State"])
        self.tree.itemDoubleClicked.connect(self._item_activated)
        self.tree.itemExpanded.connect(self._remember_expanded)
        self.tree.itemCollapsed.connect(self._forget_expanded)
        self.tree.setContextMenuPolicy(Qt.ContextMenuPolicy.CustomContextMenu)
        self.tree.customContextMenuRequested.connect(self._open_context_menu)
        layout.addWidget(self.tree)

    def bind_session(self, session) -> None:
        self.index_service = getattr(session, "workspace_index_service", None)
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

    def _current_index(self) -> WorkspaceIndex | None:
        return self._index_snapshot

    def _entry_for_path(self, path: Path | None) -> WorkspaceEntry | None:
        if path is None or self._index_snapshot is None:
            return None
        return self._index_snapshot.entry(path)

    def _build_item(self, entry: WorkspaceEntry) -> QTreeWidgetItem | None:
        if not self._subtree_matches_filter(entry):
            return None

        label = entry.path.name if entry.path != self.workspace_root else (entry.path.name or str(entry.path))
        badge_text = ", ".join(sorted(entry.badges))
        item = QTreeWidgetItem([label, badge_text])
        item.setData(0, Qt.ItemDataRole.UserRole, str(entry.path))
        item.setToolTip(0, self._tooltip(entry))
        item.setToolTip(1, self._tooltip(entry))
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
        return query in entry.search_text

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
        for issue in entry.issues:
            lines.append(f"[{issue.severity}] {issue.code}: {issue.message}")
        return "\n".join(lines)

    def _apply_visual_state(self, item: QTreeWidgetItem, entry: WorkspaceEntry) -> None:
        item.setForeground(0, QBrush())
        item.setForeground(1, QBrush())
        item.setBackground(0, QBrush())
        item.setBackground(1, QBrush())

        if "stale" in entry.badges:
            item.setForeground(1, QBrush(QColor("#f6c177")))
        if "invalid" in entry.badges:
            item.setForeground(0, QBrush(QColor("#ff8e8e")))
        if "generated" in entry.badges:
            item.setForeground(1, QBrush(QColor("#7dd3fc")))
        if self._current_file is not None and entry.path == self._current_file:
            highlight = QBrush(QColor("#31548a"))
            item.setBackground(0, highlight)
            item.setBackground(1, highlight)

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

    def _item_activated(self, item: QTreeWidgetItem) -> None:
        path = self._item_path(item)
        if path is not None and path.exists() and path.is_file():
            self.file_open_requested.emit(path)

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
        open_action = menu.addAction("Open in Code Editor")
        source_action = menu.addAction("Open Source Definition")
        runtime_action = menu.addAction("Open Runtime Export")
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
        else:
            open_action.setEnabled(path.exists() and path.is_file())
            source_action.setEnabled(bool(entry and entry.links.get("source_definition") and entry.links["source_definition"].exists()))
            runtime_action.setEnabled(bool(entry and entry.links.get("runtime_export") and entry.links["runtime_export"].exists()))

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
                self._open_related(path, "source_definition")
            elif chosen == runtime_action and path is not None:
                self._open_related(path, "runtime_export")
            elif chosen == refresh_action:
                self.refresh_view()
        except Exception as exc:  # noqa: BLE001
            self.notifications.emit(f"Project Browser action failed: {exc}")
            self.refresh_view()

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
        result = QMessageBox.question(
            self,
            "Delete",
            f"Delete {path.name}? This removes it from disk.",
            QMessageBox.StandardButton.Yes | QMessageBox.StandardButton.No,
        )
        if result != QMessageBox.StandardButton.Yes:
            return
        if path.is_dir():
            shutil.rmtree(path)
        else:
            path.unlink(missing_ok=True)
        self.refresh_view()

    def _open_related(self, path: Path, relation: str) -> None:
        if self.index_service is None:
            return
        target = self.index_service.related_path(path, relation)
        if target is None or not target.exists() or not target.is_file():
            self.notifications.emit(f"No {relation.replace('_', ' ')} available for {path.name}")
            return
        self.file_open_requested.emit(target)

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
