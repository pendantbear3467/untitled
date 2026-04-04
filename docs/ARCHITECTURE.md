# ExtremeCraft Platform Architecture

This document describes the architecture that is actually live in the repository today.

## Current Runtime Topology

- Host runtime repo roots:
	- `src/main/java` and `src/main/resources` (transitional monolith runtime owner)
	- `platform/src/main/java` (Forge/bootstrap/wiring/glue)
	- `progression/src/main/java` (progression authority source root)
- Included Gradle modules:
	- `api`
	- `core`

The runtime currently ships as a single Forge artifact (`extremecraft`) that consumes `:api` and `:core`.

## Extraction Status

- Extraction-ready now:
	- `api`
	- `core`
- Next wave (after one more cleanup pass):
	- `progression` (subproject next, repo later)
- Host-only in this pass:
	- `platform`
	- `src`
- Long-term optional:
	- `tools`
	- domain repos such as tech/magic/world only after ownership and dependency boundaries are proven

## Why Platform And Src Stay Host-Owned

- `platform` still owns Forge bootstrap, packet/event registration glue, and cross-domain runtime wiring.
- `src` remains the canonical owner for many gameplay systems that have not been safely extracted.
- Splitting either early would create dependency cycles and brittle bootstrap integration.

## Progression Authority Status

- Public write boundary: `ProgressionFacade`
- Cross-domain read boundary: `ProgressionReadAccess`
- Legacy wrappers are compatibility adapters, not authority owners
- Source-policy guardrails exist under host tests and must remain green during migration

## Migration Rules For Contributors

1. Do not treat folder existence as module or repo readiness.
2. Do not split `platform` or `src` until coupling is reduced and enforced by build boundaries.
3. Keep progression writes routed through `ProgressionFacade`.
4. Keep docs synchronized with active ownership and build wiring.

## Related Docs

- `ARCHITECTURE.md`
- `docs/CANONICAL_OWNERSHIP_MAP.md`
- `docs/ECOSYSTEM_MODULE_DEPENDENCY_GRAPH.md`
- `docs/REPO_STRUCTURE_RESET_PLAN.md`
- `docs/REPO_SPLIT_EXECUTION_PLAN.md`
