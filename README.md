# EXTREMECRAFT PLATFORM + ASSET STUDIO

ExtremeCraft now includes a full ecosystem toolchain for large Minecraft mod development:

- `extremecraft_sdk/` for addon/content-pack authoring
- `compiler/` for module compilation to distributable JAR artifacts
- Asset Studio CLI + GUI for generation, validation, repair, release, and modpack assembly

## Platform Capabilities

- Forge registry code generation (`GeneratedItems`, `GeneratedBlocks`, `GeneratedMachines`, `GeneratedRecipes`, `GeneratedWorldgen`)
- Graph-based dependency resolver with material/machine/addon/API dependencies and version checks
- Datapack compilation pipeline (recipes, loot tables, tags, advancements, worldgen features)
- Procedural texture styles: `industrial`, `ancient`, `arcane`, `mechanical`, `crystalline`
- Blockbench import/export with animation bundle support, texture baking, and blockstate generation
- Plugin metadata + marketplace index architecture (`workspace/plugin_marketplace/index.json`)
- Dynamic runtime module jar loader (`<game-dir>/extremecraft/modules/*.jar`)
- Automated validation for registry conflicts, missing assets, and invalid datapacks
- Auto-generated addon/API documentation inside module build output

## Install

```bash
python -m venv .venv
.venv\Scripts\activate
pip install -e .[all]
```

## Core Commands

```bash
# SDK scaffold + validation + generation
assetstudio sdk init-addon mythril_expansion
assetstudio sdk validate mythril_expansion
assetstudio sdk generate mythril_expansion

# Compile addon to module artifact (registry code + datapack + docs)
assetstudio compile expansion mythril_expansion

# Addon lifecycle
assetstudio addon list
assetstudio addon build-all
assetstudio addon install workspace/addons/mythril_expansion
assetstudio addon remove mythril_expansion

# Batch content generation
assetstudio generate material_set mythril --tier 5 --style industrial

# Validation + repair
assetstudio validate --strict
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

## Compiler Structure

```text
compiler/
  module_builder.py
  code_generator.py
  datapack_builder.py
  documentation_generator.py
  asset_builder.py
  dependency_resolver.py
```

## GUI

```bash
assetstudio --gui
```

The GUI includes the asset wizard, visual definition editors, project browser, console, and animated preview controls.
