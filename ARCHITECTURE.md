# ARCHITECTURE

This document summarizes the runtime architecture used by the Forge mod in `src/main/java` and `src/main/resources`.

## Game Engine Systems

Primary bootstrap: `com.extremecraft.core.ExtremeCraft`.

Core responsibilities:

- Registers blocks, items, menus, entities, recipes, sounds.
- Boots API/provider wiring (`ExtremeCraftAPI`, provider impl, module loader).
- Registers gameplay systems to Forge event bus.
- Registers client-only overlays/screens/renderers during client setup.

Design intent:

- Keep startup orchestration centralized in `ExtremeCraft`.
- Keep gameplay domain logic in dedicated subsystem packages.

## Machine Systems

Primary components:

- `MachineRegistry`: runtime machine and machine-recipe catalog.
- `MachineProcessingLogic`: per-tick processing execution for machine block entities.
- `MachineBlockEntity` + machine menu/screen classes for inventory/UI state.

Data flow:

1. Machine definitions/recipes are loaded from datapack resources.
2. Registries expose immutable snapshots for hot-path reads.
3. Tick logic resolves recipe, consumes energy, mutates inventories on completion.

Why it exists:

- Keeps machine balancing/data in JSON.
- Keeps execution semantics in one deterministic server-side loop.

## Ability System

Primary components:

- `AbilityRegistry`: data definitions + runtime executors.
- `AbilityEngine`: validation, target resolution, mana/cooldown checks, execution.
- `AbilityExecutor` and effect types for data-defined behavior.

Data flow:

1. Ability JSON is loaded into definitions.
2. Runtime abilities can be registered programmatically.
3. Cast requests pass through cooldown/mana/class/target validation.
4. Effects execute and sync services update client state.

Why it exists:

- Supports both built-in Java abilities and datapack-driven abilities.
- Keeps gating/security checks in one execution path.

## Packet Security

Primary component: `com.extremecraft.network.security.ServerPacketLimiter`.

Responsibilities:

- Per-player, per-key request limiting.
- Tick delta throttling.
- Fixed request-window caps.

Why it exists:

- Prevents abusive packet spam from overwhelming server handlers.
- Lets each handler apply tailored limits without duplicating limiter logic.

## Tick Scheduling

Primary components:

- `DwServerTicker` (server-side offhand break replay loop).
- `ServerDeferredWorkEvents`/`ServerDeferredWorkQueue` (deferred task processing with tick budgets).
- `PlayerRuntimeCleanupEvents` (state cleanup).

Why it exists:

- Moves delayed gameplay actions onto deterministic server ticks.
- Avoids doing expensive or timing-sensitive work directly inside packet/event callbacks.

## Datapack Loading

Bootstrap: `PlatformDataLoaderBootstrap.registerAll()`.

Loader pattern:

- Domain loader per content type (`MachineDataLoader`, `AbilityDataLoader`, `SkillTreeDataLoader`, etc.).
- Loaders parse JSON and populate domain registries.
- Validation services run after load to report schema/reference issues.

Runtime behavior:

- Reload listeners are registered on Forge event bus.
- Content is reloaded from datapacks/resource packs without code changes.
- Systems read from registries, not directly from disk.

## Canonical Ownership (Alpha Hardening)

The following ownership boundaries are now canonical and should be treated as stable extension points.

- Network authority: `com.extremecraft.network.ModNetwork` is the only packet registration owner.
- Legacy network facade: `com.extremecraft.net.DwNetwork` is compatibility-only and must not register packets.
- Progression mutation authority: `com.extremecraft.progression.ProgressionMutationService` is the runtime mutation entrypoint for XP/level updates.

### Migration Shims

These shims exist to preserve save compatibility and keep older systems operational while overlap is being retired.

- `DwNetwork.CH` remains as a deprecated channel alias to `ModNetwork.CHANNEL`.
- `ProgressionMutationService` updates canonical progression data first, then mirrors legacy level/stats capabilities.
- Legacy callers may continue using `LevelService.grantXp`/`setLevel`; those methods now route through the canonical mutation facade.

### Contributor Guidance

- New gameplay C2S packets: register only in `ModNetwork.init()`.
- New XP or level-granting gameplay events: call `ProgressionMutationService`, not direct capability services.
- Legacy progression capability classes remain supported but should be treated as mirror/read-compat layers until fully retired.

## Codebase Map

- Gameplay systems: `src/main/java/com/extremecraft/**`
- Datapack/runtime content: `src/main/resources/data/extremecraft/**`
- Assets (models, lang, textures): `src/main/resources/assets/extremecraft/**`
- Python/content tooling: `tools/`, `assetstudio.py`, `generate_assets.py`, `main.py`

## Finishing-Pass Safe Zones

- Validation/reporting improvements in existing validator services.
- Asset hygiene fixes (missing model/lang/texture wiring).
- Loader and debug message clarity.
- Contributor docs and examples.

## Shared Implementation Contract

For first-major-release implementation boundaries and required systems, use:

- `docs/SHARED_IMPLEMENTATION_CONTRACT.md`

For Python/PyQt tooling evolution into full embedded ExtremeCraft Studio, use:

- `docs/EXTREMECRAFT_STUDIO_SHARED_CONTRACT.md`

This contract is the canonical baseline for server authority, progression-domain separation, safety caps, and scope discipline.

## Senior-Scope Zones (Do Not Touch in Routine PRs)

- Cross-system architecture rewrites.
- Packet architecture redesign.
- Registry ownership or namespace migration.
- Progression/combat/machine authority changes.
- Any save-format or migration behavior that can impact existing worlds.

