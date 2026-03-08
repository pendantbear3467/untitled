from __future__ import annotations

from dataclasses import dataclass, field
from typing import Any


@dataclass(frozen=True)
class Vec3:
    x: float
    y: float
    z: float


@dataclass
class FaceMapping:
    texture: str = ""
    uv: tuple[float, float, float, float] = (0.0, 0.0, 16.0, 16.0)
    rotation: int = 0


@dataclass
class ModelCube:
    id: str
    from_pos: Vec3
    to_pos: Vec3
    pivot: Vec3 = field(default_factory=lambda: Vec3(0.0, 0.0, 0.0))
    rotation: Vec3 = field(default_factory=lambda: Vec3(0.0, 0.0, 0.0))
    inflate: float = 0.0
    texture: str = ""
    mirror: bool = False
    faces: dict[str, FaceMapping] = field(default_factory=dict)


@dataclass
class ModelBone:
    id: str
    pivot: Vec3 = field(default_factory=lambda: Vec3(0.0, 0.0, 0.0))
    rotation: Vec3 = field(default_factory=lambda: Vec3(0.0, 0.0, 0.0))
    cubes: list[str] = field(default_factory=list)
    children: list[str] = field(default_factory=list)
    parent: str | None = None
    metadata: dict[str, Any] = field(default_factory=dict)


@dataclass
class ModelDocument:
    name: str
    model_type: str = "block"
    schema_version: int = 1
    texture_width: int = 64
    texture_height: int = 64
    cubes: dict[str, ModelCube] = field(default_factory=dict)
    bones: dict[str, ModelBone] = field(default_factory=dict)
    metadata: dict[str, Any] = field(default_factory=dict)

    def ensure_root_bone(self) -> None:
        if "root" not in self.bones:
            self.bones["root"] = ModelBone(id="root")
