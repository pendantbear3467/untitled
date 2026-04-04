---
mode: 'agent'
description: 'Run a non-destructive one-pass repo split for api/core with baseline validation and export report.'
---

Use existing split docs and run a full one-pass execution without deleting host-owned runtime code.

Required sequence:

1. Read `docs/REPO_SPLIT_EXECUTION_PLAN.md`.
2. Run baseline checks:
   - `./gradlew.bat compileJava`
   - `./gradlew.bat test --tests com.extremecraft.gameplay.GameplayAuthoritySourceTest --tests com.extremecraft.gameplay.ProgressionBoundarySourceTest`
3. Execute exporter:
   - `powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\export_split_repos.ps1`
4. Verify outputs:
   - `workspace/repo-split/extremecraft-api/`
   - `workspace/repo-split/extremecraft-core/`
   - `workspace/repo-split/EXPORT_REPORT.md`
5. Report what was reused vs newly added, and list any risk flags.

Constraints:

- Do not delete `platform/`, `src/`, or `progression/`.
- Do not create duplicate strategy docs if one already exists.
- Keep changes additive and reversible.
