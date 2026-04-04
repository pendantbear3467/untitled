# Copilot Instructions

## Repo Split Safety

- Use `docs/REPO_SPLIT_EXECUTION_PLAN.md` as split source-of-truth.
- Treat `platform/`, `src/`, and `progression/` as host-owned in this phase.
- Do not remove or relocate host-owned runtime code during split passes.

## Execution Method

- Prefer idempotent scripts for split exports.
- Export to `workspace/repo-split/` and keep host tree unchanged.
- Before/after structural work, run:
  - `./gradlew.bat compileJava`
  - `./gradlew.bat test --tests com.extremecraft.gameplay.GameplayAuthoritySourceTest --tests com.extremecraft.gameplay.ProgressionBoundarySourceTest`

## Anti-Duplication Rule

- Check existing docs under `docs/` before creating new plans.
- If existing file already captures a decision, update it instead of duplicating it.
