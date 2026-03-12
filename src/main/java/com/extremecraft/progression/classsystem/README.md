# Progression Classsystem

Status: `LIVE_RUNTIME`

This folder owns canonical datapack-backed class definitions and class ability definitions used by progression and class ability runtime.

Runtime-critical files:
- `data/ClassDefinitionLoader`
- `data/ClassAbilityLoader`
- `data/ClassDefinitions`
- `data/ClassAbilityDefinitions`
- `ability/ClassAbilityService`

Live data owner:
- `data/extremecraft/classes/*.json`
- `data/extremecraft/class_abilities/*.json`

Consumed by:
- `classsystem/ClassAccessResolver`
- `progression/GuildQuestRewardService`
- Class ability packets and runtime state sync

Adapter notes:
- `classsystem/` still exposes the bridge used by legacy consumers.

Safe future additions:
1. Add new class abilities in the datapack folder and implement any new runtime effect in `ability/ClassAbilityService`.
2. Keep class definition loading here, not in the platform metadata loaders.

Common mistakes:
- Adding a class ability JSON without a corresponding runtime effect path.
- Duplicating class definitions in both this folder and `classsystem/`.
