# scripts

Helper automation scripts for contributors belong here.

This folder is for repo-level automation tasks. Python compatibility launchers for
Asset Studio and generators live in `../tools/scripts/`.

Recommended use:

- local validation wrappers
- release helper scripts
- repeatable repo maintenance commands

Repo split helper:

- `export_split_repos.ps1`: non-destructive exporter that generates standalone-ready
	`extremecraft-api` and `extremecraft-core` snapshots under
	`workspace/repo-split/`.
