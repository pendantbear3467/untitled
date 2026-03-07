from __future__ import annotations

CURRENT_GRAPH_VERSION = 2


def migrate_payload(payload: dict) -> dict:
    version = int(payload.get("graph_version", 1))
    migrated = dict(payload)

    if version < 2:
        migrated = _migrate_v1_to_v2(migrated)
        version = 2

    migrated["graph_version"] = version
    return migrated


def _migrate_v1_to_v2(payload: dict) -> dict:
    migrated = dict(payload)
    links = []
    for raw in migrated.get("links", []):
        link = dict(raw)
        if "from_port" not in link:
            link["from_port"] = "out"
        if "to_port" not in link:
            link["to_port"] = "in"
        links.append(link)
    migrated["links"] = links

    for node in migrated.get("nodes", []):
        node.setdefault("parameters", {})
        node.setdefault("inputs", [])
        node.setdefault("outputs", [])

    return migrated
