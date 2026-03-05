#!/usr/bin/env python3
"""Deterministic Extreme Craft asset generator.

This script discovers IDs from registry Java sources and generates missing
assets while preserving existing hand-made textures and valid JSON files.
"""

from __future__ import annotations

import hashlib
import json
import re
from dataclasses import dataclass
from pathlib import Path

from PIL import Image, ImageDraw

MODID = "extremecraft"
HASH_SEED = "extremecraft-assets-v2"

REPO_ROOT = Path(__file__).resolve().parents[1]
JAVA_SRC = REPO_ROOT / "src" / "main" / "java"
ASSETS_ROOT = REPO_ROOT / "src" / "main" / "resources" / "assets" / MODID

BLOCKSTATE_DIR = ASSETS_ROOT / "blockstates"
BLOCK_MODEL_DIR = ASSETS_ROOT / "models" / "block"
ITEM_MODEL_DIR = ASSETS_ROOT / "models" / "item"
ITEM_TEXTURE_DIR = ASSETS_ROOT / "textures" / "item"
BLOCK_TEXTURE_DIR = ASSETS_ROOT / "textures" / "block"

BLOCK_SIZE = 32
ITEM_SIZE = 32

REGISTER_LITERAL_RE = re.compile(r'\.register\("([a-z0-9_]+)"\s*,')
REGISTER_HELPER_RE = re.compile(r'\bregister\("([a-z0-9_]+)"\)')
BLOCK_ITEM_LITERAL_RE = re.compile(
    r'\.register\("([a-z0-9_]+)"\s*,\s*\(\)\s*->\s*new\s+BlockItem\(',
    re.DOTALL,
)
MATERIAL_RE = re.compile(
    r'new\s+OreMaterialDefinition\("([a-z0-9_]+)"\s*,\s*[^,]+\s*,\s*(true|false)\s*,\s*(true|false)',
    re.IGNORECASE,
)
MACHINE_RE = re.compile(r'new\s+MachineDefinition\("([a-z0-9_]+)"')
CABLE_RE = re.compile(r'[A-Z_]+\("([a-z0-9_]+)"\s*,')
ARMOR_SET_RE = re.compile(r'registerArmorSet\("([a-z0-9_]+)"')


@dataclass
class AssetStats:
    generated: int = 0
    repaired: int = 0


@dataclass
class Discovery:
    materials: list[tuple[str, bool]]
    material_ids: list[str]
    machine_ids: list[str]
    cable_ids: list[str]
    item_ids: list[str]
    block_ids: list[str]


def ensure_dirs() -> None:
    for directory in (
        BLOCKSTATE_DIR,
        BLOCK_MODEL_DIR,
        ITEM_MODEL_DIR,
        ITEM_TEXTURE_DIR,
        BLOCK_TEXTURE_DIR,
    ):
        directory.mkdir(parents=True, exist_ok=True)


def read_text(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def stable_int(key: str, lo: int, hi: int) -> int:
    digest = hashlib.sha256(f"{HASH_SEED}:{key}".encode("utf-8")).digest()
    span = hi - lo + 1
    return lo + (int.from_bytes(digest[:4], "big") % span)


def color_for(name: str) -> tuple[int, int, int]:
    digest = hashlib.sha256(f"{HASH_SEED}:color:{name}".encode("utf-8")).hexdigest()
    return (
        70 + int(digest[0:2], 16) % 150,
        70 + int(digest[2:4], 16) % 150,
        70 + int(digest[4:6], 16) % 150,
    )


def write_json(path: Path, data: dict) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8")


def read_json(path: Path) -> tuple[dict | None, bool]:
    try:
        return json.loads(path.read_text(encoding="utf-8")), True
    except Exception:
        return None, False


def parse_register_ids(path: Path) -> set[str]:
    text = read_text(path)
    ids = set(REGISTER_LITERAL_RE.findall(text))
    ids.update(REGISTER_HELPER_RE.findall(text))
    return ids


def parse_block_item_ids(path: Path) -> set[str]:
    return set(BLOCK_ITEM_LITERAL_RE.findall(read_text(path)))


def parse_materials(path: Path) -> list[tuple[str, bool]]:
    out: list[tuple[str, bool]] = []
    for match in MATERIAL_RE.finditer(read_text(path)):
        out.append((match.group(1), match.group(2).lower() == "true"))
    # Preserve source ordering while deduplicating.
    seen: set[str] = set()
    deduped: list[tuple[str, bool]] = []
    for material, has_tools in out:
        if material in seen:
            continue
        seen.add(material)
        deduped.append((material, has_tools))
    return deduped


def load_ids() -> Discovery:
    tech_items = JAVA_SRC / "com" / "extremecraft" / "future" / "registry" / "TechItems.java"
    tech_blocks = JAVA_SRC / "com" / "extremecraft" / "future" / "registry" / "TechBlocks.java"
    mod_items = JAVA_SRC / "com" / "extremecraft" / "registry" / "ModItems.java"
    ec_items = JAVA_SRC / "com" / "extremecraft" / "registry" / "ECItems.java"
    machine_catalog = JAVA_SRC / "com" / "extremecraft" / "machine" / "core" / "MachineCatalog.java"
    ore_catalog = JAVA_SRC / "com" / "extremecraft" / "machine" / "material" / "OreMaterialCatalog.java"
    cable_tier = JAVA_SRC / "com" / "extremecraft" / "machine" / "cable" / "CableTier.java"

    materials = parse_materials(ore_catalog)
    material_ids = {material for material, _ in materials}

    machine_ids = set(MACHINE_RE.findall(read_text(machine_catalog)))
    cable_ids = set(CABLE_RE.findall(read_text(cable_tier)))

    item_ids = set()
    for source in (tech_items, mod_items, ec_items):
        item_ids.update(parse_register_ids(source))

    block_ids = set(parse_register_ids(tech_blocks))
    block_ids.update(parse_block_item_ids(tech_items))
    block_ids.update(parse_block_item_ids(mod_items))

    armor_sets = set(ARMOR_SET_RE.findall(read_text(tech_items)))

    for material, has_tools in materials:
        item_ids.update(
            {
                f"raw_{material}",
                f"{material}_ingot",
                f"{material}_dust",
                f"{material}_nugget",
                f"{material}_ore",
                f"{material}_block",
            }
        )
        block_ids.update({f"{material}_ore", f"{material}_block"})
        if has_tools:
            item_ids.update(
                {
                    f"{material}_pickaxe",
                    f"{material}_sword",
                    f"{material}_axe",
                    f"{material}_shovel",
                    f"{material}_hammer",
                    f"{material}_hoe",
                }
            )

    for armor in armor_sets:
        item_ids.update(
            {
                f"{armor}_helmet",
                f"{armor}_chestplate",
                f"{armor}_leggings",
                f"{armor}_boots",
            }
        )

    item_ids.update(machine_ids)
    item_ids.update(cable_ids)
    block_ids.update(machine_ids)
    block_ids.update(cable_ids)

    return Discovery(
        materials=sorted(materials, key=lambda entry: entry[0]),
        material_ids=sorted(material_ids),
        machine_ids=sorted(machine_ids),
        cable_ids=sorted(cable_ids),
        item_ids=sorted(item_ids),
        block_ids=sorted(block_ids),
    )


def save_png(path: Path, image: Image.Image, stats: AssetStats) -> None:
    if path.exists():
        return
    path.parent.mkdir(parents=True, exist_ok=True)
    image.save(path, format="PNG")
    stats.generated += 1


def gen_block_texture(name: str) -> Image.Image:
    base = color_for(name)
    image = Image.new("RGBA", (BLOCK_SIZE, BLOCK_SIZE), (*base, 255))
    draw = ImageDraw.Draw(image)
    dark = tuple(max(0, channel - 35) for channel in base)
    light = tuple(min(255, channel + 22) for channel in base)

    draw.rectangle((0, 0, BLOCK_SIZE - 1, BLOCK_SIZE - 1), outline=(*dark, 255))
    draw.line((1, 1, BLOCK_SIZE - 2, 1), fill=(*light, 255))
    draw.line((1, 1, 1, BLOCK_SIZE - 2), fill=(*light, 255))
    for step in (8, 16, 24):
        draw.line((step, 0, step, BLOCK_SIZE - 1), fill=(*dark, 120))
        draw.line((0, step, BLOCK_SIZE - 1, step), fill=(*dark, 120))

    return image


def gen_item_texture(name: str) -> Image.Image:
    base = color_for(name)
    image = Image.new("RGBA", (ITEM_SIZE, ITEM_SIZE), (0, 0, 0, 0))
    draw = ImageDraw.Draw(image)

    if name.endswith("_ingot"):
        draw.rounded_rectangle(
            (4, 11, 27, 21),
            radius=3,
            fill=(*base, 255),
            outline=(*tuple(max(0, channel - 45) for channel in base), 255),
        )
    elif name.endswith("_dust"):
        for idx in range(80):
            x = stable_int(f"{name}:dust:x:{idx}", 3, ITEM_SIZE - 4)
            y = stable_int(f"{name}:dust:y:{idx}", 3, ITEM_SIZE - 4)
            jitter = (
                max(0, min(255, base[0] + stable_int(f"{name}:r:{idx}", -20, 20))),
                max(0, min(255, base[1] + stable_int(f"{name}:g:{idx}", -20, 20))),
                max(0, min(255, base[2] + stable_int(f"{name}:b:{idx}", -20, 20))),
            )
            draw.rectangle((x, y, x + 1, y + 1), fill=(*jitter, 255))
    else:
        points: list[tuple[int, int]] = []
        center_x = ITEM_SIZE // 2
        center_y = ITEM_SIZE // 2
        for idx in range(9):
            points.append(
                (
                    center_x + stable_int(f"{name}:px:{idx}", -9, 9),
                    center_y + stable_int(f"{name}:py:{idx}", -9, 9),
                )
            )
        draw.polygon(points, fill=(*base, 255))
        draw.polygon(points, outline=(*tuple(max(0, channel - 45) for channel in base), 255))

    return image


def expected_item_model(item_id: str, block_ids: set[str]) -> dict:
    if item_id in block_ids:
        return {"parent": f"{MODID}:block/{item_id}"}
    return {
        "parent": "item/generated",
        "textures": {"layer0": f"{MODID}:item/{item_id}"},
    }


def ensure_json_asset(path: Path, desired: dict, stats: AssetStats, allowed_parent: str | None = None) -> None:
    if path.exists():
        data, ok = read_json(path)
        if not ok:
            write_json(path, desired)
            stats.repaired += 1
            return

        if allowed_parent is not None and data.get("parent") != allowed_parent:
            write_json(path, desired)
            stats.repaired += 1
            return

        if data == desired:
            return

        # Keep custom valid JSON unless it has a known-incorrect parent.
        return

    write_json(path, desired)
    stats.generated += 1


def ensure_item_model(item_id: str, block_ids: set[str], stats: AssetStats) -> None:
    path = ITEM_MODEL_DIR / f"{item_id}.json"
    desired = expected_item_model(item_id, block_ids)
    allowed_parent = desired["parent"]

    if path.exists():
        data, ok = read_json(path)
        if not ok:
            write_json(path, desired)
            stats.repaired += 1
            return

        if data.get("parent") != allowed_parent:
            write_json(path, desired)
            stats.repaired += 1
            return
        return

    write_json(path, desired)
    stats.generated += 1


def ensure_generic_item_texture(item_id: str, block_ids: set[str], stats: AssetStats) -> None:
    if item_id in block_ids:
        return
    path = ITEM_TEXTURE_DIR / f"{item_id}.png"
    if path.exists():
        return
    save_png(path, gen_item_texture(item_id), stats)


def ensure_block_assets(block_id: str, stats: AssetStats) -> None:
    block_texture = BLOCK_TEXTURE_DIR / f"{block_id}.png"
    if not block_texture.exists():
        save_png(block_texture, gen_block_texture(block_id), stats)

    block_model = BLOCK_MODEL_DIR / f"{block_id}.json"
    desired_model = {
        "parent": "block/cube_all",
        "textures": {"all": f"{MODID}:block/{block_id}"},
    }
    ensure_json_asset(block_model, desired_model, stats, allowed_parent="block/cube_all")

    blockstate = BLOCKSTATE_DIR / f"{block_id}.json"
    desired_blockstate = {
        "variants": {
            "": {
                "model": f"{MODID}:block/{block_id}",
            }
        }
    }

    if blockstate.exists():
        data, ok = read_json(blockstate)
        if not ok:
            write_json(blockstate, desired_blockstate)
            stats.repaired += 1
            return
        return

    write_json(blockstate, desired_blockstate)
    stats.generated += 1


def ensure_material_assets(material: str, block_ids: set[str], stats: AssetStats) -> None:
    for block_id in (f"{material}_ore", f"{material}_block"):
        ensure_block_assets(block_id, stats)
        ensure_item_model(block_id, block_ids, stats)

    for item_id in (
        f"raw_{material}",
        f"{material}_ingot",
        f"{material}_dust",
        f"{material}_nugget",
    ):
        ensure_item_model(item_id, block_ids, stats)
        ensure_generic_item_texture(item_id, block_ids, stats)


def ensure_machine_assets(machine_ids: list[str], block_ids: set[str], stats: AssetStats) -> None:
    for machine_id in machine_ids:
        ensure_block_assets(machine_id, stats)
        ensure_item_model(machine_id, block_ids, stats)


def ensure_cable_assets(cable_ids: list[str], block_ids: set[str], stats: AssetStats) -> None:
    for cable_id in cable_ids:
        ensure_block_assets(cable_id, stats)
        ensure_item_model(cable_id, block_ids, stats)


def validate_assets(discovery: Discovery) -> dict[str, int]:
    block_ids = set(discovery.block_ids)
    item_ids = set(discovery.item_ids)

    missing_models = 0
    missing_textures = 0
    missing_blockstates = 0
    broken_json = 0

    for block_id in sorted(block_ids):
        if not (BLOCK_MODEL_DIR / f"{block_id}.json").exists():
            missing_models += 1
        if not (BLOCK_TEXTURE_DIR / f"{block_id}.png").exists():
            missing_textures += 1
        if not (BLOCKSTATE_DIR / f"{block_id}.json").exists():
            missing_blockstates += 1

    for item_id in sorted(item_ids):
        if not (ITEM_MODEL_DIR / f"{item_id}.json").exists():
            missing_models += 1
        if item_id not in block_ids and not (ITEM_TEXTURE_DIR / f"{item_id}.png").exists():
            missing_textures += 1

    for json_dir in (BLOCK_MODEL_DIR, ITEM_MODEL_DIR, BLOCKSTATE_DIR):
        for json_path in json_dir.glob("*.json"):
            _, ok = read_json(json_path)
            if not ok:
                broken_json += 1

    return {
        "missing_models": missing_models,
        "missing_textures": missing_textures,
        "missing_blockstates": missing_blockstates,
        "broken_json": broken_json,
    }


def main() -> None:
    ensure_dirs()
    discovery = load_ids()
    stats = AssetStats()
    block_ids = set(discovery.block_ids)

    for material in discovery.material_ids:
        ensure_material_assets(material, block_ids, stats)

    ensure_machine_assets(discovery.machine_ids, block_ids, stats)
    ensure_cable_assets(discovery.cable_ids, block_ids, stats)

    for block_id in discovery.block_ids:
        ensure_block_assets(block_id, stats)

    for item_id in discovery.item_ids:
        ensure_item_model(item_id, block_ids, stats)
        ensure_generic_item_texture(item_id, block_ids, stats)

    validation = validate_assets(discovery)

    print("\nAsset Generation Summary")
    print(f"materials: {len(discovery.material_ids)}")
    print(f"machines: {len(discovery.machine_ids)}")
    print(f"cable tiers: {len(discovery.cable_ids)}")
    print(f"items: {len(discovery.item_ids)}")
    print(f"blocks: {len(discovery.block_ids)}")
    print(f"assets generated: {stats.generated}")
    print(f"assets repaired: {stats.repaired}")
    print("validation:")
    print(f"  missing models: {validation['missing_models']}")
    print(f"  missing textures: {validation['missing_textures']}")
    print(f"  missing blockstates: {validation['missing_blockstates']}")
    print(f"  broken json: {validation['broken_json']}")


if __name__ == "__main__":
    main()