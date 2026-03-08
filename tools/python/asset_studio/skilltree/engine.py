from __future__ import annotations

import json
from dataclasses import dataclass
from datetime import UTC, datetime
from pathlib import Path
from typing import Any

from asset_studio.skilltree.balance import BalanceAnalyzer
from asset_studio.skilltree.history import DocumentHistory
from asset_studio.skilltree.models import (
    DEFAULT_CATEGORIES,
    DEFAULT_CLASSES,
    DocumentDiff,
    Modifier,
    NodePaletteEntry,
    ProgressionDocument,
    ProgressionNode,
    SimulationRequest,
    ValidationReport,
)
from asset_studio.skilltree.serializer import (
    LEGACY_PROJECT_FORMAT,
    LoadResult,
    ProjectLoadResult,
    RUNTIME_EXPORT_FORMAT,
    STUDIO_EXPORT_FORMAT,
    STUDIO_PROJECT_FORMAT,
    WORKSPACE_EXPORT_FORMAT,
    load_payload,
    load_project_file,
    load_tree_file,
    read_tree,
    tree_to_dict,
    write_project,
    write_tree,
    write_validation_report,
)
from asset_studio.skilltree.simulator import ProgressionSimulator
from asset_studio.skilltree.validator import ProgressionValidator


@dataclass(slots=True)
class AutosaveSnapshot:
    path: Path
    tree_name: str
    created_at: str


class SkillTreeEngine:
    def __init__(self, root: Path) -> None:
        self.root = root if root.name == "skilltrees" else root / "skilltrees"
        self.root.mkdir(parents=True, exist_ok=True)
        self.workspace_root = self.root.parent
        self.autosave_root = self.root / "autosave"
        self.recovery_root = self.root / "recovery"
        self.export_root = self.workspace_root / "exports" / "skilltrees"
        self.report_root = self.workspace_root / "reports" / "skilltree"
        self.autosave_root.mkdir(parents=True, exist_ok=True)
        self.recovery_root.mkdir(parents=True, exist_ok=True)
        self.export_root.mkdir(parents=True, exist_ok=True)
        self.report_root.mkdir(parents=True, exist_ok=True)

        self.validator = ProgressionValidator(
            allowed_categories=DEFAULT_CATEGORIES,
            known_classes=DEFAULT_CLASSES,
        )
        self.simulator = ProgressionSimulator()
        self.balance = BalanceAnalyzer(simulator=self.simulator)
        self.history = DocumentHistory(max_entries=100)

    def create_tree(self, name: str, owner: str, class_id: str = "adventurer") -> ProgressionDocument:
        tree_name = str(name).strip() or "skill_tree"
        tree = ProgressionDocument(name=tree_name, owner=owner or "default", class_id=class_id or "adventurer")
        starter = ProgressionNode(
            id="starter",
            display_name="Starter",
            category="combat",
            x=0.0,
            y=0.0,
            cost=1,
            requires=[],
            required_level=1,
            required_class="",
            modifiers=[],
            tags=["root"],
        )
        tree.nodes[starter.id] = starter
        tree.sync_links_and_requires(prefer="requires")
        self.save_tree(tree)
        return tree

    def save_tree(self, tree: ProgressionDocument, *, autosave: bool = False, format: str = STUDIO_EXPORT_FORMAT) -> Path:
        path = self._autosave_path(tree.name) if autosave else self.root / f"{tree.name}.json"
        write_tree(path, tree, format=format)
        return path

    def autosave_tree(self, tree: ProgressionDocument, *, reason: str = "autosave") -> Path:
        snapshot_path = self._autosave_path(tree.name)
        payload = tree_to_dict(tree, format=STUDIO_EXPORT_FORMAT)
        payload.setdefault("metadata", {})["autosaveReason"] = reason
        snapshot_path.write_text(json.dumps(payload, indent=2) + "\n", encoding="utf-8")
        return snapshot_path

    def list_recovery_snapshots(self, tree_name: str | None = None) -> list[AutosaveSnapshot]:
        snapshots: list[AutosaveSnapshot] = []
        pattern = f"{tree_name}__*.json" if tree_name else "*.json"
        for path in sorted(self.autosave_root.glob(pattern)):
            snapshots.append(
                AutosaveSnapshot(
                    path=path,
                    tree_name=path.stem.split("__", 1)[0],
                    created_at=datetime.fromtimestamp(path.stat().st_mtime, UTC).isoformat(),
                )
            )
        return snapshots

    def recover_tree(self, snapshot_path: Path) -> ProgressionDocument:
        result = load_tree_file(snapshot_path)
        self.save_tree(result.document)
        return result.document

    def load_tree(self, name: str) -> ProgressionDocument:
        return read_tree(self.root / f"{name}.json")

    def load_tree_with_report(self, name: str) -> LoadResult:
        return load_tree_file(self.root / f"{name}.json")

    def load_project_file(self, path: Path) -> ProjectLoadResult:
        return load_project_file(path)

    def import_tree(self, file_path: Path) -> ProgressionDocument:
        result = self.safe_import_tree(file_path)
        self.save_tree(result.document)
        return result.document

    def safe_import_tree(self, file_path: Path) -> LoadResult:
        result = load_tree_file(file_path)
        validation = self.validate(result.document)
        result.report.extend(validation.issues)
        return result

    def import_project(self, file_path: Path) -> ProjectLoadResult:
        result = load_project_file(file_path)
        for document in result.documents:
            self.save_tree(document)
        return result

    def export_tree(self, name: str, target: Path, *, format: str = WORKSPACE_EXPORT_FORMAT) -> Path:
        tree = self.load_tree(name)
        write_tree(target, tree, format=format)
        return target

    def export_project(
        self,
        target: Path,
        tree_names: list[str] | None = None,
        *,
        format: str = STUDIO_PROJECT_FORMAT,
        active_tree_name: str = "",
    ) -> Path:
        documents = [self.load_tree(name) for name in (tree_names or self.list_trees())]
        write_project(target, documents, format=format, active_tree_name=active_tree_name)
        return target

    def export_validation_report(self, tree: ProgressionDocument, target: Path) -> Path:
        report = self.validate(tree)
        write_validation_report(target, tree, report)
        return target

    def list_trees(self) -> list[str]:
        return sorted(path.stem for path in self.root.glob("*.json") if path.is_file())

    def validate(self, tree: ProgressionDocument) -> ValidationReport:
        return self.validator.validate(tree)

    def simulate(self, tree: ProgressionDocument, request: SimulationRequest) -> Any:
        return self.simulator.simulate(tree, request)

    def analyze_balance(self, tree: ProgressionDocument, request: SimulationRequest | None = None) -> Any:
        return self.balance.analyze(tree, request)

    def palette(self) -> list[NodePaletteEntry]:
        return [
            NodePaletteEntry(
                id=f"{category}_skill",
                label=f"{category.title()} Skill",
                category=category,
                defaults={
                    "display_name": f"{category.title()} Skill",
                    "category": category,
                    "cost": 1,
                    "required_level": 1,
                    "required_class": "",
                    "tags": [category],
                },
                tags=[category],
            )
            for category in DEFAULT_CATEGORIES
        ] + [
            NodePaletteEntry(
                id="capstone",
                label="Capstone",
                category="combat",
                defaults={
                    "display_name": "Capstone",
                    "category": "combat",
                    "cost": 5,
                    "required_level": 20,
                    "required_class": "",
                    "tags": ["capstone"],
                },
                tags=["capstone"],
            )
        ]

    def list_templates(self) -> list[Path]:
        template_root = self.workspace_root / "templates"
        if not template_root.exists():
            return []
        return sorted(path for path in template_root.iterdir() if path.suffix in {".json", ".template", ".yaml"} or path.name.endswith(".template.json"))

    def search_nodes(
        self,
        tree: ProgressionDocument,
        *,
        text: str = "",
        category: str = "",
        required_class: str = "",
        modifier: str = "",
        tag: str = "",
    ) -> list[ProgressionNode]:
        query = text.strip().lower()
        results: list[ProgressionNode] = []
        for node in tree.nodes.values():
            haystack = " ".join([
                node.id,
                node.display_name,
                node.category,
                node.required_class,
                " ".join(node.normalized_tags()),
                " ".join(mod.type for mod in node.modifiers),
            ]).lower()
            if query and query not in haystack:
                continue
            if category and node.category != category:
                continue
            if required_class and node.required_class != required_class:
                continue
            if modifier and modifier not in {mod.type for mod in node.modifiers}:
                continue
            if tag and tag not in node.normalized_tags():
                continue
            results.append(node)
        return sorted(results, key=lambda node: node.id)

    def bulk_edit(self, tree: ProgressionDocument, node_ids: list[str], updates: dict[str, Any]) -> list[str]:
        changed: list[str] = []
        for node_id in sorted(set(node_ids)):
            node = tree.nodes.get(node_id)
            if node is None:
                continue
            if "category" in updates and updates["category"]:
                node.category = str(updates["category"])
            if "required_class" in updates:
                node.required_class = str(updates.get("required_class", ""))
            if "required_level" in updates and updates["required_level"] is not None:
                node.required_level = int(updates["required_level"])
            if "cost" in updates and updates["cost"] is not None:
                node.cost = int(updates["cost"])
            if "add_tags" in updates:
                node.tags = sorted(set(node.tags) | {str(tag).strip() for tag in updates["add_tags"] if str(tag).strip()})
            if "remove_tags" in updates:
                remove_tags = {str(tag).strip() for tag in updates["remove_tags"] if str(tag).strip()}
                node.tags = [tag for tag in node.tags if tag not in remove_tags]
            changed.append(node_id)
        tree.sync_links_and_requires(prefer="requires")
        return changed

    def copy_selection(self, tree: ProgressionDocument, node_ids: list[str]) -> dict[str, Any]:
        selected = {node_id for node_id in node_ids if node_id in tree.nodes}
        payload = {
            "schemaVersion": 1,
            "graphType": "skilltree-selection",
            "nodes": [
                tree_to_dict(
                    ProgressionDocument(name=tree.name, nodes={node_id: tree.nodes[node_id].clone()}),
                    format=STUDIO_EXPORT_FORMAT,
                )["nodes"][0]
                for node_id in sorted(selected)
            ],
            "links": [
                link.to_dict()
                for link in tree.normalized_links()
                if link.source in selected and link.target in selected
            ],
        }
        return payload

    def paste_selection(
        self,
        tree: ProgressionDocument,
        payload: dict[str, Any],
        *,
        x_offset: float = 80.0,
        y_offset: float = 80.0,
    ) -> list[str]:
        result = load_payload(
            {
                "tree_name": tree.name,
                "nodes": payload.get("nodes", []),
                "links": payload.get("links", []),
            }
        )
        source = result.document
        id_map: dict[str, str] = {}
        new_ids: list[str] = []
        for node in source.nodes.values():
            new_id = tree.next_node_id(node.id)
            id_map[node.id] = new_id
            clone = node.clone()
            clone.id = new_id
            clone.display_name = f"{clone.display_name} Copy" if clone.display_name else new_id
            clone.x += x_offset
            clone.y += y_offset
            clone.requires = []
            tree.nodes[new_id] = clone
            new_ids.append(new_id)
        for link in source.normalized_links():
            if link.source in id_map and link.target in id_map:
                self.link_nodes(tree, id_map[link.source], id_map[link.target])
        tree.sync_links_and_requires(prefer="requires")
        return sorted(new_ids)

    def link_nodes(self, tree: ProgressionDocument, source_id: str, target_id: str) -> ValidationReport:
        report = self.validator.validate_link(tree, source_id, target_id)
        if report.has_errors:
            return report
        target = tree.nodes[target_id]
        target.requires = sorted({*target.requires, source_id})
        tree.sync_links_and_requires(prefer="requires")
        return report

    def unlink_nodes(self, tree: ProgressionDocument, source_id: str, target_id: str) -> None:
        target = tree.nodes.get(target_id)
        if target is None:
            return
        target.requires = [req_id for req_id in target.requires if req_id != source_id]
        tree.sync_links_and_requires(prefer="requires")

    def add_node(self, tree: ProgressionDocument, node: ProgressionNode) -> ProgressionNode:
        node_id = node.id.strip() or tree.next_node_id("node")
        if node_id in tree.nodes:
            node_id = tree.next_node_id(node_id)
        clone = node.clone()
        clone.id = node_id
        tree.nodes[node_id] = clone
        tree.sync_links_and_requires(prefer="requires")
        return clone

    def duplicate_node(self, tree: ProgressionDocument, node_id: str, *, x_offset: float = 60.0, y_offset: float = 40.0) -> ProgressionNode:
        source = tree.nodes[node_id]
        clone = source.clone()
        clone.id = tree.next_node_id(node_id)
        clone.display_name = f"{source.display_name} Copy" if source.display_name else clone.id
        clone.x += x_offset
        clone.y += y_offset
        clone.requires = list(source.requires)
        tree.nodes[clone.id] = clone
        tree.sync_links_and_requires(prefer="requires")
        return clone

    def delete_nodes(self, tree: ProgressionDocument, node_ids: list[str]) -> None:
        remove_ids = set(node_ids)
        for node_id in remove_ids:
            tree.nodes.pop(node_id, None)
        for node in tree.nodes.values():
            node.requires = [req_id for req_id in node.requires if req_id not in remove_ids]
        tree.sync_links_and_requires(prefer="requires")

    def auto_arrange(self, tree: ProgressionDocument, *, x_gap: float = 180.0, y_gap: float = 120.0) -> None:
        ordered = tree.deterministic_node_ids()
        depths: dict[str, int] = {}
        for node_id in ordered:
            requirements = [req_id for req_id in tree.nodes[node_id].normalized_requires() if req_id in tree.nodes]
            depths[node_id] = (max(depths.get(req_id, 0) for req_id in requirements) + 1) if requirements else 0
        lanes: dict[int, list[str]] = {}
        for node_id, depth in depths.items():
            lanes.setdefault(depth, []).append(node_id)
        for depth, node_ids in sorted(lanes.items()):
            for index, node_id in enumerate(sorted(node_ids)):
                tree.nodes[node_id].x = float(index * x_gap)
                tree.nodes[node_id].y = float(depth * y_gap)

    def build_preview_data(self, tree: ProgressionDocument) -> dict[str, Any]:
        tree = tree.clone()
        tree.sync_links_and_requires(prefer="requires")
        return {
            "tree": tree.name,
            "classId": tree.class_id,
            "nodes": [
                {
                    "id": node.id,
                    "displayName": node.display_name,
                    "category": node.category,
                    "position": {"x": node.x, "y": node.y},
                    "tags": node.normalized_tags(),
                }
                for node in (tree.nodes[node_id] for node_id in tree.deterministic_node_ids())
            ],
            "links": [
                {"source": link.source, "target": link.target, "type": link.link_type}
                for link in tree.normalized_links()
            ],
        }

    def diff_trees(self, left: ProgressionDocument, right: ProgressionDocument) -> DocumentDiff:
        left_links = {(link.source, link.target) for link in left.normalized_links()}
        right_links = {(link.source, link.target) for link in right.normalized_links()}
        changed_nodes = []
        for node_id in sorted(set(left.nodes) & set(right.nodes)):
            if tree_to_dict(ProgressionDocument(name=left.name, nodes={node_id: left.nodes[node_id].clone()}), format=STUDIO_EXPORT_FORMAT)["nodes"][0] != tree_to_dict(ProgressionDocument(name=right.name, nodes={node_id: right.nodes[node_id].clone()}), format=STUDIO_EXPORT_FORMAT)["nodes"][0]:
                changed_nodes.append(node_id)
        changed_metadata = []
        for field_name in ("name", "owner", "class_id", "graph_type"):
            if getattr(left, field_name) != getattr(right, field_name):
                changed_metadata.append(field_name)
        return DocumentDiff(
            added_nodes=sorted(set(right.nodes) - set(left.nodes)),
            removed_nodes=sorted(set(left.nodes) - set(right.nodes)),
            changed_nodes=changed_nodes,
            added_links=sorted(right_links - left_links),
            removed_links=sorted(left_links - right_links),
            changed_metadata=changed_metadata,
        )

    def compare_branches(self, tree: ProgressionDocument, left_root: str, right_root: str) -> dict[str, Any]:
        analysis = self.balance.analyze(tree)
        left_branch = analysis.branches.get(left_root)
        right_branch = analysis.branches.get(right_root)
        return {
            "left": left_branch.to_dict() if left_branch else None,
            "right": right_branch.to_dict() if right_branch else None,
        }

    def default_export_target(self, tree_name: str, *, runtime: bool = False) -> Path:
        suffix = "runtime" if runtime else "workspace"
        return self.export_root / f"{tree_name}.{suffix}.json"

    def default_project_export_target(self, *, legacy: bool = False) -> Path:
        name = ".extremecraft_project.json" if legacy else "skilltree.project.json"
        return self.export_root / name

    def _autosave_path(self, tree_name: str) -> Path:
        timestamp = datetime.now(UTC).strftime("%Y%m%dT%H%M%SZ")
        return self.autosave_root / f"{tree_name}__{timestamp}.json"
