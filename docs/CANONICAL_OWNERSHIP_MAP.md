# Canonical Ownership Map

This document is the repository-level source of truth for "what do I edit if I
want gameplay to change?" in ExtremeCraft.

Use this before editing a folder that has a similarly named mirror, adapter, or
generated snapshot elsewhere in the repo.

## Classification Legend

- `CANONICAL LIVE RUNTIME OWNER`: primary code/data path that changes live gameplay.
- `COMPATIBILITY ADAPTER`: still read by runtime, but only to bridge older callers.
- `METADATA / VALIDATION MIRROR`: loaded for validation, sync, debug, or snapshots; not the live gameplay owner.
- `GENERATED REFERENCE SNAPSHOT`: intentionally kept output for inspection/comparison/package review.
- `TOOLING SOURCE`: generators, editors, packaging tools.
- `DISTRIBUTABLE / BUILD OUTPUT`: packaged jars, zips, reports, transient build products.
- `LEGACY / DISCONNECTED`: older path kept for history or limited compatibility; do not start new work here.

## Build And Runtime Truth

- Current Forge runtime code is compiled from `src/main/java` and `api/src/main/java`.
- Current Forge runtime resources are loaded from `src/main/resources`.
- Top-level folders such as `core/`, `gameplay/`, `tech/`, `magic/`, `worldgen/`, and `client/` are currently modularization scaffolds plus build placeholders, not the live gameplay code owner.
- `src/settings.gradle`, `src/.gradle`, and `src/build` are legacy nested-project/build artifacts inside the live source tree. They are not ownership signals.

## Top-Level Repository Areas

- `src/main/java` and `src/main/resources`: `CANONICAL LIVE RUNTIME OWNER`
- `api/src/main/java`: `CANONICAL LIVE RUNTIME OWNER` for public API contracts, not gameplay mutation logic
- `tools/**`: `TOOLING SOURCE`
- `workspace/**`: mixed `TOOLING SOURCE`, `GENERATED REFERENCE SNAPSHOT`, and local scratch state
- `docs/generated/**`: `GENERATED REFERENCE SNAPSHOT`
- `build/**`, `run/**`, `src/build/**`, `src/.gradle/**`: `DISTRIBUTABLE / BUILD OUTPUT`
- `core/`, `gameplay/`, `tech/`, `magic/`, `worldgen/`, `client/`: `LEGACY / DISCONNECTED` as runtime code owners for now

## Machines

Classification: `CANONICAL LIVE RUNTIME OWNER`

Edit here to change live machine behavior:

- Java runtime catalog and ticking:
  - `src/main/java/com/extremecraft/machine/core/MachineCatalog.java`
  - `src/main/java/com/extremecraft/machine/core/TechMachineBlockEntity.java`
  - `src/main/java/com/extremecraft/machine/core/MachineTickScheduler.java`
  - `src/main/java/com/extremecraft/machine/core/MachineProcessingService.java`
  - `src/main/java/com/extremecraft/machine/core/MachineRecipeService.java`
- Block/item/entity/menu registration:
  - `src/main/java/com/extremecraft/future/registry/TechBlocks.java`
  - `src/main/java/com/extremecraft/future/registry/TechItems.java`
  - `src/main/java/com/extremecraft/future/registry/TechBlockEntities.java`
  - `src/main/java/com/extremecraft/future/registry/TechMenuTypes.java`
- Live processing recipes:
  - `src/main/resources/data/extremecraft/recipes/machine_processing/**`

Do not edit first:

- `src/main/resources/data/extremecraft/machines/*.json`
  - `METADATA / VALIDATION MIRROR` for validation, sync snapshots, and future migration work
- `src/main/java/com/extremecraft/platform/data/loader/MachineDataLoader.java`
  - metadata mirror loader for `machines/*.json`
- `src/main/java/com/extremecraft/machine/MachineRegistry.java`
  - `COMPATIBILITY ADAPTER` for the older generic machine catalog
- `src/main/java/com/extremecraft/machines/**`
  - `LEGACY / DISCONNECTED` except the standalone pulverizer path that still exists for compatibility

Ownership notes:

- Active tech machine stage/process defaults are code-owned in `MachineCatalog`.
- Active tech machine processing recipes are recipe-manager owned in `recipes/machine_processing`.
- `machines/*.json` alone will not change active tech machine ticks.

## Progression

Classification: `CANONICAL LIVE RUNTIME OWNER`

Canonical write paths:

- Player XP and level:
  - `src/main/java/com/extremecraft/progression/ProgressionFacade.java`
  - `src/main/java/com/extremecraft/progression/ProgressionMutationService.java`
  - `src/main/java/com/extremecraft/progression/ProgressionService.java`
- Skill XP:
  - `src/main/java/com/extremecraft/progression/ProgressionFacade.java`
  - `src/main/java/com/extremecraft/progression/SkillProgressionService.java`
  - backing state in `src/main/java/com/extremecraft/skills/**`
- Class XP:
  - `src/main/java/com/extremecraft/progression/GuildQuestRewardService.java`
  - `src/main/java/com/extremecraft/progression/ClassProgressionService.java`
  - backing state in `src/main/java/com/extremecraft/progression/PlayerProgressData.java`

Progression reward application:

- Live gameplay XP hooks: `src/main/java/com/extremecraft/progression/ProgressionEvents.java`
- Guild quest claim rewards: `src/main/java/com/extremecraft/progression/GuildQuestRewardService.java`

Compatibility mirrors that should not be the first write target:

- `src/main/java/com/extremecraft/progression/level/LevelService.java`
- `src/main/java/com/extremecraft/progression/PlayerStatsService.java`
- `src/main/java/com/extremecraft/progression/capability/PlayerStatsCapability.java`

Legacy/disconnected:

- `src/main/java/com/extremecraft/game/ProgressionSystem.java`
- `src/main/java/com/extremecraft/progression/XPManager.java` as an older facade kept for compatibility

Rule of thumb:

- Skill XP is awarded through gameplay-facing progression services and skill state.
- Class XP is awarded through guild quest claim flow, not through unrelated systems.
- Do not mutate `PlayerProgressData` directly outside progression services unless you are extending the canonical reward path itself.

## Quests

Classification: `CANONICAL LIVE RUNTIME OWNER`

Edit here to change live quest content:

- `src/main/resources/data/extremecraft/extremecraft_quests/*.json`
- Loader/registry: `src/main/java/com/extremecraft/quest/QuestManager.java`

Edit here to change live guild quest rewards:

- Reward application: `src/main/java/com/extremecraft/progression/GuildQuestRewardService.java`

Do not edit first:

- `src/main/resources/data/extremecraft/quests/*.json`
  - `METADATA / VALIDATION MIRROR`
- `src/main/java/com/extremecraft/platform/data/loader/QuestDataLoader.java`
  - mirror loader for structured metadata inspection

## Stages And Unlock Rules

Classification: `CANONICAL LIVE RUNTIME OWNER`

Edit here to change live stage gates and unlock rules:

- Stage ladder and stage-to-unlock mapping:
  - `src/main/resources/data/extremecraft/progression/stages/*.json`
  - loader: `src/main/java/com/extremecraft/progression/StageDataLoader.java`
- Explicit unlock rules:
  - `src/main/resources/data/extremecraft/progression/unlocks/*.json`
  - loader: `src/main/java/com/extremecraft/progression/unlock/UnlockRuleLoader.java`
- Runtime enforcement:
  - `src/main/java/com/extremecraft/progression/ProgressionGate.java`
  - `src/main/java/com/extremecraft/progression/unlock/UnlockAccessService.java`
  - `src/main/java/com/extremecraft/progression/stage/StageManager.java`

Do not edit first:

- `default_curve.json`
  - progression-curve metadata, not the stage ownership path

## Skills And Skill Trees

Classification: `CANONICAL LIVE RUNTIME OWNER`

Edit here to change live skill definitions:

- `src/main/resources/data/extremecraft/skills/*.json`
- loader: `src/main/java/com/extremecraft/skills/SkillRegistry.java`

Edit here to change live skill-tree topology, costs, and node effects:

- `src/main/resources/data/extremecraft/skill_trees/*.json`
- loader/service:
  - `src/main/java/com/extremecraft/progression/skilltree/SkillTreeDataLoader.java`
  - `src/main/java/com/extremecraft/progression/skilltree/SkillTreeManager.java`
  - `src/main/java/com/extremecraft/progression/skilltree/SkillTreeService.java`

Compatibility mirrors:

- `src/main/java/com/extremecraft/progression/skilltree/SkillTreeRegistry.java`
  - synchronized legacy mirror populated from `SkillTreeManager`
- `src/main/java/com/extremecraft/platform/data/loader/SkillTreeDataLoader.java`
  - metadata/snapshot loader for validation and client summaries

Legacy/disconnected:

- `src/main/resources/data/extremecraft/skilltrees/*.json`
  - older folder name retained for reference/validation; not the live gameplay owner

## Classes, Class Abilities, Generic Abilities, And Spells

### Class Definitions

Classification: `CANONICAL LIVE RUNTIME OWNER`

Edit here:

- `src/main/resources/data/extremecraft/classes/*.json`
- canonical loader: `src/main/java/com/extremecraft/progression/classsystem/data/ClassDefinitionLoader.java`

Shared runtime readers:

- `src/main/java/com/extremecraft/progression/classsystem/data/ClassDefinitions.java`
- `src/main/java/com/extremecraft/classsystem/ClassRegistry.java`
- `src/main/java/com/extremecraft/classsystem/ClassAccessResolver.java`

Interpretation:

- `classes/*.json` is intentionally shared.
- `progression/classsystem/data` is the canonical definition owner.
- `classsystem/ClassRegistry` is a compatibility adapter for older callers.

### Class Abilities

Classification: `CANONICAL LIVE RUNTIME OWNER`

Edit here:

- `src/main/resources/data/extremecraft/class_abilities/*.json`
- loader: `src/main/java/com/extremecraft/progression/classsystem/data/ClassAbilityLoader.java`
- runtime effect execution: `src/main/java/com/extremecraft/progression/classsystem/ability/ClassAbilityService.java`

### Generic Abilities

Classification: `CANONICAL LIVE RUNTIME OWNER`

Edit here:

- `src/main/resources/data/extremecraft/abilities/*.json`
- loader/registry/runtime:
  - `src/main/java/com/extremecraft/ability/AbilityRegistry.java`
  - `src/main/java/com/extremecraft/ability/AbilityEngine.java`
  - `src/main/java/com/extremecraft/ability/AbilityExecutor.java`

Important shared-use note:

- `abilities/*.json` is also read by `src/main/java/com/extremecraft/modules/loader/ModuleAbilityLoader.java`
- This is intentional for module-triggered abilities that reuse the shared ability schema
- Editing a shared ability id can affect both generic ability behavior and modular gear behavior

Metadata-only mirror:

- `src/main/resources/data/extremecraft/abilities_platform/*.json`
- loader: `src/main/java/com/extremecraft/platform/data/loader/AbilityDataLoader.java`

### Spells

Classification: `CANONICAL LIVE RUNTIME OWNER`

Edit here:

- `src/main/resources/data/extremecraft/spells/*.json`
- loader/registry/runtime:
  - `src/main/java/com/extremecraft/magic/SpellLoader.java`
  - `src/main/java/com/extremecraft/magic/SpellRegistry.java`
  - `src/main/java/com/extremecraft/magic/SpellExecutor.java`
  - `src/main/java/com/extremecraft/magic/SpellService.java`

Important note:

- Spell JSON is not stored as generic ability JSON.
- `SpellExecutor` compiles spell data into generic `AbilityDefinition` payloads at cast time.
- Editing `abilities/*.json` alone will not change spell behavior.

## Modules And Modular Gear

Classification: `CANONICAL LIVE RUNTIME OWNER`

Edit here for installable armor/tool modules:

- `src/main/resources/data/extremecraft/armor_modules/*.json`
- `src/main/resources/data/extremecraft/tool_modules/*.json`
- loader/runtime:
  - `src/main/java/com/extremecraft/modules/loader/ModuleDefinitionLoader.java`
  - `src/main/java/com/extremecraft/modules/service/ModuleInstallService.java`
  - `src/main/java/com/extremecraft/modules/runtime/ModuleRuntimeService.java`
  - `src/main/java/com/extremecraft/modules/runtime/ModuleRuntimeEvents.java`

Shared module ability definitions:

- `src/main/resources/data/extremecraft/abilities/*.json`
- loader: `src/main/java/com/extremecraft/modules/loader/ModuleAbilityLoader.java`

Do not edit first:

- `src/main/resources/data/extremecraft/modules/*.json`
  - `METADATA / VALIDATION MIRROR`
- `src/main/java/com/extremecraft/platform/data/loader/ModuleDataLoader.java`
  - metadata mirror loader
- `src/main/java/com/extremecraft/item/module/**`
  - `LEGACY / DISCONNECTED` generic module path

## Materials And World Generation

### Materials

Mixed ownership:

- Ore/material registrations used by live tech blocks/items:
  - `src/main/java/com/extremecraft/machine/material/OreMaterialCatalog.java`
  - `src/main/java/com/extremecraft/future/registry/TechBlocks.java`
  - `src/main/java/com/extremecraft/future/registry/TechItems.java`
- Metadata mirrors:
  - `src/main/resources/data/extremecraft/materials/*.json`
  - `src/main/java/com/extremecraft/platform/data/loader/MaterialDataLoader.java`
- Legacy summary/helpers:
  - `src/main/java/com/extremecraft/materials/ModMaterials.java`
  - `src/main/java/com/extremecraft/worldgen/ModWorldGen.java`

Rule of thumb:

- If you want new ore-backed blocks/items/tools to exist in runtime, start with `OreMaterialCatalog` and the `future/registry` chain.
- `materials/*.json` is useful metadata, but it is not the authoritative owner for runtime ore/item registration today.

### World Generation

Classification: live runtime is datapack-owned

Edit here to change real ore/structure placement:

- `src/main/resources/data/extremecraft/worldgen/**`
- `src/main/resources/data/extremecraft/forge/**`

Validation/summary helpers:

- `src/main/java/com/extremecraft/worldgen/validation/WorldgenConsistencyValidator.java`
- `src/main/java/com/extremecraft/worldgen/WorldgenFeatureRegistry.java`
- `src/main/java/com/extremecraft/worldgen/OreGenerationManager.java`

Metadata-only mirror:

- `src/main/resources/data/extremecraft/world_generation/*.json`
- `src/main/java/com/extremecraft/platform/data/loader/WorldGenerationDataLoader.java`

## Network Packets And Sync

Classification: `CANONICAL LIVE RUNTIME OWNER`

Packet/channel owner:

- `src/main/java/com/extremecraft/network/ModNetwork.java`

Gameplay mutation and sync handlers:

- `src/main/java/com/extremecraft/network/packet/**`
- `src/main/java/com/extremecraft/network/sync/**`

Metadata snapshot sync:

- `src/main/java/com/extremecraft/platform/data/sync/**`
- packets:
  - `SyncMachinesPacket`
  - `SyncMaterialsPacket`
  - `SyncSkillTreesPacket`

Important note:

- Those metadata snapshot packets keep client/debug views in sync.
- They are not the gameplay mutation owners for machines, materials, or skill trees.

## Generated, Tooling, And Workspace Output

### Tooling Source

- `tools/**`: canonical generator/editor/tooling source
- `scripts/**`: maintainer automation helpers

### Intentionally Preserved Generated / Reference Output

- `tools/generated/**`
  - generated preview/model output kept for inspection/reference
- `workspace/generated/**`
  - generated content bundles kept for inspection
- `workspace/exports/**`
  - exported datapack/gui/model/skilltree output for comparison and packaging review
- `workspace/build/**`
  - packaged addon/module/modpack/build artifacts intentionally kept for current-state inspection
- `docs/generated/**`
  - generated reference docs/artifacts
- `build/extremecraft-validation-report.txt`
  - intentionally preserved validation artifact
- `build/reports/**`
  - intentionally preserved high-signal build reports

### Local Scratch / Not Source Of Truth

- `workspace/.studio/autosave/**`
- `workspace/.studio/logs/**`
- `workspace/.studio/recovery/**`
- `workspace/logs/**`
- `run/**`
- general Gradle caches and temp folders

These may be useful operationally, but they are not gameplay ownership paths.

## Quick Edit Lanes

- Machine behavior: `machine/core/**` plus `future/registry/**`
- Machine recipes: `data/extremecraft/recipes/machine_processing/**`
- Quest rewards: `progression/GuildQuestRewardService.java`
- Class XP rewards: `GuildQuestRewardService.java` via guild quest claim flow
- Skill XP gain rules: `progression/ProgressionEvents.java` and `SkillProgressionService.java`
- Skill-tree nodes/effects: `data/extremecraft/skill_trees/*.json`
- Spell definitions: `data/extremecraft/spells/*.json`
- Class abilities: `data/extremecraft/class_abilities/*.json` plus `ClassAbilityService`
- Stage/unlock rules: `data/extremecraft/progression/stages/*.json` and `progression/unlocks/*.json`
- Modular gear behavior: `modules/**`, `armor_modules/*.json`, `tool_modules/*.json`, shared `abilities/*.json`
- Ore/material runtime registration: `OreMaterialCatalog.java`, `TechBlocks.java`, `TechItems.java`
- Ore/material placement: `data/extremecraft/worldgen/**` and `data/extremecraft/forge/**`
