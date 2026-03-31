# Recipes Datapack Content

Status: `LIVE_RUNTIME`

This folder owns active vanilla/custom recipe manager content.

Owns:
- Standard crafting/smelting JSON
- `machine_processing/` for live tech machine processing recipes
- `pulverizing/` for the legacy standalone pulverizer path
- `generated/` for generated but still live recipe payloads

Consumed by:
- Vanilla recipe manager
- `machine/recipe/MachineProcessingRecipe`
- `machines/recipe/PulverizerRecipe`

Not the owner:
- `data/extremecraft/ec_recipes` is metadata and validation support only.

Common mistakes:
- Adding tech machine recipes outside `machine_processing/`.
- Assuming generated files are safe to hand-edit without checking the generator path.
