from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path


@dataclass
class GraphPreview:
    items: list[Path]
    textures: list[Path]
    recipes: list[Path]
    machines: list[Path]
    skill_trees: list[Path]


class GraphPreviewBuilder:
    def build(self, workspace_root: Path) -> GraphPreview:
        assets = workspace_root / "assets"
        data = workspace_root / "data"
        skills = workspace_root / "skilltrees"

        return GraphPreview(
            items=sorted((assets / "models" / "item").glob("*.json")) if (assets / "models" / "item").exists() else [],
            textures=sorted((assets / "textures").rglob("*.png")) if (assets / "textures").exists() else [],
            recipes=sorted((data / "recipes").glob("*.json")) if (data / "recipes").exists() else [],
            machines=sorted((workspace_root / "machines").glob("*.json")) if (workspace_root / "machines").exists() else [],
            skill_trees=sorted(skills.glob("*.json")) if skills.exists() else [],
        )
