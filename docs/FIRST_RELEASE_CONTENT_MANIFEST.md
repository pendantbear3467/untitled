# First Release Content Manifest

This manifest separates live first-release content owners from metadata-only scaffolds so contributors do not mistake placeholders for shipped runtime.

## Live first-release data owners

- Progression ladder, stage grants, and unlock rules: `data/extremecraft/progression/*`
- Machine processing recipes: `data/extremecraft/recipes/machine_processing/*.json`
- Canonical classes: `data/extremecraft/classes/*.json`
- Class abilities: `data/extremecraft/class_abilities/*.json`
- Skills and skill trees: `data/extremecraft/skills/*.json`, `data/extremecraft/skill_trees/*.json`
- Spell definitions: `data/extremecraft/spells/*.json`
- Guild/class progression quests: `data/extremecraft/extremecraft_quests/*.json`
- Radiation source definitions: `data/extremecraft/radiation_sources/*.json`
- Contamination profiles and terrain mutation rules: `data/extremecraft/contamination/*.json`, `data/extremecraft/contamination_terrain/*.json`

## Code-owned runtime domains

- Tech machine definitions and stage defaults stay code-owned in `src/main/java/com/extremecraft/machine/core`
- The first-release reactor line stays code-owned under `src/main/java/com/extremecraft/reactor`
- Canonical progression state and mutation stay code-owned under `src/main/java/com/extremecraft/progression`

## Metadata-only or scaffold-only paths

- `data/extremecraft/materials/*.json`
  These are planning/reference metadata, not a live material authority.
- `data/extremecraft/machines/*.json`
  These do not own active machine runtime behavior.
- `data/extremecraft/ec_recipes/*.json`
  Legacy recipe-id metadata, not the canonical processing loader.
- `data/extremecraft/quests/*.json`
  Reference content scaffolds, not the first-release guild/class quest authority.
- `data/extremecraft/reactor_parts/*.json`
  Reference-only until a dedicated runtime loader exists.
- `data/extremecraft/endgame_core/*.json`
  Future-phase scaffold, not first-release shipped runtime.
- `data/extremecraft/spell_schools/*.json`
  Taxonomy scaffold; spells remain the live first-release runtime content.
- `data/extremecraft/worldgen_weights/*.json`
  Tuning scaffold, not a live first-release worldgen authority.

## Notes

- Do not promote metadata folders into runtime owners without migrating the matching Java service/load path.
- Keep first-release additions on the live owner paths above; leave future-phase scaffolds clearly marked as non-canonical.
