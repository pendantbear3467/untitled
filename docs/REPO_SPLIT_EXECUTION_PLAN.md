# Repository Split Execution Plan

Status: executable migration checklist for the current codebase state.

This plan is intentionally conservative. It reflects what is clean enough now and what must remain host-owned.

## A. Repositories To Create Now

### 1) extremecraft-api

- Source folder: `api/`
- Why now:
  - already an included Gradle module
  - clear integration contract surface
  - low coupling risk relative to runtime bootstrap
- Downstream consumers:
  - host runtime repo
  - extremecraft-core
  - future compat/addon repos

### 2) extremecraft-core

- Source folder: `core/`
- Why now:
  - already an included Gradle module
  - explicit shared contracts ownership
  - current dependency direction is stable enough for extraction
- Downstream consumers:
  - host runtime repo
  - progression (next as subproject)
  - future compat/addon repos

## B. Keep In Host Repo For Now

- `platform/`
  - keeps Forge/bootstrap/registration/packet/event glue that is still cross-domain
- `src/`
  - shrinking monolith bucket still owning many live runtime systems
- `progression/`
  - authority-hardened but not yet isolated from host runtime imports
- `tools/`
  - useful candidate later, but still mixed with local/generated workflows
- `datapacks/`, `tests/`, `docs/`, `scripts/`, `examples/`, `workspace/`
  - support/workspace content, not standalone repos for this migration phase

## C. Support-Only Folders (Not Standalone Repos)

- docs: documentation and architecture/audit records
- scripts: helper scripts
- tests: host/runtime and tooling validation
- examples: templates/examples
- workspace: local scratch, snapshots, and tooling state
- datapacks: contributor content workspace tied to host runtime paths

## D. Phased Split Order

### Phase 1 (safe now)

- create `extremecraft-api`
- create `extremecraft-core`

### Phase 2 (next pass)

- promote `progression/` to true included Gradle subproject inside host
- reduce direct imports from progression into host-only runtime packages
- only then evaluate `extremecraft-progression` repo extraction

### Phase 3 (long-term optional)

- evaluate `tools` split after CLI/API hardening
- evaluate domain splits (tech/magic/world) only when ownership and dependencies are proven by code/tests

## E. Concrete Move Order

1. Baseline and freeze
- create a pre-split tag (example: `split-baseline-YYYYMMDD`)
- verify baseline:
  - `./gradlew.bat compileJava`
  - `./gradlew.bat test --tests com.extremecraft.gameplay.GameplayAuthoritySourceTest --tests com.extremecraft.gameplay.ProgressionBoundarySourceTest`

2. Extract `api`
- create `extremecraft-api` repo from `api/`
- wire host via composite build or published artifact
- rerun compile + boundary tests

3. Extract `core`
- create `extremecraft-core` repo from `core/`
- wire host similarly
- rerun compile + boundary tests

4. Hold host-owned runtime boundaries
- keep `platform/`, `src/`, and `progression/` in host repo
- do not split bootstrap, packet registration, event glue, or host-coupled runtime code

5. Progression preparation pass (no repo split)
- keep progression authority boundaries unchanged (`ProgressionFacade`, `ProgressionReadAccess`)
- reduce obvious host-only coupling in small safe slices
- convert progression to an included Gradle subproject once compile boundaries are satisfiable

## F. Verification Commands

- `./gradlew.bat compileJava`
- `./gradlew.bat test --tests com.extremecraft.gameplay.GameplayAuthoritySourceTest --tests com.extremecraft.gameplay.ProgressionBoundarySourceTest`
- optional validator:
  - `./gradlew.bat validateExtremeCraft`

## G. Rollback Points

- rollback point 1: before extracting `api`
- rollback point 2: after `api` extraction, before `core`
- rollback point 3: after `core` extraction, before progression subproject work

If any step fails compile/boundary tests, reset to previous rollback point and re-apply only the last known-good extraction.

## H. Current Risk Flags

- progression still imports host-owned runtime packages directly
- platform bootstrap class is still highly coupled across runtime domains
- src remains canonical owner for many gameplay paths
- splitting any of those early risks circular dependencies and brittle startup wiring
