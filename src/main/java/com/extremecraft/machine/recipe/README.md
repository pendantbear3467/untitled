# Machine Recipe

Status: `LIVE_RUNTIME`

This folder owns the custom recipe type and serializer used by active tech machines.

Runtime-critical files:
- `MachineProcessingRecipe`
- `ModTechRecipeTypes`

Consumed by:
- `machine/core/MachineRecipeService`
- `future/registry/TechRecipeSerializers`
- Vanilla recipe manager reloads using `data/extremecraft/recipes/machine_processing`

Metadata-only or parallel paths:
- `data/extremecraft/ec_recipes` is validation/debug metadata, not the active recipe manager owner.
- `machines/recipe` is the legacy pulverizer-only recipe chain.

Safe future additions:
1. Add new machine processing JSON to `recipes/machine_processing`.
2. Keep one machine id per recipe file.
3. Use item/tag inputs that the vanilla recipe manager can resolve.

Common mistakes:
- Adding machine recipes inside `data/extremecraft/machines/*.json` and expecting `TechMachineBlockEntity` to consume them.
- Forgetting to register or reuse the correct recipe type.
