# Ecosystem Mutation Audit

## Scope

Audit target: progression mutations (player XP, skill XP, class XP, quest progress/completion, unlocks, class switching, progression sync).

Goal: ensure canonical write authority routes through `ProgressionFacade` and progression-owned services.

## Mutation Entry Points Found

## Valid canonical write paths

- `ProgressionFacade.grantPlayerXp` -> `ProgressionMutationService`
- `ProgressionFacade.grantCombatSkillXp` -> `SkillProgressionService`
- `ProgressionFacade.grantClassXp` -> `ClassProgressionService`
- `ProgressionFacade.claimGuildQuestReward` -> `QuestRewardService`
- `ProgressionFacade.addQuestProgress`
- `ProgressionFacade.markQuestCompleted`
- `ProgressionFacade.grantUnlock` / `grantUnlocks`
- `ProgressionFacade.unlockClass`
- `ProgressionFacade.switchClass`

## Previously risky or fragmented paths (now fixed)

1. Non-combat skill XP writes from gameplay events
- Previous state:
  - exploration/crafting/mining wrote skill XP with non-combat sources.
- Fix:
  - removed non-combat `grantSkillXp` calls from progression event hooks.
  - `SkillProgressionService` now enforces combat-only (plus debug command override).

2. Quest reward authority naming mismatch
- Previous state:
  - canonical flow existed under `GuildQuestRewardService` name only.
- Fix:
  - introduced `QuestRewardService` as canonical owner.
  - `GuildQuestRewardService` now deprecated adapter delegating to `QuestRewardService`.

3. Fragmented progression sync calls
- Previous state:
  - direct `ProgressionService.flushDirty` / mirror sync spread across classes.
- Fix:
  - introduced `ProgressionSyncService` and routed key call sites through it.

4. Direct point consumption in skill-node unlock path
- Previous state:
  - `PlayerStatsService` consumed/returned player skill points by directly mutating `PlayerProgressData`.
- Fix:
  - switched to `ProgressionFacade.consumePlayerSkillPoints` and `ProgressionFacade.addPlayerSkillPoints`.

## Guardrails Added

1. Read vs write boundary
- Added Core read interface:
  - `com.extremecraft.ecosystem.core.progression.ProgressionReadAccess`
- Added package-restricted write contract:
  - `com.extremecraft.progression.InternalProgressionWriteAccess`
- `ProgressionFacade` now exposes read access and routes writes through internal write access implementation.

2. Illegal mutation warnings
- `ProgressionService` now logs warning when mutation methods are called from outside `com.extremecraft.progression.*`:
  - `[ProgressionGuard] External progression mutation attempt ...`

## Current Rule Enforcement Status

- Skill XP policy:
  - live writes accepted only for `Source.COMBAT` and `Source.DEBUG_COMMAND`.
  - other sources rejected with guard warning.
- Class XP policy:
  - accepted only for `Source.GUILD_QUEST` and `Source.DEBUG_COMMAND`.

## Remaining Known Legacy Risks

- `com.extremecraft.game.ProgressionSystem` remains as deprecated legacy prototype path (historical fallback), with separate persistent-data writes.
- No broad delete performed; legacy containment remains intentional until final retirement phase.

## Next Safe Extraction Targets

1. Move progression read call sites in non-progression packages to `ProgressionFacade.readAccess()` where practical.
2. Convert remaining internal direct flush/sync calls to `ProgressionSyncService` for consistency.
3. Add automated static check to flag direct calls to `ProgressionService` from outside progression package.
4. Begin adapter-first containment for machine/magic/skills ownership transitions with feature-flagged authority disable paths.
