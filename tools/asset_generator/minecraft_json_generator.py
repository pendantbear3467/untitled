from __future__ import annotations

import json
from pathlib import Path

MODID = "extremecraft"


def write_json(path: Path, data: dict, dry_run: bool = False) -> bool:
    if dry_run:
        return not path.exists()
    path.parent.mkdir(parents=True, exist_ok=True)
    serialized = json.dumps(data, indent=2) + "\n"
    if path.exists() and path.read_text(encoding="utf-8") == serialized:
        return False
    path.write_text(serialized, encoding="utf-8")
    return True


def generate_cube_block_model(block_id: str) -> dict:
    return {
        "parent": "block/cube_all",
        "textures": {"all": f"{MODID}:block/{block_id}"},
    }


def generate_item_model(item_id: str, is_block_item: bool) -> dict:
    if is_block_item:
        return {"parent": f"{MODID}:block/{item_id}"}
    return {
        "parent": "item/generated",
        "textures": {"layer0": f"{MODID}:item/{item_id}"},
    }


def generate_single_variant_blockstate(block_id: str) -> dict:
    return {"variants": {"": {"model": f"{MODID}:block/{block_id}"}}}
