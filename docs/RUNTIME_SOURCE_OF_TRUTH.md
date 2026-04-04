# Runtime Source Of Truth

This is the short contributor guide for live Forge runtime ownership. For the long-form map, see `docs/CANONICAL_OWNERSHIP_MAP.md`.

## Canonical Owners

- Machines:
  - Runtime defaults and family identity: `machine/core/MachineCatalog.java`
  - Tick/runtime owner: `machine/core/TechMachineBlockEntity.java`, `MachineTickScheduler.java`, `MachineProcessingService.java`
  - Live recipe source: `data/extremecraft/recipes/machine_processing/*.json`
  - `data/extremecraft/machines/*.json` is metadata-only and does not drive the active tech machine loop.
- Abilities:
  - Active ability trigger owner: `ability/AbilityEngine.java`
  - Shared compiled-definition execution path: `ability/AbilityExecutor.executeDefinition(...)`
  - Live active ability data: `data/extremecraft/abilities/*.json`
- Spells:
  - Trigger owner: `magic/SpellExecutor.java`
  - Live spell data: `data/extremecraft/spells/*.json`
  - Spells compile into `AbilityDefinition` at cast time; `abilities/*.json` does not own spell behavior.
- Module-triggered effects:
  - Trigger owner: `modules/runtime/ModuleRuntimeService.java`
  - Live module trigger data: `data/extremecraft/module_abilities/*.json`
  - Old trigger-bearing files under `abilities/*.json` are compatibility fallback only and now warn.
- Classes:
  - Canonical class-definition source: `progression/classsystem/data/ClassDefinitionLoader.java` + `ClassDefinitions.java`
  - Live data: `data/extremecraft/classes/*.json`
  - `classsystem/ClassRegistry.java` is compatibility-only and projects canonical definitions.
- Class abilities:
  - Trigger owner: `progression/classsystem/ability/ClassAbilityService.java`
  - Live data: `data/extremecraft/class_abilities/*.json`
  - Supported class abilities compile into the shared `AbilityExecutor` path.
- Progression:
  - External mutation facade: `progression/ProgressionFacade.java`
  - Player XP/level owner: `ProgressionMutationService.java`
  - Skill XP owner: `SkillProgressionService.java`
  - Class XP owner: `ClassProgressionService.java`
  - Guild quest reward owner: `GuildQuestRewardService.java`
- Quests:
  - Live quest definitions: `quest/QuestManager.java`
  - Live data: `data/extremecraft/extremecraft_quests/*.json`
  - `data/extremecraft/quests/*.json` is metadata-only.
- Research:
  - Live gameplay owner: `research/ResearchManager.java`
  - `platform/data/loader/ResearchDataLoader.java` is a snapshot/debug mirror.
- Materials:
  - Live registration still starts in `machine/material/OreMaterialCatalog.java` plus `future/registry/**`
  - `data/extremecraft/materials/*.json` is metadata-only.
- World generation:
  - Live placement data: `data/extremecraft/worldgen/**` and `data/extremecraft/forge/**`
  - `data/extremecraft/world_generation/*.json` is metadata-only.

## Wrong-Edit Hazards

- Editing `machines/*.json` does not change active tech-machine ticks.
- Editing `quests/*.json` does not create live quests.
- Editing `abilities/*.json` changes active abilities, not spells.
- Editing `spells/*.json` changes spells, not active abilities with the same id.
- Editing `materials/*.json` or `world_generation/*.json` does not change live registries or placement by itself.
- Direct capability mutation is not the source-of-truth path; use `ProgressionFacade`, `SkillProgressionService`, `ClassProgressionService`, or `GuildQuestRewardService`.

## Legacy Kept Intentionally

- `machines/**` and `MachineRegistry` remain as legacy compatibility/runtime residue, mainly around the standalone pulverizer path.
- `classsystem/ClassRegistry` remains as an adapter for old callers.
- Trigger-bearing module ability files under `abilities/*.json` still load as a warned fallback until packs move to `module_abilities/*.json`.
