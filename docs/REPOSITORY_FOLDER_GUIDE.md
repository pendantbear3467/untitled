# Repository Folder Guide

This guide defines what each top-level folder currently means in the host workspace.

## Classification Legend

- host runtime: loaded by Forge runtime from the host repository
- real module: included Gradle module with clear ownership
- candidate future module: extraction target, not split-ready yet
- tooling: developer utilities and generators
- docs/support: contributor and process support content
- generated/local-only: build/runtime/editor local state

## Top-Level Folder Classification

- `api/` - real module
  - public integration contracts
  - first-wave repo candidate (create now)

- `core/` - real module
  - shared contracts and extracted core services
  - first-wave repo candidate (create now)

- `platform/` - host runtime
  - Forge/bootstrap/registration/packet/event wiring and glue
  - must remain host-owned in this pass

- `progression/` - candidate future module
  - progression authority source root
  - subproject-next, repo-later

- `src/` - host runtime
  - transitional monolith runtime owner for systems not yet extracted
  - not a standalone repo target

- `tools/` - tooling
  - generators, editors, validation helpers
  - optional long-term repo candidate after maturity cleanup

- `docs/` - docs/support
- `scripts/` - docs/support tooling wrappers
- `tests/` - host/tooling validation
- `examples/` - docs/support templates
- `datapacks/` - contributor content workspace
- `workspace/` - tooling/workspace support and snapshots

## Generated/Local-Only Areas

- `build/`, `bin/`, `run/`, `logs/`
- `.gradle/`, `.gradle-user*/`, `.venv/`
- `.tmp-*`, `__pycache__/`
- `workspace/.studio/autosave/`, `workspace/.studio/logs/`, `workspace/.studio/recovery/`

These are not architectural ownership signals.

## Build And Runtime Truth

- Included Gradle modules: `api`, `core`
- Active host runtime roots:
  - `platform/src/main/java`
  - `progression/src/main/java`
  - `src/main/java`
  - `src/main/resources`

## Placement Rules

1. Runtime gameplay/Forge-loaded code belongs in host runtime roots unless explicitly extracted.
2. Shared contracts belong in `api` and `core` only.
3. Progression mutation writes must continue to flow through `ProgressionFacade`.
4. Do not create fake module boundaries by splitting folders that are still tightly coupled.
