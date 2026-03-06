# Module Development

## Addon lifecycle

1. Depend on `extremecraft-core`.
2. Implement `ExtremeCraftModule`.
3. Publish service declaration in `META-INF/services/com.extremecraft.api.module.ExtremeCraftModule`.
4. Register custom definitions through `ExtremeCraftAPI`.

## Datapack namespaces

Recommended data roots:

- `data/extremecraft/skills/`
- `data/extremecraft/research/`
- `data/extremecraft/machines/`
- `data/extremecraft/armor_modules/`
- `data/extremecraft/tool_modules/`
- `data/extremecraft/abilities/`
- `data/extremecraft/quests/`

Use IDs that are globally unique and stable.
