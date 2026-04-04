# Progression Subproject Readiness

Status: included Gradle subproject in bridge mode. Repo extraction remains
deferred.

This document does not claim progression is repo-ready.

## Current Decision

- `progression` stays in the host repo
- `:progression` remains useful as an included project for ownership/build
  visibility
- repo split is blocked until the host bridge and remaining host imports are
  removed

## Guardrails That Must Stay Intact

- write boundary: `ProgressionFacade`
- read boundary: `ProgressionReadAccess`
- source-policy tests:
  - `GameplayAuthoritySourceTest`
  - `ProgressionBoundarySourceTest`

## What Improved In This Pass

- Host quest model/catalog imports were reduced again through the core quest
  descriptor/catalog bridge.
- Progression reward claim and command flows now consume read-only
  `ProgressionQuestDescriptor` contracts instead of host `QuestDefinition` /
  `QuestManager` types directly.
- `progression` README/docs now correctly describe bridge mode instead of
  pretending the repo split is already done.

## Remaining Direct Host Runtime Coupling

Representative direct imports still present in `progression/src/main/java`:

- `com.extremecraft.network.*`
- `com.extremecraft.classsystem.*`
- `com.extremecraft.skills.*`
- `com.extremecraft.ability.*`
- `com.extremecraft.magic.*`
- `com.extremecraft.machine.*`
- `com.extremecraft.materials.*`
- host client GUI classes

These are the reasons repo extraction is still deferred.

## Why The Bridge Still Exists

1. Root runtime still compiles `progression/src/main/java`.
2. `progression/build.gradle` still uses host output/classpath as `compileOnly`.
3. Path-sensitive boundary tests still scan both `src/main/java` and
   `progression/src/main/java`.
4. Platform/bootstrap behavior in `platform` still wires progression-adjacent
   lifecycle and runtime services.

## Required Cleanup Before Repo Split

1. Remove direct host package imports from progression.
2. Replace host compile-classpath bridge with explicit module dependencies only.
3. Stop compiling progression sources directly in the host runtime.
4. Keep authority/boundary tests green after each seam reduction.

## Safe Next Step

Keep `progression` as a bridge-mode included project and continue only narrow,
read-only seam reductions until the host bridge can be removed cleanly.
