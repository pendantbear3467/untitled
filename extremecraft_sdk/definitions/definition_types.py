from __future__ import annotations

from dataclasses import dataclass, field
from pathlib import Path


SUPPORTED_DEFINITION_TYPES: set[str] = {
    "material",
    "machine",
    "weapon",
    "tool",
    "armor",
    "skill_tree",
    "quest",
    "worldgen",
    "item",
    "block",
    "recipe",
}


@dataclass
class ContentDefinition:
    type: str
    id: str
    payload: dict
    source_path: Path


@dataclass
class AddonSpec:
    name: str
    namespace: str
    version: str
    definitions: list[ContentDefinition] = field(default_factory=list)
    dependencies: list[str] = field(default_factory=list)
    root: Path | None = None
