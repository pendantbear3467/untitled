# Asset Studio Developer Guide

## Overview

EXTREMECRAFT ASSET STUDIO orchestrates the full ExtremeCraft ecosystem platform:

- SDK (`extremecraft_sdk`) for addon definitions and validation
- Compiler (`compiler`) for addon-to-module builds
- Repair, release, registry diff/history, and modpack pipelines
- GUI visual editors and animation preview tooling

## Studio Shared Contract

The architecture and implementation contract for evolving Asset Studio into full
ExtremeCraft Studio is defined in:

- `docs/EXTREMECRAFT_STUDIO_SHARED_CONTRACT.md`

Contributors extending desktop tooling should treat this contract as required,
especially for modularity, failure isolation, migration safety, and compatibility.

## Primary Command Surface

- `assetstudio generate ...`
- `assetstudio sdk ...`
- `assetstudio compile expansion <addon_name>`
- `assetstudio addon list|build-all|install|remove`
- `assetstudio validate --strict`
- `assetstudio repair run`
- `assetstudio registry scan|diff|history`
- `assetstudio release build|publish`
- `assetstudio modpack build <name>`

## Compiler Outputs

`assetstudio compile expansion <addon>` now emits:

- Forge registry Java classes (`GeneratedItems`, `GeneratedBlocks`, `GeneratedMachines`, `GeneratedRecipes`, `GeneratedWorldgen`)
- dependency graph metadata with load ordering
- compiled datapack roots (`recipes`, `loot_tables`, `tags`, `advancements`, `worldgen`)
- generated markdown docs for addons and API surface

## Plugin Marketplace Metadata

Plugin registry supports versioned metadata:

- `name`
- `version`
- `dependencies`
- `compatible_platform_version`

Resolved plugin metadata is exported to `workspace/plugin_marketplace/index.json`.

## Blockbench Upgrades

Import/export now supports:

- `.bbmodel` animation bundles
- texture baking (`data:image/png;base64` on export)
- automatic blockstate generation on import

## Validation Coverage

Validation pipeline includes checks for:

- registry conflicts
- missing textures/models references
- invalid datapack JSON shapes
- missing worldgen pair files
