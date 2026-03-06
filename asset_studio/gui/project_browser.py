from __future__ import annotations

from pathlib import Path

from PyQt6.QtWidgets import QTreeWidget, QTreeWidgetItem


class ProjectBrowser(QTreeWidget):
    def __init__(self) -> None:
        super().__init__()
        self.setHeaderLabel("Workspace")

    def load_workspace(self, workspace_root: Path) -> None:
        self.clear()
        root = QTreeWidgetItem([workspace_root.name])
        self.addTopLevelItem(root)
        self._populate(root, workspace_root)
        self.expandToDepth(1)

    def _populate(self, parent: QTreeWidgetItem, directory: Path) -> None:
        entries = sorted(directory.iterdir(), key=lambda p: (p.is_file(), p.name.lower()))
        for entry in entries:
            item = QTreeWidgetItem([entry.name])
            parent.addChild(item)
            if entry.is_dir() and entry.name not in {".git", "__pycache__"}:
                self._populate(item, entry)
