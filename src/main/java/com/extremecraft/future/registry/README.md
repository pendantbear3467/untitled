# Future Registry

Status: `LIVE_RUNTIME`

Despite the folder name, this is the active deferred-register owner for most generated tech blocks, items, menus, block entities, and recipe serializers.

Runtime-critical files:
- `TechBlocks`
- `TechItems`
- `TechBlockEntities`
- `TechMenuTypes`
- `TechRecipeSerializers`

Consumed by:
- Forge registries during startup
- `machine/core`
- Dynamic ore/material registration
- Active tech machine UI and recipe systems

Legacy overlap:
- `registry/` still owns the standalone pulverizer path and a small set of older items.

Safe future additions:
1. Register new generated tech content here.
2. Keep ids aligned with asset/data folder names.
3. Add supporting models/lang/recipes/stage data in the matching resource folders.

Common mistakes:
- Assuming the folder is dormant because it is named `future`.
- Registering duplicate ids in both `registry/` and `future/registry/`.
