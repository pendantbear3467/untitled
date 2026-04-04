# ExtremeCraft Ecosystem Migration Plan

## Scope

This document starts the migration from the current monolith runtime in `src/main/**` toward the target ecosystem:

- `extremecraft-core`
- `extremecraft-progression`
- `extremecraft-tech-compat`
- `extremecraft-magic-compat`
- `extremecraft-skills-compat`

Primary rule: progression truth remains authoritative in progression services only. Compat modules may query progression and register bonuses/hooks, but must not mutate progression state directly.

## Current Baseline

- Build is already multi-project in Gradle, but active runtime logic is still centralized under `src/main/**`.
- Existing top-level modules (`core`, `gameplay`, `tech`, `magic`, `worldgen`, `client`) are largely scaffolds.
- Progression authority services already exist (`ProgressionFacade`, `ProgressionMutationService`, `SkillProgressionService`, `ClassProgressionService`, `GuildQuestRewardService`), but legacy mirrors and duplicate package areas still coexist.

## Classification Matrix (Phase 1)

Legend:
- `KEEP`: keep where it is for now.
- `MOVE`: move to target module/package during extraction phase.
- `WRAP`: keep live location, add adapter/bridge to target module boundary.
- `DEPRECATE`: retain temporarily, mark legacy usage path.
- `DELETE`: safe candidate only after reference sweep.

### Required major groups

| Group | Current primary locations | Target owner | Classification | Notes |
| --- | --- | --- | --- | --- |
| core | `com.extremecraft.core`, `com.extremecraft.foundation`, `com.extremecraft.platform` (selected parts) | Core | MOVE + WRAP | Core constants, runtime foundation events, optional compat contracts should converge in Core. |
| api | `api/src/main/java/com/extremecraft/api/**` | Core | KEEP | Public contracts already isolated in `api` module; keep stable and consume from Core/other modules. |
| network | `com.extremecraft.network`, `com.extremecraft.net`, `com.extremecraft.network.sync` | Core + Progression | WRAP then MOVE | Shared channel/bootstrap belongs in Core; progression sync packet ownership should move to Progression package slice. |
| progression | `com.extremecraft.progression/**` | Progression | KEEP + HARDEN | Canonical authority already present; continue consolidating all progression mutations here. |
| classsystem | `com.extremecraft.classsystem/**`, `com.extremecraft.progression.classsystem/**` | Progression | MOVE + DEPRECATE old | Keep canonical class definitions/services under progression namespace; deprecate duplicate/legacy classsystem readers. |
| quest | `com.extremecraft.quest/**`, `com.extremecraft.game.Quest*` | Progression | KEEP canonical + DEPRECATE legacy | `quest/**` + guild reward path are canonical; old `game` quest structures are legacy. |
| skilltree | `com.extremecraft.progression.skilltree/**`, `com.extremecraft.skills/**` | Progression (+ Skills Compat bridge) | KEEP + WRAP | Skill tree unlock authority stays in Progression; external skill backend integration goes through Skills Compat. |
| magic | `com.extremecraft.magic/**`, `mana/**` | Magic Compat | MOVE + WRAP | Shift from custom magic authority toward compat bridge ownership; keep runtime adapter layer during migration. |
| machine | `com.extremecraft.machine/**`, `com.extremecraft.machines/**`, `com.extremecraft.energy/**`, `com.extremecraft.reactor/**` | Tech Compat | MOVE + DEPRECATE custom authority | Tech systems should become integration/bridge-heavy; deprecated custom ownership where external mods become source of truth. |
| modules | `com.extremecraft.modules/**`, `com.extremecraft.item.module/**` | Core (generic contracts) + Tech/Magic/Skills compat implementations | WRAP + DEPRECATE legacy item.module | Keep modular runtime hooks but move domain-specific effects to compat modules over time. |
| worldgen | `com.extremecraft.worldgen/**`, `com.extremecraft.world/**` | Core (shared contracts) + dedicated worldgen module later | KEEP | Not in current 5-target split; keep stable and isolate from progression mutations. |
| gui | `com.extremecraft.client.gui/**`, `com.extremecraft.gui/**` | Progression (progression UI) + compat module UIs by domain | MOVE later | Progression-facing screens should move to progression module; machine/magic/skill overlays to corresponding compat modules. |
| registry | `com.extremecraft.registry/**`, `future/registry/**`, data registries under `platform/data/registry/**` | Core (common registries) + domain modules | WRAP then MOVE | Shared registry abstractions belong in Core; domain registries move to module owners. |
| compat | `com.extremecraft.platform.hook/**`, `OptionalCompatHooks`, mixed compat in domain packages | Core + specific compat modules | MOVE + WRAP | Core owns stable compat hook contracts; implementation per compat module. |
| config | `com.extremecraft.config/**` | Core | KEEP + WRAP | Shared config helpers and common toggles belong in Core with adapters for legacy access. |

### Additional high-value groups discovered during audit

| Group | Current primary locations | Target owner | Classification | Notes |
| --- | --- | --- | --- | --- |
| game (legacy) | `com.extremecraft.game/**` | Progression (if still needed) | DEPRECATE | Appears duplicated by progression/quest/class services. |
| research | `com.extremecraft.research/**` | Progression or Tech Compat (decision pending) | WRAP | Needs ownership decision based on whether research remains progression authority or compat bridge. |
| ability | `com.extremecraft.ability/**` | Progression (authority checks) + compat adapters | KEEP + WRAP | Runtime casting remains; progression gating should continue through canonical unlock/stage services. |
| platform.data loader/registry | `com.extremecraft.platform.data/**` | Core contracts + domain-owned implementations | WRAP then MOVE | Preserve as metadata/sync mirror until each domain migrates canonical ownership. |
| future | `com.extremecraft.future/**` | Core contracts + Tech Compat impls | WRAP + DEPRECATE parts | Contains useful interfaces but also legacy placeholders. |

## Deletion Safety Classification

- Safe now: none (no broad delete in this pass).
- Legacy/deprecate-first candidates:
  - `com.extremecraft.game.*` duplicates of quest/progression/class concepts.
  - `com.extremecraft.machines.*` where superseded by `machine.core` or external compat path.
  - `com.extremecraft.item.module.*` where superseded by `modules.*` runtime path.
- Delete only after:
  - no references in Java and resources
  - no packet registration/handler dependency
  - no adapter fallback path

## Phase Plan

## Phase 2 (this pass: started)

Completed in this pass:
- Added new Gradle module scaffolds:
  - `extremecraft-core`
  - `extremecraft-progression`
  - `extremecraft-tech-compat`
  - `extremecraft-magic-compat`
  - `extremecraft-skills-compat`
- Added placeholder build files and READMEs for each module.
- Kept runtime source location unchanged to minimize compile risk.

## Phase 3 (this pass: started)

Initial extraction target:
- Shared service contracts/registry adapter boundary in a new core namespace.
- Shared compat hook boundary in a new core namespace.
- Keep existing callers stable via delegating adapters in old package locations.

## Phase 4 target (progression authority hardening)

Canonical progression surface (target names):
- `ProgressionFacade` (already present)
- `SkillProgressionService` (already present; combat-first XP enforcement)
- `ClassProgressionService` (already present; guild quest path enforcement)
- `QuestRewardService` (currently `GuildQuestRewardService`; alias/refactor planned)
- `ProgressionSyncService` (to isolate progression-owned sync from generic runtime sync)

Rules to enforce:
- Skill XP from gameplay/combat paths only.
- Class XP from guild quest completion path only.
- Non-progression modules can query progression but must not mutate progression capability state directly.

## Phase 5 target (external dependency ownership)

Migration intent:
- Mekanism / Extreme Reactors / Ars Nouveau / Pufferfish Skills become domain authorities.
- ExtremeCraft domain modules become integration, bridge, bonus, and unlock-policy layers.

Immediate action pattern:
- Deactivate hard authority behavior behind feature flags.
- Keep compatibility adapters while external integrations are introduced.
- Route gating decisions through progression unlock interfaces instead of custom subsystem-owned state.

## Phase 6 target (safe deletion)

Deletion checklist:
1. No remaining references.
2. No adapter/shim fallback dependency.
3. Replacement boundary is live.
4. Fallback migration path documented.

If any check fails: move to `legacy` package and annotate deprecated.

## Manual Follow-up Required

1. Decide final ownership of `research/**` (Progression vs Tech Compat).
2. Define exact package move order for `client/gui` by domain.
3. Add CI checks that reject direct progression mutation outside progression services.
4. Add a "single progression authority" architecture test using static reference scanning.
