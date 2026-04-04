# ExtremeCraft Forge 1.20.1 Workspace Inventory

## Real Status
- **api/** - ✅ Real Gradle module (no host dependencies), ready for repo extraction
- **core/** - ✅ Real Gradle module (compileOnly Forge), ready for repo extraction
- **progression/** - ⚠️ Included Gradle subproject in bridge mode (depends on host runtime)
- **platform/src/** - Host runtime (Forge bootstrap, registration, packet, event wiring)
- **src/** - Host runtime (transitional gameplay monolith; src/settings.gradle is legacy artifact)

## Compile Status
✅ `./gradlew.bat compileJava` passes clean

## Root-Level Folder Classification
- `api/`, `core/` → real modules, first-wave extraction ready
- `progression/` → included subproject, authority-hardened but host-coupled (repo later)
- `platform/`, `src/` → host runtime, not split in this pass
- `build/`, `bin/`, `run/` → generated/local (properly gitignored)
- `tools/`, `docs/`, `examples/`, `datapacks/`, `tests/` → support/tooling
- `.github/`, `scripts/`, `workspace/`, `config/` → configuration/workspace

## Docs Quality
- ARCHITECTURE.md covers system subsystems well
- REPOSITORY_FOLDER_GUIDE.md is current and accurate
- CONTRIBUTING.md provides clear contributor orientation
- PROGRESSION_SUBPROJECT_READINESS.md documents bridge-mode status
- CANONICAL_OWNERSHIP_MAP.md exists (check for updates)

## .gitignore Notes
- Complex but correct: data tree pattern rules work well
- No tracked noise detected
- build/*, run/*, logs/* properly excluded
- Carefully preserves data/extremecraft structural markers

## Next Action Items
1. Verify api/ and core/ are truly extraction-ready (no stray refs)
2. Tighten module doc comments if needed
3. Ensure .gitignore reflects current build practices
4. Update REPOSITORY_FOLDER_GUIDE.md with repo extraction readiness summary
5. Document any progression prep work needed before split
