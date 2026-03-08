from __future__ import annotations

from pathlib import Path

from PyQt6.QtCore import Qt, pyqtSignal
from PyQt6.QtWidgets import QTreeWidget, QTreeWidgetItem


class ProjectBrowser(QTreeWidget):
    file_open_requested = pyqtSignal(Path)

    def __init__(self) -> None:
        super().__init__()
        self.setHeaderLabel("Workspace")
        self.itemDoubleClicked.connect(self._item_activated)

    def load_workspace(self, workspace_root: Path) -> None:
        self.clear()
        root = QTreeWidgetItem([workspace_root.name])
        root.setData(0, Qt.ItemDataRole.UserRole, str(workspace_root))
        self.addTopLevelItem(root)
        self._populate(root, workspace_root)
        self.expandToDepth(1)

    def _populate(self, parent: QTreeWidgetItem, directory: Path) -> None:
        try:
            entries = sorted(directory.iterdir(), key=lambda p: (p.is_file(), p.name.lower()))
        except OSError:
            return
        for entry in entries:
            item = QTreeWidgetItem([entry.name])
            item.setData(0, Qt.ItemDataRole.UserRole, str(entry))
            parent.addChild(item)
            if entry.is_dir() and entry.name not in {".git", "__pycache__", "build", ".gradle"}:
                self._populate(item, entry)

    def _item_activated(self, item: QTreeWidgetItem) -> None:
        value = item.data(0, Qt.ItemDataRole.UserRole)
        if not value:
            return
        path = Path(str(value))
        if path.is_file():
            self.file_open_requested.emit(path)
