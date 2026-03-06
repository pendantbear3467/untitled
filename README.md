# EXTREMECRAFT PLATFORM + ASSET STUDIO

ExtremeCraft now includes a full ecosystem toolchain for large Minecraft mega-mod development:

- `extremecraft_sdk/` for addon/content-pack authoring
- `compiler/` for module compilation to distributable JAR artifacts
- Expanded Asset Studio CLI + GUI for generation, validation, repair, release, and modpack assembly

## Platform Capabilities

- SDK definitions in JSON or Python (`material`, `machine`, `weapon`, `tool`, `armor`, `skill_tree`, `quest`, `worldgen`)
- Batch generation for complete material sets
- Automatic validation and auto-repair for missing textures/models/recipes
- Registry scan, history, and diff tooling (removed/renamed/conflict detection)
- Animated preview pipeline (renderer + timeline control)
- Visual editors (materials, machines, weapons, worldgen, quests, skill trees)
- Addon module compiler with dependency/conflict resolution
- Release pipeline (artifact + changelog + GitHub/CurseForge publish workflow)
- Modpack builder pipeline (manifest + distributable zip)
- Plugin ecosystem with generator/validator/repair/editor/datapack hooks

## Install

```bash
python -m venv .venv
.venv\Scripts\activate
pip install -e .[all]
```

## Core Commands

```bash
# SDK scaffold + validation
assetstudio sdk init-addon mythril_expansion
assetstudio sdk validate mythril_expansion

# Compile addon to module artifact
assetstudio compile expansion mythril_expansion

# Batch content generation
assetstudio generate material_set mythril --tier 5 --style metallic

# Auto repair missing assets
assetstudio repair run

# Registry tools
assetstudio registry scan --source src/main/java --save-history snapshot_a
assetstudio registry diff snapshot_a snapshot_b
assetstudio registry history list

# Release automation
assetstudio release build --name extremecraft-v0.2.0
assetstudio release publish --name extremecraft-v0.2.0

# Modpack build
assetstudio modpack build extreme_adventure_pack
```

## SDK Structure

```text
extremecraft_sdk/
  api/
  definitions/
  generators/
  validators/
  plugins/
  templates/
```

Templates are available in `extremecraft_sdk/templates/*.json`.

## Compiler Structure

```text
compiler/
  module_builder.py
  code_generator.py
  datapack_builder.py
  asset_builder.py
  dependency_resolver.py
```

## Plugin API Extensions

Plugins auto-load from repository `plugins/` and optional workspace `workspace/plugins/`.

Supported hook registration:

- `register_generator(name, handler)`
- `register_validator(name, handler)`
- `register_asset_repair(name, handler)`
- `register_gui_editor(name, handler)`
- `register_datapack_rule(name, handler)`

## GUI

```bash
assetstudio --gui
```

The GUI includes the asset wizard, visual definition editors, project browser, console, and animated preview controls.
