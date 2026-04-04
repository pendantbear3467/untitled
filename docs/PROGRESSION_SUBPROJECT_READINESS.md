# Progression Subproject Readiness

Status: progression is now an included Gradle subproject in bridge mode in the host repo; repo extraction remains deferred.

This report does not claim progression is repo-ready yet.

## Current Decision

- progression is authority-hardened and now included as project `:progression`
- progression is subproject now, repo later
- progression should not be split into its own repository in this pass

## Pre-Conversion Blockers (Audited)

Before including `:progression`, the following blockers were confirmed and remain relevant for repo extraction:

1. Direct imports from progression into host-owned runtime packages (`quest`, `network`, `machine`, `reactor`, and other host domains).
2. Path-sensitive source-policy tests scanning both `src/main/java` and `progression/src/main/java`.
3. Host bootstrap/wiring in `platform` still orchestrates progression-adjacent lifecycle and events.
4. Runtime behavior depends on host-coupled classpaths and cannot yet assume isolated progression publication.

These blockers do not prevent included-subproject conversion, but they do block clean external repo extraction.

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

## Included-Subproject Conversion Completed (Bridge Mode)

1. `progression/build.gradle` is active.
2. `settings.gradle` includes `:progression`.
3. Root runtime still compiles `progression/src/main/java` to preserve behavior while host coupling remains.
4. Progression subproject compiles against host output/classpath as a temporary bridge.

## Small Coupling Reduction Completed

- Extracted shared constants contract to `core/src/main/java/com/extremecraft/ecosystem/core/ECConstants.java`.
- Updated progression capability/stage/skilltree event surfaces to consume core constants instead of host-only `com.extremecraft.core.ECConstants`.
- Kept `platform/src/main/java/com/extremecraft/core/ECConstants.java` as a compatibility adapter to avoid runtime behavior changes.

## Additional Read-Only Seam Completed

- Added `core/src/main/java/com/extremecraft/ecosystem/core/progression/ProgressionRuntimeFlags.java` as a tiny shared progression runtime flag holder.
- Updated `ProgressionGate`, `UnlockAccessService`, and `UnlockRuleLoader` to read the debug-bypass flag from core instead of host config.
- Kept `Config` as the host-side publisher of that flag via config reload events, so runtime behavior remains unchanged.
- Remaining direct host coupling still includes progression reads into host quest, skill, network, machine, and reactor/runtime packages.

## Required Cleanup Before Repo Split

1. Progression compiles independently as a Gradle subproject.
2. Host runtime depends on progression through explicit module dependency.
3. Progression no longer depends on host-only implementation packages directly.
4. Boundary tests pass with progression as a real module boundary.

## Safe Next Step

Convert progression into an included Gradle subproject only after coupling-reduction commits are complete and compile-test validation remains green.
