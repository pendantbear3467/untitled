from __future__ import annotations

from dataclasses import dataclass, field
from typing import Any


CURRENT_SCHEMA_VERSION = 2


@dataclass(slots=True)
class MigrationNotice:
    severity: str
    code: str
    message: str

    def to_dict(self) -> dict[str, str]:
        return {
            "severity": self.severity,
            "code": self.code,
            "message": self.message,
        }


@dataclass(slots=True)
class MigrationResult:
    payload: dict[str, Any]
    notices: list[MigrationNotice] = field(default_factory=list)


def migrate_payload(payload: dict[str, Any]) -> MigrationResult:
    migrated = dict(payload)
    notices: list[MigrationNotice] = []

    if "tree" in migrated and "tree_name" not in migrated:
        migrated["tree_name"] = migrated.get("tree", "skill_tree")
        notices.append(
            MigrationNotice(
                severity="info",
                code="runtime-export-imported",
                message="Imported legacy runtime skill tree export into the studio document model.",
            )
        )

    version = int(migrated.get("schemaVersion", migrated.get("schema_version", 1)))
    if version < 2:
        migrated, extra_notices = _migrate_v1_to_v2(migrated)
        notices.extend(extra_notices)
        version = 2

    migrated.setdefault("tree_name", migrated.get("name", "skill_tree"))
    migrated.setdefault("owner", "default")
    migrated.setdefault("class_id", "adventurer")
    migrated.setdefault("graphType", "skilltree")
    migrated.setdefault("nodes", [])
    migrated.setdefault("links", [])
    migrated.setdefault("regions", [])
    migrated.setdefault("annotations", [])
    migrated.setdefault("bookmarks", [])
    migrated.setdefault("metadata", {})
    migrated.setdefault("preferences", {})
    migrated["schemaVersion"] = version
    return MigrationResult(payload=migrated, notices=notices)


def _migrate_v1_to_v2(payload: dict[str, Any]) -> tuple[dict[str, Any], list[MigrationNotice]]:
    migrated = dict(payload)
    notices = [
        MigrationNotice(
            severity="warning",
            code="legacy-schema-upgraded",
            message="Legacy skill tree document upgraded to schemaVersion 2 during import.",
        )
    ]

    migrated["schemaVersion"] = 2
    migrated.setdefault("graphType", "skilltree")
    migrated.setdefault("tree_name", migrated.get("name", migrated.get("tree", "skill_tree")))
    migrated.setdefault("owner", "default")
    migrated.setdefault("class_id", "adventurer")
    migrated.setdefault("regions", [])
    migrated.setdefault("annotations", [])
    migrated.setdefault("bookmarks", [])
    migrated.setdefault("metadata", {})
    migrated.setdefault("preferences", {})

    nodes = []
    links = []
    for raw_node in migrated.get("nodes", []):
        if not isinstance(raw_node, dict):
            continue
        node = dict(raw_node)
        node.setdefault("tags", [])
        node.setdefault("metadata", {})
        node.setdefault("note", "")
        node.setdefault("icon", "")
        nodes.append(node)
        target_id = str(node.get("id", "")).strip()
        for req_id in node.get("requires", []):
            source_id = str(req_id).strip()
            if source_id and target_id:
                links.append(
                    {
                        "source": source_id,
                        "target": target_id,
                        "type": "prerequisite",
                        "required": True,
                    }
                )

    if links:
        migrated["links"] = links
    migrated["nodes"] = nodes
    return migrated, notices
