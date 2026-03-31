# Data-Driven Content

ExtremeCraft prioritizes datapack-driven balancing and extension.

## Supported roots

- `data/extremecraft/skills/*.json`
- `data/extremecraft/research/*.json`
- `data/extremecraft/machines/*.json` (`METADATA / VALIDATION MIRROR`, not the live tech machine tick owner)
- `data/extremecraft/armor_modules/*.json`
- `data/extremecraft/tool_modules/*.json`
- `data/extremecraft/abilities/*.json`
- `data/extremecraft/extremecraft_quests/*.json` (`CANONICAL LIVE RUNTIME OWNER` for live quests)
- `data/extremecraft/quests/*.json` (`METADATA / VALIDATION MIRROR`)
- `data/extremecraft/classes/*.json`
- `data/extremecraft/class_abilities/*.json`
- `data/extremecraft/skill_trees/*.json`
- `data/extremecraft/tech_trees/*.json` (`METADATA / VALIDATION MIRROR`)
- `data/extremecraft/materials/*.json` (`METADATA / VALIDATION MIRROR`)
- `data/extremecraft/world_generation/*.json` (`METADATA / VALIDATION MIRROR`)

## Runtime tools

- `/ec reload` reloads selected datapacks
- `/ec debug` prints module/API counters
- `/ec modules` lists loaded extension modules

## Ownership Notes

- Active tech machine runtime uses `machine/core` plus `recipes/machine_processing`.
- Active quests use `extremecraft_quests`, not `quests`.
- Active modular gear uses `armor_modules` and `tool_modules`, not `modules`.
- Active skill trees use `skill_trees`, while `skilltrees` is a legacy folder name kept for compatibility/reference.
- Active world placement uses `worldgen/**` and `forge/**`, not `world_generation/*.json`.

See `docs/CANONICAL_OWNERSHIP_MAP.md` before starting work in a similarly named folder.
