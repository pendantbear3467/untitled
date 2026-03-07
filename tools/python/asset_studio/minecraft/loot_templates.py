from __future__ import annotations


def simple_self_drop(block_id: str) -> dict:
    return {
        "type": "minecraft:block",
        "pools": [
            {
                "rolls": 1,
                "entries": [
                    {
                        "type": "minecraft:item",
                        "name": f"extremecraft:{block_id}",
                    }
                ],
            }
        ],
    }
