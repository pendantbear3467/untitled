# Repository Folder Guide

This guide defines what each top-level folder currently means in the host workspace and extraction readiness.

## Classification Legend

- **host runtime**: loaded by Forge runtime from the host repository; not module-extracted yet
- **real module**: included Gradle module with clear ownership and stable boundaries
- **included subproject**: extracted Gradle module, compiled in bridge mode or independently
- **candidate future module**: extraction target, authority-hardened but not split-ready yet
- **tooling**: developer utilities, generators, validation helpers
- **docs/support**: contributor process content and workspace support
- **generated/local-only**: build/runtime/editor local state (properly gitignored)

## Top-Level Folder Classification & Extraction Status

### Extraction-Ready (First Wave) ✅

- **`api/`** - real module
  - Public integration contracts and provider abstraction
  - Zero runtime-internal dependencies
  - Ready for independent repository creation now
  - Build: `api:compileJava` passes cleanly

- **`core/`** - real module
  - Shared contracts, registries, and extracted core services
  - Internal package: `com.extremecraft.ecosystem.core.*`
  - Dependencies: `:api` only (no host runtime imports)
  - Ready for independent repository creation now
  - Build: `core:compileJava` passes cleanly

### Included Subproject (Bridge Mode, Repo Deferred) ⚠️

- **`progression/`** - included subproject
  - Progression authority source root
  - Authority-hardened with read-only bridge contracts in `:core`
  - Current blocker: still imports host-owned runtime packages for quest, network, machine, reactor
  - Status: included in `settings.gradle`, bridge-mode classpath dependencies configured
  - Next step: reduce direct host coupling before independent repo extraction
  - Build: `progression:compileJava` passes via bridge-mode compilation
  - Reference: `docs/PROGRESSION_SUBPROJECT_READINESS.md`

### Host Runtime (Not Yet Extracted)

- **`platform/src/main/java`** - host runtime
  - Forge bootstrap, registry wiring, packet channels, event binding, and loader registration
  - Cannot be extracted until `:progression` is decoupled from host runtime packages
  - Keep in host repository in this pass

- **`src/main/java`** - host runtime
  - Transitional gameplay monolith
  - Contains domain logic for quest, ability, machine, spell, skill, entity, magic systems
  - Not a standalone repo target; systems will be extracted incrementally
  - Keep in host repository in this pass

- **`src/main/resources`** - host runtime resources
  - Runtime assets: `assets/extremecraft/**` (models, textures, lang, sounds)
  - Built-in datapack content: `data/extremecraft/**`
  - Keep in host repository

### Support & Tooling (Not Repository Targets)

- **`tools/`** - tooling
  - Python/asset gen/validation helpers and CLI wrappers
  - Generators, editors, schema validators
  - Optional long-term repo candidate after maturity cleanup
  - Keep in host repository in this pass

- **`docs/`** - documentation and reports
  - ARCHITECTURE, CONTRIBUTING, API usage guides
  - Authority maps, migration plans, validation reports
  - Keep in host repository

- **`scripts/`** - support scripts
  - Contributor workflow helpers
  - Keep in host repository

- **`tests/`** - validation tests
  - Boundary tests (`*SourceTest.java`)
  - Keep in host repository

- **`examples/`** - addon/template projects
  - Keep in host repository

- **`datapacks/`** - contributor external content workspace
  - Keep in host repository

- **`workspace/`** - IDE/tooling state and snapshots
  - Not for distributed workflow
  - Keep in host repository

## Generated/Local-Only Areas (All Properly Excluded)

- `build/`, `bin/`, `run/`, `logs/` — Gradle build outputs, runtime directory, JVM logs
- `.gradle/`, `.gradle-user*/` — Gradle daemon state and user settings
- `.venv/`, `__pycache__/` — Python virtual environment and compiled modules
- `.tmp-*`, `workspace/.studio/*` — Temporary and editor state files

These are already gitignored and not architectural signals.

## Build & Runtime Truth (Current)

**Gradle includes:**
```
include 'api', 'core', 'progression'
```

**Active host runtime source roots (compiled into mod JAR):**
- `platform/src/main/java`
- `progression/src/main/java` (temporary, via root sourceSets config)
- `src/main/java`
- `src/main/resources`

**Module build outputs (published as artifacts):**
- `:api` — compileOnly consumers, public API contracts
- `:core` — implementation scope, shared core services
- `:progression` — bridge-mode, imported by root via classpath bridge

## Contributor Placement Rules

1. **Host runtime code** (gameplay logic, Forge integration, network handlers) → `src/main/java` or `platform/src/main/java`
2. **Shared contracts** (only data types, interfaces, read-only facades) → `core/src/main/java`
3. **Integration API** (addon hooks, provider abstractions) → `api/src/main/java`
4. **Progression authority** (state mutation, unlock logic, XP gates) → `progression/src/main/java`
   - But: progression still depends on host-owned runtime packages (quest, network, etc.) until coupling is reduced
5. **Do not** artificially split folders still tightly coupled

## Next Steps to Enable Repository Extraction

Standard next step for `api` and `core`: Create separate repository templates using these modules immediately.

For `progression` before independent repo split:
1. Reduce direct imports into host-owned runtime packages
2. Replace with read-only bridge contracts in `:core`
3. Consider new module `:gameplay` for non-progression host runtime
4. Once progression compiles independently, extract to its own repository

Reference for progression prep: `docs/PROGRESSION_SUBPROJECT_READINESS.md`
