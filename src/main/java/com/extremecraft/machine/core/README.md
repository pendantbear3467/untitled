# Machine Core

Status: `LIVE_RUNTIME`

This folder owns the active tech machine block runtime path.

Runtime-critical files:
- `MachineCatalog` is the live machine definition/catalog owner for registered tech machines.
- `MachineBlock` is the live block interaction gate for tech machines.
- `TechMachineBlockEntity` is the live block entity for registered tech machines.
- `MachineProcessingService` and `MachineRecipeService` own active machine processing and recipe lookup.
- `MachineTickScheduler` owns bounded per-tick machine execution cadence.

Consumed by:
- `future/registry/TechBlocks`
- `future/registry/TechBlockEntities`
- `future/registry/TechMenuTypes`
- `client/gui/machine/TechMachineScreen`

Metadata-only inputs:
- `MachineCatalog` stage/tick numbers are code-owned.
- Actual processing recipes come from `data/extremecraft/recipes/machine_processing`.

Legacy or adapter notes:
- `machine/` and `machines/` contain older paths kept for compatibility and the single-block pulverizer chain.
- The first-release reactor line still uses the compatibility runtime id `fusion_reactor`; treat it as the canonical first-release fission reactor in player-facing content and docs.

Safe future additions:
1. Add the machine definition in `MachineCatalog`.
2. Register the block/item/menu path through `future/registry`.
3. Add processing JSON under `recipes/machine_processing`.
4. Add stage/unlock data under `data/extremecraft/progression`.

Common mistakes:
- Treating `data/extremecraft/machines/*.json` as the live owner for tech machine behavior.
- Adding a block without its recipe, stage, and registry chain.
