# Repository Folder Guide

This guide explains what each top-level folder in this repository is for,
what belongs there, and whether it is core ExtremeCraft mod runtime code,
auxiliary tooling, documentation/examples, or generated output.

## Classification Legend

- Core Runtime: loaded by Minecraft Forge at runtime.
- Tooling: developer utilities, generators, Asset Studio, and helpers.
- Documentation/Examples: reference material and example projects.
- Generated/Transient: build products, caches, and local runtime data.
- Placeholder/Future: intentional top-level structure for future modularization.

## Top-Level Folder Map

### `src/` (Core Runtime + Legacy Nested-Project Residue)

Primary runtime source for the mod.

- `src/main/java/com/extremecraft/**`: gameplay/runtime logic.
- `src/main/resources/assets/extremecraft/**`: textures, models, language files.
- `src/main/resources/data/extremecraft/**`: built-in datapack definitions.
- `src/main/resources/META-INF/`: Forge metadata (`mods.toml`) and module descriptors.
- `src/.gradle/`, `src/build/`, `src/settings.gradle`: older nested-project/build residue and not ownership signals for current runtime work.

What to put here:

- code that must ship inside the mod jar
- runtime assets and data-driven gameplay definitions

What not to put here:

- Python scripts, generators, CI helpers, temporary exports

### `api/` (Core Runtime Integration Surface)

Integration-facing Java API module for external usage.

Typical contents:

- API interfaces and extension points
- provider and registration contracts consumed by integrations/addons

### `tools/` (Tooling)

Developer tooling zone.

- `tools/python/`: Asset Studio, SDK, compiler, plugin runtime.
- `tools/scripts/`: compatibility launchers (`assetstudio.py`, `main.py`, generator wrappers).
- `tools/asset_generator/`: modular asset generation implementation.
- `tools/generate_assets.py`, `tools/generate_gui_assets.py`: maintained generator entrypoints.
- `tools/content_completion.py`, `tools/stabilize_content.py`: validation/maintenance scripts used by checks.

### `docs/` (Documentation/Examples)

Detailed reference guides, architecture notes, generated audits, and schemas.

Use this folder for:

- system architecture explainers
- development guides
- gameplay/data format specs
- validation and report artifacts intended for contributor consumption

### `examples/` (Documentation/Examples)

Example templates and walkthrough-oriented artifacts.

- `examples/extremecraft-addon-template/`: addon/module reference template.

### `datapacks/` (Tooling + Content Authoring Workspace)

Contributor-facing external datapack authoring area.

Built-in shipped datapack content remains in `src/main/resources/data/extremecraft`.

### `scripts/` (Tooling)

Repository-level automation helper scripts for maintainers.

Note: launcher wrappers are intentionally located in `tools/scripts`, not here.

### `mod/` (Placeholder/Future)

Orientation anchor documenting runtime mod concerns and future modularization intent.

### `client/`, `core/`, `gameplay/`, `magic/`, `tech/`, `worldgen/` (Placeholder/Future + Modularization Scaffolds)

Top-level domains reserved for possible future modular decomposition.

Current active runtime implementation remains centered under `src/`. Treat these folders as orientation scaffolds and build placeholders unless/until source sets are explicitly rewired.

### `workspace/` (Tooling Scratch Space)

Local scratch/intermediate workspace for tooling outputs.

Do not treat this as canonical runtime source.

Nuance:

- `workspace/build/`, `workspace/generated/`, and `workspace/exports/` may contain intentionally versioned current-state snapshots and packaging output for inspection.
- `workspace/.studio/autosave/`, `workspace/.studio/logs/`, and `workspace/.studio/recovery/` are local state, not canonical project content.

### `tests/` (Tooling Validation)

Current tests are primarily Python tooling validation suites (Asset Studio and related flows).

### `gradle/` (Build Infrastructure)

Gradle wrapper and build infrastructure files required for Java/Forge builds.

### `run/` (Generated/Transient Runtime)

Local development runtime output for launched client/server sessions.

Contains local state like world data, server settings, logs, crash reports.

### `build/`, `bin/` (Generated/Transient With A Few Preserved Reports)

Build output trees and compiled artifacts.

Do not hand-edit these folders.

Nuance:

- `build/extremecraft-validation-report.txt` and `build/reports/**` are intentionally preserved high-signal artifacts.
- The rest of these trees are build output, not gameplay source.

### `.gradle*`, `.venv/`, `__pycache__/` (Generated/Transient)

Local cache/environment folders.

## Root Files That Define Behavior

- `build.gradle`: main build configuration.
- `settings.gradle`: module includes and build layout.
- `src/settings.gradle`: legacy nested-project artifact and not the current build root.
- `pyproject.toml`: Python package metadata for Asset Studio package distribution.
- `README.md`: main onboarding entrypoint.
- `ARCHITECTURE.md`: high-level architecture overview.
- `docs/CANONICAL_OWNERSHIP_MAP.md`: edit-first ownership map for live gameplay changes.
- `API.md`: public API orientation.
- `DATAPACK_FORMAT.md`: data contract and schema guidance.

## Placement Rules

1. Runtime Forge-loaded code/assets/data belongs in `src/main/**`.
2. Tooling and generators belong under `tools/**`.
3. Launcher wrappers belong in `tools/scripts/**`.
4. Contributor docs belong in `docs/**` or the closest folder-specific README.
5. Build outputs and local run artifacts are not source-of-truth and must not be manually curated as project source.
