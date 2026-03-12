# Progression

Status: `LIVE_RUNTIME`

This folder owns the canonical player progression state, XP/level mutation path, stage gating, quest reward claiming, and progression-facing gameplay events.

Runtime-critical files:
- `ProgressionMutationService` is the canonical XP/level mutation entrypoint.
- `ProgressionFacade` is the safe cross-system bridge for XP, skill XP, class XP, and guild quest rewards.
- `ProgressionGate` is the canonical machine/recipe stage gate and stage-grant route.
- `ProgressionEvents` applies live XP/quest/skill hooks during gameplay.
- `StageDataLoader` and `unlock/UnlockRuleLoader` load stage and unlock metadata used by the live gate.

Metadata-only or low-authority files:
- `ProgressionRegistry` and `ProgressionDefinition` are curve metadata and are not the stage owner.

Consumed by:
- Machines via `ProgressionGate`
- Quest rewards via `GuildQuestRewardService`
- Class, skill, and UI sync services

Legacy or adapter paths:
- `level/LevelService` and `PlayerStatsService` are compatibility mirrors kept synchronized by `ProgressionMutationService`.
- Canonical player skill points live in `PlayerProgressData`; `PlayerStatsCapability.skillPoints` is only a synchronized compatibility mirror for skill-tree/runtime/UI consumers that still read stats capability state.
- `game/ProgressionSystem` is legacy and not the live owner.

Safe future additions:
1. Add new stage unlocks in `src/main/resources/data/extremecraft/progression/`.
2. Route new XP grants through `ProgressionFacade` or `ProgressionMutationService`.
3. Route new machine/recipe gating through `ProgressionGate`.

Common mistakes:
- Adding hardcoded stage checks in unrelated gameplay classes.
- Mutating legacy level or stats capabilities directly.
- Introducing a new formal stage name without migrating the whole ladder.
