# Machine Core Runtime

This folder contains the live generic machine runtime used by catalog-based tech blocks.

What this folder does:
- Declares canonical machine ids and stage gates in `MachineCatalog.java`.
- Owns the shared block entity runtime in `TechMachineBlockEntity.java`.
- Resolves recipes and processing through `MachineRecipeService.java` and `MachineProcessingService.java`.
- Handles shared ticking, energy, menu sync, and fallback processing.

Safe gameplay edits:
- Add or retune machine stage/process values in `MachineCatalog.java`.
- Adjust shared processing behavior in `MachineProcessingService.java`.
- Edit built-in machine metadata JSON under `src/main/resources/data/extremecraft/machines/`.

Metadata versus live runtime:
- This folder is the live owner for generic machine behavior.
- `src/main/resources/data/extremecraft/machines/*.json` is descriptive machine metadata, not the only authority for runtime stage gates.
- `src/main/java/com/extremecraft/progression/tech/TechTreeManager.java` currently looks reference-only from this scan and is not the canonical machine runtime gate.

Loaders and services touching this subsystem:
- `src/main/java/com/extremecraft/progression/ProgressionGate.java`
- `src/main/java/com/extremecraft/entity/system/GameplayMechanicsEvents.java`
- `src/main/java/com/extremecraft/command/ECValidationService.java`

Future additions should follow:
- Add new generic machines by extending `MachineCatalog` and machine datapack metadata together.
- Keep runtime ids aligned with actual block ids.
- If a folder contains a legacy or prototype machine chain, document it rather than redirecting live gameplay ownership to it.

Common edit paths:
- Change unlock stage for a live machine: `MachineCatalog.java`.
- Add baseline processing recipe coverage: `src/main/resources/data/extremecraft/recipes/machine_processing/generated/`.
- Adjust UI sync/state: `TechMachineBlockEntity.java` and `TechMachineMenu.java`.