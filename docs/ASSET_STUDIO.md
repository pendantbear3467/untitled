# Asset Studio Developer Guide

## Overview

EXTREMECRAFT ASSET STUDIO is now the orchestration layer for the full ExtremeCraft ecosystem platform:

- SDK (`extremecraft_sdk`) for addon definitions and validation
- Compiler (`compiler`) for addon-to-module builds
- Repair, release, registry diff/history, and modpack pipelines
- GUI visual editors and animation preview tooling

## Primary Command Surface

- `assetstudio generate ...`
- `assetstudio sdk ...`
- `assetstudio compile expansion <addon_name>`
- `assetstudio repair run`
- `assetstudio registry scan|diff|history`
- `assetstudio release build|publish`
- `assetstudio modpack build <name>`

## GUI Additions

- Material editor
- Machine editor
- Weapon editor
- Worldgen editor
- Quest editor
- Skill tree editor
- Animated timeline preview

## Plugin Extension Points

Plugin registry now supports:

- generators
- validators
- asset repair handlers
- GUI editors
- datapack rules
- texture styles
- templates/exporters (legacy compatibility)

Auto-load locations:

- `plugins/` (repository root)
- `workspace/plugins/` (project-local)
