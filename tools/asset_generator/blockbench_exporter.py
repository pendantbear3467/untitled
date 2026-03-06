from __future__ import annotations

import json
from pathlib import Path


def _cube_element(from_pos: list[int], to_pos: list[int]) -> dict:
    return {
        "from": from_pos,
        "to": to_pos,
        "faces": {
            "north": {"uv": [0, 0, 16, 16], "texture": "#0"},
            "east": {"uv": [0, 0, 16, 16], "texture": "#0"},
            "south": {"uv": [0, 0, 16, 16], "texture": "#0"},
            "west": {"uv": [0, 0, 16, 16], "texture": "#0"},
            "up": {"uv": [0, 0, 16, 16], "texture": "#0"},
            "down": {"uv": [0, 0, 16, 16], "texture": "#0"},
        },
    }


def _template(name: str, texture_path: str, elements: list[dict]) -> dict:
    return {
        "meta": {
            "format_version": "4.9",
            "model_format": "java_block",
            "box_uv": False,
        },
        "name": name,
        "resolution": {"width": 16, "height": 16},
        "textures": [{"name": "texture", "path": texture_path, "id": "0"}],
        "elements": elements,
    }


def ore_block_model(name: str) -> dict:
    return _template(name, f"textures/block/{name}.png", [_cube_element([0, 0, 0], [16, 16, 16])])


def machine_casing_model(name: str) -> dict:
    return _template(name, f"textures/block/{name}.png", [_cube_element([0, 0, 0], [16, 16, 16])])


def tool_model(name: str) -> dict:
    handle = _cube_element([7, 0, 7], [9, 12, 9])
    head = _cube_element([5, 12, 5], [11, 16, 11])
    return _template(name, f"textures/item/{name}.png", [handle, head])


def drill_model(name: str) -> dict:
    body = _cube_element([4, 4, 2], [12, 12, 14])
    bit = _cube_element([6, 6, 14], [10, 10, 16])
    return _template(name, f"textures/item/{name}.png", [body, bit])


def armor_icon_model(name: str) -> dict:
    return _template(name, f"textures/item/{name}.png", [_cube_element([1, 1, 1], [15, 15, 2])])


def write_bbmodel(path: Path, data: dict, dry_run: bool = False) -> bool:
    if dry_run:
        return not path.exists()
    path.parent.mkdir(parents=True, exist_ok=True)
    serialized = json.dumps(data, indent=2) + "\n"
    if path.exists() and path.read_text(encoding="utf-8") == serialized:
        return False
    path.write_text(serialized, encoding="utf-8")
    return True
