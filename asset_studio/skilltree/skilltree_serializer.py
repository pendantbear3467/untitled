from __future__ import annotations

import json
from pathlib import Path

from asset_studio.skilltree.models import SkillNode, SkillTree


def tree_to_dict(tree: SkillTree) -> dict:
    return {
        "tree_name": tree.name,
        "owner": tree.owner,
        "class_id": tree.class_id,
        "nodes": [
            {
                "id": node.id,
                "displayName": node.display_name,
                "category": node.category,
                "x": node.x,
                "y": node.y,
                "cost": node.cost,
                "requires": list(node.requires),
                "requiredLevel": node.required_level,
                "requiredClass": node.required_class,
                "modifiers": list(node.modifiers),
            }
            for node in tree.nodes.values()
        ],
    }


def tree_from_dict(payload: dict) -> SkillTree:
    nodes: dict[str, SkillNode] = {}
    for raw in payload.get("nodes", []):
        node = SkillNode(
            id=str(raw.get("id", "")).strip(),
            display_name=str(raw.get("displayName", "")).strip(),
            category=str(raw.get("category", "combat")),
            x=int(raw.get("x", 0)),
            y=int(raw.get("y", 0)),
            cost=int(raw.get("cost", 1)),
            requires=[str(v) for v in raw.get("requires", [])],
            required_level=int(raw.get("requiredLevel", 1)),
            required_class=str(raw.get("requiredClass", "")),
            modifiers=[dict(v) for v in raw.get("modifiers", []) if isinstance(v, dict)],
        )
        if node.id:
            nodes[node.id] = node

    return SkillTree(
        name=str(payload.get("tree_name", "skill_tree")),
        owner=str(payload.get("owner", "default")),
        class_id=str(payload.get("class_id", "adventurer")),
        nodes=nodes,
    )


def write_tree(path: Path, tree: SkillTree) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(tree_to_dict(tree), indent=2) + "\n", encoding="utf-8")


def read_tree(path: Path) -> SkillTree:
    payload = json.loads(path.read_text(encoding="utf-8"))
    return tree_from_dict(payload)
