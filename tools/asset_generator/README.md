# ExtremeCraft Asset Generator

Modular procedural asset generation toolkit for ExtremeCraft.

## Overview

This package replaces the legacy monolithic generator with a scalable, material-driven pipeline.

Main entrypoint:
- `tools/generate_assets.py`

Core modules:
- `generator_core.py`: orchestration, scope selection, parallel processing, watch mode
- `material_catalog.py`: loads `tools/materials.json`
- `texture_generator.py`: procedural texture synthesis (ore, metal, tools, armor, emissive)
- `model_generator.py`: Minecraft model/blockstate writes + Blockbench template export
- `minecraft_json_generator.py`: JSON schema output helpers
- `blockbench_exporter.py`: `.bbmodel` template generation
- `gui_generator.py`: GUI panel/icon/animated slot generation
- `noise_algorithms.py`: deterministic noise utilities
- `color_palettes.py`: color conversion and blending helpers

## Requirements

Use the project virtual environment and install dependencies:

```powershell
.\.venv\Scripts\python -m pip install pillow numpy
```

## CLI Usage

```powershell
.\.venv\Scripts\python tools/generate_assets.py --materials
.\.venv\Scripts\python tools/generate_assets.py --ores
.\.venv\Scripts\python tools/generate_assets.py --items
.\.venv\Scripts\python tools/generate_assets.py --machines
.\.venv\Scripts\python tools/generate_assets.py --gui
.\.venv\Scripts\python tools/generate_assets.py --all
```

Common options:
- `--export-blockbench`: write `.bbmodel` templates to `tools/generated/blockbench/`
- `--export-minecraft`: force Minecraft JSON outputs
- `--preview`: generate preview sheet at `tools/generated/previews/materials_preview.png`
- `--seed <int>`: deterministic generation seed
- `--workers <int>`: override auto worker count
- `--dry-run`: calculate outputs without writing files
- `--watch`: regenerate when `tools/materials.json` changes
- `--core`: backward-compatible alias of `--materials`

## Material Catalog

Edit `tools/materials.json` to add new materials.

Example:

```json
{
  "materials": [
    { "name": "titanium", "color": "#9bb7d4", "tier": 3, "glow": false },
    { "name": "uranium", "color": "#62ff3a", "tier": 4, "glow": true }
  ]
}
```

Each material drives generation for:
- ore textures and ore block model/blockstate/item model
- storage block textures and models
- ingot, nugget, raw, dust item textures/models
- tool textures/models (pickaxe/sword/axe/shovel/hoe/hammer/drill)
- armor icon textures/models
- machine casing textures and models
- optional emissive layers for glow materials (`*_e.png`)

## Output Paths

Minecraft resources:
- `src/main/resources/assets/extremecraft/textures/block/`
- `src/main/resources/assets/extremecraft/textures/item/`
- `src/main/resources/assets/extremecraft/textures/gui/`
- `src/main/resources/assets/extremecraft/models/block/`
- `src/main/resources/assets/extremecraft/models/item/`
- `src/main/resources/assets/extremecraft/blockstates/`

Generated tooling outputs:
- `tools/generated/blockbench/`
- `tools/generated/previews/`

## Extending The Pipeline

To add a new asset family:
1. Add generation logic in `texture_generator.py` and/or `model_generator.py`.
2. Add a scope flag in `PipelineOptions` in `generator_core.py`.
3. Wire the generation branch inside `process_material()` in `generator_core.py`.
4. Expose a CLI argument in `tools/generate_assets.py`.

Recommended conventions:
- Keep names deterministic and derived from material IDs.
- Keep outputs idempotent so repeated runs are safe.
- Prefer adding a dedicated module rather than growing `generator_core.py`.
