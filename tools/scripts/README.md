# tools/scripts

Compatibility launcher scripts for contributor workflows.

These wrappers are intentionally separate from runtime mod code and from the
main tooling implementations under `tools/python` and `tools/generate_assets.py`.

Current launchers:

- `assetstudio.py`: launches `asset_studio.main` with the local tools Python path.
- `main.py`: equivalent launcher alias for `asset_studio.main`.
- `generate_assets.py`: wrapper around the maintained `tools/generate_assets.py` CLI.
- `Python-Generator.py`: legacy generator alias kept for older workflows.

Why this folder exists:

- keeps repository root focused on mod runtime/build metadata
- keeps non-runtime helper entrypoints grouped in one place
- preserves contributor command compatibility without polluting root layout
