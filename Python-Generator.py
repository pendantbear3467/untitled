#!/usr/bin/env python3
"""
Extreme Craft asset generator.

Generates placeholder textures and missing model/blockstate JSON for all currently
registered content discovered in the Java registry sources.

What it covers:
- Materials from OreMaterialCatalog (ores, blocks, raw/ingot/dust/nugget)
- Machine blocks from MachineCatalog
- Cables from CableTier
- Static and loop-generated items from TechItems/ModItems/ECItems
- Armor/tool/generated item placeholders when models/textures are missing

Output target defaults to:
  src/main/resources/assets/extremecraft

Usage:
  py -3 Python-Generator.py

Dependencies:
  Pillow (pip install pillow)
"""

from __future__ import annotations

import hashlib
import json
import re
import sys
from pathlib import Path
from typing import Dict, Iterable, List, Set, Tuple

try:
    from PIL import Image, ImageDraw
except ImportError as exc:
    print("ERROR: Pillow is required. Install with: pip install pillow")
    raise SystemExit(1) from exc

MODID = "extremecraft"
ROOT = Path(__file__).resolve().parent
JAVA = ROOT / "src" / "main" / "java"
ASSETS = ROOT / "src" / "main" / "resources" / "assets" / MODID

BLOCK_SIZE = 64
ITEM_SIZE = 32

MATERIAL_DEFAULTS: Dict[str, Tuple[int, int, int]] = {
    "copper": (184, 115, 51),
    "tin": (170, 170, 190),
    "lead": (90, 90, 100),
    "silver": (220, 220, 220),
    "nickel": (145, 142, 115),
    "aluminum": (175, 185, 200),
    "titanium": (180, 180, 195),
    "platinum": (225, 225, 230),
    "cobalt": (30, 80, 175),
    "ardite": (230, 120, 35),
    "uranium": (92, 183, 91),
    "iridium": (212, 205, 214),
    "osmium": (101, 116, 176),
    "mythril": (0, 162, 232),
    "void_crystal": (123, 61, 155),
    "draconium": (226, 65, 177),
    "aetherium": (135, 206, 235),
    "singularity_ore": (255, 215, 0),
    "bronze": (180, 120, 70),
    "steel": (125, 130, 140),
    "iron": (185, 185, 185),
}


def read_text(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def ensure_dir(path: Path) -> None:
    path.mkdir(parents=True, exist_ok=True)


def write_json(path: Path, data: dict) -> None:
    ensure_dir(path.parent)
    path.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8")


def stable_rng_int(seed: str, lo: int, hi: int) -> int:
    digest = hashlib.sha256(seed.encode("utf-8")).hexdigest()
    num = int(digest[:8], 16)
    return lo + (num % (hi - lo + 1))


def vary(c: Tuple[int, int, int], delta: int, seed: str) -> Tuple[int, int, int]:
    return tuple(max(0, min(255, c[i] + stable_rng_int(f"{seed}:{i}", -delta, delta))) for i in range(3))


def color_for(name: str) -> Tuple[int, int, int]:
    base = MATERIAL_DEFAULTS.get(name)
    if base:
        return base
    # Deterministic fallback color by name hash.
    h = hashlib.md5(name.encode("utf-8")).hexdigest()
    return (80 + int(h[0:2], 16) % 140, 80 + int(h[2:4], 16) % 140, 80 + int(h[4:6], 16) % 140)


def save_png(img: Image.Image, path: Path) -> None:
    ensure_dir(path.parent)
    img.save(path, format="PNG")


def gen_ore_texture(name: str, col: Tuple[int, int, int]) -> Image.Image:
    img = Image.new("RGB", (BLOCK_SIZE, BLOCK_SIZE))
    for y in range(BLOCK_SIZE):
        for x in range(BLOCK_SIZE):
            g = stable_rng_int(f"stone:{name}:{x}:{y}", 75, 125)
            img.putpixel((x, y), (g, g, g))
    d = ImageDraw.Draw(img)
    for i in range(14):
        r = stable_rng_int(f"{name}:r:{i}", 6, 14)
        x = stable_rng_int(f"{name}:x:{i}", 0, BLOCK_SIZE - r - 1)
        y = stable_rng_int(f"{name}:y:{i}", 0, BLOCK_SIZE - r - 1)
        d.ellipse((x, y, x + r, y + r), fill=vary(col, 18, f"{name}:blob:{i}"))
    return img


def gen_block_texture(name: str, col: Tuple[int, int, int]) -> Image.Image:
    img = Image.new("RGB", (BLOCK_SIZE, BLOCK_SIZE), col)
    d = ImageDraw.Draw(img)
    dark = tuple(max(0, c - 38) for c in col)
    light = tuple(min(255, c + 24) for c in col)
    d.rectangle((0, 0, BLOCK_SIZE - 1, BLOCK_SIZE - 1), outline=dark)
    d.line((1, 1, BLOCK_SIZE - 2, 1), fill=light)
    d.line((1, 1, 1, BLOCK_SIZE - 2), fill=light)
    for step in (16, 32, 48):
        d.line((step, 0, step, BLOCK_SIZE - 1), fill=dark)
        d.line((0, step, BLOCK_SIZE - 1, step), fill=dark)
    return img


def gen_item_blob(name: str, col: Tuple[int, int, int], speck: bool = False) -> Image.Image:
    img = Image.new("RGBA", (ITEM_SIZE, ITEM_SIZE), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    if speck:
        for i in range(70):
            x = stable_rng_int(f"{name}:sx:{i}", 2, ITEM_SIZE - 3)
            y = stable_rng_int(f"{name}:sy:{i}", 2, ITEM_SIZE - 3)
            c = vary(col, 24, f"{name}:sc:{i}")
            d.rectangle((x, y, x + 1, y + 1), fill=(*c, 255))
        return img

    points: List[Tuple[int, int]] = []
    cx, cy = ITEM_SIZE // 2, ITEM_SIZE // 2
    for i in range(8):
        px = cx + stable_rng_int(f"{name}:px:{i}", -9, 9)
        py = cy + stable_rng_int(f"{name}:py:{i}", -9, 9)
        points.append((px, py))
    d.polygon(points, fill=(*col, 255))
    d.polygon(points, outline=(*tuple(max(0, c - 40) for c in col), 255))
    return img


def gen_ingot(name: str, col: Tuple[int, int, int]) -> Image.Image:
    img = Image.new("RGBA", (ITEM_SIZE, ITEM_SIZE), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    x0, y0, x1, y1 = 4, 11, 28, 21
    for y in range(y0, y1 + 1):
        ratio = (y - y0) / max(1, (y1 - y0))
        shade = (int(col[0] * (1.10 - ratio * 0.25)), int(col[1] * (1.10 - ratio * 0.25)), int(col[2] * (1.10 - ratio * 0.25)))
        d.line((x0, y, x1, y), fill=(*shade, 255))
    d.rectangle((x0, y0, x1, y1), outline=(*tuple(max(0, c - 55) for c in col), 255))
    return img


def model_item_generated(tex: str) -> dict:
    return {"parent": "item/generated", "textures": {"layer0": f"{MODID}:item/{tex}"}}


def model_item_block_parent(block_id: str) -> dict:
    return {"parent": f"{MODID}:block/{block_id}"}


def model_block_cube_all(tex: str) -> dict:
    return {"parent": "block/cube_all", "textures": {"all": f"{MODID}:block/{tex}"}}


def blockstate_single(model: str) -> dict:
    return {"variants": {"": {"model": f"{MODID}:block/{model}"}}}


def parse_materials(path: Path) -> List[Tuple[str, bool]]:
    text = read_text(path)
    out: List[Tuple[str, bool]] = []
    pattern = re.compile(
        r'new\s+OreMaterialDefinition\("([a-z0-9_]+)"\s*,\s*[^,]+\s*,\s*(true|false)\s*,\s*(true|false)',
        re.IGNORECASE,
    )
    for m in pattern.finditer(text):
        mat = m.group(1)
        has_tools = m.group(2).lower() == "true"
        out.append((mat, has_tools))
    return out


def parse_register_ids(path: Path) -> Set[str]:
    text = read_text(path)
    return set(re.findall(r'\.register\("([a-z0-9_]+)"\s*,', text))


def parse_machine_ids(path: Path) -> Set[str]:
    text = read_text(path)
    return set(re.findall(r'new\s+MachineDefinition\("([a-z0-9_]+)"', text))


def parse_cable_ids(path: Path) -> Set[str]:
    text = read_text(path)
    return set(re.findall(r'[A-Z_]+\("([a-z0-9_]+)"\s*,', text))


def parse_armor_sets(path: Path) -> Set[str]:
    text = read_text(path)
    return set(re.findall(r'registerArmorSet\("([a-z0-9_]+)"', text))


def existing_model_ids(model_dir: Path) -> Set[str]:
    if not model_dir.exists():
        return set()
    return {p.stem for p in model_dir.glob("*.json")}


def ensure_material_assets(material: str, col: Tuple[int, int, int], textures_item: Path, textures_block: Path) -> None:
    # Block textures
    ore = textures_block / f"{material}_ore.png"
    block = textures_block / f"{material}_block.png"
    if not ore.exists():
        save_png(gen_ore_texture(material, col), ore)
    if not block.exists():
        save_png(gen_block_texture(f"{material}_block", col), block)

    # Item textures
    raw = textures_item / f"raw_{material}.png"
    ingot = textures_item / f"{material}_ingot.png"
    dust = textures_item / f"{material}_dust.png"
    nugget = textures_item / f"{material}_nugget.png"
    if not raw.exists():
        save_png(gen_item_blob(f"raw_{material}", col), raw)
    if not ingot.exists():
        save_png(gen_ingot(f"{material}_ingot", col), ingot)
    if not dust.exists():
        save_png(gen_item_blob(f"{material}_dust", col, speck=True), dust)
    if not nugget.exists():
        save_png(gen_item_blob(f"{material}_nugget", col), nugget)


def ensure_machine_assets(machine_ids: Iterable[str], textures_block: Path, block_models: Path, blockstates: Path, item_models: Path) -> None:
    for machine_id in sorted(set(machine_ids)):
        col = color_for(machine_id.replace("_generator", ""))
        tex = textures_block / f"{machine_id}.png"
        if not tex.exists():
            save_png(gen_block_texture(machine_id, col), tex)

        bm = block_models / f"{machine_id}.json"
        if not bm.exists():
            write_json(bm, model_block_cube_all(machine_id))

        bs = blockstates / f"{machine_id}.json"
        if not bs.exists():
            write_json(bs, blockstate_single(machine_id))

        im = item_models / f"{machine_id}.json"
        if not im.exists():
            write_json(im, model_item_block_parent(machine_id))


def ensure_cable_assets(cables: Iterable[str], textures_block: Path, block_models: Path, blockstates: Path, item_models: Path) -> None:
    palette = {
        "copper_cable": (190, 115, 55),
        "gold_cable": (220, 185, 55),
        "superconductive_cable": (120, 220, 255),
    }
    for cid in sorted(set(cables)):
        col = palette.get(cid, color_for(cid))
        tex = textures_block / f"{cid}.png"
        if not tex.exists():
            save_png(gen_block_texture(cid, col), tex)

        bm = block_models / f"{cid}.json"
        if not bm.exists():
            write_json(bm, model_block_cube_all(cid))

        bs = blockstates / f"{cid}.json"
        if not bs.exists():
            write_json(bs, blockstate_single(cid))

        im = item_models / f"{cid}.json"
        if not im.exists():
            write_json(im, model_item_block_parent(cid))


def ensure_generic_item_texture(item_id: str, textures_item: Path) -> None:
    target = textures_item / f"{item_id}.png"
    if target.exists():
        return

    col = color_for(item_id.replace("_helmet", "").replace("_chestplate", "").replace("_leggings", "").replace("_boots", ""))

    if item_id.endswith("_dust"):
        img = gen_item_blob(item_id, col, speck=True)
    elif item_id.endswith("_ingot"):
        img = gen_ingot(item_id, col)
    elif item_id.startswith("raw_") or item_id.endswith("_nugget"):
        img = gen_item_blob(item_id, col)
    else:
        # Simple emblem-like generated icon.
        img = Image.new("RGBA", (ITEM_SIZE, ITEM_SIZE), (0, 0, 0, 0))
        d = ImageDraw.Draw(img)
        d.rounded_rectangle((4, 4, 28, 28), radius=6, fill=(*col, 255), outline=(*tuple(max(0, c - 40) for c in col), 255))
        d.line((8, 8, 24, 24), fill=(255, 255, 255, 140), width=2)
    save_png(img, target)


def ensure_item_model(item_id: str, item_models: Path, block_ids: Set[str]) -> None:
    path = item_models / f"{item_id}.json"
    desired_is_block_parent = item_id in block_ids

    if path.exists():
        # Repair previously generated wrong block parents for non-block items.
        try:
            data = json.loads(path.read_text(encoding="utf-8"))
            parent = data.get("parent", "")
            has_block_parent = isinstance(parent, str) and parent == f"{MODID}:block/{item_id}"
            if has_block_parent and not desired_is_block_parent:
                write_json(path, model_item_generated(item_id))
            return
        except Exception:
            # If malformed, overwrite below.
            pass

    if desired_is_block_parent:
        write_json(path, model_item_block_parent(item_id))
    else:
        write_json(path, model_item_generated(item_id))


def ensure_material_models(materials: Iterable[str], block_models: Path, blockstates: Path, item_models: Path) -> None:
    for m in sorted(set(materials)):
        ore_id = f"{m}_ore"
        blk_id = f"{m}_block"

        if not (block_models / f"{ore_id}.json").exists():
            write_json(block_models / f"{ore_id}.json", model_block_cube_all(ore_id))
        if not (block_models / f"{blk_id}.json").exists():
            write_json(block_models / f"{blk_id}.json", model_block_cube_all(blk_id))

        if not (blockstates / f"{ore_id}.json").exists():
            write_json(blockstates / f"{ore_id}.json", blockstate_single(ore_id))
        if not (blockstates / f"{blk_id}.json").exists():
            write_json(blockstates / f"{blk_id}.json", blockstate_single(blk_id))

        for iid in (f"raw_{m}", f"{m}_ingot", f"{m}_dust", f"{m}_nugget", ore_id, blk_id):
            if not (item_models / f"{iid}.json").exists():
                if iid.endswith("_ore") or iid.endswith("_block"):
                    write_json(item_models / f"{iid}.json", model_item_block_parent(iid))
                else:
                    write_json(item_models / f"{iid}.json", model_item_generated(iid))


def load_ids() -> Tuple[Set[str], Set[str], List[Tuple[str, bool]], Set[str], Set[str], Set[str], Set[str]]:
    tech_items = JAVA / "com" / "extremecraft" / "future" / "registry" / "TechItems.java"
    tech_blocks = JAVA / "com" / "extremecraft" / "future" / "registry" / "TechBlocks.java"
    mod_items = JAVA / "com" / "extremecraft" / "registry" / "ModItems.java"
    ec_items = JAVA / "com" / "extremecraft" / "registry" / "ECItems.java"
    ore_catalog = JAVA / "com" / "extremecraft" / "machine" / "material" / "OreMaterialCatalog.java"
    machine_catalog = JAVA / "com" / "extremecraft" / "machine" / "core" / "MachineCatalog.java"
    cable_tier = JAVA / "com" / "extremecraft" / "machine" / "cable" / "CableTier.java"

    static_item_ids = parse_register_ids(tech_items) | parse_register_ids(mod_items) | parse_register_ids(ec_items)
    block_ids = parse_register_ids(tech_blocks)
    materials = parse_materials(ore_catalog)
    machine_ids = parse_machine_ids(machine_catalog)
    cable_ids = parse_cable_ids(cable_tier)
    armor_sets = parse_armor_sets(tech_items)

    material_ids = {m for m, _ in materials}

    # Loop-generated IDs in TechItems.
    for m, has_tools in materials:
        static_item_ids.update({
            f"raw_{m}",
            f"{m}_ingot",
            f"{m}_dust",
            f"{m}_nugget",
            f"{m}_ore",
            f"{m}_block",
        })
        if has_tools:
            static_item_ids.update({
                f"{m}_pickaxe",
                f"{m}_sword",
                f"{m}_axe",
                f"{m}_shovel",
                f"{m}_hammer",
                f"{m}_hoe",
            })

    # Loop-generated armor IDs in TechItems.
    for armor in armor_sets:
        static_item_ids.update({
            f"{armor}_helmet",
            f"{armor}_chestplate",
            f"{armor}_leggings",
            f"{armor}_boots",
        })

    # Loop-generated block items from machine/cables.
    static_item_ids.update(machine_ids)
    static_item_ids.update(cable_ids)

    # Known runtime-only IDs referenced by recent gameplay additions.
    static_item_ids.update({
        "coal_generator",
        "crusher",
        "smelter",
        "copper_cable",
        "bronze_ingot",
        "steel_ingot",
        "bronze_dust",
        "steel_dust",
    })

    # Block IDs implied by machine and material loops.
    block_ids.update(machine_ids)
    block_ids.update(cable_ids)
    for m in material_ids:
        block_ids.update({f"{m}_ore", f"{m}_block"})

    return static_item_ids, block_ids, materials, material_ids, machine_ids, cable_ids, armor_sets


def main() -> None:
    textures_item = ASSETS / "textures" / "item"
    textures_block = ASSETS / "textures" / "block"
    item_models = ASSETS / "models" / "item"
    block_models = ASSETS / "models" / "block"
    blockstates = ASSETS / "blockstates"

    ensure_dir(textures_item)
    ensure_dir(textures_block)
    ensure_dir(item_models)
    ensure_dir(block_models)
    ensure_dir(blockstates)

    item_ids, block_ids, materials, material_ids, machine_ids, cable_ids, _ = load_ids()

    print(f"Discovered {len(material_ids)} materials, {len(machine_ids)} machines, {len(cable_ids)} cable tiers.")
    print(f"Ensuring assets for {len(item_ids)} items and {len(block_ids)} blocks under {ASSETS}")

    # Material families.
    for mat, _has_tools in sorted(materials):
        ensure_material_assets(mat, color_for(mat), textures_item, textures_block)
    ensure_material_models(material_ids, block_models, blockstates, item_models)

    # Machine/cable blocks.
    ensure_machine_assets(machine_ids, textures_block, block_models, blockstates, item_models)
    ensure_cable_assets(cable_ids, textures_block, block_models, blockstates, item_models)

    # Remaining generic item models/textures.
    for iid in sorted(item_ids):
        ensure_item_model(iid, item_models, block_ids)
        ensure_generic_item_texture(iid, textures_item)

    # Ensure generic block textures/models/state for any remaining block IDs.
    for bid in sorted(block_ids):
        tex = textures_block / f"{bid}.png"
        if not tex.exists():
            save_png(gen_block_texture(bid, color_for(bid)), tex)

        bmodel = block_models / f"{bid}.json"
        if not bmodel.exists():
            write_json(bmodel, model_block_cube_all(bid))

        bstate = blockstates / f"{bid}.json"
        if not bstate.exists():
            write_json(bstate, blockstate_single(bid))

    print("Done. Generated/filled missing textures/models/blockstates.")


if __name__ == "__main__":
    try:
        main()
    except Exception as exc:  # noqa: BLE001
        print(f"Asset generation failed: {exc}")
        raise
