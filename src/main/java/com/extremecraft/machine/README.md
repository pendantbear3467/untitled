# Machine Root

Status: `ADAPTER` plus `LEGACY`

This folder contains the older generic machine runtime/catalog path and compatibility hooks. It is not the authoritative owner for the active multi-machine tech block chain.

Runtime-critical files:
- `MachineRegistry` still supports API/runtime registration hooks and older generic machine definitions.

Adapter-only files:
- `MachineDefinition`, `MachineRecipe`, `MachineBlockEntity`, and `MachineProcessingLogic` support the generic legacy machine path.
- `MachineTooltipHandler` and sync helpers can still surface runtime state for older content.

Live owner that replaced this path:
- `com.extremecraft.machine.core`
- `com.extremecraft.machine.recipe`
- `com.extremecraft.future.registry`
- Vanilla datapack recipes under `data/extremecraft/recipes/machine_processing`

Safe future additions:
1. Add new live machines in `machine/core` and `future/registry`, not here.
2. Use this folder only when you are preserving compatibility for existing generic machine integrations.

Common mistakes:
- Assuming `MachineRegistry` drives `TechMachineBlockEntity`.
- Adding new machine gameplay here because the folder name is shorter.
