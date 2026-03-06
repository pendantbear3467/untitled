#!/usr/bin/env python3
"""Stabilize ExtremeCraft datapack/resource completeness from runtime registries.

This script is intentionally idempotent and additive:
- It preserves existing files/values.
- It only adds missing translations/assets/data files.
- It avoids touching SDK and generator architecture.
"""

from __future__ import annotations

import json
import re
from dataclasses import dataclass
from pathlib import Path
from typing import Iterable

try:
    from PIL import Image, ImageDraw
except ImportError:  # pragma: no cover
    Image = None
    ImageDraw = None

ROOT = Path(__file__).resolve().parents[1]

LANG_PATH = ROOT / "src/main/resources/assets/extremecraft/lang/en_us.json"
ITEM_TEXTURE_DIR = ROOT / "src/main/resources/assets/extremecraft/textures/item"
ABILITY_TEXTURE_DIR = ROOT / "src/main/resources/assets/extremecraft/textures/gui/abilities"

PICKAXE_TAG = ROOT / "src/main/resources/data/minecraft/tags/blocks/mineable/pickaxe.json"
STONE_TOOL_TAG = ROOT / "src/main/resources/data/minecraft/tags/blocks/needs_stone_tool.json"

LOOT_BLOCKS_DIR = ROOT / "src/main/resources/data/extremecraft/loot_tables/blocks"
RECIPES_DIR = ROOT / "src/main/resources/data/extremecraft/recipes"
GENERATED_RECIPES_DIR = RECIPES_DIR / "generated"

WORLDGEN_CONFIGURED_DIR = ROOT / "src/main/resources/data/extremecraft/worldgen/configured_feature"
WORLDGEN_PLACED_DIR = ROOT / "src/main/resources/data/extremecraft/worldgen/placed_feature"
BIOME_MODIFIER_DIR = ROOT / "src/main/resources/data/extremecraft/forge/biome_modifier"


@dataclass(frozen=True)
class OreMaterial:
    material_id: str
    has_tools: bool
    harvest_level: int
    min_y: int
    max_y: int
    vein_size: int
    count: int
    dimensions: tuple[str, ...]


def _read_text(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def _read_json(path: Path, default: object) -> object:
    if not path.exists():
        return default
    with path.open("r", encoding="utf-8") as handle:
        return json.load(handle)


def _write_json(path: Path, payload: object) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8") as handle:
        json.dump(payload, handle, indent=2)
        handle.write("\n")


def _normalize_ore_id(material_id: str) -> str:
    return material_id if material_id.endswith("_ore") else f"{material_id}_ore"


def _read_ore_materials() -> list[OreMaterial]:
    catalog = _read_text(ROOT / "src/main/java/com/extremecraft/machine/material/OreMaterialCatalog.java")
    pattern = re.compile(
        r'new OreMaterialDefinition\("([a-z0-9_]+)",\s*MaterialTier\.[A-Z_]+,\s*'
        r'(true|false),\s*(true|false),\s*(\d+),\s*(-?\d+),\s*(-?\d+),\s*(\d+),\s*(\d+),\s*Set\.of\(([^)]*)\)'
    )
    materials: list[OreMaterial] = []
    for match in pattern.finditer(catalog):
        raw_dimensions = match.group(9)
        dims = tuple(re.findall(r'"([a-z0-9:_]+)"', raw_dimensions))
        materials.append(
            OreMaterial(
                material_id=match.group(1),
                has_tools=match.group(2) == "true",
                harvest_level=int(match.group(4)),
                min_y=int(match.group(5)),
                max_y=int(match.group(6)),
                vein_size=int(match.group(7)),
                count=int(match.group(8)),
                dimensions=dims,
            )
        )
    return materials


def _read_machine_ids() -> list[str]:
    source = _read_text(ROOT / "src/main/java/com/extremecraft/machine/core/MachineCatalog.java")
    return re.findall(r'new MachineDefinition\("([a-z0-9_]+)"', source)


def _read_cable_ids() -> list[str]:
    source = _read_text(ROOT / "src/main/java/com/extremecraft/machine/cable/CableTier.java")
    return re.findall(r'\("([a-z0-9_]+)",\s*\d+\)', source)


def _read_mod_item_ids() -> list[str]:
    source = _read_text(ROOT / "src/main/java/com/extremecraft/registry/ModItems.java")
    return re.findall(r'ITEMS\.register\("([a-z0-9_]+)"', source)


def _read_mod_block_ids() -> list[str]:
    source = _read_text(ROOT / "src/main/java/com/extremecraft/registry/ModBlocks.java")
    return re.findall(r'BLOCKS\.register\("([a-z0-9_]+)"', source)


def _read_explicit_tech_item_ids() -> list[str]:
    source = _read_text(ROOT / "src/main/java/com/extremecraft/future/registry/TechItems.java")
    return re.findall(r'ITEMS\.register\("([a-z0-9_]+)"', source)


def _read_armor_sets() -> list[str]:
    source = _read_text(ROOT / "src/main/java/com/extremecraft/future/registry/TechItems.java")
    return re.findall(r'registerArmorSet\("([a-z0-9_]+)"', source)


def _title_from_id(value: str) -> str:
    return " ".join(token.capitalize() for token in value.split("_") if token)


def _ensure_png(path: Path, label: str) -> bool:
    if path.exists():
        return False
    path.parent.mkdir(parents=True, exist_ok=True)

    if Image is None or ImageDraw is None:
        # Minimal valid 1x1 PNG when Pillow is unavailable.
        path.write_bytes(
            bytes.fromhex(
                "89504E470D0A1A0A"
                "0000000D4948445200000001000000010802000000907753DE"
                "0000000C4944415408D763F8FFFF3F0005FE02FEA7D6059F"
                "0000000049454E44AE426082"
            )
        )
        return True

    image = Image.new("RGBA", (16, 16), (46, 58, 74, 255))
    draw = ImageDraw.Draw(image)
    draw.rectangle((1, 1, 14, 14), outline=(160, 210, 255, 255), width=1)
    draw.text((4, 4), label[:2].upper(), fill=(240, 245, 255, 255))
    image.save(path)
    return True


def _all_recipe_outputs() -> set[str]:
    outputs: set[str] = set()
    for path in RECIPES_DIR.rglob("*.json"):
        payload = _read_json(path, {})
        if not isinstance(payload, dict):
            continue
        result = payload.get("result")
        if isinstance(result, dict):
            item = result.get("item")
            if isinstance(item, str):
                outputs.add(item)
        elif isinstance(result, str):
            outputs.add(result)
    return outputs


def _ensure_generated_recipe(path: Path, payload: dict) -> bool:
    if path.exists():
        return False
    _write_json(path, payload)
    return True


def _shaped(pattern: list[str], key: dict[str, dict[str, str]], item: str, count: int = 1) -> dict:
    return {
        "type": "minecraft:crafting_shaped",
        "pattern": pattern,
        "key": key,
        "result": {"item": item, "count": count},
    }


def _shapeless(ingredients: list[dict[str, str]], item: str, count: int = 1) -> dict:
    return {
        "type": "minecraft:crafting_shapeless",
        "ingredients": ingredients,
        "result": {"item": item, "count": count},
    }


def _smelting(ingredient: str, result: str, xp: float = 0.7, cook_time: int = 200) -> dict:
    return {
        "type": "minecraft:smelting",
        "ingredient": {"item": ingredient},
        "result": result,
        "experience": xp,
        "cookingtime": cook_time,
    }


def stabilize() -> dict[str, int]:
    metrics = {
        "lang_added": 0,
        "textures_created": 0,
        "tags_updated": 0,
        "loot_tables_created": 0,
        "worldgen_created": 0,
        "recipes_created": 0,
    }

    materials = _read_ore_materials()
    machine_ids = _read_machine_ids()
    cable_ids = _read_cable_ids()
    mod_item_ids = _read_mod_item_ids()
    mod_block_ids = _read_mod_block_ids()
    explicit_tech_items = _read_explicit_tech_item_ids()
    armor_sets = _read_armor_sets()

    material_ids = [m.material_id for m in materials]

    item_ids: set[str] = set(mod_item_ids) | set(explicit_tech_items) | set(machine_ids) | set(cable_ids)
    block_ids: set[str] = set(mod_block_ids) | set(machine_ids) | set(cable_ids)

    for material in materials:
        ore_block_id = _normalize_ore_id(material.material_id)
        storage_block_id = f"{material.material_id}_block"
        item_ids.update(
            {
                f"raw_{material.material_id}",
                f"{material.material_id}_ingot",
                f"{material.material_id}_dust",
                f"{material.material_id}_nugget",
                ore_block_id,
                storage_block_id,
            }
        )
        block_ids.update({ore_block_id, storage_block_id})

        if material.has_tools:
            item_ids.update(
                {
                    f"{material.material_id}_pickaxe",
                    f"{material.material_id}_sword",
                    f"{material.material_id}_axe",
                    f"{material.material_id}_shovel",
                    f"{material.material_id}_hammer",
                    f"{material.material_id}_hoe",
                }
            )

    for set_id in armor_sets:
        item_ids.update({f"{set_id}_helmet", f"{set_id}_chestplate", f"{set_id}_leggings", f"{set_id}_boots"})

    # Localization coverage.
    lang = _read_json(LANG_PATH, {})
    if not isinstance(lang, dict):
        lang = {}

    for item_id in sorted(item_ids):
        key = f"item.extremecraft.{item_id}"
        if key not in lang:
            lang[key] = _title_from_id(item_id)
            metrics["lang_added"] += 1

    for block_id in sorted(block_ids):
        key = f"block.extremecraft.{block_id}"
        if key not in lang:
            lang[key] = _title_from_id(block_id)
            metrics["lang_added"] += 1

    _write_json(LANG_PATH, dict(sorted(lang.items(), key=lambda entry: entry[0])))

    # Missing textures and fallback icon.
    for missing_item in (
        "arcane_staff",
        "chrono_pickaxe",
        "graviton_hammer",
        "modular_mining_drill",
        "pioneer_chestplate",
        "quantum_multi_tool",
        "spell_book",
    ):
        if _ensure_png(ITEM_TEXTURE_DIR / f"{missing_item}.png", missing_item):
            metrics["textures_created"] += 1

    if _ensure_png(ABILITY_TEXTURE_DIR / "ability_default.png", "df"):
        metrics["textures_created"] += 1

    # Mining tags.
    pickaxe_tag = _read_json(PICKAXE_TAG, {"replace": False, "values": []})
    stone_tag = _read_json(STONE_TOOL_TAG, {"replace": False, "values": []})
    if not isinstance(pickaxe_tag, dict):
        pickaxe_tag = {"replace": False, "values": []}
    if not isinstance(stone_tag, dict):
        stone_tag = {"replace": False, "values": []}

    pickaxe_values = set(pickaxe_tag.get("values", []))
    stone_values = set(stone_tag.get("values", []))

    for block_id in sorted(block_ids):
        pickaxe_values.add(f"extremecraft:{block_id}")

    harvest_by_material = {m.material_id: m.harvest_level for m in materials}
    for block_id in sorted(block_ids):
        include_stone = False
        if block_id in machine_ids or block_id in cable_ids:
            include_stone = True
        elif block_id.endswith("_block"):
            material_id = block_id[:-6]
            include_stone = harvest_by_material.get(material_id, 1) >= 1
        else:
            base = block_id[:-4] if block_id.endswith("_ore") else block_id
            include_stone = harvest_by_material.get(base, 1) >= 1
        if include_stone:
            stone_values.add(f"extremecraft:{block_id}")

    pickaxe_tag["replace"] = False
    pickaxe_tag["values"] = sorted(pickaxe_values)
    stone_tag["replace"] = False
    stone_tag["values"] = sorted(stone_values)
    _write_json(PICKAXE_TAG, pickaxe_tag)
    _write_json(STONE_TOOL_TAG, stone_tag)
    metrics["tags_updated"] += 2

    # Loot tables (self-drop by default; ores drop raw material when available).
    known_item_ids = set(item_ids)
    for block_id in sorted(block_ids):
        table_path = LOOT_BLOCKS_DIR / f"{block_id}.json"
        if table_path.exists():
            continue

        item_name = f"extremecraft:{block_id}"
        if block_id.endswith("_ore"):
            material_id = block_id[:-4]
            raw_id = f"raw_{material_id}"
            if raw_id in known_item_ids:
                item_name = f"extremecraft:{raw_id}"

        payload = {
            "type": "minecraft:block",
            "pools": [
                {
                    "rolls": 1,
                    "entries": [
                        {
                            "type": "minecraft:item",
                            "name": item_name,
                        }
                    ],
                }
            ],
        }
        _write_json(table_path, payload)
        metrics["loot_tables_created"] += 1

    # Worldgen files from material catalog (fixes gaps and _ore_ore hazards).
    for material in materials:
        ore_id = _normalize_ore_id(material.material_id)
        configured_path = WORLDGEN_CONFIGURED_DIR / f"{ore_id}.json"
        placed_path = WORLDGEN_PLACED_DIR / f"{ore_id}.json"
        modifier_path = BIOME_MODIFIER_DIR / f"add_{ore_id}.json"

        if not configured_path.exists():
            configured = {
                "type": "minecraft:ore",
                "config": {
                    "size": material.vein_size,
                    "discard_chance_on_air_exposure": 0,
                    "targets": [
                        {
                            "target": {
                                "predicate_type": "minecraft:tag_match",
                                "tag": "minecraft:stone_ore_replaceables",
                            },
                            "state": {"Name": f"extremecraft:{ore_id}"},
                        },
                        {
                            "target": {
                                "predicate_type": "minecraft:tag_match",
                                "tag": "minecraft:deepslate_ore_replaceables",
                            },
                            "state": {"Name": f"extremecraft:{ore_id}"},
                        },
                    ],
                },
            }
            _write_json(configured_path, configured)
            metrics["worldgen_created"] += 1

        if not placed_path.exists():
            placed = {
                "feature": f"extremecraft:{ore_id}",
                "placement": [
                    {"count": material.count, "type": "minecraft:count"},
                    {"type": "minecraft:in_square"},
                    {
                        "height": {
                            "min_inclusive": {"absolute": material.min_y},
                            "type": "minecraft:uniform",
                            "max_inclusive": {"absolute": material.max_y},
                        },
                        "type": "minecraft:height_range",
                    },
                    {"type": "minecraft:biome"},
                ],
            }
            _write_json(placed_path, placed)
            metrics["worldgen_created"] += 1

        if not modifier_path.exists():
            if "minecraft:the_nether" in material.dimensions:
                biome_selector = "#minecraft:is_nether"
            elif "minecraft:the_end" in material.dimensions:
                biome_selector = "#minecraft:is_end"
            else:
                biome_selector = "#minecraft:is_overworld"

            modifier = {
                "type": "forge:add_features",
                "biomes": biome_selector,
                "features": [f"extremecraft:{ore_id}"],
                "step": "underground_ores",
            }
            _write_json(modifier_path, modifier)
            metrics["worldgen_created"] += 1

        duplicated = WORLDGEN_CONFIGURED_DIR / f"{ore_id}_ore.json"
        if duplicated.exists() and ore_id.endswith("_ore"):
            duplicated.unlink()

    # Acquisition path recipe generation.
    GENERATED_RECIPES_DIR.mkdir(parents=True, exist_ok=True)
    existing_outputs = _all_recipe_outputs()

    def ensure_recipe(name: str, payload: dict, output: str) -> None:
        path = GENERATED_RECIPES_DIR / f"{name}.json"
        if output in existing_outputs:
            return
        if _ensure_generated_recipe(path, payload):
            existing_outputs.add(output)
            metrics["recipes_created"] += 1

    for material in materials:
        mid = material.material_id
        ingot = f"extremecraft:{mid}_ingot"
        dust = f"extremecraft:{mid}_dust"
        nugget = f"extremecraft:{mid}_nugget"
        raw = f"extremecraft:raw_{mid}"
        storage = f"extremecraft:{mid}_block"

        ensure_recipe(f"{mid}_nugget_from_ingot", _shapeless([{"item": ingot}], nugget, 9), nugget)
        ensure_recipe(
            f"{mid}_ingot_from_nuggets",
            _shaped(["NNN", "NNN", "NNN"], {"N": {"item": nugget}}, ingot, 1),
            ingot,
        )
        ensure_recipe(
            f"{mid}_block_from_ingots",
            _shaped(["III", "III", "III"], {"I": {"item": ingot}}, storage, 1),
            storage,
        )
        ensure_recipe(f"{mid}_ingot_smelting_from_dust", _smelting(dust, ingot), ingot)
        ensure_recipe(f"{mid}_ingot_smelting_from_raw", _smelting(raw, ingot), ingot)

        if material.has_tools:
            ensure_recipe(
                f"{mid}_pickaxe",
                _shaped(["III", " S ", " S "], {"I": {"item": ingot}, "S": {"item": "minecraft:stick"}}, f"extremecraft:{mid}_pickaxe"),
                f"extremecraft:{mid}_pickaxe",
            )
            ensure_recipe(
                f"{mid}_sword",
                _shaped([" I ", " I ", " S "], {"I": {"item": ingot}, "S": {"item": "minecraft:stick"}}, f"extremecraft:{mid}_sword"),
                f"extremecraft:{mid}_sword",
            )
            ensure_recipe(
                f"{mid}_axe",
                _shaped(["II ", "IS ", " S "], {"I": {"item": ingot}, "S": {"item": "minecraft:stick"}}, f"extremecraft:{mid}_axe"),
                f"extremecraft:{mid}_axe",
            )
            ensure_recipe(
                f"{mid}_shovel",
                _shaped([" I ", " S ", " S "], {"I": {"item": ingot}, "S": {"item": "minecraft:stick"}}, f"extremecraft:{mid}_shovel"),
                f"extremecraft:{mid}_shovel",
            )
            ensure_recipe(
                f"{mid}_hoe",
                _shaped(["II ", " S ", " S "], {"I": {"item": ingot}, "S": {"item": "minecraft:stick"}}, f"extremecraft:{mid}_hoe"),
                f"extremecraft:{mid}_hoe",
            )
            ensure_recipe(
                f"{mid}_hammer",
                _shaped(["III", "ISI", " S "], {"I": {"item": ingot}, "S": {"item": "minecraft:stick"}}, f"extremecraft:{mid}_hammer"),
                f"extremecraft:{mid}_hammer",
            )

    for armor_set in armor_sets:
        ingot = f"extremecraft:{armor_set}_ingot"
        fallback = "minecraft:iron_ingot"
        material_item = ingot if ingot in item_ids else fallback
        ensure_recipe(
            f"{armor_set}_helmet",
            _shaped(["III", "I I", "   "], {"I": {"item": material_item}}, f"extremecraft:{armor_set}_helmet"),
            f"extremecraft:{armor_set}_helmet",
        )
        ensure_recipe(
            f"{armor_set}_chestplate",
            _shaped(["I I", "III", "III"], {"I": {"item": material_item}}, f"extremecraft:{armor_set}_chestplate"),
            f"extremecraft:{armor_set}_chestplate",
        )
        ensure_recipe(
            f"{armor_set}_leggings",
            _shaped(["III", "I I", "I I"], {"I": {"item": material_item}}, f"extremecraft:{armor_set}_leggings"),
            f"extremecraft:{armor_set}_leggings",
        )
        ensure_recipe(
            f"{armor_set}_boots",
            _shaped(["I I", "I I", "   "], {"I": {"item": material_item}}, f"extremecraft:{armor_set}_boots"),
            f"extremecraft:{armor_set}_boots",
        )

    previous_machine: str | None = None
    for machine in machine_ids:
        center_item = previous_machine if previous_machine is not None else "minecraft:furnace"
        payload = _shaped(
            ["IRI", "CMC", "IRI"],
            {
                "I": {"item": "minecraft:iron_ingot"},
                "R": {"item": "minecraft:redstone"},
                "C": {"item": "minecraft:copper_ingot"},
                "M": {"item": center_item},
            },
            f"extremecraft:{machine}",
        )
        ensure_recipe(f"machine_{machine}", payload, f"extremecraft:{machine}")
        previous_machine = f"extremecraft:{machine}"

    cable_map = {
        "copper_cable": "minecraft:copper_ingot",
        "gold_cable": "minecraft:gold_ingot",
        "superconductive_cable": "minecraft:diamond",
    }
    for cable_id in cable_ids:
        core_item = cable_map.get(cable_id, "minecraft:copper_ingot")
        ensure_recipe(
            f"{cable_id}",
            _shaped(
                [" R ", "ICI", " R "],
                {
                    "R": {"item": "minecraft:redstone"},
                    "I": {"item": core_item},
                    "C": {"item": "minecraft:copper_ingot"},
                },
                f"extremecraft:{cable_id}",
                3,
            ),
            f"extremecraft:{cable_id}",
        )

    special_recipes: dict[str, dict] = {
        "mana_crystal": _shaped(
            [" A ", "ARA", " A "],
            {"A": {"item": "minecraft:amethyst_shard"}, "R": {"item": "minecraft:redstone"}},
            "extremecraft:mana_crystal",
        ),
        "arcane_dust": _shapeless(
            [{"item": "minecraft:glowstone_dust"}, {"item": "minecraft:redstone"}],
            "extremecraft:arcane_dust",
            2,
        ),
        "spell_book": _shaped(
            [" AD", "AB ", "   "],
            {"A": {"item": "extremecraft:arcane_dust"}, "D": {"item": "minecraft:book"}, "B": {"item": "minecraft:blaze_powder"}},
            "extremecraft:spell_book",
        ),
        "ancient_rune": _shaped(
            ["SCS", "CMC", "SCS"],
            {"S": {"item": "minecraft:stone"}, "C": {"item": "extremecraft:mana_crystal"}, "M": {"item": "minecraft:mossy_cobblestone"}},
            "extremecraft:ancient_rune",
        ),
        "void_essence": _shapeless(
            [{"item": "extremecraft:void_crystal_dust"}, {"item": "minecraft:ender_pearl"}],
            "extremecraft:void_essence",
            1,
        ),
        "quantum_processor": _shaped(
            ["RGR", "GDG", "RGR"],
            {"R": {"item": "minecraft:redstone"}, "G": {"item": "minecraft:gold_ingot"}, "D": {"item": "minecraft:diamond"}},
            "extremecraft:quantum_processor",
        ),
        "rune_core": _shaped(
            ["AMA", "MRM", "AMA"],
            {"A": {"item": "extremecraft:arcane_dust"}, "M": {"item": "extremecraft:mana_crystal"}, "R": {"item": "extremecraft:ancient_rune"}},
            "extremecraft:rune_core",
        ),
        "dimensional_core": _shaped(
            ["EVE", "VQV", "EVE"],
            {"E": {"item": "minecraft:ender_pearl"}, "V": {"item": "extremecraft:void_essence"}, "Q": {"item": "extremecraft:quantum_processor"}},
            "extremecraft:dimensional_core",
        ),
        "infinity_ingot": _shaped(
            ["DSD", "SNS", "DSD"],
            {"D": {"item": "extremecraft:draconium_ingot"}, "S": {"item": "extremecraft:singularity_ore_ingot"}, "N": {"item": "minecraft:netherite_ingot"}},
            "extremecraft:infinity_ingot",
        ),
        "singularity_core": _shaped(
            ["ISI", "SDS", "ISI"],
            {"I": {"item": "extremecraft:infinity_ingot"}, "S": {"item": "extremecraft:singularity_ore_ingot"}, "D": {"item": "extremecraft:dimensional_core"}},
            "extremecraft:singularity_core",
        ),
        "celestial_engine": _shaped(
            ["ASA", "SCS", "ADA"],
            {"A": {"item": "extremecraft:aetherium_ingot"}, "S": {"item": "extremecraft:singularity_core"}, "C": {"item": "extremecraft:celestial_chestplate"}, "D": {"item": "extremecraft:dimensional_core"}},
            "extremecraft:celestial_engine",
        ),
        "infinity_sword": _shaped(
            [" I ", " I ", " S "],
            {"I": {"item": "extremecraft:infinity_ingot"}, "S": {"item": "minecraft:stick"}},
            "extremecraft:infinity_sword",
        ),
        "quantum_pickaxe": _shaped(
            ["III", " S ", " S "],
            {"I": {"item": "extremecraft:infinity_ingot"}, "S": {"item": "minecraft:stick"}},
            "extremecraft:quantum_pickaxe",
        ),
        "modular_drill": _shaped(
            ["IRI", "DCD", " I "],
            {
                "I": {"item": "extremecraft:infinity_ingot"},
                "R": {"item": "extremecraft:rune_core"},
                "D": {"item": "extremecraft:dimensional_core"},
                "C": {"item": "extremecraft:celestial_engine"},
            },
            "extremecraft:modular_drill",
        ),
    }

    for key, payload in special_recipes.items():
        ensure_recipe(f"special_{key}", payload, f"extremecraft:{key}")

    return metrics


def main() -> None:
    metrics = stabilize()
    print("Stabilization complete")
    for key, value in metrics.items():
        print(f"- {key}: {value}")


if __name__ == "__main__":
    main()
