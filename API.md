# API

This document covers runtime extension points for integrating new ExtremeCraft content and behavior without patching core gameplay logic.

## Extension Entry Points

## `ExtremeCraftHooks`

Location:

- `src/main/java/com/extremecraft/platform/hook/ExtremeCraftHooks.java`

Purpose:

- Stable static hook surface for runtime integrations.

Supported registrations:

- `registerRuntimeAbility(String id, Ability ability)`
- `registerAbilityDefinition(AbilityDefinition definition)`
- `registerMachineDefinition(MachineDefinition definition)`
- `registerMachineRecipe(MachineRecipe recipe)`
- `registerEntityExtension(EntityType<?> entityType, EntityExtension extension)`

Example:

```java
ExtremeCraftHooks.registerMachineDefinition(
    new MachineDefinition("my_machine", "advanced", 2, 2, 160, 40, true, true, List.of("my_recipe"))
);
```

## `EntityExtensionRegistry`

Location:

- `src/main/java/com/extremecraft/entity/extension/EntityExtensionRegistry.java`

Purpose:

- Register per-entity-type extension logic executed on server ticks.

Usage:

- Register via `ExtremeCraftHooks.registerEntityExtension(...)`.
- Implement `EntityExtension` and provide `onServerTick(LivingEntity entity)` logic.

Why use it:

- Adds mob behavior safely without editing base entity classes.

## Machine Registration

Primary APIs:

- `MachineRegistry.registerMachineDefinition(...)`
- `MachineRegistry.registerRecipeDefinition(...)`
- Hook wrappers in `ExtremeCraftHooks`.

Guidelines:

- Normalize ids to lowercase.
- Keep recipe `machineId` aligned with machine definition id.
- Prefer datapack definitions for balancing; use runtime registration for dynamic integrations.

## Ability Registration

Primary APIs:

- `AbilityRegistry.register(String id, Ability ability)`
- `AbilityRegistry.registerDefinition(AbilityDefinition definition)`
- Hook wrappers in `ExtremeCraftHooks`.

Guidelines:

- Use definition registration for data-driven abilities.
- Use runtime ability registration for Java-executed custom effects.
- Preserve `AbilityEngine` security gates (cooldowns, mana, target validation).

## API Module (`api/`)

Public interfaces and model definitions are also exposed through the `api` Gradle module:

- `ExtremeCraftAPI`
- `ExtremeCraftApiProvider`
- API definition records (`MachineDefinition`, `AbilityDefinition`, `RecipeDefinition`, etc.)

Use the API module for integration code that should not depend on internal runtime package structure.
