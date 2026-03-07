from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path

from asset_studio.skilltree.models import SkillNode, SkillTree
from asset_studio.skilltree.skilltree_serializer import read_tree, write_tree


@dataclass
class SkillTreeValidation:
    errors: list[str]
    warnings: list[str]


class SkillTreeEngine:
    def __init__(self, root: Path) -> None:
        self.root = root

    def create_tree(self, name: str, owner: str, class_id: str = "adventurer") -> SkillTree:
        tree = SkillTree(name=name, owner=owner, class_id=class_id)
        starter = SkillNode(
            id="starter",
            display_name="Starter",
            category="combat",
            x=0,
            y=0,
            cost=1,
            requires=[],
            required_level=1,
        )
        tree.nodes[starter.id] = starter
        self.save_tree(tree)
        return tree

    def save_tree(self, tree: SkillTree) -> Path:
        path = self.root / f"{tree.name}.json"
        write_tree(path, tree)
        return path

    def load_tree(self, name: str) -> SkillTree:
        return read_tree(self.root / f"{name}.json")

    def import_tree(self, file_path: Path) -> SkillTree:
        tree = read_tree(file_path)
        self.save_tree(tree)
        return tree

    def export_tree(self, name: str, target: Path) -> Path:
        tree = self.load_tree(name)
        write_tree(target, tree)
        return target

    def list_trees(self) -> list[str]:
        return sorted(path.stem for path in self.root.glob("*.json"))

    def validate(self, tree: SkillTree) -> SkillTreeValidation:
        errors: list[str] = []
        warnings: list[str] = []

        if not tree.nodes:
            errors.append("Tree has no nodes")

        for node in tree.nodes.values():
            if not node.display_name:
                warnings.append(f"Node '{node.id}' has empty display name")
            if node.cost < 1:
                errors.append(f"Node '{node.id}' has invalid cost {node.cost}")
            for req in node.requires:
                if req not in tree.nodes:
                    errors.append(f"Node '{node.id}' requires missing node '{req}'")

        return SkillTreeValidation(errors=errors, warnings=warnings)
