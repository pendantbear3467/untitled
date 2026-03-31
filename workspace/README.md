# workspace

Scratch and intermediate workspace area used by local tooling.

Use this folder for temporary, user-generated outputs that should not be treated
as authoritative source content for the mod runtime.

Do not store canonical Java/runtime resources here.

Nuance:

- `build/`, `generated/`, and `exports/` may be intentionally versioned snapshots
  or packaging outputs kept for inspection/comparison.
- `.studio/autosave/`, `.studio/logs/`, and `.studio/recovery/` are local tool
  state, not project source.

Canonical locations remain:

- Runtime Java code: `../src/main/java`
- Runtime resources and built-in datapack data: `../src/main/resources`
- External datapack authoring workspace: `../datapacks`
- Tooling/runtime-independent generators: `../tools`
