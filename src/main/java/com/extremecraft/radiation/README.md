# Radiation Runtime

This folder contains the live radiation and contamination runtime.

What this folder does:
- Samples ambient radiation and player protection.
- Stores and decays chunk contamination.
- Applies dose debuffs.
- Releases fallout from meltdowns and other events.
- Applies bounded contamination terrain mutations through the contamination terrain datapack rules.

Safe gameplay edits:
- Radiation math and thresholds in `RadiationService.java`, `RadiationSourceService.java`, and `ChunkContaminationService.java`.
- Terrain fallout behavior in `ContaminationTerrainService.java`.
- Built-in datapack inputs under `src/main/resources/data/extremecraft/radiation_sources/`, `contamination/`, and `contamination_terrain/`.

Metadata versus live runtime:
- This folder is live runtime code.
- `docs/` files and placeholder JSON notes are not authoritative runtime owners.

Loaders and services touching this subsystem:
- `src/main/java/com/extremecraft/platform/data/loader/RadiationSourceDataLoader.java`
- `src/main/java/com/extremecraft/platform/data/loader/ContaminationDataLoader.java`
- `src/main/java/com/extremecraft/platform/data/loader/ContaminationTerrainDataLoader.java`
- `src/main/java/com/extremecraft/reactor/ReactorSafetyService.java`

Future additions should follow:
- Extend the existing services here instead of creating a second contamination manager.
- Use datapack-backed definitions for new radiation sources and terrain rules.
- If custom contaminated blocks are added later, plug them into `contamination_terrain` rules rather than bypassing chunk contamination.

Common edit paths:
- New radiation-emitting item/block/event: add a JSON entry under `radiation_sources/`.
- Tune contamination decay: edit `contamination/default_profile.json`.
- Add surface fallout conversion: edit `contamination_terrain/*.json`.