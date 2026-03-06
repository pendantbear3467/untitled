from __future__ import annotations

from dataclasses import dataclass, field


@dataclass
class SkillNode:
    id: str
    display_name: str
    category: str = "combat"
    x: int = 0
    y: int = 0
    cost: int = 1
    requires: list[str] = field(default_factory=list)
    required_level: int = 1
    required_class: str = ""
    modifiers: list[dict] = field(default_factory=list)


@dataclass
class SkillTree:
    name: str
    owner: str = "default"
    class_id: str = "adventurer"
    nodes: dict[str, SkillNode] = field(default_factory=dict)
