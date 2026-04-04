# Modules

Status: `LIVE_RUNTIME` with `VALIDATION_ONLY` overlap

This folder owns live modular gear definitions, install/remove services, passive modifiers, and module-triggered abilities.

Runtime-critical files:
- `loader/ModuleDefinitionLoader` loads live armor/tool module definitions from `armor_modules` and `tool_modules`.
- `loader/ModuleAbilityLoader` loads live module trigger payloads from `module_abilities` and only falls back to trigger-bearing `abilities` files for compatibility.
- `service/ModuleInstallService` mutates installed module state.
- `runtime/ModuleRuntimeService` applies passive effects and ability cooldown/runtime behavior.
- `registry/*Registry` classes are the live runtime stores.

Execution note:
- Module trigger timing/cooldowns stay in `modules/runtime`.
- Active triggered effects compile into the shared `ability/AbilityExecutor` path instead of maintaining a separate effect execution system.

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
- Putting new module trigger payloads in `data/extremecraft/abilities` instead of `data/extremecraft/module_abilities`.
