# Workspace Real State Audit

Status: audited against the checked-in workspace state on 2026-04-04.

This document records what the build actually does now, not what a later repo
split might do.

## Decision Snapshot

- Create separate repos now: `api`, `core`
- Keep as included subproject now, repo later: `progression`
- Keep host-owned in this pass: `platform/src`, `src`
- Do not overstate readiness: tech, magic, machine, world, and similar domain
  splits are not real modules yet

## Actual Gradle / Build Truth

- Included Gradle projects: `:api`, `:core`, `:progression`
- Host runtime source roots:
  - default `src/main/java`
  - `platform/src/main/java`
  - `progression/src/main/java` (bridge mode)
- Host runtime resources:
  - default `src/main/resources`
  - `platform/src/main/resources`
- Host project dependencies:
  - `implementation project(':api')`
  - `implementation project(':core')`
- Bridge exception:
  - `:progression` also compiles against host output/classpath with `compileOnly`
    until remaining host-runtime imports are removed

This means `api` and `core` are real modules today. `progression` is a real
included project for ownership/build visibility, but it is not yet an isolated
module boundary because the host still compiles the same sources.

## Top-Level Folder Classification

| Folder | Classification | Notes |
| --- | --- | --- |
| `.github/` | docs/support | repo automation and templates |
| `.gradle/` | generated/local-only | local Gradle cache |
| `.tmp-tests/` | generated/local-only | local temporary test output |
| `.vscode/` | generated/local-only | editor-local settings only |
| `api/` | real module | create repo now |
| `bin/` | generated/local-only | generated output |
| `build/` | generated/local-only | Gradle output; not architecture |
| `config/` | tooling | build/lint configuration |
| `core/` | real module | create repo now |
| `datapacks/` | docs/support | contributor datapack workspace |
| `docs/` | docs/support | design, audits, reports |
| `examples/` | docs/support | templates and examples |
| `gradle/` | tooling | Gradle wrapper/support files |
| `platform/` | host runtime | host bootstrap, registration, wiring |
| `progression/` | candidate future module | included subproject in bridge mode; repo later |
| `scripts/` | tooling | workflow wrappers |
| `src/` | host runtime | remaining gameplay/runtime owner |
| `tests/` | tooling | Python/tooling validation suite |
| `tools/` | tooling | generators, validators, studio/tooling code |
| `workspace/` | tooling | tooling inputs plus generated local outputs |

## Architectural Truths Contributors Must Not Miss

1. `platform/src` stays in the host workspace for now.
2. `src/main/java/com/extremecraft/ecosystem/core/**` still contains host-owned
   compat/network residue. That package prefix does not mean the code belongs to
   the extracted `core/` repo candidate.
3. `api` and `core` are ready to create now because they no longer depend on
   host-only runtime packages.
4. `progression` is not repo-ready yet because it still compiles against host
   outputs and still imports host runtime packages directly.
5. `workspace/build`, `workspace/generated`, `workspace/exports`, and root
   `build/` are generated/local output areas, not source ownership signals.

## Progression Remaining Host Coupling

Representative direct host-runtime imports still present in
`progression/src/main/java` after this audit:

- network/sync: `ModNetwork`, runtime sync services, packet classes
- classsystem runtime: `ClassAccessResolver`, `ClassPassives`, `PlayerClass`
- skills runtime: `PlayerSkillsCapability`, `SkillRegistry`, `SkillsApi`
- ability/magic runtime: `AbilityExecutor`, `AbilityTargetResolver`,
  `ManaService`
- machine/material/runtime UI: `MachineCatalog`, `ModMaterials`,
  client GUI classes

These are the blockers that keep `progression` in "subproject now, repo later"
status.
