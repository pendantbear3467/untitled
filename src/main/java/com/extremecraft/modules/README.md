# Modules

Status: `LIVE_RUNTIME` with `VALIDATION_ONLY` overlap

This folder owns live modular gear definitions, install/remove services, passive modifiers, and module-triggered abilities.

Runtime-critical files:
- `loader/ModuleDefinitionLoader` loads live armor/tool module definitions from `armor_modules` and `tool_modules`.
- `service/ModuleInstallService` mutates installed module state.
- `runtime/ModuleRuntimeService` applies passive effects and ability cooldown/runtime behavior.
- `registry/*Registry` classes are the live runtime stores.

Metadata-only overlap:
- `platform/data/loader/ModuleDataLoader` loads `data/extremecraft/modules/*.json` for platform metadata and debug surfaces.
- The `data/extremecraft/modules` folder is not the live owner for installed module behavior.

Safe future additions:
1. Put live gear modules in `armor_modules` or `tool_modules`.
2. Put shared runtime code in this folder.
3. Treat `data/extremecraft/modules` as metadata until the migration is completed deliberately.

Common mistakes:
- Adding a new installable module only under `data/extremecraft/modules`.
- Mixing armor/tool module ownership in one loader path.
