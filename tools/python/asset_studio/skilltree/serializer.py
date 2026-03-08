from __future__ import annotations

import json
from dataclasses import dataclass, field
from pathlib import Path
from typing import Any

from asset_studio.skilltree.migrations import MigrationNotice, migrate_payload
from asset_studio.skilltree.models import (
    GraphBookmark,
    ProgressionAnnotation,
    ProgressionDocument,
    ProgressionLink,
    ProgressionNode,
    ProgressionRegion,
    SkillTree,
    ValidationIssue,
    ValidationReport,
)


WORKSPACE_EXPORT_FORMAT = "workspace"
RUNTIME_EXPORT_FORMAT = "runtime"
STUDIO_EXPORT_FORMAT = "studio"


@dataclass(slots=True)
class LoadResult:
    document: ProgressionDocument
    report: ValidationReport = field(default_factory=ValidationReport)


def tree_to_dict(tree: ProgressionDocument, format: str = WORKSPACE_EXPORT_FORMAT) -> dict[str, Any]:
    tree = tree.clone()
    tree.sync_links_and_requires(prefer="requires")
    ordered_nodes = [_node_to_payload(tree.nodes[node_id], include_editor_fields=format == STUDIO_EXPORT_FORMAT) for node_id in tree.deterministic_node_ids()]

    if format == RUNTIME_EXPORT_FORMAT:
        return {
            "tree": tree.name,
            "nodes": ordered_nodes,
        }

    if format == STUDIO_EXPORT_FORMAT:
        return {
            "schemaVersion": tree.schema_version,
            "graphType": tree.graph_type,
            "tree_name": tree.name,
            "owner": tree.owner,
            "class_id": tree.class_id,
            "nodes": ordered_nodes,
            "links": [link.to_dict() for link in tree.normalized_links()],
            "regions": [region.to_dict() for region in sorted(tree.regions, key=lambda item: item.id)],
            "annotations": [annotation.to_dict() for annotation in sorted(tree.annotations, key=lambda item: item.id)],
            "bookmarks": [bookmark.to_dict() for bookmark in sorted(tree.bookmarks, key=lambda item: item.id)],
            "metadata": dict(tree.metadata),
            "preferences": dict(tree.preferences),
        }

    return {
        "tree_name": tree.name,
        "owner": tree.owner,
        "class_id": tree.class_id,
        "nodes": ordered_nodes,
    }


def project_to_dict(documents: list[ProgressionDocument]) -> dict[str, Any]:
    ordered_documents = sorted(documents, key=lambda document: document.name)
    return {
        "schemaVersion": 2,
        "graphType": "skilltree-project",
        "trees": [tree_to_dict(document, format=STUDIO_EXPORT_FORMAT) for document in ordered_documents],
    }


def validation_report_to_dict(document: ProgressionDocument, report: ValidationReport) -> dict[str, Any]:
    return {
        "schemaVersion": 1,
        "graphType": "skilltree-validation-report",
        "tree_name": document.name,
        "report": report.to_dict(),
    }


def load_payload(payload: dict[str, Any]) -> LoadResult:
    migration = migrate_payload(payload)
    report = ValidationReport(source=str(migration.payload.get("tree_name", migration.payload.get("tree", "skill_tree"))))
    report.extend([_notice_to_issue(notice) for notice in migration.notices])

    raw_nodes = migration.payload.get("nodes", [])
    nodes: dict[str, ProgressionNode] = {}
    seen_node_ids: set[str] = set()
    for index, raw in enumerate(raw_nodes):
        if not isinstance(raw, dict):
            report.add(
                "error",
                "invalid-node-payload",
                f"Node entry at index {index} is not a JSON object.",
                details={"index": index},
            )
            continue
        node = ProgressionNode.from_payload(raw)
        if not node.id:
            report.add(
                "error",
                "missing-node-id",
                f"Node entry at index {index} is missing an id.",
                details={"index": index},
            )
            continue
        if node.id in seen_node_ids:
            report.add(
                "error",
                "duplicate-node-id",
                f"Duplicate node id '{node.id}' found during import.",
                node_id=node.id,
            )
            continue
        seen_node_ids.add(node.id)
        nodes[node.id] = node

    links: list[ProgressionLink] = []
    seen_link_keys: set[tuple[str, str, str]] = set()
    for raw in migration.payload.get("links", []):
        if not isinstance(raw, dict):
            continue
        link = ProgressionLink.from_dict(raw)
        if not link.source or not link.target:
            report.add(
                "warning",
                "invalid-link-payload",
                "Skipped a malformed link entry during import.",
            )
            continue
        if link.key() in seen_link_keys:
            report.add(
                "warning",
                "duplicate-link",
                f"Duplicate link '{link.source}' -> '{link.target}' was collapsed during import.",
            )
            continue
        seen_link_keys.add(link.key())
        links.append(link)

    document = SkillTree(
        name=str(migration.payload.get("tree_name", "skill_tree")),
        owner=str(migration.payload.get("owner", "default")),
        class_id=str(migration.payload.get("class_id", "adventurer")),
        graph_type=str(migration.payload.get("graphType", "skilltree")),
        schema_version=int(migration.payload.get("schemaVersion", 2)),
        nodes=nodes,
        links=links,
        regions=[ProgressionRegion.from_dict(raw) for raw in migration.payload.get("regions", []) if isinstance(raw, dict)],
        annotations=[ProgressionAnnotation.from_dict(raw) for raw in migration.payload.get("annotations", []) if isinstance(raw, dict)],
        bookmarks=[GraphBookmark.from_dict(raw) for raw in migration.payload.get("bookmarks", []) if isinstance(raw, dict)],
        metadata=dict(migration.payload.get("metadata", {})),
        preferences=dict(migration.payload.get("preferences", {})),
    )
    document.sync_links_and_requires(prefer="links" if links else "requires")
    return LoadResult(document=document, report=report)


def tree_from_dict(payload: dict[str, Any]) -> ProgressionDocument:
    return load_payload(payload).document


def load_tree_file(path: Path) -> LoadResult:
    payload = json.loads(path.read_text(encoding="utf-8"))
    result = load_payload(payload)
    result.report.source = str(path)
    return result


def read_tree(path: Path) -> ProgressionDocument:
    return load_tree_file(path).document


def write_tree(path: Path, tree: ProgressionDocument, format: str = WORKSPACE_EXPORT_FORMAT) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    payload = tree_to_dict(tree, format=format)
    path.write_text(json.dumps(payload, indent=2) + "\n", encoding="utf-8")


def write_project(path: Path, documents: list[ProgressionDocument]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(project_to_dict(documents), indent=2) + "\n", encoding="utf-8")


def write_validation_report(path: Path, document: ProgressionDocument, report: ValidationReport) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(validation_report_to_dict(document, report), indent=2) + "\n", encoding="utf-8")


def _node_to_payload(node: ProgressionNode, *, include_editor_fields: bool) -> dict[str, Any]:
    payload: dict[str, Any] = {
        "id": node.id,
        "displayName": node.display_name,
        "category": node.category,
        "x": int(node.x),
        "y": int(node.y),
        "cost": int(node.cost),
        "requires": node.normalized_requires(),
        "requiredLevel": int(node.required_level),
        "requiredClass": node.required_class,
        "modifiers": [modifier.to_dict() for modifier in node.modifiers],
    }
    if include_editor_fields:
        payload["tags"] = node.normalized_tags()
        payload["metadata"] = dict(node.metadata)
        payload["note"] = node.note
        payload["icon"] = node.icon
    return payload


def _notice_to_issue(notice: MigrationNotice) -> ValidationIssue:
    return ValidationIssue(severity=notice.severity, code=notice.code, message=notice.message)
