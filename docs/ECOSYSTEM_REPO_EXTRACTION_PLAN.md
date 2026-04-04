# Ecosystem Repo Extraction Plan

## Current Answer

Create now:

1. `extremecraft-api`
2. `extremecraft-core`

Do not create yet:

1. `extremecraft-progression`
2. tech/magic/machine/world/domain repos

## Why `api` And `core` Are Ready Now

- `api` is a real extracted Gradle module with only API/JDK contracts.
- `core` is a real extracted Gradle module with JDK-only contracts/bridges.
- The host workspace now consumes both through explicit project dependencies.

## Why `progression` Is Not Repo-Ready Yet

- Host still compiles `progression/src/main/java` directly.
- `:progression` still compiles against host output/classpath as a bridge.
- Direct imports to host runtime packages still remain.
- Client/UI, packet, classsystem, skills, and machine/material paths are still
  host-coupled.

## Extraction Order

1. `extremecraft-api`
   - lowest-risk first-wave repo
2. `extremecraft-core`
   - stable shared contract layer
3. `extremecraft-progression`
   - only after host bridge removal and direct host imports are reduced
4. future domain repos
   - only after real ownership/build boundaries exist

## Current Blocking Items

1. `platform` still owns Forge/bootstrap/runtime glue and must stay host-owned.
2. `src` still owns most gameplay implementation.
3. `progression` still needs host runtime classes for compile/runtime behavior.
4. Some host-owned code still uses package prefixes that resemble extracted
   module ownership (`com.extremecraft.ecosystem.core` under `src/main/java`).
5. No tech/magic/world/machine split currently has a build-enforced module
   boundary.

## Safe Next Steps

1. Keep `api` and `core` clean and publishable.
2. Continue narrow progression read-only seam extraction.
3. Remove progression's host compile bridge only after imports/tests prove it.
4. Refuse future repo claims for domain splits until the build graph matches the
   docs.
