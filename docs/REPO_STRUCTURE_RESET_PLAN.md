# Repository Structure Reset Plan

Status: active migration audit

This document is the current inventory and cleanup plan for the workspace root. It records what is active, transitional, legacy, generated, or local-only so contributors do not mistake scaffolding for runtime ownership.

## Current Root Inventory

### Curated / keep

- `README.md` - keep; root onboarding entry point.
- `CONTRIBUTING.md` - keep for now; contributor guardrails.
- `ARCHITECTURE.md` - keep for now; root pointer/summary.
- `API.md` - keep; public API orientation.
- `DATAPACK_FORMAT.md` - keep; data contract reference.
- `build.gradle` - keep; root build wiring.
- `settings.gradle` - keep; workspace project includes.
- `gradle.properties` - keep; build properties.
- `gradlew`, `gradlew.bat`, `gradle/` - keep; wrapper infrastructure.
- `.github/` - keep; repository automation.
- `api/` - active Gradle module; public API contracts.
- `core/` - active Gradle module after normalization; shared contracts and extracted core library.
- `platform/` - active top-level source root; Forge bootstrap, hook glue, and migration adapters.
- `progression/` - active top-level source root; progression authority and read/write facades.
- `src/` - transitional runtime monolith; gameplay systems not yet safely split.
- `tools/` - active tooling; Python generators and editors.
- `docs/` - active documentation and audit home.
- `scripts/` - tracked helper scripts.
- `tests/` - Python/tooling validation.
- `datapacks/` - contributor datapack workspace.
- `examples/` - examples and templates.
- `workspace/` - tooling scratch/snapshot area; inspect only when needed.

### Generated / transient / local-only

- `build/` - generated Gradle output and reports.
- `bin/` - generated output.
- `logs/` - runtime logs.
- `run/` - local dev runtime state.
- `.gradle/` - Gradle cache.
- `.gradle-user/`, `.gradle-user-codex/`, `.gradle-user-temp/` - local Gradle user state.
- `.tmp-studio-smoke/`, `.tmp-tests/` - temporary validation artifacts.
- `.venv/` - local Python environment.
- `__pycache__/`, `asset_studio/__pycache__/`, `extremecraft_asset_studio.egg-info/` - Python cache/packaging residue.
- `.idea/`, `.vscode/` - editor state/configuration.

### Legacy / placeholder / merged away

- `mod/` - old orientation anchor; replaced by `platform/` and root docs.
- `client/` - placeholder modularization scaffold; no longer a root owner.
- `gameplay/` - placeholder modularization scaffold; runtime remains in `src/`.
- `magic/` - placeholder modularization scaffold; runtime remains in `src/`.
- `tech/` - placeholder modularization scaffold; runtime remains in `src/`.
- `worldgen/` - placeholder modularization scaffold; runtime remains in `src/`.
- `extremecraft-core/` - merged into `core/`.
- `extremecraft-magic-compat/` - placeholder scaffold; not a live owner.
- `extremecraft-tech-compat/` - placeholder scaffold; not a live owner.
- `extremecraft-skills-compat/` - placeholder scaffold; not a live owner.

### External tool package residue

- `asset_studio/` - root-level Python package residue; the maintained package code lives under `tools/python/asset_studio/`.

### Root command catalog

- `commands to use` - tracked command reference; planned to move under `docs/` so the root stays quieter.

## Old Root Tree Snapshot

```text
README.md
ARCHITECTURE.md
API.md
DATAPACK_FORMAT.md
CONTRIBUTING.md
CODE_OF_CONDUCT.md
build.gradle
settings.gradle
gradle.properties
gradlew
gradlew.bat
api/
asset_studio/
bin/
build/
client/
config/
core/
datapacks/
docs/
examples/
extremecraft-core/
extremecraft-magic-compat/
extremecraft-progression/
extremecraft-skills-compat/
extremecraft-tech-compat/
gameplay/
gradle/
logs/
magic/
mod/
run/
scripts/
src/
tech/
tests/
tools/
workspace/
worldgen/
.gradle/
.gradle-user/
.gradle-user-codex/
.gradle-user-temp/
.idea/
.tmp-studio-smoke/
.tmp-tests/
.venv/
.vscode/
__pycache__/
```

## Proposed Curated Root Tree

```text
README.md
CONTRIBUTING.md
ARCHITECTURE.md
API.md
DATAPACK_FORMAT.md
build.gradle
settings.gradle
gradle.properties
gradlew
gradlew.bat
.gitignore
.github/
api/
core/
platform/
src/
tools/
docs/
scripts/
tests/
datapacks/
examples/
workspace/
gradle/
```

## Notes On Ownership

- `core/` is the canonical home for the extracted shared contracts module that used to live under `extremecraft-core/`.
- `platform/` is the canonical top-level source root for Forge bootstrap, hook glue, and transitional adapters.
- `progression/` is the canonical top-level source root for progression authority.
- `src/` remains the transitional runtime source root for gameplay systems that are not yet safe to split.
- The removed scaffolds were placeholders only; they did not own live runtime behavior.
- Generated output, logs, caches, and local environments are not architectural roots and should stay ignored.

## Follow-Up Cuts

1. Move the root command catalog into `docs/` and leave a short pointer in the root if needed.
2. Continue extracting only safe runtime seams out of `src/` after verifying ownership and compile impact.
3. Keep the progression authority boundary intact while any future migration happens.
