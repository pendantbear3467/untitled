# Asset Studio Developer Guide

## Overview

EXTREMECRAFT ASSET STUDIO is the high-level developer application layered on top of the existing procedural generator system in `tools/asset_generator`.

Core goals:
- Preserve existing generator behavior.
- Add a professional CLI + GUI workflow.
- Keep extension points stable for contributors.

## Entry Points

- CLI + GUI dispatcher: `asset_studio/main.py`
- Root launcher script: `assetstudio.py`
- Installed console command (from `pyproject.toml`): `assetstudio`

## Architecture

- `asset_studio/cli`: command routing for generate/build/validate/export
- `asset_studio/gui`: PyQt6 window, menu bar, wizard, preview
- `asset_studio/generators`: item/block/ore/tool/armor/machine bundles
- `asset_studio/blockbench`: `.bbmodel` import/export adapters
- `asset_studio/textures`: procedural texture abstraction and style palettes
- `asset_studio/minecraft`: JSON template helpers
- `asset_studio/project`: workspace management, registry scan, validator, DB
- `asset_studio/plugins`: plugin registry and loader

## Workspace Format

Each workspace contains:
- `project.json`
- `asset_database.json`
- `assets/`
- `data/`
- `blockbench/`
- `previews/`
- `build/`
- `exports/`

## Registry Scanner

`assetstudio scan-registry` inspects Java sources for `.register("id")` patterns and classifies entries into item/block/machine buckets using simple type hints.

## Validation

`assetstudio validate` checks:
- Broken JSON in assets/data
- Missing textures referenced by item models
- Missing tool recipes for generated pickaxes
- Suspicious block model parents

## Notes

- Texture generation reuses the existing core under `tools/asset_generator` through `ProceduralTextureEngine`.
- GUI preview is OpenGL-backed (`QOpenGLWidget`) and supports interactive preview modes (`texture`, `item`, `block`, `animated`) with live texture loading after generation.

## Packaging

Install in editable mode:

```bash
pip install -e .
```

Install with optional extras:

```bash
pip install -e .[core]
pip install -e .[gui]
pip install -e .[all]
```
