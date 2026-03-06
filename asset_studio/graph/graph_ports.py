from __future__ import annotations

from dataclasses import dataclass
from enum import Enum


class PortDirection(str, Enum):
    INPUT = "input"
    OUTPUT = "output"


class PortType(str, Enum):
    ANY = "any"
    MATERIAL = "material"
    ITEM = "item"
    TOOL = "tool"
    BLOCK = "block"
    RECIPE = "recipe"
    LOOT = "loot"
    TAG = "tag"
    TEXTURE = "texture"
    DATAPACK = "datapack"
    SKILL_NODE = "skill_node"
    MACHINE = "machine"


@dataclass(frozen=True)
class GraphPort:
    name: str
    direction: PortDirection
    port_type: PortType
