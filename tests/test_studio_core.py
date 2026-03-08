from __future__ import annotations

import json
import shutil
import sys
import unittest
import uuid
from pathlib import Path

TOOLS_PYTHON = Path(__file__).resolve().parents[1] / "tools" / "python"
if str(TOOLS_PYTHON) not in sys.path:
    sys.path.insert(0, str(TOOLS_PYTHON))

from asset_studio.code.editor_service import EditorService
from asset_studio.core.recovery_service import RecoveryService
from asset_studio.core.studio_session import StudioSession
from asset_studio.gui_studio.models import GuiBounds, GuiWidget
from asset_studio.model_studio.models import ModelBone
from asset_studio.runtime.run_service import RunConfiguration


class StudioCoreTests(unittest.TestCase):
    def setUp(self) -> None:
        self.temp_root = Path.cwd() / ".tmp-tests"
        self.temp_root.mkdir(parents=True, exist_ok=True)
        self.workspace = self.temp_root / uuid.uuid4().hex
        self.workspace.mkdir(parents=True, exist_ok=True)

    def tearDown(self) -> None:
        if self.workspace.exists():
            shutil.rmtree(self.workspace, ignore_errors=True)

    @classmethod
    def tearDownClass(cls) -> None:
        temp_root = Path.cwd() / ".tmp-tests"
        if temp_root.exists():
            shutil.rmtree(temp_root, ignore_errors=True)

    def test_studio_session_bootstraps_services_editors_and_build_paths(self) -> None:
        session = StudioSession.open(self.workspace, Path.cwd())
        self.assertIn("code_editor_service", session.app_context.services)
        self.assertIn("gui_studio_engine", session.app_context.services)
        self.assertIn("model_studio_engine", session.app_context.services)
        self.assertIsNotNone(session.command_registry.get("workspace.validate"))
        self.assertIsNotNone(session.editor_registry.get("progression_studio"))
        self.assertIsNotNone(session.editor_registry.get("code_studio"))
        self.assertTrue((self.workspace / ".studio" / "autosave").exists())
        self.assertTrue((self.workspace / "gui_screens").exists())
        self.assertTrue((self.workspace / "models" / "studio").exists())

        asset_path = self.workspace / "assets" / "lang" / "en_us.json"
        asset_path.parent.mkdir(parents=True, exist_ok=True)
        asset_path.write_text(json.dumps({"item.extremecraft.test": "Test"}, indent=2) + "\n", encoding="utf-8")

        build_result = session.build_service.build_workspace("assets")
        export_result = session.build_service.export_pack("resourcepack")
        self.assertTrue(build_result.success)
        self.assertTrue(export_result.success)
        self.assertTrue((self.workspace / "build" / "assets").exists())
        self.assertTrue((self.workspace / "exports" / "resourcepack_assets").exists())

    def test_code_editor_service_tracks_dirty_search_replace_and_snapshots(self) -> None:
        recovery = RecoveryService(self.workspace)
        editor = EditorService(recovery_service=recovery)
        source = self.workspace / "scripts" / "example.txt"
        source.parent.mkdir(parents=True, exist_ok=True)
        source.write_text("hello world\nhello studio\n", encoding="utf-8")

        document = editor.open_document(source)
        matches = editor.search("hello")
        self.assertEqual(len(matches.matches), 2)

        replaced = editor.replace_all(document.document_id, "studio", "platform")
        self.assertEqual(replaced, 1)
        self.assertTrue(document.dirty)

        editor.save_document(document.document_id)
        self.assertIn("platform", source.read_text(encoding="utf-8"))
        snapshots = recovery.list_snapshots(document_id=document.document_id)
        self.assertTrue(snapshots)

    def test_gui_and_model_studio_backends_round_trip_and_validate(self) -> None:
        session = StudioSession.open(self.workspace, Path.cwd())

        gui_document = session.gui_studio_engine.create_document("crusher_screen", screen_type="machine")
        session.gui_studio_engine.add_widget(
            gui_document,
            GuiWidget(
                id="progress_bar",
                widget_type="progress",
                label="Progress",
                bounds=GuiBounds(x=20, y=20, width=24, height=12),
                properties={"value": 25, "max": 100},
                tags=["machine", "progress"],
            ),
            parent_id="root_panel",
        )
        gui_report = session.gui_studio_engine.validate_document(gui_document)
        self.assertEqual(len(gui_report.errors), 0)
        gui_path = session.gui_studio_engine.save_document(gui_document)
        gui_loaded = session.gui_studio_engine.load_document(gui_path)
        self.assertEqual(gui_loaded.document.name, "crusher_screen")
        gui_preview_ids = [widget["id"] for widget in session.gui_studio_engine.preview_payload(gui_loaded.document)["widgets"]]
        self.assertIn("progress_bar", gui_preview_ids)

        model_document = session.model_studio_engine.create_document("crusher_block", model_type="block")
        session.model_studio_engine.add_bone(model_document, ModelBone(id="arm"), parent_id="root")
        model_report = session.model_studio_engine.validate_document(model_document)
        self.assertEqual(len(model_report.errors), 0)
        model_path = session.model_studio_engine.save_document(model_document)
        model_loaded = session.model_studio_engine.load_document(model_path)
        self.assertEqual(model_loaded.document.name, "crusher_block")
        self.assertIn("body", session.model_studio_engine.preview_payload(model_loaded.document)["cubes"][0]["id"])

    def test_recovery_safe_load_and_run_service_capture_logs(self) -> None:
        recovery = RecoveryService(self.workspace)
        broken = self.workspace / "broken.json"
        broken.write_text("{ not valid json", encoding="utf-8")
        payload, warnings = recovery.safe_load_json(broken)
        self.assertIsNone(payload)
        self.assertTrue(warnings)
        self.assertTrue(any("Corrupted JSON recovered" in warning for warning in warnings))

        session = StudioSession.open(self.workspace, Path.cwd())
        session.run_service.save_configuration(
            RunConfiguration(
                name="client",
                command=[sys.executable, "-c", "print('hello studio')"],
                kind="client",
            )
        )
        handle = session.run_service.run_client()
        result = handle.wait(10)
        self.assertIsNotNone(result)
        assert result is not None
        self.assertTrue(result.success)
        self.assertIn("hello studio", result.stdout)
        self.assertIn("hello studio", session.log_model.to_text())


if __name__ == "__main__":
    unittest.main()
