# Progression Mutation Audit

This document records the active write surface for progression during the ecosystem migration.

The target boundary is:

`ProgressionFacade` -> internal progression services -> backing capabilities/mirrors

Compat modules should read through `ProgressionReadAccess` and should not own progression mutation.

## Canonical Write Boundary

- Public write entrypoint: `src/main/java/com/extremecraft/progression/ProgressionFacade.java`
- Public read contract: `src/main/java/com/extremecraft/ecosystem/core/progression/ProgressionReadAccess.java`
- Internal player XP/level owner: `src/main/java/com/extremecraft/progression/ProgressionMutationService.java`
- Internal capability mutation owner: `src/main/java/com/extremecraft/progression/ProgressionService.java`
- Internal skill XP owner: `src/main/java/com/extremecraft/progression/SkillProgressionService.java`
- Internal class XP owner: `src/main/java/com/extremecraft/progression/ClassProgressionService.java`
- Internal quest reward owner: `src/main/java/com/extremecraft/progression/QuestRewardService.java`

## Mutation Audit

| Entry point | Domain | Classification | Current state |
| --- | --- | --- | --- |
| `ProgressionEvents.onKill` | player XP, combat skill XP, quest progress | valid | Uses `ProgressionFacade.grantPlayerXp`, `grantCombatSkillXp`, and `addQuestProgress` |
| `ProgressionEvents.onCraft` | player XP, quest progress | valid | Uses `ProgressionFacade.grantPlayerXp` and `addQuestProgress` |
| `ProgressionEvents.onBlockBreak` | player XP, quest progress | valid | Uses `ProgressionFacade.grantPlayerXp` and `addQuestProgress` |
| `ProgressionEvents.onPlayerTick` region discovery | discovered regions, player XP, quest progress | valid | Uses `ProgressionFacade.discoverRegion` and follow-up facade writes |
| `QuestRewardService.claimQuestReward` | quest claim, unlocks, player XP, class XP, stage grants | valid | Orchestrates quest rewards through `ProgressionFacade`; does not mutate capabilities directly |
| `SkillTreeService` / `PlayerStatsService.tryUnlockSkillNode` | player skill-point spend | valid | Routes skill-point consumption/refund through `ProgressionFacade` |
| `ProgressCommands` | admin level/class/quest commands | valid | Level set and quest claim route through `ProgressionFacade` |
| `ECDevCommands` | debug XP, skill XP, class XP, level set | valid | Debug mutation calls now route through `ProgressionFacade` |
| `LevelService.grantXp` / `LevelService.setLevel` | legacy player-level API | legacy adapter | Delegates back into `ProgressionFacade`; no longer owns gameplay authority |
| `XPManager.grant` | legacy XP wrapper | legacy adapter | Delegates back into `ProgressionFacade` |
| `GuildQuestRewardService.claimQuestReward` | legacy quest reward wrapper | legacy adapter | Delegates to `QuestRewardService` |
| `ProgressionService.*` mutators | internal capability write helpers | internal only | Package-restricted and guarded; callers should come through `ProgressionFacade` |
| `ProgressionMutationService.*` | internal XP/level mutation helpers | internal only | Package-restricted and guarded; callers should come through `ProgressionFacade` |
| `PlayerProgressData` mutators | backing state | internal only | Write methods are package-restricted; collection views are read-only |
| `PlayerSkillsCapability` mutators | backing state | guarded backing state | Still public because of package layout, but now warn when bypassing `SkillProgressionService` |

## Invalid Or Risky Paths Removed From Authority

- `ProgressCommands` no longer set levels through `ProgressionMutationService` directly.
- `ECDevCommands` no longer treat `LevelService` as the primary XP/level owner.
- `ProgressionEvents` no longer call progression discovery writes through `ProgressionService` directly.
- `PlayerStatsService` no longer consumes/refunds player skill points by mutating `PlayerProgressData` directly.
- `PlayerProgressData` no longer exposes mutable collection getters as an edit surface.
- Live skill XP policy remains combat-only plus admin/debug override; non-combat enum sources are explicitly rejected until a new canonical rule is introduced.

## Enforcement Strategy

- `ProgressionFacade` is the only intended public mutation API.
- `ProgressionReadAccess` is the intended cross-module read API.
- `ProgressionMutationAuthority` warns when internal mutation services are called without entering through the facade.
- `ProgressionService` and `ProgressionMutationService` are no longer public architecture owners.
- `PlayerProgressData` mutation methods are package-restricted and its exposed collections are unmodifiable.
- `LevelService`, `XPManager`, and `GuildQuestRewardService` are explicitly deprecated compatibility adapters.

## Dependency Transition Guidance

The following domains should not own progression mutation:

- Machines: read progression gates/unlocks, but write through `ProgressionFacade` only when a machine flow is explicitly designed to award progression.
- Reactors: expose telemetry or hooks, but do not mutate progression state directly.
- Magic: cast systems may query progression/class/unlock state; they should award progression only through `ProgressionFacade`.
- Skills: compat/stat modules should treat `PlayerSkillsCapability` as backing state and route combat/debug skill XP through `SkillProgressionService` via the facade.

## Next Safe Extraction Steps

1. Replace direct `ProgressApi` reads in compat/integration modules with injected `ProgressionReadAccess` usage.
2. Move legacy mirror update APIs such as `PlayerProgressCapabilityApi.update*` behind deprecation warnings or adapter-only packages.
3. Split primary-stat upgrades out of `PlayerStatsService` into an explicit progression-facing upgrade service if that domain is kept.
4. Extract a dedicated skill-state write contract so `PlayerSkillsCapability` no longer needs public mutators.
5. Migrate compat modules (`extremecraft-tech-compat`, `extremecraft-magic-compat`, `extremecraft-skills-compat`) to depend on the read contract plus facade hooks only.
