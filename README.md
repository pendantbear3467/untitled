# EXTREMECRAFT ASSET STUDIO

EXTREMECRAFT ASSET STUDIO is a modular, open-source developer application for generating Minecraft mod assets.

It supports:
- Forge
- NeoForge
- Fabric
- Blockbench import/export workflows
- DataPack and ResourcePack structures

## Features
- CLI and desktop GUI (PyQt6)
- Asset wizard for rapid generation
- Procedural texture engine (16x/32x/64x/128x)
- Blockbench `.bbmodel` import and export
- Registry scanner for Java `DeferredRegister` discovery
- Asset validator (JSON, textures, model parents, recipes)
- Workspace-based projects with persistent asset database
- Plugin loader for community extension points

## Quick Start

```bash
python -m venv .venv
.venv\\Scripts\\activate
pip install pillow numpy pyqt6
```

### CLI

```bash
python assetstudio.py generate tool mythril_pickaxe --material mythril --tier 4
python assetstudio.py generate ore mythril --tier 4 --style metallic
python assetstudio.py validate
python assetstudio.py build datapack
python assetstudio.py export resourcepack
python assetstudio.py scan-registry --source src/main/java
```

### GUI

```bash
python assetstudio.py --gui
```

## Project Structure

Main app source:
- `asset_studio/main.py`
- `asset_studio/cli/`
- `asset_studio/gui/`
- `asset_studio/generators/`
- `asset_studio/blockbench/`
- `asset_studio/textures/`
- `asset_studio/minecraft/`
- `asset_studio/project/`
- `asset_studio/plugins/`

Repository support folders:
- `docs/`
- `examples/`
- `plugins/`
- `templates/`

## Plugin API

Drop plugin files into the repository-level `plugins/` folder and expose:

```python
def register(registry):
    registry.generators["my_generator"] = MyGeneratorClass
```

See `plugins/sample_create_compat.py`.

## License and Contribution

This project is intended to be community-driven. See `CONTRIBUTING.md` and `CODE_OF_CONDUCT.md`.
