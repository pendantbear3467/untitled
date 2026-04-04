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

## Verified Runtime Ownership Table

| Subsystem | Canonical live runtime owner | Write/edit path that changes gameplay | Live read/enforcement path | Adapter / mirror / legacy notes |
| --- | --- | --- | --- | --- |
| Machines | `machine/core/**` + `future/registry/**` | `MachineCatalog`, `TechMachineBlockEntity`, `MachineTickScheduler`, `MachineProcessingService`, `recipes/machine_processing/**` | `MachineBlock.use` gates player access; `TechMachineBlockEntity` + `MachineRecipeService` + `MachineTickScheduler` drive runtime | `machines/*.json` + `MachineRegistry` + `machine/MachineBlockEntity` are compatibility/mirror/legacy, not the active tech-machine owner |
| Progression | `progression/ProgressionFacade`, `ProgressionMutationService`, `ProgressionService` | `ProgressionFacade` write methods | `ProgressionEvents`, `QuestRewardService`, commands, `SkillTreeService`, and canonical services | `ProgressionReadAccess` is the read contract; `LevelService`, `XPManager`, `GuildQuestRewardService`, `PlayerStatsService`, and legacy capabilities mirror/delegate canonical state |
| Quests | `quest/QuestManager` + `progression/QuestRewardService` | `data/extremecraft/extremecraft_quests/*.json`, `QuestRewardService` | `QuestManager.all/byId`, `ProgressionEvents.incrementQuest`, `ProgressCommands.claim` | `data/extremecraft/quests/*.json` + `platform/data/loader/QuestDataLoader` are metadata-only |
| Guild rewards | `QuestRewardService` | same service + live quest JSON rewards | `ProgressCommands.claim` -> `ProgressionFacade.claimGuildQuestReward` -> `QuestRewardService` | `GuildQuestRewardService` is now a compatibility adapter; reward mutations route through canonical progression services instead of direct ad-hoc writes |
| Stages | `stage/PlayerStageData`, `StageManager`, `StageDataLoader` | `data/extremecraft/progression/stages/*.json`, `ProgressionGate.grantStage` | `StageManager.hasStage`, `MachineBlock.use`, `UnlockRuleLoader.canUnlock`, `RuntimeSyncService.syncStageState` | Stage mapping JSON is live; stage state is capability-backed and mirrored to clients through `SyncStageStateS2CPacket` |
| Unlocks | `unlock/UnlockRuleLoader`, `UnlockAccessService`, `ProgressionGate` | `data/extremecraft/progression/unlocks/*.json`, progression grant paths | `MachineBlock.use`, `AbilityEngine`, `ClassAbilityService`, `SpellExecutor`, `UnlockAccessService` | `canUseRecipe` is a real helper but not automatically applied to autonomous machine processing |
| Skills | `skills/SkillRegistry` + `SkillProgressionService` | `data/extremecraft/skills/*.json`, `SkillProgressionService` | `ProgressionEvents` gameplay hooks, `SkillsApi`, runtime stat calculations | Direct `PlayerSkillsCapability` mutation should be treated as backing-state only |
| Skill trees | `progression/skilltree/SkillTreeManager`, `SkillTreeService` | `data/extremecraft/skill_trees/*.json`, `SkillTreeService.tryUnlock*` | `UnlockSkillNodeC2S`, `UpgradeStatPacket` compatibility shim, `PlayerStatsService.tryUnlockSkillNode` | `skilltrees/*.json` is legacy folder naming; `SkillTreeRegistry` is a synchronized compatibility mirror |
| Classes | `progression/classsystem/data/ClassDefinitionLoader` + `ClassDefinitions` | `data/extremecraft/classes/*.json` | `ClassAccessResolver` now resolves canonical definitions first | `ClassRegistry` is now a compatibility adapter projected from canonical class definitions |
| Class abilities | `ClassAbilityLoader` + `ClassAbilityService` | `data/extremecraft/class_abilities/*.json` | `ActivateClassAbilityC2SPacket` -> `ClassAbilityService.tryActivate` | Unlock/class enforcement now happens in `ClassAbilityService`; older client/UI code is not authoritative |
| Abilities | `AbilityRegistry`, `AbilityEngine`, `AbilityExecutor` | `data/extremecraft/abilities/*.json` and runtime handlers in `ability/**` | `ActivateAbilityC2SPacket` -> `AbilityEngine.cast` | `abilities_platform/*.json` + `AbilityDataLoader` are metadata-only; module trigger payloads moved to `module_abilities/*.json` with warned fallback from `abilities/*.json` |
| Spells | `SpellLoader`, `SpellRegistry`, `SpellExecutor` | `data/extremecraft/spells/*.json` | `SpellExecutor.tryCast*` and spell-item/keybind entrypoints | `SpellDefinition.java` is an older parallel model; live spell runtime compiles `Spell` into `AbilityDefinition` at cast time |
| Modules | `modules/loader/**`, `modules/service/**`, `modules/runtime/**` | `armor_modules/*.json`, `tool_modules/*.json`, `module_abilities/*.json` | `ModuleInstallService`, `ModuleRuntimeService`, module packets/events, `PlayerStatsGameplayEvents.onBreakSpeed` for mining-speed stat application | `data/extremecraft/modules/*.json` + `item/module/ModuleLoader` are mirror/legacy; `item/module/**` is not the canonical modular-gear path |
| Modular gear install/runtime | `ModuleInstallService` + `ModuleRuntimeService` | install/remove through service, runtime triggers in `ModuleRuntimeEvents` | `InstallModuleC2SPacket`, `RemoveModuleC2SPacket`, `ModuleRuntimeEvents` | Required skill-node checks remain live here; module definitions are read from armor/tool module registries |
| Materials | `machine/material/OreMaterialCatalog` + `future/registry/TechBlocks/TechItems` | code-owned material catalog and registry chain | ore/item/block registration and any runtime stat/lookups using registered items/blocks | `materials/*.json` + `MaterialDataLoader` are metadata mirrors |
| World generation | datapack `worldgen/**` + `forge/**` | `data/extremecraft/worldgen/**`, `data/extremecraft/forge/**` | vanilla/Forge worldgen bootstrap at runtime | `world_generation/*.json` + `WorldGenerationDataLoader` are profile metadata only |
| Network packet ownership | `network/ModNetwork` | `ModNetwork.init()` and packet handlers under `network/packet/**` | server-authoritative handlers in registered packets only | Packet classes not registered in `ModNetwork` are not live network authority even if they still compile |
| Runtime sync | `network/sync/RuntimeSyncService` | sync requests from canonical services only | sync-only S2C payload generation, including dedicated stage reporting | Snapshot sync is mirror-only, not mutation authority |
| Generated workspace outputs | `workspace/build`, `workspace/generated`, `workspace/exports`, `tools/generated`, `docs/generated` | tooling/generator/export flows | inspection, packaging review, snapshot comparison | intentionally preserved generated/reference outputs; do not edit first for gameplay |

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
- Player-facing machine stage/unlock enforcement happens in `machine/core/MachineBlock.java`.
- `ProgressionGate.canUseRecipe(...)` is a real helper for player-owned crafting flows, but it is not automatically applied to autonomous tech-machine server ticks because those run without player context.

## Progression

Classification: `CANONICAL LIVE RUNTIME OWNER`

Canonical write paths:

- Public mutation boundary:
  - `src/main/java/com/extremecraft/progression/ProgressionFacade.java`
- Internal player XP and level owner:
  - `src/main/java/com/extremecraft/progression/ProgressionMutationService.java`
  - `src/main/java/com/extremecraft/progression/ProgressionService.java`
- Cross-module read boundary:
  - `src/main/java/com/extremecraft/ecosystem/core/progression/ProgressionReadAccess.java`
- Skill XP:
  - `src/main/java/com/extremecraft/progression/ProgressionFacade.java`
  - `src/main/java/com/extremecraft/progression/SkillProgressionService.java`
  - backing state in `src/main/java/com/extremecraft/skills/**`
- Class XP:
  - `src/main/java/com/extremecraft/progression/QuestRewardService.java`
  - `src/main/java/com/extremecraft/progression/ClassProgressionService.java`
  - backing state in `src/main/java/com/extremecraft/progression/PlayerProgressData.java`

Progression reward application:

- Live gameplay XP/quest/skill hooks: `src/main/java/com/extremecraft/progression/ProgressionEvents.java`
- Guild quest claim rewards: `src/main/java/com/extremecraft/progression/QuestRewardService.java`

Compatibility mirrors that should not be the first write target:

- `src/main/java/com/extremecraft/progression/level/LevelService.java`
- `src/main/java/com/extremecraft/progression/GuildQuestRewardService.java`
- `src/main/java/com/extremecraft/progression/PlayerStatsService.java`
- `src/main/java/com/extremecraft/progression/capability/PlayerStatsCapability.java`
- `src/main/java/com/extremecraft/progression/capability/PlayerStatsGameplayEvents.java`
  - still live for resource regen/module stat side effects, but no longer a progression XP owner

Legacy/disconnected:

- `src/main/java/com/extremecraft/game/ProgressionSystem.java`
- `src/main/java/com/extremecraft/progression/XPManager.java` as an older facade kept for compatibility

Rule of thumb:

- Use `ProgressionFacade` for all gameplay writes.
- Use `ProgressionReadAccess` or `ProgressionFacade.readAccess()` for cross-module reads.
- Skill XP is currently combat-only plus debug override through gameplay-facing progression services and skill state.
- Class XP is awarded through guild quest claim flow, not through unrelated systems.
- Do not mutate `PlayerProgressData` directly outside progression services; its mutable collections are no longer an edit surface.

## Quests

Classification: `CANONICAL LIVE RUNTIME OWNER`

Edit here to change live quest content:

- `src/main/resources/data/extremecraft/extremecraft_quests/*.json`
- Loader/registry: `src/main/java/com/extremecraft/quest/QuestManager.java`

Edit here to change live guild quest rewards:

- Reward application: `src/main/java/com/extremecraft/progression/QuestRewardService.java`
- `GuildQuestRewardService.java` is retained only as a legacy adapter.

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
  - sync/reporting mirror only:
    - `src/main/java/com/extremecraft/network/sync/RuntimeSyncService.java`
    - `src/main/java/com/extremecraft/network/sync/SyncStageStateS2CPacket.java`
    - `src/main/java/com/extremecraft/network/sync/RuntimeSyncClientState.java`
  - actual live callers:
    - `src/main/java/com/extremecraft/machine/core/MachineBlock.java`
    - `src/main/java/com/extremecraft/ability/AbilityEngine.java`
    - `src/main/java/com/extremecraft/progression/classsystem/ability/ClassAbilityService.java`
    - `src/main/java/com/extremecraft/magic/SpellExecutor.java`

Do not edit first:

- `default_curve.json`
  - progression-curve metadata, not the stage ownership path
- `ProgressionGate.canUseRecipe(...)`
  - helper is live code, but no current autonomous machine tick path calls it because recipe processing does not run with a player context

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
- `src/main/java/com/extremecraft/classsystem/ClassAccessResolver.java`

Interpretation:

- `classes/*.json` is gameplay-authoritative.
- `progression/classsystem/data` is the canonical definition owner and primary runtime read source.
- `classsystem/ClassRegistry` is now a compatibility projection over canonical definitions, not a second datapack loader.

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

Execution note:

- `AbilityExecutor.executeDefinition(...)` is the shared compiled-definition effect path.
- `AbilityEngine` owns active ability cast triggers.
- `SpellExecutor` owns spell cast triggers and compiles spells into `AbilityDefinition`.
- `ClassAbilityService` owns class ability triggers and compiles supported class abilities into `AbilityDefinition`.
- `ModuleRuntimeService` owns module-trigger timing/cooldowns and compiles active trigger payloads from `module_abilities/*.json` into `AbilityDefinition`.
- Unlock-rule enforcement for generic abilities happens in `src/main/java/com/extremecraft/ability/AbilityEngine.java`

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
- Spell/class/unlock gating is enforced in `SpellExecutor`, not in item client code or packet senders.

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
  - passive mining/break-speed application:
    - `src/main/java/com/extremecraft/progression/capability/PlayerStatsGameplayEvents.java`
  - live tool wrapper example:
    - `src/main/java/com/extremecraft/item/tool/ModularDrillItem.java`

Canonical module trigger ability definitions:

- `src/main/resources/data/extremecraft/module_abilities/*.json`
- loader: `src/main/java/com/extremecraft/modules/loader/ModuleAbilityLoader.java`

Compatibility fallback:

- trigger-bearing legacy files under `src/main/resources/data/extremecraft/abilities/*.json`
- kept only to avoid breaking older packs immediately; validation now warns on this layout

Do not edit first:

- `src/main/resources/data/extremecraft/modules/*.json`
  - `METADATA / VALIDATION MIRROR`
- `src/main/java/com/extremecraft/platform/data/loader/ModuleDataLoader.java`
  - metadata mirror loader
- `src/main/java/com/extremecraft/item/module/**`
  - `LEGACY / DISCONNECTED` generic module path
  - `ModuleLoader` is not the canonical modular-gear loader and is not the edit-first path for live armor/tool modules
  - `TechItems.modular_drill` no longer depends on this legacy path; it now routes through the canonical modular runtime

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
- Unregistered packet classes such as `UpgradeSkillPacket`, `RequestPlayerProgressSyncPacket`, and `SyncPlayerProgressCapabilityPacket` are compatibility residue and are not the live packet authority unless they are explicitly re-added to `ModNetwork.init()`.

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
