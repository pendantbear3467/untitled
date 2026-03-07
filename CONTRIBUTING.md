# CONTRIBUTING

Thank you for contributing to ExtremeCraft.

## Development Setup

Prerequisites:

- Java 17
- Python 3.10+ (recommended for tooling and content validation)

Setup:

1. Clone the repository.
2. Run `./gradlew.bat compileJava` to verify Java toolchain setup.
3. Run `./gradlew.bat runClient` for a dev client session.
4. Optional tooling setup: create a Python venv and install project extras from `pyproject.toml`.

## Creating Datapacks

1. Add JSON under `data/extremecraft/...` using `DATAPACK_FORMAT.md`.
2. Keep ids lowercase and stable.
3. Validate with `./gradlew.bat check`.
4. Include at least one concrete test scenario in your PR notes.

External datapack workspace for contributors is under `datapacks/`.

## Adding Machines Safely

1. Prefer data-first changes in machine JSON and recipes.
2. If Java updates are needed, keep logic inside machine subsystems (`MachineRegistry`, `MachineProcessingLogic`, machine block entities).
3. Do not bypass energy/input/output checks.
4. Ensure recipe ids and machine ids remain consistent.

## Systems That Must Not Be Modified In Routine Contributions

Do not change these systems unless the PR is explicitly scoped for engine/security work:

- Packet security/rate limiting behavior.
- Core mod bootstrap lifecycle ordering.
- Progression gating security checks in ability execution.
- Deferred tick scheduling semantics.
- API compatibility contracts in `api/` and hook surfaces.

## Repository Orientation For Contributors

- Gameplay/runtime Java: `src/main/java`
- Runtime assets/datapack content: `src/main/resources`
- Python asset/compiler/editor tooling: `tools/python`
- Validation/build helper tools: `tools/`
- Datapack workspace and examples: `datapacks/`, `examples/`

## Pull Request Expectations

1. Keep changes scoped and reviewable.
2. Avoid unrelated refactors.
3. Document validation steps you ran (`compileJava`, `check`, client launch).
4. Add/update docs when formats or extension points change.
