# First Release Content Manifest (Scaffold)

This manifest lists additive content scaffolding introduced for first-release playtesting.

## Data-driven systems wired to active loaders

- Materials: `data/extremecraft/materials/*.json`
- Machines: `data/extremecraft/machines/*.json`
- Machine recipe ids: `data/extremecraft/ec_recipes/*.json`
- Classes: `data/extremecraft/classes/*.json`
- Skills: `data/extremecraft/skills/*.json`
- Skill trees: `data/extremecraft/skill_trees/*.json`
- Spells: `data/extremecraft/spells/*.json`
- Guild quests (class progression): `data/extremecraft/extremecraft_quests/*.json`
- Content quests: `data/extremecraft/quests/*.json`

## Placeholder schema scaffolds (backend hooks pending)

- Radiation source definitions: `data/extremecraft/radiation_sources/*.json`
- Reactor part definitions: `data/extremecraft/reactor_parts/*.json`
- Endgame core stage requirements: `data/extremecraft/endgame_core/*.json`
- Spell schools: `data/extremecraft/spell_schools/*.json`
- Worldgen weight presets: `data/extremecraft/worldgen_weights/*.json`

## Notes

- These files are additive and keep existing IDs untouched.
- Placeholder-only systems are isolated under dedicated folders until canonical backend loaders are finalized.
