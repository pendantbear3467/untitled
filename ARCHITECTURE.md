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

- `machine/core/MachineCatalog`: canonical tech machine definition owner.
- `machine/core/TechMachineBlockEntity`: active multi-machine block entity runtime.
- `machine/core/MachineTickScheduler`: bounded server tick owner for tech machines.
- `machine/core/MachineProcessingService` + `machine/core/MachineRecipeService`: live processing and recipe resolution path.
- `future/registry/*`: live tech machine block/item/block-entity/menu registration chain.

Data flow:

1. `MachineCatalog` and `future/registry/*` define which tech machines exist at runtime.
2. Vanilla recipe manager loads `data/extremecraft/recipes/machine_processing/**`.
3. `MachineRecipeService` resolves recipes, `MachineProcessingService` executes them, and `MachineTickScheduler` owns the active server tick loop.

Why it exists:

- Keeps active machine execution in one deterministic server-side loop.
- Separates live machine runtime from metadata-only mirrors such as `data/extremecraft/machines`.

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
- Keeps unlock/class gating and security checks in one execution path.

Related live gates:

- `AbilityEngine` enforces generic ability unlock/class access.
- `ClassAbilityService` enforces class-ability unlock/class access.
- `SpellExecutor` enforces spell unlock/class access and compiles spell JSON into runtime ability payloads.

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
- Gameplay systems read from canonical runtime registries/services, not directly from disk.
- `platform/data/loader/*` contains metadata, validation, and snapshot mirrors; those loaders are not automatically the live gameplay owner for the similarly named content folder.

## Canonical Ownership (Alpha Hardening)

The following ownership boundaries are now canonical and should be treated as stable extension points.

- Network authority: `com.extremecraft.network.ModNetwork` is the only packet registration owner.
- Legacy network facade: `com.extremecraft.net.DwNetwork` is compatibility-only and must not register packets.
- Progression mutation authority: `com.extremecraft.progression.ProgressionMutationService` is the runtime mutation entrypoint for XP/level updates.
- Full per-domain ownership map: `docs/CANONICAL_OWNERSHIP_MAP.md`

### Migration Shims

These shims exist to preserve save compatibility and keep older systems operational while overlap is being retired.

- `DwNetwork.CH` remains as a deprecated channel alias to `ModNetwork.CHANNEL`.
- `ProgressionMutationService` updates canonical progression data first, then mirrors legacy level/stats capabilities.
- Legacy callers may continue using `LevelService.grantXp`/`setLevel`; those methods now route through the canonical mutation facade.
- `ClassRegistry` still loads `data/extremecraft/classes`, but live class reads should resolve through `ClassAccessResolver`, which now prefers canonical `progression.classsystem.data` definitions.
- `PlayerStatsGameplayEvents` remains live for resource regen and applied mining/break-speed stat effects, but gameplay XP/quest progression writes are owned by `ProgressionEvents`.
- Stage state is server-authoritative in `StageManager` and mirrored to clients only through `RuntimeSyncService` + `SyncStageStateS2CPacket`.
- The live `modular_drill` item now routes through the canonical modular runtime; the older `item.module` package remains legacy-only.

### Contributor Guidance

- New gameplay C2S packets: register only in `ModNetwork.init()`.
- New XP or level-granting gameplay events: call `ProgressionMutationService`, not direct capability services.
- Legacy progression capability classes remain supported but should be treated as mirror/read-compat layers until fully retired.

## Codebase Map

For a top-level folder-by-folder reference, see `docs/REPOSITORY_FOLDER_GUIDE.md`.
For a complete docs/README navigation index, see `docs/DOCUMENTATION_INDEX.md`.

- Gameplay systems: `src/main/java/com/extremecraft/**`
- Datapack/runtime content: `src/main/resources/data/extremecraft/**`
- Assets (models, lang, textures): `src/main/resources/assets/extremecraft/**`
- Python/content tooling: `tools/`, `tools/scripts/assetstudio.py`, `tools/scripts/generate_assets.py`, `tools/scripts/main.py`

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
