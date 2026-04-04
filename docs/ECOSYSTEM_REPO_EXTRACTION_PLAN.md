# Ecosystem Repo Extraction Plan

## Objective

Transition from one repository with internal module separation to independently extractable repositories:

1. `extremecraft-core`
2. `extremecraft-progression`
3. `extremecraft-tech-compat`
4. `extremecraft-magic-compat`
5. `extremecraft-skills-compat`

## Recommended Split Order

1. `extremecraft-core`
- Rationale: lowest gameplay authority risk; shared contracts can be stabilized first.

2. `extremecraft-progression`
- Rationale: must lock progression authority boundaries before compat extraction.

3. `extremecraft-tech-compat`
- Rationale: highest likely dependency complexity; extract after core/progression contracts stabilize.

4. `extremecraft-magic-compat`
- Rationale: similar pattern to tech compat, usually fewer machine-side couplings.

5. `extremecraft-skills-compat`
- Rationale: depends on final external skill backend transition timing.

## Current Blocking Items

1. Root runtime source set still owns most gameplay implementations.
2. Several core/progression classes remain in root package paths due staged migration safety.
3. Legacy adapters still active in root (`platform/*`, old compatibility layers).
4. Domain systems (machine/magic/skills) still partially monolith-owned.
5. Some module contracts still use runtime-specific assumptions not yet fully abstracted.

## Adapter Deletion Strategy After Split

Delete only after all checks pass:

1. equivalent module-owned implementation exists
2. no reference from root runtime or compat modules
3. no migration fallback requirement
4. integration tests pass with adapter removed

Initial post-split deletion candidates:

- deprecated legacy progression adapters after progression repo fully consumes canonical services
- root ecosystem package markers once all ownership classes are moved

## Root Runtime Reduction Plan

Stage 1 (in progress):
- move stable shared contracts/services to module source sets
- keep root as consumer

Stage 2:
- move canonical progression implementation ownership to progression module
- keep root bridge classes only

Stage 3:
- migrate domain compat ownership into respective compat modules
- root becomes launch/wiring layer only

## CurseForge Relationship Proposal

- `ExtremeCraft Core`:
  - foundational required dependency for ecosystem modules

- `ExtremeCraft Progression`:
  - depends on Core
  - authoritative progression gameplay layer

- `ExtremeCraft Tech Compat`:
  - optional addon
  - depends on Core + Progression + external tech mods

- `ExtremeCraft Magic Compat`:
  - optional addon
  - depends on Core + Progression + Ars Nouveau

- `ExtremeCraft Skills Compat`:
  - optional addon
  - depends on Core + Progression + Pufferfish Skills

## Safe Next Extraction Targets

1. Move additional `com.extremecraft.ecosystem.core.*` classes (compat/network contracts) into `extremecraft-core` once their direct runtime couplings are split into adapter + contract.
2. Move progression service implementations into `extremecraft-progression` in dependency-safe slices.
3. Introduce module-local tests for dependency direction and forbidden import checks.
4. Add CI check for cycle detection and forbidden project dependency edges.
