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
    try:
        path.write_text(json_dump(obj), encoding="utf-8")
    except PermissionError:
        alt_name = path.stem.replace("_to_", "_from_") + path.suffix
        alt = path.with_name(alt_name)
        if alt.exists():
            return False
        alt.write_text(json_dump(obj), encoding="utf-8")
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

def generate_content() -> dict[str, int]:
    graph = build_runtime_graph()
    materials = [MaterialDef(**m) for m in graph["materials"]]
    machines = [MachineDef(**m) for m in graph["machines"]]
    runtime_items = set(graph["runtime_items"])
    runtime_blocks = set(graph["runtime_blocks"])
    machine_by_id = {m.id: m for m in machines}
    existing_outputs = parse_existing_recipe_outputs()

    created = {
        "recipes": 0,
        "machine_recipes": 0,
        "pulverizing_recipes": 0,
        "machine_defs": 0,
        "loot_tables": 0,
        "worldgen_files": 0,
        "lang_keys": 0,
        "item_textures": 0,
        "ability_icons": 0,
        "tag_updates": 0,
        "graph": 0,
    }

    REPORT_DIR.mkdir(parents=True, exist_ok=True)
    GRAPH_FILE.write_text(json_dump(graph), encoding="utf-8")
    created["graph"] += 1

    for mat in materials:
        ore = f"{mat.id}_ore"
        raw = f"raw_{mat.id}"
        dust = f"{mat.id}_dust"
        ingot = f"{mat.id}_ingot"
        nugget = f"{mat.id}_nugget"
        block = f"{mat.id}_block"

        if item_id_exists(runtime_items, ingot) and item_id_exists(runtime_items, nugget):
            created["recipes"] += int(write_shapeless(f"{ingot}_to_nuggets", nugget, [f"extremecraft:{ingot}"], 9))
            created["recipes"] += int(
                write_json_missing(
                    recipe_path(f"{nugget}_to_ingot"),
                    {
                        "type": "minecraft:crafting_shaped",
                        "pattern": ["NNN", "NNN", "NNN"],
                        "key": {"N": {"item": f"extremecraft:{nugget}"}},
                        "result": {"item": f"extremecraft:{ingot}", "count": 1},
                    },
                )
            )

        if item_id_exists(runtime_items, ingot) and block in runtime_items:
            created["recipes"] += int(
                write_json_missing(
                    recipe_path(f"{block}_from_ingots"),
                    {
                        "type": "minecraft:crafting_shaped",
                        "pattern": ["III", "III", "III"],
                        "key": {"I": {"item": f"extremecraft:{ingot}"}},
                        "result": {"item": f"extremecraft:{block}", "count": 1},
                    },
                )
            )
            created["recipes"] += int(write_shapeless(f"{block}_to_ingots", ingot, [f"extremecraft:{block}"], 9))

        if item_id_exists(runtime_items, dust) and item_id_exists(runtime_items, ingot):
            created["recipes"] += int(write_smelting(f"{dust}_smelting", dust, ingot))
            created["recipes"] += int(write_blasting(f"{dust}_blasting", dust, ingot))
            if "smelter" in machine_by_id:
                md = machine_by_id["smelter"]
                created["machine_recipes"] += int(
                    write_machine_recipe(
                        f"smelter_{dust}_to_ingot",
                        "smelter",
                        dust,
                        ingot,
                        1,
                        md.process_time,
                        md.energy_per_tick,
                    )
                )

        if item_id_exists(runtime_items, raw) and item_id_exists(runtime_items, dust):
            created["pulverizing_recipes"] += int(write_pulverizing(f"pulverize_{raw}_to_{dust}", raw, dust, 2, 220, 24))

        if item_id_exists(runtime_items, ore) and item_id_exists(runtime_items, raw) and "crusher" in machine_by_id:
            md = machine_by_id["crusher"]
            created["machine_recipes"] += int(
                write_machine_recipe(
                    f"crusher_{ore}_to_{raw}",
                    "crusher",
                    ore,
                    raw,
                    2,
                    md.process_time,
                    md.energy_per_tick,
                )
            )
        elif item_id_exists(runtime_items, ore) and item_id_exists(runtime_items, dust) and "crusher" in machine_by_id:
            md = machine_by_id["crusher"]
            created["machine_recipes"] += int(
                write_machine_recipe(
                    f"crusher_{ore}_to_{dust}",
                    "crusher",
                    ore,
                    dust,
                    2,
                    md.process_time,
                    md.energy_per_tick,
                )
            )

    for mat in materials:
        ingot = f"{mat.id}_ingot"
        if not item_id_exists(runtime_items, ingot):
            continue
        if item_id_exists(runtime_items, f"{mat.id}_pickaxe"):
            created["recipes"] += int(write_shaped(f"{mat.id}_pickaxe", f"{mat.id}_pickaxe", ["III", " S ", " S "], {"I": f"extremecraft:{ingot}", "S": "minecraft:stick"}))
        if item_id_exists(runtime_items, f"{mat.id}_sword"):
            created["recipes"] += int(write_shaped(f"{mat.id}_sword", f"{mat.id}_sword", [" I ", " I ", " S "], {"I": f"extremecraft:{ingot}", "S": "minecraft:stick"}))
        if item_id_exists(runtime_items, f"{mat.id}_axe"):
            created["recipes"] += int(write_shaped(f"{mat.id}_axe", f"{mat.id}_axe", ["II ", "IS ", " S "], {"I": f"extremecraft:{ingot}", "S": "minecraft:stick"}))
        if item_id_exists(runtime_items, f"{mat.id}_shovel"):
            created["recipes"] += int(write_shaped(f"{mat.id}_shovel", f"{mat.id}_shovel", [" I ", " S ", " S "], {"I": f"extremecraft:{ingot}", "S": "minecraft:stick"}))
        if item_id_exists(runtime_items, f"{mat.id}_hoe"):
            created["recipes"] += int(write_shaped(f"{mat.id}_hoe", f"{mat.id}_hoe", ["II ", " S ", " S "], {"I": f"extremecraft:{ingot}", "S": "minecraft:stick"}))
        if item_id_exists(runtime_items, f"{mat.id}_hammer"):
            created["recipes"] += int(write_shaped(f"{mat.id}_hammer", f"{mat.id}_hammer", ["III", "ISI", " S "], {"I": f"extremecraft:{ingot}", "S": "minecraft:stick"}))

    for armor_set in ("copper", "titanium", "mythril", "draconium", "void", "aether", "celestial"):
        ingot = f"{armor_set}_ingot"
        if armor_set == "void":
            ingot = "void_crystal_ingot"
        elif armor_set == "aether":
            ingot = "aetherium_ingot"
        if not item_id_exists(runtime_items, ingot):
            ingot_item = ec_or_vanilla(runtime_items, ["steel_ingot", "titanium_ingot", "minecraft:iron_ingot"])
        else:
            ingot_item = f"extremecraft:{ingot}"
        if item_id_exists(runtime_items, f"{armor_set}_helmet"):
            created["recipes"] += int(write_shaped(f"{armor_set}_helmet", f"{armor_set}_helmet", ["III", "I I"], {"I": ingot_item}))
        if item_id_exists(runtime_items, f"{armor_set}_chestplate"):
            created["recipes"] += int(write_shaped(f"{armor_set}_chestplate", f"{armor_set}_chestplate", ["I I", "III", "III"], {"I": ingot_item}))
        if item_id_exists(runtime_items, f"{armor_set}_leggings"):
            created["recipes"] += int(write_shaped(f"{armor_set}_leggings", f"{armor_set}_leggings", ["III", "I I", "I I"], {"I": ingot_item}))
        if item_id_exists(runtime_items, f"{armor_set}_boots"):
            created["recipes"] += int(write_shaped(f"{armor_set}_boots", f"{armor_set}_boots", ["I I", "I I"], {"I": ingot_item}))

    explicit_recipes: list[tuple[str, dict]] = [
        ("mana_crystal", {"pattern": [" A ", "AQA", " A "], "key": {"A": "minecraft:amethyst_shard", "Q": "minecraft:quartz"}}),
        ("arcane_dust", {"pattern": [" G ", "RMR", " G "], "key": {"G": "minecraft:glowstone_dust", "R": "minecraft:redstone", "M": "extremecraft:mana_crystal"}}),
        ("spell_book", {"pattern": [" AA", " BM", "BB "], "key": {"A": "extremecraft:arcane_dust", "B": "minecraft:book", "M": "extremecraft:mana_crystal"}}),
        ("ancient_rune", {"pattern": ["SCS", "RMR", "SCS"], "key": {"S": "minecraft:stone", "C": "minecraft:chiseled_stone_bricks", "R": "extremecraft:arcane_dust", "M": "extremecraft:mana_crystal"}}),
        ("void_essence", {"pattern": [" O ", "ECE", " O "], "key": {"O": "minecraft:obsidian", "C": "minecraft:crying_obsidian", "E": "minecraft:ender_pearl"}}),
        ("quantum_processor", {"pattern": ["RGR", "DTD", "RGR"], "key": {"R": "minecraft:redstone", "G": "minecraft:gold_ingot", "D": "minecraft:diamond", "T": "extremecraft:titanium_ingot"}}),
        ("rune_core", {"pattern": ["ARA", "RMR", "ARA"], "key": {"A": "extremecraft:arcane_dust", "R": "extremecraft:ancient_rune", "M": "extremecraft:mana_crystal"}}),
        ("dimensional_core", {"pattern": ["VEV", "EQE", "VEV"], "key": {"V": "extremecraft:void_essence", "E": "minecraft:ender_eye", "Q": "extremecraft:quantum_processor"}}),
        ("singularity_core", {"pattern": ["VDV", "DND", "VDV"], "key": {"V": "extremecraft:void_essence", "D": "minecraft:diamond_block", "N": "minecraft:nether_star"}}),
        ("celestial_engine", {"pattern": ["AEA", "SCS", "AEA"], "key": {"A": "extremecraft:aetherium_ingot", "E": "extremecraft:dimensional_core", "S": "extremecraft:singularity_core", "C": "extremecraft:quantum_processor"}}),
        ("infinity_ingot", {"pattern": ["ADA", "DSD", "ADA"], "key": {"A": "extremecraft:aetherium_ingot", "D": "extremecraft:draconium_ingot", "S": "extremecraft:singularity_core"}}),
        ("pioneer_chestplate", {"pattern": ["I I", "IQI", "III"], "key": {"I": "extremecraft:steel_ingot", "Q": "extremecraft:quantum_processor"}}),
        ("modular_mining_drill", {"pattern": ["ITI", "RQC", " S "], "key": {"I": "extremecraft:steel_ingot", "T": "extremecraft:titanium_ingot", "R": "minecraft:redstone", "Q": "extremecraft:quantum_processor", "C": "extremecraft:copper_cable", "S": "minecraft:stick"}}),
        ("modular_drill", {"pattern": ["ITI", "DQC", " S "], "key": {"I": "extremecraft:infinity_ingot", "T": "extremecraft:titanium_ingot", "D": "extremecraft:dimensional_core", "Q": "extremecraft:quantum_processor", "C": "extremecraft:superconductive_cable", "S": "minecraft:stick"}}),
        ("graviton_hammer", {"pattern": ["III", "IQI", " S "], "key": {"I": "extremecraft:infinity_ingot", "Q": "extremecraft:quantum_processor", "S": "minecraft:stick"}}),
        ("arcane_staff", {"pattern": ["  R", " S ", "S  "], "key": {"R": "extremecraft:rune_core", "S": "minecraft:blaze_rod"}}),
        ("chrono_pickaxe", {"pattern": ["III", " Q ", " S "], "key": {"I": "extremecraft:draconium_ingot", "Q": "extremecraft:dimensional_core", "S": "minecraft:stick"}}),
        ("quantum_multi_tool", {"pattern": ["PAH", "QD ", " S "], "key": {"P": "extremecraft:quantum_pickaxe", "A": "extremecraft:arcane_staff", "H": "extremecraft:graviton_hammer", "Q": "extremecraft:quantum_processor", "D": "extremecraft:modular_drill", "S": "minecraft:stick"}}),
        ("infinity_sword", {"pattern": [" I ", " I ", " S "], "key": {"I": "extremecraft:infinity_ingot", "S": "extremecraft:void_essence"}}),
        ("quantum_pickaxe", {"pattern": ["III", " Q ", " S "], "key": {"I": "extremecraft:infinity_ingot", "Q": "extremecraft:quantum_processor", "S": "minecraft:stick"}}),
    ]
    for rid, r in explicit_recipes:
        if rid not in runtime_items:
            continue
        missing_ingredients = [v for v in r["key"].values() if v.startswith("extremecraft:") and v.replace("extremecraft:", "") not in runtime_items]
        if missing_ingredients:
            continue
        created["recipes"] += int(write_shaped(f"{rid}_crafted", rid, r["pattern"], r["key"]))

    stage_metals = {
        "primitive": ["minecraft:iron_ingot"],
        "industrial": ["steel_ingot", "bronze_ingot", "minecraft:iron_ingot"],
        "automation": ["steel_ingot", "bronze_ingot", "minecraft:gold_ingot"],
        "energy": ["steel_ingot", "titanium_ingot", "minecraft:gold_ingot"],
        "advanced": ["titanium_ingot", "mythril_ingot", "draconium_ingot"],
        "endgame": ["draconium_ingot", "infinity_ingot", "minecraft:netherite_ingot"],
    }
    for machine in machines:
        if machine.id in existing_outputs:
            continue
        metal = ec_or_vanilla(runtime_items, stage_metals.get(machine.stage, ["minecraft:iron_ingot"]))
        circuit = ec_or_vanilla(runtime_items, ["quantum_processor", "minecraft:redstone", "minecraft:comparator"])
        core = ec_or_vanilla(runtime_items, ["dimensional_core", "rune_core", "minecraft:furnace"])
        binder = ec_or_vanilla(runtime_items, ["copper_cable", "gold_cable", "minecraft:redstone"])
        created["recipes"] += int(write_shaped(f"{machine.id}_machine", machine.id, ["ABA", "CDC", "AEA"], {"A": metal, "B": circuit, "C": binder, "D": core, "E": "minecraft:furnace"}))

    cable_recipes = {
        "copper_cable": ("extremecraft:copper_ingot", "minecraft:redstone"),
        "gold_cable": ("minecraft:gold_ingot", "minecraft:redstone"),
        "superconductive_cable": ("extremecraft:titanium_ingot", "extremecraft:quantum_processor"),
    }
    for cable, (a, b) in cable_recipes.items():
        if cable in runtime_items:
            created["recipes"] += int(write_shaped(f"{cable}_line", cable, ["ABA"], {"A": a, "B": b}, count=3))

    tier_by_stage = {
        "primitive": "pioneer",
        "industrial": "industrial",
        "automation": "automation",
        "energy": "energy",
        "advanced": "advanced",
        "endgame": "endgame",
    }
    for machine in machines:
        machine_def_file = MACHINE_DEFS / f"{machine.id}.json"
        created["machine_defs"] += int(write_json_missing(machine_def_file, {
            "id": machine.id,
            "display_name": humanize_id(machine.id),
            "tier": tier_by_stage.get(machine.stage, machine.stage),
            "energy_per_tick": machine.energy_per_tick,
            "processing": {
                "speed": 1.0 if machine.process_time <= 0 else round(max(0.4, 160.0 / max(1, machine.process_time)), 2),
                "parallel_operations": max(1, machine.output_multiplier),
            },
            "recipes": "extremecraft:machine_processing",
        }))
        if machine.generation_per_tick <= 0:
            baseline_input = "copper_ore" if "crusher" in machine.id or "pulverizer" in machine.id else "copper_dust"
            baseline_output = "raw_copper" if "crusher" in machine.id else "copper_ingot"
            if "pulverizer" in machine.id:
                baseline_input = "raw_copper"
                baseline_output = "copper_dust"
            if baseline_input in runtime_items and baseline_output in runtime_items:
                created["machine_recipes"] += int(write_machine_recipe(f"{machine.id}_baseline", machine.id, baseline_input, baseline_output, 1 if baseline_output.endswith("_ingot") else 2, machine.process_time if machine.process_time > 0 else 120, machine.energy_per_tick if machine.energy_per_tick > 0 else 20))

    for block in sorted(runtime_blocks):
        loot_file = LOOT_BLOCKS / f"{block}.json"
        if loot_file.exists():
            continue
        if block.endswith("_ore"):
            material = block[: -len("_ore")]
            raw_item = f"raw_{material}"
            default_drop = raw_item if raw_item in runtime_items else block
            loot = {
                "type": "minecraft:block",
                "pools": [{
                    "rolls": 1,
                    "entries": [{"type": "minecraft:alternatives", "children": [
                        {"type": "minecraft:item", "name": f"extremecraft:{block}", "conditions": [{"condition": "minecraft:match_tool", "predicate": {"enchantments": [{"enchantment": "minecraft:silk_touch", "levels": {"min": 1}}]}}]},
                        {"type": "minecraft:item", "name": f"extremecraft:{default_drop}", "functions": [{"function": "minecraft:apply_bonus", "enchantment": "minecraft:fortune", "formula": "minecraft:ore_drops"}, {"function": "minecraft:explosion_decay"}]}
                    ]}],
                    "conditions": [{"condition": "minecraft:survives_explosion"}],
                }],
            }
            created["loot_tables"] += int(write_json_missing(loot_file, loot))
        else:
            created["loot_tables"] += int(write_json_missing(loot_file, {
                "type": "minecraft:block",
                "pools": [{
                    "rolls": 1,
                    "entries": [{"type": "minecraft:item", "name": f"extremecraft:{block}"}],
                    "conditions": [{"condition": "minecraft:survives_explosion"}],
                }],
            }))

    pickaxe_tag = RES / "data" / "minecraft" / "tags" / "blocks" / "mineable" / "pickaxe.json"
    needs_stone_tag = RES / "data" / "minecraft" / "tags" / "blocks" / "needs_stone_tool.json"
    for tag_path in (pickaxe_tag, needs_stone_tag):
        data = json.loads(tag_path.read_text(encoding="utf-8"))
        values = set(data.get("values", []))
        before = len(values)
        for block in runtime_blocks:
            values.add(f"extremecraft:{block}")
        data["values"] = sorted(values)
        if len(values) != before:
            tag_path.write_text(json_dump(data), encoding="utf-8")
            created["tag_updates"] += 1

    for mat in materials:
        feature_id = mat.id if mat.id.endswith("_ore") else f"{mat.id}_ore"
        ore_block = f"{mat.id}_ore"
        cfg_file = WORLDGEN_CFG / f"{feature_id}.json"
        placed_file = WORLDGEN_PLACED / f"{feature_id}.json"
        biome_file = BIOME_MOD / f"add_{feature_id}.json"

        if not cfg_file.exists():
            if any("the_nether" in b for b in mat.biomes):
                targets = [{"target": {"predicate_type": "minecraft:tag_match", "tag": "minecraft:base_stone_nether"}, "state": {"Name": f"extremecraft:{ore_block}"}}]
            elif any("the_end" in b for b in mat.biomes):
                targets = [{"target": {"predicate_type": "minecraft:block_match", "block": "minecraft:end_stone"}, "state": {"Name": f"extremecraft:{ore_block}"}}]
            else:
                targets = [
                    {"target": {"predicate_type": "minecraft:tag_match", "tag": "minecraft:stone_ore_replaceables"}, "state": {"Name": f"extremecraft:{ore_block}"}},
                    {"target": {"predicate_type": "minecraft:tag_match", "tag": "minecraft:deepslate_ore_replaceables"}, "state": {"Name": f"extremecraft:{ore_block}"}},
                ]
            created["worldgen_files"] += int(write_json_missing(cfg_file, {"type": "minecraft:ore", "config": {"size": mat.vein_size, "discard_chance_on_air_exposure": 0, "targets": targets}}))
        if not placed_file.exists():
            created["worldgen_files"] += int(write_json_missing(placed_file, {
                "feature": f"extremecraft:{feature_id}",
                "placement": [
                    {"count": mat.vein_count, "type": "minecraft:count"},
                    {"type": "minecraft:in_square"},
                    {"type": "minecraft:height_range", "height": {"type": "minecraft:uniform", "min_inclusive": {"absolute": mat.min_y}, "max_inclusive": {"absolute": mat.max_y}}},
                    {"type": "minecraft:biome"},
                ],
            }))
        if not biome_file.exists():
            biome_tag = "#minecraft:is_overworld"
            if any("the_nether" in b for b in mat.biomes):
                biome_tag = "#minecraft:is_nether"
            elif any("the_end" in b for b in mat.biomes):
                biome_tag = "#minecraft:is_end"
            created["worldgen_files"] += int(write_json_missing(biome_file, {"type": "forge:add_features", "biomes": biome_tag, "features": [f"extremecraft:{feature_id}"], "step": "underground_ores"}))

    lang = json.loads(LANG_FILE.read_text(encoding="utf-8"))
    keys_before = len(lang)
    for item in sorted(runtime_items):
        key = f"item.extremecraft.{item}"
        if key not in lang:
            lang[key] = humanize_id(item)
    for block in sorted(runtime_blocks):
        key = f"block.extremecraft.{block}"
        if key not in lang:
            lang[key] = humanize_id(block)
    if len(lang) != keys_before:
        LANG_FILE.write_text(json_dump(lang), encoding="utf-8")
    created["lang_keys"] += len(lang) - keys_before

    item_colors = {
        "magic items": (122, 92, 220, 255),
        "technology items": (63, 149, 203, 255),
        "tools": (120, 120, 120, 255),
        "armor": (144, 144, 144, 255),
        "components": (97, 164, 120, 255),
    }
    category_lookup = {}
    for cname, ids in graph["categories"].items():
        for i in ids:
            category_lookup[i] = cname

    for item in sorted(runtime_items):
        model_path = ASSETS / "models" / "item" / f"{item}.json"
        tex_path = ITEM_TEXTURES / f"{item}.png"
        if not model_path.exists() or tex_path.exists():
            continue
        color = item_colors.get(category_lookup.get(item, "components"), (128, 128, 128, 255))
        write_placeholder_png(tex_path, color)
        created["item_textures"] += 1

    if ABILITY_DATA.exists():
        for ability in sorted(p.stem for p in ABILITY_DATA.glob("*.json")):
            icon = ABILITY_TEXTURES / f"{ability}.png"
            if icon.exists():
                continue
            write_placeholder_png(icon, (80, 130, 240, 255))
            created["ability_icons"] += 1

    return created


def write_placeholder_png(path: Path, rgba: tuple[int, int, int, int], size: int = 16) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    r, g, b, a = rgba
    pixels = []
    for y in range(size):
        row = bytearray([0])
        for x in range(size):
            border = x in (0, size - 1) or y in (0, size - 1)
            diag = x == y or x + y == size - 1
            if border:
                row.extend((20, 20, 20, 255))
            elif diag:
                row.extend((min(255, r + 40), min(255, g + 40), min(255, b + 40), a))
            else:
                row.extend((r, g, b, a))
        pixels.append(bytes(row))
    raw = b"".join(pixels)
    compressed = zlib.compress(raw, level=9)

    def chunk(tag: bytes, data: bytes) -> bytes:
        return struct.pack(">I", len(data)) + tag + data + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF)

    ihdr = struct.pack(">IIBBBBB", size, size, 8, 6, 0, 0, 0)
    png = b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", ihdr) + chunk(b"IDAT", compressed) + chunk(b"IEND", b"")
    path.write_bytes(png)


def validate_content() -> tuple[bool, list[str]]:
    graph = build_runtime_graph()
    runtime_items = set(graph["runtime_items"])
    runtime_blocks = set(graph["runtime_blocks"])
    lang = json.loads(LANG_FILE.read_text(encoding="utf-8"))
    errors: list[str] = []

    for item in sorted(runtime_items):
        if f"item.extremecraft.{item}" not in lang:
            errors.append(f"Missing lang key for item: {item}")
    for block in sorted(runtime_blocks):
        if f"block.extremecraft.{block}" not in lang:
            errors.append(f"Missing lang key for block: {block}")

    for block in sorted(runtime_blocks):
        if not (LOOT_BLOCKS / f"{block}.json").exists():
            errors.append(f"Missing loot table: {block}")

    for item in sorted(runtime_items):
        model = ASSETS / "models" / "item" / f"{item}.json"
        tex = ITEM_TEXTURES / f"{item}.png"
        if model.exists() and not tex.exists():
            errors.append(f"Missing item texture: {item}")

    outputs = parse_existing_recipe_outputs()
    for item in sorted(runtime_items):
        if item in outputs:
            continue
        if item.endswith("_ore") and (LOOT_BLOCKS / f"{item}.json").exists():
            continue
        if item.endswith("_block") and (LOOT_BLOCKS / f"{item}.json").exists():
            continue
        if item in runtime_blocks and (LOOT_BLOCKS / f"{item}.json").exists():
            continue
        errors.append(f"No acquisition path detected: {item}")

    return (len(errors) == 0, errors)


def main() -> int:
    parser = argparse.ArgumentParser(description="ExtremeCraft content completion and validation")
    parser.add_argument("command", choices=("generate", "validate", "graph"))
    args = parser.parse_args()

    if args.command == "graph":
        REPORT_DIR.mkdir(parents=True, exist_ok=True)
        GRAPH_FILE.write_text(json_dump(build_runtime_graph()), encoding="utf-8")
        print(f"Wrote runtime graph: {GRAPH_FILE}")
        return 0

    if args.command == "generate":
        created = generate_content()
        print(json.dumps(created, indent=2))
        return 0

    ok, errors = validate_content()
    if ok:
        print("Content validation passed.")
        return 0
    print("Content validation failed:")
    for e in errors:
        print(f" - {e}")
    return 1


if __name__ == "__main__":
    raise SystemExit(main())
