# Class System

Status: `ADAPTER` plus `LIVE_RUNTIME`

This folder owns the gameplay-facing class access bridge used by abilities, spells, and UI checks.

Runtime-critical files:
- `ClassRegistry` loads live class runtime metadata from `data/extremecraft/classes`.
- `ClassAbilityBindings` is the gameplay access gate for class-based ability/spell checks.
- `ClassAccessResolver` bridges legacy callers onto canonical progression class definitions.
- `ClassRequirements` currently enforces level-based class usage checks.

Canonical data source behind this bridge:
- `progression/classsystem`
- `data/extremecraft/classes`
- `data/extremecraft/class_abilities`

Legacy overlap:
- Older callers still expect `classsystem.PlayerClass` and `ClassRegistry`.
- `ClassAccessResolver` is the adapter that prevents another split from forming.

Safe future additions:
1. Add live class data in the datapack folders.
2. Route new runtime class access checks through `ClassAbilityBindings` or `ClassAccessResolver`.
3. Keep this folder focused on access/gating, not content storage duplication.

Common mistakes:
- Creating a third class registry.
- Putting canonical class content only in Java instead of the datapack owner path.
