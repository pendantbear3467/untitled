# Repository Folder Guide

Use `docs/WORKSPACE_REAL_STATE_AUDIT.md` for the full matrix. This file is the
short contributor version.

## Current Meaning Of The Main Top-Level Folders

- `api/`: real module, create-now repo candidate
- `core/`: real module, create-now repo candidate
- `progression/`: included subproject in bridge mode, repo later
- `platform/`: host runtime bootstrap/wiring, stays host-owned now
- `src/`: host runtime gameplay owner, stays host-owned now
- `tools/`, `scripts/`, `tests/`, `config/`, `gradle/`: tooling/support
- `docs/`, `examples/`, `datapacks/`: docs/support
- `workspace/`: tooling workspace plus generated local outputs

## Generated / Local-Only Areas

- root `build/`, `bin/`, `run/`, `logs/`
- `.gradle/`, `.gradle-user*/`, `.venv/`, `.tmp-*`, `__pycache__/`
- `workspace/build/`, `workspace/generated/`, `workspace/exports/`
- `workspace/.studio/autosave/`, `workspace/.studio/logs/`, `workspace/.studio/recovery/`

These are not module boundaries or source-of-truth runtime owners.

## Build And Runtime Truth

- Included Gradle projects: `api`, `core`, `progression`
- Real extracted modules already consumed by host: `api`, `core`
- Host runtime roots:
  - `src/main/java`
  - `platform/src/main/java`
  - `progression/src/main/java` (bridge mode)
  - `src/main/resources`
  - `platform/src/main/resources`

## Placement Rules

1. Runtime gameplay/Forge-loaded code belongs in host runtime roots unless it is already extracted into `api` or `core`.
2. Shared contracts belong in `api` and `core` only.
3. Progression mutation writes must continue to flow through `ProgressionFacade`.
4. Do not treat future tech/magic/world/machine splits as real modules until build and source ownership enforce them.
