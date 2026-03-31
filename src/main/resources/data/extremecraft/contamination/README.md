# Contamination Datapack Folder

This folder contains the live chunk-contamination profile loaded by `ContaminationDataLoader`.

What this folder does:
- Defines chunk-level contamination math used by `ChunkContaminationService`.
- Controls max contamination, natural decay, scrub rate, dose decay, and debuff threshold.

Safe gameplay edits:
- Tune numeric thresholds in JSON files here.
- Add alternate profiles if the runtime is later expanded to select between them.

Metadata versus live runtime:
- Files in this folder are live runtime inputs.
- The authoritative Java owner is `src/main/java/com/extremecraft/radiation/ChunkContaminationService.java`.

Loaders and services touching this folder:
- `src/main/java/com/extremecraft/platform/data/loader/ContaminationDataLoader.java`
- `src/main/java/com/extremecraft/platform/data/validator/DataValidationService.java`
- `src/main/java/com/extremecraft/radiation/ChunkContaminationService.java`

Future additions should follow:
- Keep ids stable and lowercase.
- Treat this folder as global contamination profile data, not per-block terrain mutation rules.
- Put terrain conversion rules under `data/extremecraft/contamination_terrain/` instead.

Common edit paths:
- Make contamination decay faster: lower `max_chunk_contamination` or raise `natural_decay_per_pulse`.
- Make radiation debuffs kick in earlier: lower `debuff_threshold`.