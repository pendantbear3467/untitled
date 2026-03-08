from __future__ import annotations

import json
import os
import shutil
import sys
import unittest
import uuid
from pathlib import Path

os.environ.setdefault("QT_QPA_PLATFORM", "offscreen")

from PyQt6.QtWidgets import QApplication

TOOLS_PYTHON = Path(__file__).resolve().parents[1] / "tools" / "python"
if str(TOOLS_PYTHON) not in sys.path:
    sys.path.insert(0, str(TOOLS_PYTHON))

from asset_studio.ai.workbench_service import AIRequest, AIWorkbenchService
from asset_studio.code.java_support import analyze_java_source
from asset_studio.core.studio_session import StudioSession
from asset_studio.gui.preview_renderer import PreviewRenderer
from asset_studio.gui.project_browser import ProjectBrowser
from asset_studio.schema.migration_service import DocumentMigrationService


def _app() -> QApplication:
    app = QApplication.instance()
    if app is None:
        app = QApplication([])
    return app


class AssetStudioIdeUpgradeTests(unittest.TestCase):
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

    def test_java_symbol_extraction_detects_primary_types_fields_methods_and_resources(self) -> None:
        source = """
package com.extremecraft.example;

import java.util.List;

public class CrusherScreen {
    private final String resourceId = \"extremecraft:studio/gui/crusher\";

    public CrusherScreen() {
    }

    public void openCrusherScreen() {
    }
}
""".strip()
        analysis = analyze_java_source(source, path=Path("src/main/java/com/extremecraft/example/CrusherScreen.java"))

        self.assertEqual(analysis.package_name, "com.extremecraft.example")
        self.assertIn("java.util.List", analysis.imports)
        self.assertIn("CrusherScreen", [symbol.name for symbol in analysis.by_type("class")])
        self.assertIn("resourceId", [symbol.name for symbol in analysis.by_type("field")])
        self.assertIn("openCrusherScreen", [symbol.name for symbol in analysis.by_type("method")])
        self.assertIn("extremecraft:studio/gui/crusher", analysis.resource_ids)

    def test_relationship_service_links_gui_model_and_java_targets(self) -> None:
        session = self._open_session()
        gui_source = self.workspace / "gui_screens" / "crusher.gui.json"
        gui_source.parent.mkdir(parents=True, exist_ok=True)
        gui_source.write_text(json.dumps({"name": "crusher", "schemaVersion": 2, "documentType": "gui-studio", "widgets": [], "rootWidgets": []}), encoding="utf-8")

        gui_runtime = self.workspace / "assets" / "extremecraft" / "studio" / "gui" / "crusher.json"
        gui_runtime.parent.mkdir(parents=True, exist_ok=True)
        gui_runtime.write_text(json.dumps({"documentType": "extremecraft-gui-runtime", "resourceId": "extremecraft:studio/gui/crusher"}), encoding="utf-8")

        model_source = self.workspace / "models" / "studio" / "crusher.model.json"
        model_source.parent.mkdir(parents=True, exist_ok=True)
        model_source.write_text(json.dumps({"name": "crusher", "schemaVersion": 2, "documentType": "cube-model-studio", "bones": [], "cubes": []}), encoding="utf-8")

        java_path = self.repo_root / "src" / "main" / "java" / "com" / "extremecraft" / "example" / "CrusherScreen.java"
        java_path.parent.mkdir(parents=True, exist_ok=True)
        java_path.write_text(
            "package com.extremecraft.example;\npublic class CrusherScreen { String id = \"extremecraft:studio/gui/crusher\"; }\n",
            encoding="utf-8",
        )
        self.addCleanup(lambda: java_path.unlink(missing_ok=True))

        session.workspace_index_service.refresh()
        session.relationship_service.refresh()
        gui_record = session.relationship_service.resolve_path(gui_source)
        self.assertIsNotNone(gui_record)
        assert gui_record is not None
        self.assertIsNotNone(gui_record.first_target("runtime_export"))
        self.assertIsNotNone(gui_record.first_target("java_target"))

        java_record = session.relationship_service.resolve_path(java_path)
        self.assertIsNotNone(java_record)
        assert java_record is not None
        self.assertIn("extremecraft:studio/gui/crusher", java_record.metadata.get("resourceIds", []))
        self.assertTrue(any(target.path == gui_runtime.resolve(strict=False) for target in java_record.targets))

    def test_schema_migration_service_recovers_malformed_documents_and_prepares_backup(self) -> None:
        service = DocumentMigrationService(self.workspace)
        gui_doc = self.workspace / "gui_screens" / "broken.gui.json"
        gui_doc.parent.mkdir(parents=True, exist_ok=True)
        gui_doc.write_text('{"documentType": "legacy-gui",', encoding="utf-8")

        preview = service.load_preview("gui", gui_doc)
        self.assertTrue(preview.applied)
        self.assertTrue(preview.errors)
        self.assertEqual(preview.payload["documentType"], "gui-studio")
        self.assertEqual(preview.payload["schemaVersion"], 2)

        prepared = service.prepare_save("gui", gui_doc, metadata={"diagnosticMode": True})
        self.assertIsNotNone(prepared)
        assert prepared is not None
        self.assertIsNotNone(prepared.backup_path)
        assert prepared.backup_path is not None
        self.assertTrue(prepared.backup_path.exists())

    def test_ai_workbench_artifact_pipeline_produces_diff_and_validated_drafts(self) -> None:
        service = AIWorkbenchService()
        artifact = service.generate_artifact(
            AIRequest(
                mode="apply to current file",
                prompt="Add a generated action",
                current_text="public class CrusherScreen {\n}\n",
                current_path=Path("src/main/java/com/extremecraft/example/CrusherScreen.java"),
            )
        )
        self.assertEqual(artifact.apply_kind, "replace-current")
        self.assertIn("generatedAction", artifact.candidate_content)
        self.assertIn("@@", artifact.diff_preview)

        gui_artifact = service.generate_artifact(AIRequest(mode="generate GUI draft", prompt="Create crusher machine gui"))
        self.assertEqual(gui_artifact.apply_kind, "open-draft")
        self.assertIn("schemaVersion=2", gui_artifact.validation_messages)
        self.assertEqual(gui_artifact.candidate_payload["documentType"], "gui-studio")

    def test_preview_renderer_defaults_to_manual_control_and_accepts_payload_overlays(self) -> None:
        renderer = PreviewRenderer()
        renderer.set_preview_document(
            "source_model",
            payload={"cubes": [{"id": "body", "from": [0, 0, 0], "to": [16, 16, 16]}]},
            metadata={"sourcePath": "models/studio/example.model.json", "resourceId": "extremecraft:studio/models/example"},
            issues=[{"severity": "warning", "message": "stale export"}],
            selection_id="body",
        )
        renderer.set_view_preset("front")
        self.assertFalse(renderer._auto_rotate)
        self.assertEqual(renderer._mode, "source_model")
        self.assertEqual(renderer._selection_id, "body")
        self.assertEqual(renderer._issues[0]["message"], "stale export")

    def test_project_browser_uses_relationship_inspector_without_crashing(self) -> None:
        session = self._open_session()
        source = self.workspace / "gui_screens" / "crusher.gui.json"
        source.parent.mkdir(parents=True, exist_ok=True)
        source.write_text(json.dumps({"name": "crusher", "schemaVersion": 2, "documentType": "gui-studio", "widgets": [], "rootWidgets": []}), encoding="utf-8")
        runtime = self.workspace / "assets" / "extremecraft" / "studio" / "gui" / "crusher.json"
        runtime.parent.mkdir(parents=True, exist_ok=True)
        runtime.write_text(json.dumps({"documentType": "extremecraft-gui-runtime", "resourceId": "extremecraft:studio/gui/crusher"}), encoding="utf-8")

        browser = ProjectBrowser()
        browser.bind_session(session)
        browser.load_workspace(self.workspace)
        browser.set_current_file(source)
        browser.refresh_view()

        self.assertGreater(browser.tree.topLevelItemCount(), 0)
        self.assertIn("gui_source", browser.detail_text.text() + browser.kind_label.text())
        self.assertTrue(browser.open_runtime_button.isEnabled() or browser.open_linked_button.isEnabled())


if __name__ == "__main__":
    unittest.main()
