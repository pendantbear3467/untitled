from __future__ import annotations

import argparse
import json
import re
import struct
import zlib
from dataclasses import dataclass
from pathlib import Path
from typing import Iterable


ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src" / "main" / "java"
RES = ROOT / "src" / "main" / "resources"
DATA = RES / "data" / "extremecraft"
ASSETS = RES / "assets" / "extremecraft"
RECIPES = DATA / "recipes"
MACHINE_DEFS = DATA / "machines"
LOOT_BLOCKS = DATA / "loot_tables" / "blocks"
WORLDGEN_CFG = DATA / "worldgen" / "configured_feature"
WORLDGEN_PLACED = DATA / "worldgen" / "placed_feature"
BIOME_MOD = DATA / "forge" / "biome_modifier"
LANG_FILE = ASSETS / "lang" / "en_us.json"
ITEM_TEXTURES = ASSETS / "textures" / "item"
ABILITY_TEXTURES = ASSETS / "textures" / "gui" / "abilities"
ABILITY_DATA = DATA / "abilities"
REPORT_DIR = ROOT / "docs" / "generated"
GRAPH_FILE = REPORT_DIR / "runtime_item_graph.json"


@dataclass(frozen=True)
class MaterialDef:
    id: str
    tier: str
    has_tools: bool
    harvest_level: int
    min_y: int
    max_y: int
    vein_count: int
    vein_size: int
    biomes: tuple[str, ...]


@dataclass(frozen=True)
class MachineDef:
    id: str
    category: str
    stage: str
    process_time: int
    energy_per_tick: int
    output_multiplier: int
    generation_per_tick: int


def parse_materials() -> list[MaterialDef]:
    src = (JAVA / "com" / "extremecraft" / "machine" / "material" / "OreMaterialCatalog.java").read_text(encoding="utf-8")
    pattern = re.compile(
        r'OreMaterialDefinition\("([a-z0-9_]+)",\s*MaterialTier\.([A-Z]+),\s*(true|false),\s*(true|false),\s*'
        r'(\d+),\s*(-?\d+),\s*(-?\d+),\s*(\d+),\s*(\d+),\s*Set\.of\(([^)]*)\)\)'
    )
    materials: list[MaterialDef] = []
    for m in pattern.finditer(src):
        biome_src = m.group(10)
        biomes = tuple(re.findall(r'"([^"]+)"', biome_src))
        materials.append(
            MaterialDef(
                id=m.group(1),
                tier=m.group(2).lower(),
                has_tools=m.group(3) == "true",
                harvest_level=int(m.group(5)),
                min_y=int(m.group(6)),
                max_y=int(m.group(7)),
                vein_count=int(m.group(8)),
                vein_size=int(m.group(9)),
                biomes=biomes if biomes else ("minecraft:overworld",),
            )
        )
    return materials


def parse_machines() -> list[MachineDef]:
    src = (JAVA / "com" / "extremecraft" / "machine" / "core" / "MachineCatalog.java").read_text(encoding="utf-8")
    pattern = re.compile(
        r'MachineDefinition\("([a-z0-9_]+)",\s*MachineCategory\.([A-Z]+),\s*ProgressionStage\.([A-Z]+),\s*'
        r'(\d+),\s*(\d+),\s*(\d+),\s*(\d+)\)'
    )
    machines: list[MachineDef] = []
    for m in pattern.finditer(src):
        machines.append(
            MachineDef(
                id=m.group(1),
                category=m.group(2).lower(),
                stage=m.group(3).lower(),
                process_time=int(m.group(4)),
                energy_per_tick=int(m.group(5)),
                output_multiplier=int(m.group(6)),
                generation_per_tick=int(m.group(7)),
            )
        )
    return machines


def parse_cable_ids() -> list[str]:
    src = (JAVA / "com" / "extremecraft" / "machine" / "cable" / "CableTier.java").read_text(encoding="utf-8")
    return re.findall(r'[A-Z]+\("([a-z0-9_]+)"', src)


def parse_moditems_ids() -> set[str]:
    ids: set[str] = set()
    mod_items = (JAVA / "com" / "extremecraft" / "registry" / "ModItems.java").read_text(encoding="utf-8")
    tech_items = (JAVA / "com" / "extremecraft" / "future" / "registry" / "TechItems.java").read_text(encoding="utf-8")
    ids.update(re.findall(r'ITEMS\.register\("([a-z0-9_]+)"', mod_items))
    ids.update(re.findall(r'ITEMS\.register\("([a-z0-9_]+)"', tech_items))
    return ids


def parse_modblock_ids() -> set[str]:
    ids: set[str] = set()
    mod_blocks = (JAVA / "com" / "extremecraft" / "registry" / "ModBlocks.java").read_text(encoding="utf-8")
    ids.update(re.findall(r'BLOCKS\.register\("([a-z0-9_]+)"', mod_blocks))
    return ids


def humanize_id(raw_id: str) -> str:
    return " ".join(p.capitalize() for p in raw_id.replace("/", "_").split("_") if p)


def recipe_path(name: str) -> Path:
    return RECIPES / "generated" / f"{name}.json"


def machine_recipe_path(name: str) -> Path:
    return RECIPES / "machine_processing" / "generated" / f"{name}.json"


def pulverizing_path(name: str) -> Path:
    return RECIPES / "pulverizing" / "generated" / f"{name}.json"


def json_dump(data: dict | list) -> str:
    return json.dumps(data, indent=2, ensure_ascii=False) + "\n"


def write_json_missing(path: Path, obj: dict | list) -> bool:
    if path.exists():
        return False
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json_dump(obj), encoding="utf-8")
    return True


def item_id_exists(runtime_items: set[str], item: str) -> bool:
    return item in runtime_items


def ec_or_vanilla(runtime_items: set[str], candidates: Iterable[str]) -> str:
    for c in candidates:
        if c.startswith("minecraft:"):
            return c
        if item_id_exists(runtime_items, c):
            return f"extremecraft:{c}"
    return "minecraft:iron_ingot"


def write_shaped(name: str, result: str, pattern: list[str], key: dict[str, str], count: int = 1) -> bool:
    return write_json_missing(
        recipe_path(name),
        {
            "type": "minecraft:crafting_shaped",
            "pattern": pattern,
            "key": {k: {"item": v} for k, v in key.items()},
            "result": {"item": f"extremecraft:{result}", "count": count},
        },
    )


def write_shapeless(name: str, result: str, ingredients: list[str], count: int = 1) -> bool:
    return write_json_missing(
        recipe_path(name),
        {
            "type": "minecraft:crafting_shapeless",
            "ingredients": [{"item": i} for i in ingredients],
            "result": {"item": f"extremecraft:{result}", "count": count},
        },
    )
