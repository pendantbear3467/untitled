# Repo Split One-Pass Runbook

Status: operational runbook for a non-destructive full split pass.

## Scope

- Executes current approved split targets only: `api` and `core`.
- Keeps host-owned runtime paths untouched (`platform`, `src`, `progression`).
- Produces standalone-ready export artifacts in `workspace/repo-split/`.

## Preconditions

1. Baseline compile and boundary tests pass.
2. No host-runtime deletion/refactor is performed as part of this pass.

## Commands

1. Baseline:

```powershell
.\gradlew.bat compileJava
.\gradlew.bat test --tests com.extremecraft.gameplay.GameplayAuthoritySourceTest --tests com.extremecraft.gameplay.ProgressionBoundarySourceTest
```

2. Export split artifacts:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\export_split_repos.ps1
```

3. Optional: keep prior exports and only fill missing outputs:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\export_split_repos.ps1 -KeepExisting
```

## Output Layout

- `workspace/repo-split/extremecraft-api/`
- `workspace/repo-split/extremecraft-core/`
- `workspace/repo-split/EXPORT_REPORT.md`

## Safety Constraints

- Export script writes only under `workspace/repo-split/`.
- Existing export folders are replaced by default for deterministic snapshots.
- Host repo source directories remain the source of truth.

## Next-Step Promotion (Manual)

After reviewing generated snapshots:

1. Initialize each export folder as its own git repo.
2. Push to remote repos (`extremecraft-api`, `extremecraft-core`).
3. Wire host to those repos using composite build or published artifacts.
4. Re-run compile and boundary tests in host.
