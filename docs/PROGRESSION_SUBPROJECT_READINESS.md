# Progression Subproject Readiness

Status: prep report for converting `progression/` into a true included Gradle subproject in the host repo.

This report does not claim progression is repo-ready yet.

## Current Decision

- progression is authority-hardened and stable as a source root
- progression is subproject-next, repo-later
- progression should not be split into its own repository in this pass

## Confirmed Authority Guardrails

- write boundary: `ProgressionFacade`
- read boundary: `ProgressionReadAccess`
- source-policy tests:
  - `GameplayAuthoritySourceTest`
  - `ProgressionBoundarySourceTest`

## Direct Host Runtime Coupling (Representative)

The progression source root still imports host-owned runtime packages including but not limited to:

- quest system (`com.extremecraft.quest.*`)
- network and packets (`com.extremecraft.network.*`)
- machine and reactor paths (`com.extremecraft.machine.*`, `com.extremecraft.reactor.*`)
- class/ability/magic/runtime packages still owned by host source roots

These imports are expected at this stage and block immediate clean repo extraction.

## Required Cleanup Before True Subproject Include

1. Identify and isolate host-only runtime dependencies behind stable contracts.
2. Move cross-domain reads/writes onto `api`/`core` contracts where practical.
3. Keep progression mutation policy internal and facade-driven.
4. Verify no direct bypass of progression mutation authority is reintroduced.

## Required Cleanup Before Repo Split

1. Progression compiles independently as a Gradle subproject.
2. Host runtime depends on progression through explicit module dependency.
3. Progression no longer depends on host-only implementation packages directly.
4. Boundary tests pass with progression as a real module boundary.

## Safe Next Step

Convert progression into an included Gradle subproject only after coupling-reduction commits are complete and compile-test validation remains green.
