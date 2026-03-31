# Contamination Terrain Rules

This folder contains the live terrain-conversion extension for chunk contamination.

What this folder does:

- Defines bounded surface block mutations that can occur inside contaminated chunks.
- Extends the existing radiation runtime without creating a second biome-corruption system.

Safe gameplay edits:

- Add new source/result block pairs.
- Raise or lower `min_chunk_contamination` and `chance_per_pulse`.

Metadata versus live runtime:

- Files in this folder are live runtime inputs.
- They are loaded by `ContaminationTerrainDataLoader` and executed by `ContaminationTerrainService`.
- `data/extremecraft/contamination/default_profile.json` remains the owner for release-time terrain seeding, cleanup targets, and contamination caps.
- First-party contaminated block variants now exist in `future/registry` and `assets/extremecraft` for dirt, stone, wood, sand, and grass.

Loaders and services touching this folder:

- `src/main/java/com/extremecraft/platform/data/loader/ContaminationTerrainDataLoader.java`
- `src/main/java/com/extremecraft/platform/data/validator/DataValidationService.java`
- `src/main/java/com/extremecraft/radiation/ContaminationTerrainService.java`
- `src/main/java/com/extremecraft/radiation/ChunkContaminationService.java`

Future additions should follow:

- Prefer destination blocks that already exist in the mod or vanilla.
- Keep rules cheap: this system is intended for light surface mutation, not full biome rewrites.
- Point `result_block` at first-party contaminated blocks or hardened fallout blocks instead of replacing this service.

Common edit paths:

- Add a harsher wasteland look: convert `minecraft:podzol` or `minecraft:rooted_dirt`.
- Add first-party contaminated variants later: point `result_block` to `extremecraft:<your_block_id>`.
