# Legacy Machines

Status: `LEGACY`

This folder holds the older single-machine runtime chain, currently centered on the standalone pulverizer implementation.

Runtime-critical files that still matter:
- `pulverizer/PulverizerBlock`
- `pulverizer/PulverizerBlockEntity`
- `pulverizer/PulverizerMenu`
- `recipe/PulverizerRecipe`

Why it still exists:
- `registry/ModBlocks`, `ModBlockEntities`, `ModMenuTypes`, and `ModRecipeSerializers` still register this path.
- Existing gameplay/content may still rely on the standalone pulverizer block and `pulverizing` recipe type.

Not the live owner for future machine work:
- Do not add new tech-era machines here.
- Use `machine/core` plus `future/registry` instead.

Common mistakes:
- Copying this pattern for new machines and creating a second runtime chain.
- Confusing `MachineBlueprints` planning text with a runtime catalog.
