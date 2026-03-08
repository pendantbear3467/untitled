# First Release Art And Blockbench Handoff

This file marks placeholder client assets intentionally used for first-release testing.

## Placeholder icon sources

- Spell icons added in `assets/extremecraft/textures/gui/spells/*.png` are temporary copies of `abilities/ability_default.png`.
- Ability icon fallbacks for new spells added in `assets/extremecraft/textures/gui/abilities/*.png` are temporary copies.
- Skill node icons added in `assets/extremecraft/textures/gui/skills/*.png` are temporary copies of `skill_node_unlocked.png`.

## Replace-later checklist

- Replace temporary spell/ability icons with school-specific art and readability-tested silhouettes.
- Replace skill node icon placeholders with tree-themed icon set (magic vs tech).
- Provide Blockbench models for first-release-specific blocks/items when backend registration lands:
  - `fission_reactor`
  - `fission_control_console`
  - `aether_collector`
  - `rune_dynamo`
  - `mana_aether_condenser`
  - `arc_infusion_machine`
  - tactical/dirty device blocks/items

## Integration note

Placeholder replacements should keep the same asset IDs and file paths to avoid logic or data migration.
