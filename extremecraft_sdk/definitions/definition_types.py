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


@dataclass(frozen=True)
class VersionedDependency:
    id: str
    version: str = "*"


@dataclass
class DependencyGraphSpec:
    materials: list[str] = field(default_factory=list)
    machines: list[str] = field(default_factory=list)
    addons: list[VersionedDependency] = field(default_factory=list)
    apis: list[VersionedDependency] = field(default_factory=list)


@dataclass
class AddonSpec:
    name: str
    namespace: str
    version: str
    definitions: list[ContentDefinition] = field(default_factory=list)
    dependencies: list[str] = field(default_factory=list)
    dependency_graph: DependencyGraphSpec = field(default_factory=DependencyGraphSpec)
    compatible_platform: str = "*"
    compatible_platform_version: str = "*"
    metadata: dict = field(default_factory=dict)
    root: Path | None = None
