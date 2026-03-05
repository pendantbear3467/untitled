# Modular Gear Framework (ExtremeCraft)

## Module System Architecture
- `com.extremecraft.modules.data`
  - `ModuleDefinition`: datapack-defined module payload (`slot_cost`, `required_skill_node`, stat modifiers, ability ids)
  - `ModuleAbilityDefinition`: datapack-defined ability (`trigger`, cooldown, mana cost, scaling)
  - `ModuleType` and `ModuleTrigger`
- `com.extremecraft.modules.loader`
  - `ModuleDefinitionLoader`: loads `/data/extremecraft/armor_modules/*.json` and `/data/extremecraft/tool_modules/*.json`
  - `ModuleAbilityLoader`: loads `/data/extremecraft/abilities/*.json`
- `com.extremecraft.modules.registry`
  - Runtime registries for armor modules, tool modules, and abilities
- `com.extremecraft.modules.item`
  - `IModularItem`, `ModuleStackData`, modular armor/tool base classes, and example gear items
- `com.extremecraft.modules.runtime`
  - `ModuleRuntimeEvents`: event-driven execution (not per-tick spam)
  - `ModuleRuntimeService`: applies stat modifiers, executes abilities, handles cooldowns
  - `ModuleAbilityClientState`: client mirror of cooldown state
- `com.extremecraft.network.packet`
  - `SyncModuleAbilityStateS2CPacket`: server->client ability cooldown sync

## Integration Points
- Player stat integration uses `PlayerStatsCapability#setEquipmentModifier/removeEquipmentModifier` and re-sync via `PlayerStatsService`.
- Skill tree integration uses `PlayerStatsCapability#isSkillUnlocked(required_skill_node)` gate per module.
- Networking integration uses `ModNetwork` packet registration and server-authoritative cooldown sync.
- Runtime path avoids heavy loops by using events and 20-tick passive checks.

## Example Flow
1. Datapack module loads into module registry.
2. Player equips modular item with installed module ids in item NBT.
3. Runtime service checks skill prerequisite.
4. Stat modifiers merge into `PlayerStatsCapability`.
5. Ability trigger (hit/right-click) executes server-side and syncs cooldown map to client.
