# DATAPACK_FORMAT

ExtremeCraft consumes JSON datapack content from `data/extremecraft/**` (built-in and external datapacks).

Use this document as the contributor schema reference.

For "which folder actually changes live gameplay?" see `docs/CANONICAL_OWNERSHIP_MAP.md`.

## Resource Location Rules

- Use lowercase ids and paths.
- Prefer explicit namespace format (`extremecraft:<path>`) for references.
- Keep file ids and JSON `id` fields aligned where possible.
- Avoid legacy folder aliases when canonical folders exist.

Validation checks in `ECValidationService` will report malformed or missing references with concrete file paths and ids.

## Machine Metadata Mirror

Path:

- `data/extremecraft/machines/<id>.json`

Ownership note:

- This folder is a metadata/validation mirror for machines.
- Live tech machine runtime behavior is owned by:
  - `src/main/java/com/extremecraft/machine/core/MachineCatalog.java`
  - `src/main/resources/data/extremecraft/recipes/machine_processing/**`
- Editing `machines/*.json` alone will not change active tech machine ticks.

Schema:

```json
{
  "id": "crusher",
  "display_name": "Crusher",
  "tier": "pioneer",
  "energy_per_tick": 16,
  "processing": {
    "speed": 0.8,
    "parallel_operations": 2
  },
  "recipes": "extremecraft:machine_processing"
}
```

Fields:

- `id` string: machine identifier.
- `tier` string: progression tier (`pioneer`, `industrial`, etc.).
- `energy_per_tick` number: base runtime energy use.
- `processing.speed` number: processing speed multiplier.
- `processing.parallel_operations` number: parallel recipe capacity.
- `recipes` string: recipe group/type key.
- `display_name` string: optional UI label.

Machine metadata validation also checks common asset/reference hygiene:

- Missing blockstate/block model/item model for machine id.
- Missing referenced recipe ids (for local namespace refs).
- Optional machine asset keys (such as `model`, `block_model`, `item_model`, `texture`, `icon`) when present.

## Skills

Path:

- `data/extremecraft/skills/<id>.json`

Schema:

```json
{
  "skill": "mining_speed",
  "max_level": 10,
  "bonus_per_level": 0.05
}
```

Fields:

- `skill` string: skill id.
- `max_level` number: cap for progression.
- `bonus_per_level` number: per-level bonus value.

Ownership note:

- Skill definitions are live here.
- Skill-tree topology is owned separately by `data/extremecraft/skill_trees/*.json`.
- The older `data/extremecraft/skilltrees/*.json` folder is legacy and should not be the first edit target.

## Validation-Friendly Authoring Tips

- Ensure every new block/item has model JSON (`assets/extremecraft/models/...`).
- Ensure every new block/item has a language key in `assets/extremecraft/lang/en_us.json`.
- Ensure every new block has a loot table (`data/extremecraft/loot_tables/blocks/...`).
- Keep recipe `item` / `result` / `tag` references parseable resource locations.
- Keep ability icon assets in either `assets/extremecraft/textures/gui/abilities/` or `assets/extremecraft/textures/gui/spells/`.

## Abilities

Paths:

- `data/extremecraft/abilities/<id>.json` (runtime ability definitions)
- `data/extremecraft/abilities_platform/<id>.json` (platform metadata definitions)

Ownership note:

- `abilities/*.json` is a shared live source for both generic ability runtime and module-triggered abilities.
- `abilities_platform/*.json` is metadata-only.

Runtime ability schema:

```json
{
  "id": "fireball",
  "trigger": "on_right_click",
  "cooldown_ticks": 60,
  "mana_cost": 18,
  "target": "projectile",
  "range": 24,
  "effects": [
    {
      "type": "projectile",
      "value": 6,
      "scalars": {
        "speed": 2.6
      }
    }
  ]
}
```

Core fields:

- `id` string: ability id.
- `cooldown_ticks` number: cooldown in ticks.
- `mana_cost` number: mana cost.
- `target` string: `self`, `entity`, `area`, `projectile`, `none`.
- `range` number: cast/search range.
- `effects` array: effect descriptors consumed by ability execution.
- `trigger` string: optional input trigger metadata.

Platform ability schema (`abilities_platform`) uses:

- `id`, `trigger`, `cooldown_ticks`, `mana_cost`, `scaling` map.

## Bosses

Path:

- `data/extremecraft/entities/<id>.json`

Schema (current content convention):

```json
{
  "id": "void_titan",
  "category": "boss",
  "phase": "late",
  "health": 340,
  "damage": 18,
  "behavior": ["void_charge", "summon_wave", "rage_phase"],
  "spawn_biomes": ["#minecraft:is_end"],
  "spawn_condition": "void_temple",
  "loot_table": "extremecraft:entities/void_titan"
}
```

Fields:

- `id` string: boss/entity id.
- `category` string: usually `boss`.
- `phase` string: progression phase marker.
- `health`, `damage` numbers: tuning values.
- `behavior` array: behavior profile tags.
- `spawn_biomes` array: biome tags/ids.
- `spawn_condition` string: spawn gate key.
- `loot_table` string: reward table id.

## Recipes

Paths:

- `data/extremecraft/recipes/<id>.json`
- `data/extremecraft/recipes/machine_processing/**/*.json`
- `data/extremecraft/ec_recipes/<id>.json` (platform recipe metadata)

Vanilla crafting recipe example:

```json
{
  "type": "minecraft:crafting_shaped",
  "pattern": ["ISI", "PFP", "IRI"],
  "key": {
    "I": { "item": "minecraft:iron_ingot" },
    "S": { "item": "minecraft:stone" },
    "P": { "item": "minecraft:piston" },
    "F": { "item": "minecraft:furnace" },
    "R": { "item": "minecraft:redstone" }
  },
  "result": {
    "item": "extremecraft:crusher",
    "count": 1
  }
}
```

Machine processing recipe example:

```json
{
  "type": "extremecraft:machine_processing",
  "machine": "crusher",
  "input": {
    "item": "extremecraft:copper_ore"
  },
  "output": {
    "item": "extremecraft:raw_copper",
    "count": 2
  },
  "process_time": 200,
  "energy_per_tick": 16
}
```

Platform recipe metadata (`ec_recipes`) typically includes:

- `id` string
- `type` string

## Validation Guidance

- Keep ids lowercase and namespace-safe.
- Keep cross-references consistent (`machine` ids, item ids, loot table ids).
- Use `extremecraft_quests/` for live quest content, not `quests/`.
- Use `armor_modules/` and `tool_modules/` for live modular gear, not `modules/`.
- Use `worldgen/` and `forge/` for live placement data, not `world_generation/`.
- Run `./gradlew check` to execute content validation (`tools/content_completion.py`).
