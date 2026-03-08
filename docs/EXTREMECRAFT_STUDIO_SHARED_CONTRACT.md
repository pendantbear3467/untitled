# ExtremeCraft Studio Shared Contract

Status: Active architecture contract for Python/PyQt Studio evolution.
Applies to: `tools/python/asset_studio/**`, launch wrappers, editor integrations, and related tooling.

## 1) Purpose

ExtremeCraft Studio is the embedded desktop development environment for ExtremeCraft mod content.

It extends the current Asset Studio foundations into one modular shell with embedded editors and guarded execution services.

Primary product outcome:

- code editing
- progression graph editing
- GUI editing
- model editing
- asset/data validation
- debug/log viewing
- build/run orchestration
- workspace/project management
- contextual hover help
- crash-safe background task execution

## 2) Foundations That Must Be Preserved

The following existing systems are required extension points and must not be casually replaced:

- Asset Studio shell and CLI entrypoint (`asset_studio.main` and wrappers such as `assetstudio.py`)
- workspace/project manager (`asset_studio.workspace.workspace_manager`)
- preview renderer (`asset_studio.gui.preview_renderer` and preview backend)
- plugin loader and marketplace metadata (`asset_studio.plugins.*`)
- validation pipeline (`asset_studio.validation.validator`)
- registry scanning and related reports
- progression/skilltree backend (`asset_studio.skilltree.*`)
- graph framework and skill tree designer
- standalone launcher paths already used by contributors
- generator/build/export tooling and existing commands

Rule of implementation:

- prefer wrappers/adapters/composition over rewrites
- preserve behavior first, then layer capability

## 3) Non-Negotiable Rules

1. Preserve working Asset Studio and skilltree CLI flows.
2. Keep skilltree backend ownership under `asset_studio.skilltree`.
3. Do not rewrite stable modules when extension wrappers are viable.
4. Keep the studio modular; no giant monolithic rewrite.
5. One failed panel/tool/plugin must not crash the full app.
6. Isolate risky work behind guarded handlers, worker jobs, or process services.
7. Project/schema changes must be migration-aware and reversible.
8. UI must include discoverable help text and contextual explanations.
9. Existing launch paths must remain valid, even as thin wrappers.
10. Design must allow future editors (quests, tech trees, spells, classes, others).

## 4) Ownership And Responsibility Split

This contract formalizes a dual-ownership model.

Codex owns:

- architecture contracts and service boundaries
- backend systems and integration contracts
- task/process safety model and worker orchestration
- persistence, migration, and compatibility policies
- reusable editor engines and shared domain services
- fault isolation and crash-recovery contracts

Copilot owns:

- GUI/UX shell behavior and docking layout implementation
- menus, toolbars, status surfaces, inspectors, and discoverability
- tooltip/help authoring in UI surfaces
- embedded editor interaction polish
- user workflow clarity across panels and tabs

Both parties must preserve compatibility constraints and avoid cross-ownership coupling.

## 5) Required Studio Modules

ExtremeCraft Studio must be implemented as composable modules hosted by a common shell.

- `studio.shell`: main window, layout state, panel lifecycle, panel hosting
- `studio.code`: embedded code editor integration and language tooling bridge
- `studio.progression`: progression editor wrapper over `asset_studio.skilltree`
- `studio.gui`: GUI editor module
- `studio.model`: model editor module
- `studio.preview`: preview integration over renderer backends
- `studio.debug`: logs, diagnostics, and task stream views
- `studio.buildrun`: build/run orchestration services and run targets
- `studio.help`: contextual docs, hover help, quick explanations
- `studio.recovery`: autosave, crash guard, session restore

Module rule:

- each module exposes a narrow service contract and panel factory
- modules register to shell through a registry, not direct hard wiring

## 6) Integration Contract Layer

All panels/tools integrate through stable contracts.

Required contracts:

- `IStudioModule`: id, display name, capability flags, lifecycle hooks
- `IPanelProvider`: create panel widget, dispose hook, state serializer
- `IWorkspaceService`: current workspace context, open/save/switch operations
- `ITaskService`: enqueue/cancel/observe tasks with structured events
- `ILogService`: leveled logs and per-module channels
- `IValidationService`: run validations and produce normalized report payloads
- `IMigrationService`: inspect, migrate, and record schema/project migrations
- `IHelpService`: tooltip/help lookup by context key and symbol

Contract rule:

- modules depend on interfaces, never concrete peer modules

## 7) Failure Isolation And Safety Model

Studio runtime must degrade safely when failures occur.

Mandatory protections:

- panel creation guarded by exception boundary; fallback panel shown on failure
- plugin load failures captured and reported without app termination
- each background job wrapped in safe runner with timeout/cancel support
- process-based isolation for risky external operations when practical
- task failures emitted as structured events to debug/log studio
- shell remains responsive while jobs execute

Recovery requirements:

- autosave checkpoints for mutable editors
- crash recovery index with last-known-good session state
- safe startup mode that disables failing optional modules/plugins

## 8) Background Tasks And Process Contracts

Risky and long-running work must not execute in the UI thread.

Task service requirements:

- run jobs using worker thread pool and process workers where needed
- represent every task with id, origin module, status, progress, and result
- enforce bounded queue behavior with backpressure policy
- include cancellation hooks and cooperative cancellation checks
- stream stdout/stderr/log records to `studio.debug`

Examples that must run through task service:

- validation scans
- registry scans/diffs
- generation and compile/export actions
- build/run launch operations
- heavy preview/model processing tasks

## 9) Persistence, Project Format, And Migration

Project/workspace state changes must be migration-aware.

Required policy:

- maintain explicit version fields for studio layout and project metadata
- migrations are additive when possible and logged in migration history
- keep compatibility shims for prior formats where feasible
- provide dry-run migration report before destructive changes
- preserve existing skilltree file compatibility and CLI behavior

Minimum migration artifacts:

- migration id
- source and target schema version
- reversible steps or explicit non-reversible marker
- post-migration validation result

## 10) Launch And Compatibility Contract

Existing launch paths remain canonical and must continue to work:

- `assetstudio.py`
- `main.py`
- `assetstudio --gui`
- existing CLI command surfaces, especially `skilltree` commands

Compatibility rule:

- if launch internals evolve, old entrypoints become thin wrappers to the new shell bootstrap
- command syntax and return codes remain stable unless explicitly versioned

## 11) UX And Help Contract

The studio must explain itself in product surfaces.

Required UX features:

- tooltips and inline help text for inspector fields and complex actions
- context-sensitive help references for active panel/tool
- command/search entry for quickly discovering tools and actions
- clear error banners with next-step suggestions
- non-blocking notifications for task completion/failure

## 12) Implementation Phasing

Phase 1: Shell hardening and contracts

- introduce contract interfaces and module registry
- wrap current panels as modules without behavior regression
- add guarded panel load boundaries and structured logging

Phase 2: Task and safety infrastructure

- central task service with worker/process execution
- debug/log studio panel backed by task/log streams
- recovery service with autosave/session restore wiring

Phase 3: Embedded tool expansion

- embedded code editor module
- GUI/model/build-run studios integrated via contracts
- help/context system and discoverability improvements

Phase 4: Migration and extension maturity

- migration service for workspace/project schema changes
- plugin and module compatibility matrix and diagnostics
- extension points for future quest/tech/spell/class editors

## 13) Acceptance Gate

A Studio change is merge-ready only when all apply:

- preserves Asset Studio CLI and skilltree ownership/behavior
- adds capability through modules/contracts, not monolithic rewrites
- isolates failures and keeps shell responsive
- routes risky tasks through guarded task/process services
- includes migration notes for schema/project changes
- keeps existing launch paths functional
- updates help/tooltips for new workflows
- includes validation/test evidence or documented test constraints

## 14) Canonical References

- `docs/ASSET_STUDIO.md`
- `docs/ARCHITECTURE.md`
- `docs/SHARED_IMPLEMENTATION_CONTRACT.md`
- `ARCHITECTURE.md`

When this contract and implementation details diverge, update this contract and the related architecture docs in the same change set.