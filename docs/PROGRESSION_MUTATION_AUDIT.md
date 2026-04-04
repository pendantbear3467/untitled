# Progression Mutation Audit

## Scope

This audit covers the live Forge runtime under `src/main/java` and focuses on
progression-domain mutation authority during the ecosystem migration.

Primary rule:

- external gameplay, packet, and command surfaces mutate progression only through `ProgressionFacade`
- backing capabilities and mirrors remain storage-only or compatibility-only
- legacy write paths stay contained, deprecated, and unregistered

## Canonical Mutation Map

External mutation entrypoint:

- `com.extremecraft.progression.ProgressionFacade`

Facade-owned execution layers:

- player XP and level: `ProgressionMutationService`
- skill XP: `SkillProgressionService`
- class XP: `ClassProgressionService`
- quest reward application: `QuestRewardService`
- skill-tree node unlocks: `progression.skilltree.SkillTreeService`

Backing-state owners:

- canonical progression capability: `PlayerProgressData`
- skill XP backing state: `skills.PlayerSkillsCapability`
- legacy level mirror: `progression.level.PlayerLevelCapability`
- legacy stats mirror: `progression.capability.PlayerStatsCapability`

## Mutation Inventory

### Valid live mutation entrypoints

| Area | File(s) | Classification | Notes |
| --- | --- | --- | --- |
| Gameplay XP + quest progress | `progression/ProgressionEvents.java` | Valid | Uses `ProgressionFacade` for XP, quest progress, region discovery, and combat skill XP. |
| Quest reward claims | `progression/ProgressCommands.java` | Valid | Claims route through `ProgressionFacade.claimGuildQuestReward`. |
| Debug/admin XP/class/skill mutation | `command/ECDevCommands.java` | Valid | Debug commands now write through `ProgressionFacade`. |
| Skill-tree unlock packets | `network/packet/UpgradeStatPacket.java`, `progression/skilltree/UnlockSkillNodeC2S.java` | Valid | Both now route through `ProgressionFacade` before `SkillTreeService`. |
| Legacy XP compatibility wrappers | `progression/level/LevelService.java`, `progression/XPManager.java` | Valid but legacy | Public wrappers delegate back into `ProgressionFacade`; they no longer own mutation. |

### Invalid or legacy mutation paths

| Area | File(s) | Classification | Containment status |
| --- | --- | --- | --- |
| Prototype persistent-data progression stack | `game/ProgressionSystem.java` | Invalid legacy authority | Deprecated, not registered by `core/ExtremeCraft`, retained for reference only. |
| Old level gameplay XP hook | `progression/level/PlayerLevelGameplayEvents.java` | Invalid legacy authority | Not registered by bootstrap; still compiles as a disconnected compatibility artifact. |
| Direct backing-capability mutation methods | `PlayerSkillsCapability`, `PlayerLevelCapability`, `PlayerStatsCapability` | Invalid if called directly | Runtime guard logging now warns when writes bypass facade scope. |
| Mirror update APIs | `progression/capability/PlayerProgressCapabilityApi.java` | Legacy mirror surface | No live callers in the runtime audit; keep compatibility-only. |

## Corrected Architecture

Write flow:

1. External caller enters `ProgressionFacade`.
2. Facade delegates to a progression-owned execution layer.
3. Execution layer mutates canonical progression state first.
4. Migration mirrors sync afterward from progression-owned services only.

Read flow:

1. Cross-domain callers should prefer `ProgressionFacade.readAccess()`.
2. Backing capabilities remain readable for progression-internal code and compatibility UI.
3. Compat modules may query progression but must not write to canonical or mirror state directly.

## Enforcement Strategy

Static restrictions:

- `ProgressionService` is package-private.
- `ProgressionMutationService` is package-private.
- `PlayerProgressData` write methods are package-private.
- `QuestRewardService` is package-private.
- `SkillProgressionService` and `ClassProgressionService` expose public enums but keep write methods package-restricted.

Runtime guardrails:

- `ProgressionMutationAuthority` tracks whether a mutation is inside facade scope.
- Canonical services warn on direct bypass attempts.
- Backing capability writes in skill/level/stats mirrors warn when mutated outside facade scope.

Source tests:

- `ProgressionBoundarySourceTest` rejects new direct callers to lower-level mutation services and backing-state writes.

## Dependency Transition Plan

Current target for ownership removal:

- machines / reactors: query progression gates only; emit unlock hooks or bonus providers, not progression writes
- magic: award progression only by calling `ProgressionFacade`; keep magic-specific state outside progression
- skills package: storage and definition backend only; progression owns mutation policy
- compat modules: adapters and read models only; no direct capability mutation

Replacement pattern:

- integration systems produce events, hooks, or bonus payloads
- progression evaluates the event and applies the mutation
- compat modules consume read-only progression state through `ProgressionReadAccess`

## Next Safe Extraction Steps

1. Move `game/ProgressionSystem` into a dedicated `legacy` package once source references remain at zero.
2. Extract facade-facing progression contracts into `extremecraft-progression` first; keep mirror types private until adapters are ready.
3. Convert machine/reactor/magic callers to depend on `ProgressionReadAccess` and explicit progression hooks rather than direct capability types.
4. Add a follow-up CI validator that fails when new non-progression packages call lower-level mutation services directly.
