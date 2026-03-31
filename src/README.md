# src

Current live Forge runtime source still lives under this directory:

- `src/main/java`
- `src/main/resources`

Important guardrail:

- `src/settings.gradle`, `src/.gradle/`, and `src/build/` are legacy nested-project/build artifacts.
- They are not the current repository build root and should not be used as ownership signals.
- When changing gameplay, follow `../docs/CANONICAL_OWNERSHIP_MAP.md`, not the presence of nested Gradle files here.
