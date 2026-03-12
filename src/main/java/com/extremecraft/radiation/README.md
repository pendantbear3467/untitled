# Radiation And Contamination

Status: `LIVE_RUNTIME`

This folder owns the canonical environmental hazard pipeline for radiation dose, chunk contamination, terrain mutation, protection, and cleanup hooks.

Runtime-critical files:
- `RadiationService` is the top-level gameplay owner for radiation and contamination ticking.
- `RadiationSourceService` resolves ambient/item/event radiation sources.
- `ChunkContaminationService` stores numeric chunk contamination authority.
- `ContaminationTerrainService` converts chunk contamination pressure into bounded terrain mutation and cleanup pulses.
- `RadiationProtectionService` owns armor-based mitigation and cleanup efficiency hooks.

Live data owner:
- `data/extremecraft/radiation_sources/*.json`
- `data/extremecraft/contamination/*.json`
- `data/extremecraft/contamination_terrain/*.json`

Safe future additions:
1. Put thresholds, cleanup behavior, and seed-release terrain variants in `data/extremecraft/contamination`, and put ongoing chunk mutation rules in `data/extremecraft/contamination_terrain`.
2. Call `RadiationService.releaseContamination` or `ContaminationTerrainService.scrubArea` from future machines/items rather than writing a second hazard system.
3. Reuse protection hooks instead of inventing separate decontamination stat checks.

Common mistakes:
- Adding another fallout/corruption subsystem outside this folder.
- Adding a second periodic terrain tick outside `ChunkContaminationService -> ContaminationTerrainService`.
- Editing terrain directly without config gates and bounded budgets.
- Reading flat `ec_radiation` or `ec_contamination` tags in UI code; player hazard HUDs should read the nested `ec_radiation` state (`ambient`, `dose`, `contamination`) owned here.
