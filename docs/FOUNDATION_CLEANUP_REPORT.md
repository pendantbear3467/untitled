# Foundation Cleanup Report

## Scan Result

Compiled runtime source set:
- `src/main/java`
- `api/src/main/java`

Not part of the current Gradle runtime source set:
- Top-level sibling folders such as `core/`, `gameplay/`, `tech/`, and `worldgen/` outside `src/`

## Ownership Map

- Progression: `LIVE_RUNTIME`
  - Owner: `src/main/java/com/extremecraft/progression`
  - Data owner: `src/main/resources/data/extremecraft/progression`
  - Legacy/adapter: `progression/level`, `PlayerStatsService`, `game/ProgressionSystem`

- Machines: `LIVE_RUNTIME` with `LEGACY` overlap
  - Owner: `src/main/java/com/extremecraft/machine/core`, `src/main/java/com/extremecraft/machine/recipe`, `src/main/java/com/extremecraft/future/registry`
  - Legacy live path: `src/main/java/com/extremecraft/machines`
  - Adapter path: `src/main/java/com/extremecraft/machine`
  - Metadata-only mirrors: `platform/data/loader/MachineDataLoader`, `data/extremecraft/machines`

- Recipes: `LIVE_RUNTIME`
  - Owner: `src/main/resources/data/extremecraft/recipes`
  - Metadata-only mirror: `src/main/resources/data/extremecraft/ec_recipes`

- Quests: `LIVE_RUNTIME`
  - Owner: `src/main/java/com/extremecraft/quest`, `data/extremecraft/extremecraft_quests`
  - Metadata-only mirror: `data/extremecraft/quests`

- Research: `EXPERIMENTAL`
  - Live definitions/capability exist in `src/main/java/com/extremecraft/research`
  - Wider gameplay application remains partially wired

- Skill trees: `LIVE_RUNTIME`
  - Owner: `src/main/java/com/extremecraft/progression/skilltree`, `data/extremecraft/skill_trees`
  - Metadata-only mirror: `platform/data/loader/SkillTreeDataLoader`
  - Legacy folder: `data/extremecraft/skilltrees`

- Classes: `LIVE_RUNTIME` with `ADAPTER` overlap
  - Owner: `src/main/java/com/extremecraft/progression/classsystem`, `src/main/java/com/extremecraft/classsystem`, `data/extremecraft/classes`, `data/extremecraft/class_abilities`
  - Metadata-only mirror: `platform/data/loader/ClassDataLoader`

- Modules: `LIVE_RUNTIME` with `VALIDATION_ONLY` overlap
  - Owner: `src/main/java/com/extremecraft/modules`, `data/extremecraft/armor_modules`, `data/extremecraft/tool_modules`
  - Metadata-only mirror: `data/extremecraft/modules`

- Worldgen: `LIVE_RUNTIME` with `VALIDATION_ONLY` overlap
  - Owner: `data/extremecraft/worldgen`, Forge biome modifier data, `src/main/java/com/extremecraft/worldgen`
  - Metadata-only mirror: `data/extremecraft/world_generation`, `data/extremecraft/worldgen_weights`

- Materials: `LIVE_RUNTIME` with `VALIDATION_ONLY` overlap
  - Owner: `src/main/java/com/extremecraft/machine/material`, `src/main/java/com/extremecraft/materials`, `src/main/java/com/extremecraft/future/registry`
  - Metadata-only mirror: `data/extremecraft/materials`

- Contamination/radiation: `LIVE_RUNTIME`
  - Owner: `src/main/java/com/extremecraft/radiation`, `data/extremecraft/radiation_sources`, `data/extremecraft/contamination`, `data/extremecraft/contamination_terrain`

- Entity rendering/model pipeline: `LIVE_RUNTIME` with `METADATA_ONLY` handoff files
  - Owner: `src/main/java/com/extremecraft/entity`, `src/main/java/com/extremecraft/client/render/entity`, `src/main/java/com/extremecraft/client/model/entity`
  - Metadata-only handoff: `assets/extremecraft/entities`, `assets/extremecraft/models/entity`

- Docs/tooling: `VALIDATION_ONLY` or contributor support
  - Owner paths: `docs/`, `tools/`, `scripts/`, `datapacks/`, `examples/`

## What Existed Already

- A canonical progression mutation facade already existed.
- A live tech machine runtime already existed in `machine/core` plus `future/registry`.
- A radiation stack already existed with numeric chunk contamination and protection hooks.
- Live entity renderer/model ownership was already in Java, not asset JSON.

## Contamination Scan Result

- Radiation dose, ambient source sampling, numeric chunk contamination, and protection hooks already existed in `src/main/java/com/extremecraft/radiation`.
- Reactor/meltdown-style contamination release hooks already existed through `RadiationService.releaseMeltdown`.
- A datapack-backed contamination profile loader already existed in `data/extremecraft/contamination`.
- A terrain mutation rule path also existed through `data/extremecraft/contamination_terrain`, but the overall terrain ownership was not documented and the live block variant set was incomplete on disk.
- This pass kept the existing hazard stack and extended it; it did not introduce a second corruption/fallout system.

## What Was Partially Wired

- Research had definition/capability support but limited broader gameplay consumption.
- Contamination existed numerically but did not yet mutate terrain blocks.
- Platform data loaders mirrored many live folders without being the gameplay owner.
- Placeholder entity metadata existed but did not fully explain runtime ownership or Blockbench replacement flow.

## What Was Duplicated

- `machine/` vs `machine/core/` vs `machines/`
- `quests/` vs `extremecraft_quests/`
- `classes/` loaded by both gameplay and platform metadata loaders
- `skill_trees/` vs `skilltrees/`
- `recipes/` vs `ec_recipes/`
- `worldgen/` vs `world_generation/`

## What Changed In This Pass

- Formal stage ladder hardened to `PRIMITIVE -> ENERGY -> INDUSTRIAL -> ADVANCED -> ENDGAME` with legacy `AUTOMATION` treated as an alias.
- Explicit stage and unlock JSON files were added under `data/extremecraft/progression`.
- Misleading `machine:windmill` stage entry was removed from live stage data.
- Existing radiation/contamination stack was extended into a single terrain-capable contamination pipeline with bounded spread, cleanup hooks, and new contaminated terrain variants.
- A hardened fallout endpoint now exists through `vitrified_fallout`, allowing future high-dose terrain escalation without adding a second hazard system.
- New README coverage was added across the major runtime, data, and asset folders listed in this pass.
- Class comments now mark several platform data loaders as metadata-only instead of gameplay owners.

## What Was Intentionally Left Alone

- The standalone pulverizer runtime path remains registered for compatibility.
- Platform metadata loaders remain in place for validation/debug tooling.
- Research was documented as experimental rather than force-migrated.
- Top-level non-`src` folders were not reorganized because they are outside the current Gradle runtime source set.

## Remaining Uncertainties

- Tracked artifacts such as `src/main/java/com/extremecraft/network/sync/RuntimeSyncService.java.bak_test`, `src/main/java/com/extremecraft/machines/base/.write_test.tmp`, and `.class` files in source folders should be checked in commit/history review before deletion.
- Research still needs a deliberate decision on how unlocks should gate gameplay beyond definition storage.
- Some generated recipe/assets content may still be sourced by tooling; verify generator ownership before hand-editing large generated sets.

## Verification

- `.\gradlew.bat compileJava` succeeded.
- `.\gradlew.bat classes` succeeded.
