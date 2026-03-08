from __future__ import annotations

import importlib
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
    from asset_studio.gui.preview_renderer import PreviewRenderer
else:  # pragma: no cover - optional test dependency
    PreviewRenderer = None


def _app():
    if QApplication is None:
        return None
    app = QApplication.instance()
    if app is None:
        app = QApplication([])
    return app


class AssetStudioConvergenceTests(unittest.TestCase):
    def setUp(self) -> None:
        _app()
        self.repo_root = Path(__file__).resolve().parents[1]
        self.temp_root = self.repo_root / ".tmp-tests"
        self.temp_root.mkdir(parents=True, exist_ok=True)
        self.workspace = self.temp_root / uuid.uuid4().hex
        self.workspace.mkdir(parents=True, exist_ok=True)
        self._sessions: list[StudioSession] = []
        self._cleanup_callbacks: list[object] = []

    def tearDown(self) -> None:
        for session in self._sessions:
            session.shutdown()
        for callback in reversed(self._cleanup_callbacks):
            callback()
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

    def _create_java_file(self, relative: Path, content: str) -> Path:
        java_root = self.repo_root / "src" / "main" / "java"
        path = java_root / relative
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(content, encoding="utf-8")
        self._cleanup_callbacks.append(lambda p=path: p.unlink(missing_ok=True))
        return path

    def test_canonical_gui_namespace_import_surface_smoke(self) -> None:
        gui_namespace = importlib.import_module("asset_studio.gui")
        studio_namespace = importlib.import_module("asset_studio.studio")

        self.assertIn("AssetStudioWindow", gui_namespace.__all__)
        self.assertIn("launch_gui", gui_namespace.__all__)
        self.assertIn("PreviewRenderer", gui_namespace.__all__)
        self.assertIn("AssetStudioWindow", studio_namespace.__all__)
        self.assertIn("compatibility", (studio_namespace.__doc__ or "").lower())
        self.assertIn("canonical", (gui_namespace.__doc__ or "").lower())

    def test_relationship_service_marks_exact_and_possible_targets_separately(self) -> None:
        session = self._open_session()
        gui_source = self.workspace / "gui_screens" / "crusher.gui.json"
        gui_source.parent.mkdir(parents=True, exist_ok=True)
        gui_source.write_text(json.dumps({"name": "crusher", "schemaVersion": 2, "documentType": "gui-studio", "widgets": [], "rootWidgets": []}), encoding="utf-8")

        gui_runtime = self.workspace / "assets" / "extremecraft" / "studio" / "gui" / "crusher.json"
        gui_runtime.parent.mkdir(parents=True, exist_ok=True)
        gui_runtime.write_text(json.dumps({"documentType": "extremecraft-gui-runtime", "resourceId": "extremecraft:studio/gui/crusher"}), encoding="utf-8")

        java_path = self._create_java_file(
            Path("com") / "extremecraft" / "_asset_studio_tests" / uuid.uuid4().hex / "CrusherScreen.java",
            "package com.extremecraft._asset_studio_tests;\n"
            "public class CrusherScreen { String id = \"extremecraft:studio/gui/crusher\"; }\n",
        )

        session.workspace_index_service.refresh()
        session.relationship_service.refresh()
        record = session.relationship_service.resolve_path(gui_source)
        self.assertIsNotNone(record)
        assert record is not None

        runtime_target = record.first_target("runtime_export")
        self.assertIsNotNone(runtime_target)
        assert runtime_target is not None
        self.assertTrue(runtime_target.authoritative)
        self.assertEqual(runtime_target.source, "index")
        self.assertEqual(runtime_target.confidence, "exact")

        java_targets = record.targets_for("java_target")
        self.assertEqual(len(java_targets), 1)
        self.assertFalse(java_targets[0].authoritative)
        self.assertIn(java_targets[0].source, {"heuristic-name", "heuristic-resource-id"})
        self.assertIsNone(session.relationship_service.first_related_path(gui_source, "java_target"))
        self.assertEqual(session.relationship_service.first_related_path(gui_source, "java_target", allow_inferred=True), java_path.resolve(strict=False))

    def test_relationship_service_does_not_auto_navigate_ambiguous_inferred_targets(self) -> None:
        session = self._open_session()
        gui_source = self.workspace / "gui_screens" / "crusher.gui.json"
        gui_source.parent.mkdir(parents=True, exist_ok=True)
        gui_source.write_text(json.dumps({"name": "crusher", "schemaVersion": 2, "documentType": "gui-studio", "widgets": [], "rootWidgets": []}), encoding="utf-8")

        java_root = Path("com") / "extremecraft" / "_asset_studio_tests" / uuid.uuid4().hex
        self._create_java_file(
            java_root / "CrusherScreen.java",
            "package com.extremecraft._asset_studio_tests;\npublic class CrusherScreen {}\n",
        )
        self._create_java_file(
            java_root / "CrusherMenu.java",
            "package com.extremecraft._asset_studio_tests;\npublic class CrusherMenu {}\n",
        )

        session.workspace_index_service.refresh()
        session.relationship_service.refresh()
        record = session.relationship_service.resolve_path(gui_source)
        self.assertIsNotNone(record)
        assert record is not None

        java_targets = record.targets_for("java_target")
        self.assertEqual(len(java_targets), 2)
        self.assertTrue(all(not target.authoritative for target in java_targets))
        self.assertIn("java_target", record.metadata.get("ambiguousRelations", []))
        self.assertIsNone(record.preferred_target("java_target", allow_inferred=True))
        self.assertIsNone(session.relationship_service.first_related_path(gui_source, "java_target", allow_inferred=True))

    @unittest.skipIf(QApplication is None, "PyQt6 is not available in this test environment")
    def test_preview_renderer_owns_variant_state_and_override_resolution(self) -> None:
        renderer = PreviewRenderer()
        renderer.set_preview_variants(
            "source_model",
            {
                "source_model": {
                    "payload": {"cubes": [{"id": "body", "from": [0, 0, 0], "to": [16, 16, 16]}]},
                    "metadata": {"sourcePath": "models/studio/body.model.json"},
                    "selection_id": "body",
                },
                "runtime_model": {
                    "payload": {"cubes": [{"id": "body_runtime", "from": [0, 0, 0], "to": [16, 16, 16]}]},
                    "metadata": {"sourcePath": "assets/extremecraft/studio/models/body.json"},
                },
            },
        )
        state = renderer.preview_state()
        self.assertEqual(state["mode"], "source_model")
        self.assertEqual(state["autoMode"], "source_model")
        self.assertEqual(state["modeOverride"], "auto")
        self.assertEqual(state["selectionId"], "body")
        self.assertIn("runtime_model", state["availableModes"])

        renderer.set_mode_override("runtime_model")
        self.assertEqual(renderer.preview_state()["mode"], "runtime_model")
        self.assertEqual(renderer.preview_state()["modeOverride"], "runtime_model")

        renderer.set_auto_rotate(True)
        renderer.set_view_preset("front")
        renderer.reset_camera()
        state = renderer.preview_state()
        self.assertTrue(state["autoRotate"])
        self.assertEqual(state["viewPreset"], "isometric")

        renderer.set_mode_override(None)
        self.assertEqual(renderer.preview_state()["mode"], "source_model")
        self.assertEqual(renderer.preview_state()["modeOverride"], "auto")


if __name__ == "__main__":
    unittest.main()
