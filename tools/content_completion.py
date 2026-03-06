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

def write_smelting(name: str, input_item: str, output_item: str, xp: float = 0.7, time: int = 200) -> bool:
    return write_json_missing(
        recipe_path(name),
        {
            "type": "minecraft:smelting",
            "ingredient": {"item": f"extremecraft:{input_item}"},
            "result": f"extremecraft:{output_item}",
            "experience": xp,
            "cookingtime": time,
        },
    )


def write_blasting(name: str, input_item: str, output_item: str, xp: float = 0.7, time: int = 100) -> bool:
    return write_json_missing(
        recipe_path(name),
        {
            "type": "minecraft:blasting",
            "ingredient": {"item": f"extremecraft:{input_item}"},
            "result": f"extremecraft:{output_item}",
            "experience": xp,
            "cookingtime": time,
        },
    )


def write_machine_recipe(name: str, machine: str, input_item: str, output_item: str, output_count: int, process: int, ept: int) -> bool:
    return write_json_missing(
        machine_recipe_path(name),
        {
            "type": "extremecraft:machine_processing",
            "machine": machine,
            "input": {"item": f"extremecraft:{input_item}"},
            "output": {"item": f"extremecraft:{output_item}", "count": output_count},
            "process_time": max(20, process),
            "energy_per_tick": max(1, ept),
        },
    )


def write_pulverizing(name: str, input_item: str, output_item: str, output_count: int, process: int, ept: int) -> bool:
    return write_json_missing(
        pulverizing_path(name),
        {
            "type": "extremecraft:pulverizing",
            "input": {"item": f"extremecraft:{input_item}"},
            "output": {"item": f"extremecraft:{output_item}", "count": output_count},
            "process_time": max(40, process),
            "energy_per_tick": max(1, ept),
        },
    )


def parse_existing_recipe_outputs() -> set[str]:
    outputs: set[str] = set()
    for path in DATA.rglob("*.json"):
        if "loot_tables" in path.parts or "worldgen" in path.parts:
            continue
        try:
            obj = json.loads(path.read_text(encoding="utf-8"))
        except Exception:
            continue
        if isinstance(obj, dict):
            result = obj.get("result")
            if isinstance(result, dict) and isinstance(result.get("item"), str):
                outputs.add(result["item"].replace("extremecraft:", ""))
            elif isinstance(result, str) and result.startswith("extremecraft:"):
                outputs.add(result.replace("extremecraft:", ""))
            output = obj.get("output")
            if isinstance(output, dict) and isinstance(output.get("item"), str):
                outputs.add(output["item"].replace("extremecraft:", ""))
    return outputs


def build_runtime_graph() -> dict:
    materials = parse_materials()
    machines = parse_machines()
    cable_ids = parse_cable_ids()

    runtime_items = parse_moditems_ids()
    runtime_blocks = parse_modblock_ids()

    armor_materials = re.findall(
        r'registerArmorSet\("([a-z0-9_]+)"',
        (JAVA / "com" / "extremecraft" / "future" / "registry" / "TechItems.java").read_text(encoding="utf-8"),
    )

    for mat in materials:
        runtime_items.update(
            {
                f"raw_{mat.id}",
                f"{mat.id}_ingot",
                f"{mat.id}_dust",
                f"{mat.id}_nugget",
                f"{mat.id}_ore",
                f"{mat.id}_block",
            }
        )
        runtime_blocks.update({f"{mat.id}_ore", f"{mat.id}_block"})
        if mat.has_tools:
            runtime_items.update(
                {
                    f"{mat.id}_pickaxe",
                    f"{mat.id}_sword",
                    f"{mat.id}_axe",
                    f"{mat.id}_shovel",
                    f"{mat.id}_hammer",
                    f"{mat.id}_hoe",
                }
            )
    for armor in armor_materials:
        runtime_items.update(
            {
                f"{armor}_helmet",
                f"{armor}_chestplate",
                f"{armor}_leggings",
                f"{armor}_boots",
            }
        )
    for machine in machines:
        runtime_items.add(machine.id)
        runtime_blocks.add(machine.id)
    runtime_items.update(cable_ids)
    runtime_blocks.update(cable_ids)

    categories: dict[str, set[str]] = {
        "raw materials": set(),
        "ores": set(),
        "ingots": set(),
        "dusts": set(),
        "nuggets": set(),
        "tools": set(),
        "armor": set(),
        "machines": set(),
        "components": set(),
        "magic items": set(),
        "technology items": set(),
    }

    tool_suffixes = ("_pickaxe", "_sword", "_axe", "_shovel", "_hammer", "_hoe", "_drill", "_staff", "_multi_tool")
    armor_suffixes = ("_helmet", "_chestplate", "_leggings", "_boots")
    magic_terms = ("arcane", "mana", "rune", "void", "spell", "aether")
    tech_terms = ("quantum", "reactor", "generator", "processor", "core", "infinity", "dimensional", "celestial")

    for rid in sorted(runtime_items):
        if rid.startswith("raw_"):
            categories["raw materials"].add(rid)
        elif rid.endswith("_ore"):
            categories["ores"].add(rid)
        elif rid.endswith("_ingot"):
            categories["ingots"].add(rid)
        elif rid.endswith("_dust"):
            categories["dusts"].add(rid)
        elif rid.endswith("_nugget"):
            categories["nuggets"].add(rid)
        elif rid.endswith(tool_suffixes):
            categories["tools"].add(rid)
        elif rid.endswith(armor_suffixes):
            categories["armor"].add(rid)
        elif rid in {m.id for m in machines}:
            categories["machines"].add(rid)
        elif any(t in rid for t in magic_terms):
            categories["magic items"].add(rid)
        elif any(t in rid for t in tech_terms):
            categories["technology items"].add(rid)
        else:
            categories["components"].add(rid)

    return {
        "materials": [m.__dict__ for m in materials],
        "machines": [m.__dict__ for m in machines],
        "runtime_items": sorted(runtime_items),
        "runtime_blocks": sorted(runtime_blocks),
        "categories": {k: sorted(v) for k, v in categories.items()},
    }
