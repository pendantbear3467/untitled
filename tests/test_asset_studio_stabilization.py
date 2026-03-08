from __future__ import annotations

import json
import os
import shutil
import subprocess
import sys
import time
import unittest
import uuid
from pathlib import Path
from unittest.mock import patch

TOOLS_PYTHON = Path(__file__).resolve().parents[1] / "tools" / "python"
if str(TOOLS_PYTHON) not in sys.path:
    sys.path.insert(0, str(TOOLS_PYTHON))

from asset_studio.core.studio_session import StudioSession
from asset_studio.project.asset_database import AssetDatabase as ProjectAssetDatabase
from asset_studio.project.workspace_manager import AssetStudioContext as ProjectAssetStudioContext
from asset_studio.project.workspace_manager import WorkspaceManager as ProjectWorkspaceManager
from asset_studio.sdk_support import sdk_status
from asset_studio.workspace import AssetDatabase as ExportedAssetDatabase
from asset_studio.workspace import AssetStudioContext as ExportedAssetStudioContext
from asset_studio.workspace import ProjectManager as ExportedProjectManager
from asset_studio.workspace import WorkspaceManager as ExportedWorkspaceManager
from asset_studio.workspace.asset_database import AssetDatabase as CanonicalAssetDatabase
from asset_studio.workspace.project_manager import ProjectManager as CanonicalProjectManager
from asset_studio.workspace.workspace_manager import AssetStudioContext as CanonicalAssetStudioContext
from asset_studio.workspace.workspace_manager import WorkspaceManager as CanonicalWorkspaceManager


class AssetStudioStabilizationTests(unittest.TestCase):
    def setUp(self) -> None:
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

    def test_repo_root_imports_asset_studio_main(self) -> None:
        result = subprocess.run(
            [sys.executable, "-c", "import asset_studio.main"],
            cwd=self.repo_root,
            capture_output=True,
            text=True,
            check=False,
        )
        self.assertEqual(result.returncode, 0, msg=result.stderr or result.stdout)

    def test_workspace_index_detects_links_stale_exports_orphans_and_duplicates(self) -> None:
        session = self._open_session()
        modid = "extremecraft"
        now = time.time()

        gui_source = self.workspace / "gui_screens" / "crusher.gui.json"
        gui_source.parent.mkdir(parents=True, exist_ok=True)
        gui_source.write_text(json.dumps({"name": "crusher"}, indent=2) + "\n", encoding="utf-8")

        gui_runtime = self.workspace / "assets" / modid / "studio" / "gui" / "crusher.json"
        gui_runtime.parent.mkdir(parents=True, exist_ok=True)
        gui_runtime.write_text(
            json.dumps({"documentType": "extremecraft-gui-runtime", "resourceId": f"{modid}:studio/gui/crusher"}, indent=2) + "\n",
            encoding="utf-8",
        )
        os.utime(gui_runtime, (now - 30, now - 30))
        os.utime(gui_source, (now, now))

        datapack_source = self.workspace / "data" / "recipes" / "crusher.json"
        datapack_source.parent.mkdir(parents=True, exist_ok=True)
        datapack_source.write_text(json.dumps({"type": "minecraft:crafting_shaped"}, indent=2) + "\n", encoding="utf-8")

        datapack_export = self.workspace / "exports" / "datapack" / "data" / "recipes" / "crusher.json"
        datapack_export.parent.mkdir(parents=True, exist_ok=True)
        datapack_export.write_text(json.dumps({"type": "minecraft:crafting_shaped"}, indent=2) + "\n", encoding="utf-8")
        os.utime(datapack_export, (now - 20, now - 20))
        os.utime(datapack_source, (now, now))

        graph_source = self.workspace / "graphs" / "crusher_bundle.json"
        graph_source.parent.mkdir(parents=True, exist_ok=True)
        graph_source.write_text(json.dumps({"nodes": []}, indent=2) + "\n", encoding="utf-8")

        generated_bundle = self.workspace / "generated" / "crusher_bundle" / "content_bundle.json"
        generated_bundle.parent.mkdir(parents=True, exist_ok=True)
        generated_bundle.write_text(json.dumps({"generated": True}, indent=2) + "\n", encoding="utf-8")
        os.utime(generated_bundle, (now - 10, now - 10))
        os.utime(graph_source, (now, now))

        item_model = self.workspace / "assets" / "models" / "item" / "missing_blade.json"
        item_model.parent.mkdir(parents=True, exist_ok=True)
        item_model.write_text(
            json.dumps({"parent": "item/generated", "textures": {"layer0": f"{modid}:item/missing_blade"}}, indent=2) + "\n",
            encoding="utf-8",
        )

        orphan_runtime = self.workspace / "assets" / modid / "studio" / "models" / "orphan.json"
        orphan_runtime.parent.mkdir(parents=True, exist_ok=True)
        orphan_runtime.write_text(
            json.dumps({"documentType": "extremecraft-model-runtime", "resourceId": f"{modid}:studio/models/orphan"}, indent=2) + "\n",
            encoding="utf-8",
        )

        duplicate_one = self.workspace / "assets" / modid / "studio" / "models" / "duplicate_one.json"
        duplicate_two = self.workspace / "assets" / modid / "studio" / "models" / "duplicate_two.json"
        duplicate_payload = {"documentType": "extremecraft-model-runtime", "resourceId": f"{modid}:studio/models/shared"}
        duplicate_one.write_text(json.dumps(duplicate_payload, indent=2) + "\n", encoding="utf-8")
        duplicate_two.write_text(json.dumps(duplicate_payload, indent=2) + "\n", encoding="utf-8")

        index = session.workspace_index_service.refresh()

        gui_source_entry = index.entry(gui_source)
        self.assertIsNotNone(gui_source_entry)
        assert gui_source_entry is not None
        self.assertEqual(gui_source_entry.kind, "gui_source")
        self.assertEqual(gui_source_entry.links.get("runtime_export"), gui_runtime.resolve(strict=False))
        self.assertIn("stale", gui_source_entry.badges)

        datapack_export_entry = index.entry(datapack_export)
        self.assertIsNotNone(datapack_export_entry)
        assert datapack_export_entry is not None
        self.assertEqual(datapack_export_entry.links.get("source_definition"), datapack_source.resolve(strict=False))
        self.assertIn("stale_export", {issue.code for issue in datapack_export_entry.issues})

        graph_entry = index.entry(graph_source)
        self.assertIsNotNone(graph_entry)
        assert graph_entry is not None
        self.assertIn("stale_generated_artifact", {issue.code for issue in graph_entry.issues})

        item_model_entry = index.entry(item_model)
        self.assertIsNotNone(item_model_entry)
        assert item_model_entry is not None
        item_codes = {issue.code for issue in item_model_entry.issues}
        self.assertIn("missing_texture", item_codes)
        self.assertIn("missing_java_target", item_codes)
        self.assertIn("missing target", item_model_entry.badges)

        orphan_runtime_entry = index.entry(orphan_runtime)
        self.assertIsNotNone(orphan_runtime_entry)
        assert orphan_runtime_entry is not None
        orphan_codes = {issue.code for issue in orphan_runtime_entry.issues}
        self.assertIn("missing_linked_source", orphan_codes)
        self.assertIn("orphaned_generated_file", orphan_codes)

        duplicate_key = f"{modid}:studio/models/shared"
        self.assertIn(duplicate_key, index.duplicate_resource_ids)
        duplicate_entry = index.entry(duplicate_one)
        self.assertIsNotNone(duplicate_entry)
        assert duplicate_entry is not None
        self.assertIn("duplicate_resource_id", {issue.code for issue in duplicate_entry.issues})

    def test_missing_sdk_is_reported_without_crashing_compile(self) -> None:
        session = self._open_session()
        with patch("asset_studio.sdk_support.import_module", side_effect=ModuleNotFoundError("extremecraft_sdk is unavailable")):
            status = sdk_status("compile expansion")
            result = session.build_service.compile_expansion("example_addon")

        self.assertFalse(status.available)
        self.assertFalse(result.success)
        self.assertIsNotNone(result.report)
        assert result.report is not None
        self.assertEqual(result.report.issues[0].code, "sdk_unavailable")
        self.assertIn("extremecraft_sdk", result.message)

    def test_session_restore_reopens_previously_open_code_documents(self) -> None:
        session_one = self._open_session()
        source = self.workspace / "scripts" / "restored.py"
        source.parent.mkdir(parents=True, exist_ok=True)
        source.write_text("print('restore me')\n", encoding="utf-8")

        session_one.code_editor_service.open_document(source)
        session_one.sync_code_session_state()

        session_two = self._open_session()
        reopened_paths = {document.path for document in session_two.code_editor_service.documents.values() if document.path is not None}
        self.assertIn(source.resolve(strict=False), reopened_paths)

    def test_project_package_wrappers_match_canonical_workspace_services(self) -> None:
        self.assertIs(ProjectAssetDatabase, CanonicalAssetDatabase)
        self.assertIs(ProjectWorkspaceManager, CanonicalWorkspaceManager)
        self.assertIs(ProjectAssetStudioContext, CanonicalAssetStudioContext)
        self.assertIs(ExportedAssetDatabase, CanonicalAssetDatabase)
        self.assertIs(ExportedWorkspaceManager, CanonicalWorkspaceManager)
        self.assertIs(ExportedAssetStudioContext, CanonicalAssetStudioContext)
        self.assertIs(ExportedProjectManager, CanonicalProjectManager)


if __name__ == "__main__":
    unittest.main()
