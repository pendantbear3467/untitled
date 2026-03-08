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

from asset_studio.main import main
from asset_studio.skilltree.engine import SkillTreeEngine
from asset_studio.skilltree.models import Modifier, ProgressionDocument, ProgressionNode, SimulationRequest
from asset_studio.skilltree.serializer import (
    LEGACY_PROJECT_FORMAT,
    RUNTIME_EXPORT_FORMAT,
    STUDIO_EXPORT_FORMAT,
    load_payload,
    tree_to_dict,
)


class SkillTreePlatformTests(unittest.TestCase):
    def setUp(self) -> None:
        self.temp_root = Path.cwd() / ".tmp-tests"
        self.temp_root.mkdir(parents=True, exist_ok=True)
        self.workspace = self.temp_root / uuid.uuid4().hex
        self.workspace.mkdir(parents=True, exist_ok=True)
        self.engine = SkillTreeEngine(self.workspace / "skilltrees")

    def tearDown(self) -> None:
        if self.workspace.exists():
            shutil.rmtree(self.workspace, ignore_errors=True)

    @classmethod
    def tearDownClass(cls) -> None:
        temp_root = Path.cwd() / ".tmp-tests"
        if temp_root.exists():
            shutil.rmtree(temp_root, ignore_errors=True)

    def test_legacy_runtime_payload_migrates_to_versioned_document(self) -> None:
        payload = {
            "tree": "combat_tree",
            "nodes": [
                {
                    "id": "starter",
                    "displayName": "Starter",
                    "category": "combat",
                    "x": 0,
                    "y": 0,
                    "cost": 1,
                    "requires": [],
                    "requiredLevel": 1,
                    "requiredClass": "",
                    "modifiers": [],
                },
                {
                    "id": "strike",
                    "displayName": "Strike",
                    "category": "combat",
                    "x": 60,
                    "y": 40,
                    "cost": 1,
                    "requires": ["starter"],
                    "requiredLevel": 2,
                    "requiredClass": "warrior",
                    "modifiers": [{"type": "attack", "value": 2}],
                },
            ],
        }

        result = load_payload(payload)
        self.assertEqual(result.document.name, "combat_tree")
        self.assertEqual(result.document.schema_version, 2)
        self.assertEqual(result.document.nodes["strike"].requires, ["starter"])
        self.assertIn("legacy runtime skill tree export", " ".join(result.report.infos + result.report.warnings).lower())

        studio_payload = tree_to_dict(result.document, format=STUDIO_EXPORT_FORMAT)
        self.assertEqual(studio_payload["schemaVersion"], 2)
        self.assertEqual(studio_payload["links"][0]["source"], "starter")

    def test_validator_reports_cycles_and_missing_dependencies(self) -> None:
        tree = ProgressionDocument(name="broken")
        tree.nodes["starter"] = ProgressionNode(id="starter", display_name="Starter")
        tree.nodes["alpha"] = ProgressionNode(id="alpha", display_name="Alpha", requires=["beta"], required_class="paladin")
        tree.nodes["beta"] = ProgressionNode(id="beta", display_name="Beta", requires=["alpha"], cost=0)
        tree.sync_links_and_requires(prefer="requires")

        report = self.engine.validate(tree)
        error_text = "\n".join(report.errors)
        warning_text = "\n".join(report.warnings)
        self.assertIn("outside 1..999", error_text)
        self.assertIn("unknown class 'paladin'", error_text)
        self.assertIn("Circular dependency detected", error_text)
        self.assertIn("unreachable", warning_text)

    def test_simulator_returns_why_locked_and_cumulative_modifiers(self) -> None:
        tree = ProgressionDocument(name="sim")
        tree.nodes["starter"] = ProgressionNode(
            id="starter",
            display_name="Starter",
            modifiers=[Modifier(type="health", value=5)],
        )
        tree.nodes["arcane_burst"] = ProgressionNode(
            id="arcane_burst",
            display_name="Arcane Burst",
            requires=["starter"],
            required_level=3,
            required_class="mage",
            modifiers=[Modifier(type="attack", value=4)],
        )
        tree.sync_links_and_requires(prefer="requires")

        result = self.engine.simulate(
            tree,
            SimulationRequest(
                player_level=1,
                skill_points=1,
                selected_class="warrior",
                requested_unlocks=["starter", "arcane_burst"],
            ),
        )

        self.assertEqual(result.unlocked_nodes, ["starter"])
        self.assertEqual(result.cumulative_modifiers["health"], 5.0)
        reasons = result.why_locked("arcane_burst")
        self.assertTrue(any(reason.code == "class-restriction" for reason in reasons))
        self.assertTrue(any(reason.code == "level-restriction" for reason in reasons))

    def test_balance_analysis_reports_unreachable_nodes_and_heatmap(self) -> None:
        tree = ProgressionDocument(name="balance")
        tree.nodes["starter"] = ProgressionNode(id="starter", display_name="Starter", modifiers=[Modifier(type="health", value=1)])
        tree.nodes["leaf"] = ProgressionNode(id="leaf", display_name="Leaf", requires=["starter"], modifiers=[Modifier(type="attack", value=2)])
        tree.nodes["cycle_a"] = ProgressionNode(id="cycle_a", display_name="Cycle A", requires=["cycle_b"])
        tree.nodes["cycle_b"] = ProgressionNode(id="cycle_b", display_name="Cycle B", requires=["cycle_a"])
        tree.sync_links_and_requires(prefer="requires")

        report = self.engine.analyze_balance(tree)
        finding_messages = "\n".join(finding.message for finding in report.findings)
        self.assertIn("unreachable", finding_messages.lower())
        self.assertIn("starter", report.heatmap)
        self.assertIn("leaf", report.path_costs)

    def test_engine_round_trip_save_load_export_project_and_diff(self) -> None:
        tree = self.engine.create_tree("combat_tree", owner="tester", class_id="warrior")
        tree.nodes["strike"] = ProgressionNode(id="strike", display_name="Strike", requires=["starter"], modifiers=[Modifier(type="attack", value=3)])
        tree.sync_links_and_requires(prefer="requires")
        self.engine.save_tree(tree)

        loaded = self.engine.load_tree("combat_tree")
        self.assertEqual(sorted(loaded.nodes), ["starter", "strike"])

        project_path = self.workspace / "exports" / "project.skilltree.json"
        self.engine.export_project(project_path)
        payload = json.loads(project_path.read_text(encoding="utf-8"))
        self.assertEqual(payload["graphType"], "skilltree-project")

        other = loaded.clone()
        other.nodes["strike"].cost = 2
        diff = self.engine.diff_trees(loaded, other)
        self.assertIn("strike", diff.changed_nodes)

    def test_legacy_project_export_and_import_preserve_tree_collection(self) -> None:
        combat = self.engine.create_tree("combat_tree", owner="tester", class_id="warrior")
        combat.nodes["strike"] = ProgressionNode(id="strike", display_name="Strike", requires=["starter"])
        combat.sync_links_and_requires(prefer="requires")
        self.engine.save_tree(combat)

        magic = self.engine.create_tree("magic_tree", owner="tester", class_id="mage")
        magic.nodes["focus"] = ProgressionNode(id="focus", display_name="Focus", requires=["starter"])
        magic.sync_links_and_requires(prefer="requires")
        self.engine.save_tree(magic)

        legacy_path = self.workspace / ".extremecraft_project.json"
        self.engine.export_project(
            legacy_path,
            tree_names=["combat_tree", "magic_tree"],
            format=LEGACY_PROJECT_FORMAT,
            active_tree_name="magic_tree",
        )
        legacy_payload = json.loads(legacy_path.read_text(encoding="utf-8"))
        self.assertEqual(legacy_payload["version"], 1)
        self.assertEqual(legacy_payload["ui"]["currentTree"], "magic_tree")
        self.assertIn("combat_tree", legacy_payload["trees"])

        imported_workspace = self.temp_root / uuid.uuid4().hex
        imported_workspace.mkdir(parents=True, exist_ok=True)
        imported_engine = SkillTreeEngine(imported_workspace / "skilltrees")
        result = imported_engine.import_project(legacy_path)
        self.assertEqual(sorted(document.name for document in result.documents), ["combat_tree", "magic_tree"])
        self.assertEqual(result.active_tree_name, "magic_tree")
        self.assertEqual(sorted(imported_engine.list_trees()), ["combat_tree", "magic_tree"])

    def test_cli_skilltree_commands_still_work(self) -> None:
        code = main(["--workspace", str(self.workspace), "skilltree", "new", "combat_tree", "--class-id", "mage"])
        self.assertEqual(code, 0)

        code = main(["--workspace", str(self.workspace), "skilltree", "validate", "combat_tree"])
        self.assertEqual(code, 0)

        runtime_export = self.workspace / "runtime_tree.json"
        code = main([
            "--workspace",
            str(self.workspace),
            "skilltree",
            "export",
            "combat_tree",
            "--out",
            str(runtime_export),
            "--format",
            RUNTIME_EXPORT_FORMAT,
        ])
        self.assertEqual(code, 0)

        payload = json.loads(runtime_export.read_text(encoding="utf-8"))
        self.assertEqual(payload["tree"], "combat_tree")

        legacy_project = self.workspace / ".extremecraft_project.json"
        code = main([
            "--workspace",
            str(self.workspace),
            "skilltree",
            "export-project",
            "--out",
            str(legacy_project),
            "--format",
            LEGACY_PROJECT_FORMAT,
            "--active-tree",
            "combat_tree",
            "combat_tree",
        ])
        self.assertEqual(code, 0)

        imported_workspace = self.temp_root / uuid.uuid4().hex
        code = main([
            "--workspace",
            str(imported_workspace),
            "skilltree",
            "import-project",
            str(legacy_project),
        ])
        self.assertEqual(code, 0)


if __name__ == "__main__":
    unittest.main()
