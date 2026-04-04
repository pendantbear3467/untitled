# workspace

Scratch and intermediate workspace area used by local tooling.

Use this folder for tooling inputs plus temporary/generated outputs that should
not be treated as authoritative source content for the mod runtime.

Do not store canonical Java/runtime resources here.

Generated subfolders:

- `build/`, `generated/`, and `exports/` are local/generated output areas.
- Only the README markers in those folders should stay tracked.
- `.studio/autosave/`, `.studio/logs/`, and `.studio/recovery/` are local tool
  state, not project source.

Canonical locations remain:

- Runtime Java code: `../src/main/java`
- Runtime resources and built-in datapack data: `../src/main/resources`
- External datapack authoring workspace: `../datapacks`
- Tooling/runtime-independent generators: `../tools`
