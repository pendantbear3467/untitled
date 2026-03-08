#!/usr/bin/env python3
"""ExtremeCraft Progression Studio launcher.

This standalone entrypoint intentionally reuses the canonical Asset Studio
skilltree backend + GUI modules so we do not maintain a second graph model,
serializer, validator, or simulation implementation.
"""

from __future__ import annotations

import argparse
import sys
from pathlib import Path

from PyQt6.QtWidgets import QApplication, QMainWindow, QStatusBar

TOOLS_ROOT = Path(__file__).resolve().parent
TOOLS_PYTHON = TOOLS_ROOT / "python"
if str(TOOLS_PYTHON) not in sys.path:
    sys.path.insert(0, str(TOOLS_PYTHON))

from asset_studio.gui.skilltree_designer import SkillTreeDesigner
from asset_studio.workspace.workspace_manager import WorkspaceManager


class SkillTreeEditorWindow(QMainWindow):
    """Standalone host for the canonical Progression Studio editor widget."""

    def __init__(self, workspace_root: Path, repo_root: Path) -> None:
        super().__init__()
        self.workspace_root = workspace_root
        self.repo_root = repo_root
        self.workspace_manager = WorkspaceManager(workspace_root=self.workspace_root, repo_root=self.repo_root)
        self.context = self.workspace_manager.load_context()

        self.setWindowTitle("ExtremeCraft Progression Studio")
        self.resize(1620, 960)
        self.setStatusBar(QStatusBar(self))

        self.designer = SkillTreeDesigner(self.context)
        self.designer.log_requested.connect(self._write_status)
        self.setCentralWidget(self.designer)

        self._write_status(f"Workspace: {self.workspace_root}")

    def _write_status(self, message: str) -> None:
        self.statusBar().showMessage(message, 5000)


def _build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        prog="extremecraft_skill_tree_editor",
        description="Launch ExtremeCraft Progression Studio.",
    )
    parser.add_argument(
        "--workspace",
        default="workspace",
        help="Workspace directory (default: workspace).",
    )
    return parser


def main(argv: list[str] | None = None) -> int:
    parser = _build_parser()
    args = parser.parse_args(argv)

    workspace_root = Path(args.workspace).resolve()
    repo_root = TOOLS_ROOT.parent.resolve()

    app = QApplication(sys.argv)
    app.setApplicationName("ExtremeCraft Progression Studio")
    window = SkillTreeEditorWindow(workspace_root=workspace_root, repo_root=repo_root)
    window.show()
    return app.exec()


if __name__ == "__main__":
    raise SystemExit(main())
