from __future__ import annotations


def shaped_pickaxe_recipe(tool_name: str, material: str) -> dict:
    return {
        "type": "minecraft:crafting_shaped",
        "pattern": ["MMM", " S ", " S "],
        "key": {
            "M": {"item": f"extremecraft:{material}_ingot"},
            "S": {"item": "minecraft:stick"},
        },
        "result": {"item": f"extremecraft:{tool_name}"},
    }
