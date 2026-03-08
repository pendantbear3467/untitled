from __future__ import annotations

import json
import os
import shutil
import sys
import unittest
import uuid
from pathlib import Path

os.environ.setdefault("QT_QPA_PLATFORM", "offscreen")

try:
    from PyQt6.QtWidgets import QApplication
except ModuleNotFoundError:  # pragma: no cover - optional test dependency
    QApplication = None

TOOLS_PYTHON = Path(__file__).resolve().parents[1] / "tools" / "python"
if str(TOOLS_PYTHON) not in sys.path:
    sys.path.insert(0, str(TOOLS_PYTHON))

from asset_studio.core.studio_session import StudioSession

if QApplication is not None:
    from asset_studio.gui.code_studio import CodeStudioPanel
    from asset_studio.gui.preview_renderer import PreviewRenderer
    from asset_studio.gui.project_browser import ProjectBrowser
else:  # pragma: no cover - optional test dependency
    CodeStudioPanel = None
    PreviewRenderer = None
    ProjectBrowser = None


def _app():
    if QApplication is None:
        return None
    app = QApplication.instance()
    if app is None:
        app = QApplication([])
    return app


@unittest.skipIf(QApplication is None, "PyQt6 is not available in this test environment")
class AssetStudioUiConvergenceTests(unittest.TestCase):
    def setUp(self) -> None:
        _app()
        self.repo_root = Path(__file__).resolve().parents[1]
        self.temp_root = self.repo_root / ".tmp-tests"
        self.temp_root.mkdir(parents=True, exist_ok=True)
        self.workspace = self.temp_root / uuid.uuid4().hex
        self.workspace.mkdir(parents=True, exist_ok=True)
        self._sessions: list[StudioSession] = []

    def tearDown(self) -> None:
        for session in self._sessions:
            session.shutdown()
        if self.workspace.exists():
            shutil.rmtree(self.workspace, ignore_errors=True)

    @classmethod
    def tearDownClass(cls) -> None:
        temp_root = Path(__file__).resolve().parents[1] / ".tmp-tests"
        if temp_root.exists():
            shutil.rmtree(temp_root, ignore_errors=True)

    def _open_session(self) -> StudioSession:
        session = StudioSession.open(self.workspace, self.repo_root)
        self._sessions.append(session)
        return session

    def test_project_browser_uses_integrated_navigation_column_summary(self) -> None:
        session = self._open_session()
        gui_source = self.workspace / "gui_screens" / "crusher.gui.json"
        gui_source.parent.mkdir(parents=True, exist_ok=True)
        gui_source.write_text(json.dumps({"name": "crusher", "schemaVersion": 2, "documentType": "gui-studio", "widgets": [], "rootWidgets": []}), encoding="utf-8")
        gui_runtime = self.workspace / "assets" / "extremecraft" / "studio" / "gui" / "crusher.json"
        gui_runtime.parent.mkdir(parents=True, exist_ok=True)
        gui_runtime.write_text(json.dumps({"documentType": "extremecraft-gui-runtime", "resourceId": "extremecraft:studio/gui/crusher"}), encoding="utf-8")

        browser = ProjectBrowser()
        browser.bind_session(session)
        browser.load_workspace(self.workspace)
        browser.set_current_file(gui_source)
        browser.refresh_view()

        self.assertEqual(browser.body_splitter.count(), 2)
        self.assertIn("files indexed", browser.summary_label.text())
        self.assertIn("exact", browser.selection_summary.text().lower())

    def test_code_studio_consolidates_inspector_into_overview_structure_problems_tabs(self) -> None:
        session = self._open_session()
        panel = CodeStudioPanel(session, self.workspace)

        tab_titles = [panel.sidebar_tabs.tabText(index) for index in range(panel.sidebar_tabs.count())]
        self.assertEqual(tab_titles, ["Overview", "Structure", "Problems"])
        self.assertFalse(panel.link_source_button.isEnabled())
        self.assertIn("diagnostics", panel.problem_summary.text().lower())

    def test_preview_renderer_tracks_visual_content_for_empty_and_loaded_states(self) -> None:
        renderer = PreviewRenderer()
        self.assertFalse(renderer.preview_state()["autoRotate"])
        self.assertFalse(renderer._has_visual_content())

        renderer.set_preview_document(
            "source_gui",
            payload={"canvas": {"width": 176, "height": 166}, "widgets": []},
            metadata={"sourcePath": "gui_screens/crusher.gui.json"},
        )
        self.assertTrue(renderer._has_visual_content())

        renderer.set_preview_document("texture", metadata={"sourcePath": "assets/textures/missing.png"}, texture_path=Path("missing.png"))
        self.assertFalse(renderer._has_visual_content())


if __name__ == "__main__":
    unittest.main()
