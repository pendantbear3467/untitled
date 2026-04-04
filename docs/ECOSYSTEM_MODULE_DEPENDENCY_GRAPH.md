# Ecosystem Module Dependency Graph

## Current Intended Graph

```mermaid
graph TD
  API[api] --> CORE[core]
  PROGRESSION[progression] --> API
  PROGRESSION --> CORE
  HOST[host runtime: platform + src + progression bridge source root] --> API
  HOST --> CORE
```

## Root Runtime Module

- Root runtime module currently depends on:
  - `:api`
  - `:core`
- Root still contains launch glue, migration adapters, progression bridge compilation, and not-yet-extracted gameplay ownership.

## Staged Graph Targets

### Stage 1 (now)

- Keep `api` and `core` as real modules and first-wave repo candidates.
- Keep `platform`, `progression`, and `src` host-owned.

### Stage 2 (next pass)

- Reduce progression coupling to host-only runtime packages.
- Keep repo split deferred until progression no longer directly depends on host-only packages.

### Stage 3 (later optional)

- Evaluate tools/domain repo splits only after ownership and dependency constraints are enforced by code and build boundaries.

## Forbidden Dependency Directions

1. `core` -> any compat module (`tech/magic/skills`): forbidden.
2. `core` -> a progression authority module: forbidden.
3. Compat module -> compat module: forbidden by default.
4. Any module -> root runtime module: forbidden.
5. Circular dependencies between any ecosystem modules: forbidden.

## Allowed Optional Runtime Relationships

- Progression remains authoritative while the monolith runtime owns the active gameplay systems.
- `platform/` is the adapter/home for bootstrap and compatibility glue.

## Publication-Oriented Relationship Model

- `core`: base required dependency.
- `platform`: host-owned runtime bootstrap and glue over core + host gameplay.
- future compat modules: optional addons depending on core + runtime contracts.
