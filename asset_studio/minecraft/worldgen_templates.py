from __future__ import annotations


def configured_ore_feature(ore_id: str) -> dict:
    return {
        "type": "minecraft:ore",
        "config": {
            "size": 9,
            "discard_chance_on_air_exposure": 0.0,
            "targets": [
                {
                    "target": {
                        "predicate_type": "minecraft:tag_match",
                        "tag": "minecraft:stone_ore_replaceables",
                    },
                    "state": {"Name": f"extremecraft:{ore_id}"},
                }
            ],
        },
    }


def placed_ore_feature(ore_id: str) -> dict:
    return {
        "feature": f"extremecraft:{ore_id}",
        "placement": [
            {"type": "minecraft:count", "count": 8},
            {"type": "minecraft:in_square"},
            {"type": "minecraft:height_range", "height": {"type": "minecraft:uniform", "min_inclusive": {"absolute": -32}, "max_inclusive": {"absolute": 64}}},
            {"type": "minecraft:biome"},
        ],
    }


def biome_modifier(ore_id: str) -> dict:
    return {
        "type": "forge:add_features",
        "biomes": "#minecraft:is_overworld",
        "features": [f"extremecraft:{ore_id}"],
        "step": "underground_ores",
    }
