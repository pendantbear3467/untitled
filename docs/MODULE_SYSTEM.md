# Module System

ExtremeCraft supports module discovery from three sources:

1. Java `ServiceLoader`
2. `META-INF/extremecraft.modules.json` manifests
3. External compiled addon jars in `/<game-dir>/extremecraft/modules/*.jar`

Use `/ec modules` to inspect loaded modules and `/ec debug_screen` for runtime debug state.

## Dynamic runtime loader

At startup, the loader scans external module jars and creates an isolated `URLClassLoader` for them. Compatible modules are registered through the same `ExtremeCraftModule` contract as built-in modules.

## Compatibility gates

Modules are loaded only when:

- API version matches `EXTREMECRAFT_API_VERSION`
- protocol version matches `EXTREMECRAFT_PROTOCOL_VERSION`

Incompatible modules are skipped and logged.
