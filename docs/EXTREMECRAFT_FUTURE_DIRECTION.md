# ExtremeCraft Future Direction (Scalable Tech + Adventure Roadmap)

## 0) Architecture Principles
- Keep systems data-driven first (JSON definitions) and code-driven second (registries consume definitions).
- Use one module per concern: resources, machines, energy, tools, magic, progression, GUI, worldgen.
- Gate all progression through a shared unlock model (`machine:*`, `item:*`, `recipe:*`, `skill:*`, `class:*`).
- Avoid hardcoding tier logic in block entities; use tier definitions and shared machine base behavior.

## 1) Target Package Expansion
- `com.extremecraft.core`: bootstrap, module wiring, constants, cross-module service locator.
- `com.extremecraft.registry`: DeferredRegisters for blocks/items/block entities/menus/recipes/worldgen.
- `com.extremecraft.machine`: machine framework, tiers, recipes, block entities, menus.
- `com.extremecraft.energy`: ECE API, cable graph, transfer simulation, generator behaviors.
- `com.extremecraft.magic`: mana resources, arcane machines, hybrid recipes.
- `com.extremecraft.progression`: stages, skills, unlock requirements, class modifiers, quest hooks.
- `com.extremecraft.gui`: player tab menus/screens and machine UIs.

## 2) Canonical Tier Model
- `EARLY`: copper/tin/lead/silver/nickel/aluminum.
- `MID`: titanium/platinum/cobalt/ardite.
- `LATE`: uranium/iridium/osmium/mythril/void_crystal.
- `ENDGAME`: draconium/aetherium/singularity_ore.

Each material entry should define:
- `id`, `tier`, `color`, `toolStats`, `armorStats`, `oreGen`, `processing`.
- Generated content: ore block, raw ore, ingot, dust, nugget, storage block, tags, lang keys, loot table, model/state.

## 3) PHASE 1 - Core Resource Expansion
### Implementation
- Add `MaterialDefinition` + JSON loader (`data/extremecraft/materials/*.json`).
- Add `ModMaterials` bootstrap class that registers:
  - ore blocks (+ deepslate/nether/end variants if configured),
  - storage blocks,
  - raw ore / ingot / dust / nugget items.
- Add tags:
  - `forge:ores/*`, `forge:raw_materials/*`, `forge:ingots/*`, `forge:dusts/*`, `forge:nuggets/*`, `forge:storage_blocks/*`.

### Processing chain contract
- Base recipes:
  - `ore -> pulverizer -> dust`
  - `dust -> furnace/blast -> ingot`
- Tier multipliers driven by machine tier config:
  - Pulverizer T1: 1-2 dust (chance table),
  - Crusher T1: 2-3 dust,
  - Enrichment: dust -> 1-2 ingots.

## 4) PHASE 2 - Machine + Energy System
### Machine framework
- Add generic machine abstractions:
  - `MachineTierDefinition` (speed, energyPerTick, outputBonus, slots),
  - `MachineRecipeDefinition` (input/output/chance/time/energy),
  - `AbstractProcessingMachineBlockEntity` extending existing base.
- Register machines with dedicated packages:
  - `machine/pulverizer`, `machine/alloyfurnace`, `machine/crusher`, `machine/compressor`,
  - `machine/electricfurnace`, `machine/enrichment`, `machine/fluidextractor`,
  - `machine/fusionalloy`, `machine/quantumfabricator`, `machine/matterconverter`, `machine/voidreactor`.

### ECE system
- Keep FE capability compatibility but expose internal unit as `ECE`.
- Add:
  - `IECEStorage`, `ECEStorageAdapter`,
  - `CableBlock` + `CableBlockEntity` tiers: copper/gold/superconductive,
  - `EnergyGraphService` (BFS graph rebuild + transfer each tick),
  - `GeneratorBehavior` strategies (coal/steam/solar/fusion).

### Progression gates
- Extend existing stage gates to include all machine ids:
  - early: primitive/industrial,
  - mid: automation/energy,
  - late-endgame: advanced/endgame.

## 5) PHASE 3 - Tools and Equipment
- Add data-driven `ToolMaterialDefinition` and `ArmorMaterialDefinition`.
- Generate pickaxe/sword/axe/shovel/hammer for each enabled material.
- Add `HammerItem` (3x3 mining) with server-side harvest validation.
- Add armor bonus system via `ArmorSetBonusRegistry`:
  - titanium: mining speed bonus,
  - mythril: mana regen,
  - draconium: flight/hover unlock,
  - void/aether: endgame resist + utility bonuses.

## 6) PHASE 4 - Magic + Tech Hybrid
- Add resources: mana_crystal, arcane_dust, ancient_rune, void_essence.
- Add magic machines:
  - rune_infuser,
  - mana_extractor,
  - arcane_forge.
- Add hybrid recipe type:
  - input domains: item + energy + mana.
  - example: `quantum_processor + rune_core -> dimensional_core`.

## 7) PHASE 5 - Player Progression
- Expand skills JSON set:
  - mining, combat, engineering, arcane.
- Add unlock rules loader:
  - `data/extremecraft/progression/unlocks/*.json`
  - conditions: skill level, quest complete, research unlock, class, stage.
- Example:
  - mining level 10 unlocks titanium mining tag,
  - engineering 20 unlocks electric machine recipes.

## 8) PHASE 6 - Extended Player UI
- Replace current tab enum with five-tab model:
  - Inventory, Dual Wield, Magic, Player Stats, Class System.
- Back each non-vanilla tab with menu + screen pair for sync safety:
  - `DualWieldMenu/Screen`,
  - `MagicMenu/Screen`,
  - `PlayerStatsMenu/Screen`,
  - `ClassMenu/Screen`.
- Keep `ExtremePlayerTabs` as entry controller intercepting vanilla inventory open.

## 9) PHASE 7 - Endgame System
- Add machines:
  - singularity_compressor,
  - planetary_extractor,
  - dimensional_reactor.
- Add items:
  - infinity_ingot,
  - singularity_core,
  - celestial_engine.
- Add ultimate equipment recipes:
  - infinity_sword,
  - celestial_armor,
  - quantum_pickaxe.

## 10) PHASE 8 - World Generation
- Add ore generation definitions in JSON:
  - min/max height,
  - vein size,
  - veins per chunk,
  - allowed dimensions/biomes.
- Wire through `BiomeModifier` + placed/configured features in `registry/worldgen`.
- Example targets:
  - mythril: deep overworld,
  - void_crystal: end only,
  - draconium: rare overworld large-depth veins.

## 11) PHASE 9 - Reserved Hooks
- Keep extension interfaces now, implementations later:
  - `IAutomationTransport` (pipes/items/fluids),
  - `IMultiblockController`,
  - `ISpaceProgressionGate`,
  - `IDimensionTravelService`,
  - `IBossProgressionTrigger`.

## 12) Immediate Build Order (Recommended)
1. Data model foundation: material + tier JSON loaders and generators.
2. Resource registration pipeline for all ore families.
3. Machine tier framework + crusher/alloy furnace/electric furnace.
4. ECE cables + graph transfer + generator implementations.
5. Skill unlock rule engine + recipe gating integration.
6. UI tab conversion to menu-backed synced tabs.
7. Endgame machine/item vertical slice.
8. Worldgen rollout and balancing pass.

## 13) Technical Debt to Resolve Early
- Consolidate duplicated/legacy domains (`com.extremecraft.game` vs newer progression/quest modules) to one authoritative progression API.
- Standardize package naming (`machines` vs target `machine`) with staged migration to avoid breaking saves/registries.
- Keep registry names immutable once released to preserve world compatibility.
