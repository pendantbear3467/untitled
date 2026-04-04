# platform

Forge bootstrap, compatibility gates, and migration-adapter source root.

Current contents:

- `src/main/java/com/extremecraft/core/**`: bootstrap entrypoint and platform-facing runtime wiring.
- `src/main/java/com/extremecraft/platform/**`: hook surfaces, data registries, compatibility gates, loaders, and sync helpers.

This folder is a real source root, not a separate Gradle module yet. The root build includes it so the workspace can separate bootstrap glue from the remaining transitional gameplay runtime without breaking the current mod launch path.
