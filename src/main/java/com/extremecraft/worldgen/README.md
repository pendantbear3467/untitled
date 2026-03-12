# Worldgen

Status: `LIVE_RUNTIME` with `VALIDATION_ONLY` overlap

This folder owns runtime-facing worldgen helpers, ore/material consistency checks, and worldgen metadata bridges.

Runtime-critical files:
- Vanilla/Forge worldgen content under `data/extremecraft/worldgen` and `data/extremecraft/forge`
- `WorldgenConsistencyValidator` validates expected ore/configured/placed/biome-modifier coverage
- `OreGenerationProfiles` and `OreMaterialCatalog` define ore generation source data

Metadata-only overlap:
- `platform/data/loader/WorldGenerationDataLoader` loads `data/extremecraft/world_generation` for metadata/debug use.
- `WorldgenFeatureRegistry` exposes a merged summary, not the live placement owner.

Experimental or stub areas:
- `DimensionHooks` is intentionally minimal.

Safe future additions:
1. Add real worldgen content under the vanilla/Forge datapack folders.
2. Keep metadata summaries in `world_generation` only if they do not pretend to place blocks directly.

Common mistakes:
- Treating `world_generation/*.json` as actual ore placement config.
- Adding ore materials without the configured/placed/biome-modifier chain.
