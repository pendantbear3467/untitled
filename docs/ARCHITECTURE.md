# ExtremeCraft Platform Architecture

This repository is evolving from a single Forge mod into a platform architecture.

## Planned modules

- `core/`: runtime kernel, lifecycle wiring, compatibility gates, module loader
- `api/`: public extension API for addon mods
- `gameplay/`: progression, classes, skills, quests
- `tech/`: machine, energy, and automation systems
- `magic/`: magic progression and ability systems
- `worldgen/`: biomes, ores, structures, dimensions
- `client/`: GUIs, rendering, client integrations
- `tools/`: generators and development tooling

## Current state

- Runtime is still built as one Forge artifact (`extremecraft-core`)
- Public API package is available at `com.extremecraft.api`
- Module extensions can be discovered via Java `ServiceLoader`
- Compatibility is gated by API and protocol versions

## Migration strategy

1. Keep runtime stable in current source tree.
2. Move systems into dedicated module folders in small slices.
3. Keep `com.extremecraft.api` backward-compatible across minor versions.
4. Version-break only when API/protocol changes require it.

## Shared implementation contract

The cross-domain implementation baseline for the first major release is defined in:

- `docs/SHARED_IMPLEMENTATION_CONTRACT.md`

Desktop tooling and embedded editor architecture baseline is defined in:

- `docs/EXTREMECRAFT_STUDIO_SHARED_CONTRACT.md`

Contributors should treat that contract as mandatory for progression boundaries, server authority, safety caps, and first-release scope control.
