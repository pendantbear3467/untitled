# ExtremeCraft Design Bible / First Release Workflow Spec

This document is the grounded internal design-and-workflow bible for ExtremeCraft as it exists in the current repository on March 12, 2026.

It reconciles the current runtime code, datapacks, existing reports, and design docs, especially:

- `docs/SHARED_IMPLEMENTATION_CONTRACT.md`
- `docs/FOUNDATION_CLEANUP_REPORT.md`
- `docs/FIRST_RELEASE_CONTENT_MANIFEST.md`
- `docs/gameplay_progression.md`
- `docs/ENTITY_MODEL_PIPELINE_AUDIT.md`
- subsystem READMEs under `src/main/java` and `src/main/resources`

When documents disagree, use this resolution order:

1. Live runtime code in `src/main/java` and `api/src/main/java`
2. Live datapack/resources under `src/main/resources`
3. `docs/FOUNDATION_CLEANUP_REPORT.md`
4. Other design docs and manifests

One critical boundary matters up front: only `src/main/java` and `api/src/main/java` are current runtime source sets. Top-level folders such as `core/`, `gameplay/`, `tech/`, and `worldgen/` outside `src/` are not current runtime owners. They are design/reference/future architecture material unless deliberately migrated into the live source set.

## 1. Executive Identity

ExtremeCraft is a hybrid progression mod built around one central fantasy:

The player starts as a survivor making crude tools and dirty power, becomes an industrial builder, learns controlled magic, pushes into radiation and contaminated technology, and eventually operates forbidden systems that feel large and dangerous without turning the server into rubble.

This is not meant to be just a machine mod, just an RPG class mod, or just a spell mod. The identity is the interaction between those pillars:

- progression gates tell the player when they have truly advanced
- classes and skills define how the player approaches that advancement
- machines scale production and unlock hazardous materials
- magic provides flexible utility, combat, and hybrid infrastructure
- radiation and contamination turn late power into environmental responsibility
- endgame systems offer spectacular power with strict technical safety limits

### Player Fantasy

The player fantasy is controlled escalation.

The player should feel that they are:

- extracting and refining increasingly rare and dangerous materials
- specializing without being locked into one playstyle forever
- discovering structures, labs, towers, ruins, and threat zones that matter
- earning access to stronger tools and machines instead of skipping directly to them
- building infrastructure that becomes visibly more advanced and more unstable
- making real decisions about whether to contain, weaponize, or cleanse dangerous power

### First-Release Fantasy

The first major release should feel like one coherent vertical slice:

- crude extraction and primitive power
- basic industrial processing and wiring
- the first class and skill divergence
- readable quests and guild-style advancement
- mana and spellbook access
- radiation and contamination as real pressure, not just lore
- one coherent reactor line with real risk
- one hybrid arcane-tech bridge layer
- one endgame monument/core that makes catastrophe feel intentional rather than random

### What This Mod Is Trying To Become

Long term, ExtremeCraft is trying to become a structured, expandable hybrid sandbox where:

- machines, magic, and hazardous science are all first-class systems
- content is mostly data-driven where practical
- addon/modular extension is possible without rewriting the foundation
- dangerous systems feel enormous in gameplay while remaining server-responsible

For first release, the right goal is not "ship every concept in the repo." The right goal is "ship one stable and honest version of the mod's identity."

## 2. Current Project State

### Runtime Boundary

Current live runtime sources:

- `src/main/java`
- `api/src/main/java`
- `src/main/resources`

Current non-runtime but important reference/design areas:

- top-level `core/`
- top-level `gameplay/`
- top-level `tech/`
- top-level `worldgen/`
- generated reports under `docs/`

### State Matrix

| Domain | State | Live owner | Grounded current reality |
| --- | --- | --- | --- |
| Progression stages and unlocks | IMPLEMENTED | `com.extremecraft.progression`, `data/extremecraft/progression` | Canonical stage ladder exists, stage/unlock loaders exist, runtime grants/checks exist, but authority drift remains around legacy mirrors. |
| Player XP and progression mutation | IMPLEMENTED | `ProgressionMutationService`, `ProgressionFacade`, `PlayerProgressData` | The canonical mutation path is present and intentionally central, but some legacy services still mirror data. |
| Skills | PARTIAL | `com.extremecraft.skills`, `com.extremecraft.progression.skilltree`, `data/extremecraft/skills`, `data/extremecraft/skill_trees` | Skill XP and skill trees exist, but skill-point authority is split between progression data and `PlayerStatsCapability`. |
| Classes | PARTIAL | `com.extremecraft.progression.classsystem`, `data/extremecraft/classes`, `data/extremecraft/class_abilities` | Data-driven classes exist, but legacy class bridge code is still present and quest rewards still reference older class ids. |
| Guild quests | PARTIAL | `com.extremecraft.quest`, `data/extremecraft/extremecraft_quests` | Quest runtime and rewards are live, but the older `data/extremecraft/quests` path still exists as misleading metadata. |
| Research | PARTIAL | `com.extremecraft.research` | There is real runtime/data capability, but it is not yet the clean, dominant first-release gate. |
| Machines | IMPLEMENTED | `com.extremecraft.machine.core`, `com.extremecraft.machine.recipe`, `com.extremecraft.future.registry` | The real machine chain is live. Some older machine paths and metadata folders still exist and can mislead contributors. |
| Machine recipes | IMPLEMENTED | `data/extremecraft/recipes/machine_processing` | Generated machine recipes are live and substantial. |
| Power and cables | IMPLEMENTED | `com.extremecraft.machine.cable`, tech machine entities | FE-style generation and transfer are live. Tiered cables exist. |
| Radiation, dose, contamination | IMPLEMENTED | `com.extremecraft.radiation`, `data/extremecraft/radiation_sources`, `data/extremecraft/contamination`, `data/extremecraft/contamination_terrain` | The hazard pipeline exists end-to-end and should remain the only contamination authority. |
| Reactors, meltdown, destructive safety caps | PARTIAL | `com.extremecraft.reactor`, `ECDestructiveEffectService` | Backend exists, meltdown logic exists, safe world-edit caps exist, but content naming and first-release reactor identity still need convergence. |
| Magic and spells | IMPLEMENTED | `com.extremecraft.magic`, `data/extremecraft/spells` | Data-driven spell loading and server-authoritative casting are real. |
| World magic / Aether | PARTIAL | `AetherNetworkService`, ritual services | Chunk-level Aether reserve/recharge exists, but world infrastructure is backend-heavy and content-light. |
| Hybrid magic-tech | PARTIAL | magic services, machine catalog, `HybridCraftingRecipe` | The bridge exists in pieces, but the full player-facing chain needs first-release curation. |
| Materials / ores / alloys | PARTIAL | `OreMaterialCatalog`, `ModMaterials`, `future.registry`, some data materials | Real material families exist, but material ownership is not fully converged. |
| Worldgen and content discovery | IMPLEMENTED | `data/extremecraft/worldgen`, `data/extremecraft/forge/biome_modifier`, worldgen helpers | Ores, structures, and spawn injection are live. |
| Bosses / events / danger zones | PARTIAL | entity systems plus worldgen/spawn data | Bosses and structure themes exist, but the reward and progression contract is not fully standardized. |
| UI / HUD / player screens | PARTIAL | `com.extremecraft.client.gui.player`, overlays, machine screens | Unified player screen exists; machine screen exists; radiation/reactor feedback still has synchronization and polish gaps. |
| Entity rendering and model pipeline | IMPLEMENTED | Java renderer/model packages | Runtime render ownership is clear; JSON asset paths are handoff metadata, not live runtime owners. |
| Modules / addon API | PARTIAL | `com.extremecraft.modules`, `com.extremecraft.api`, module docs | A real extension vision exists, but first release should keep this practical and not overpromise marketplace maturity. |
| Debug / validation / tooling | IMPLEMENTED | `com.extremecraft.dev.validation`, platform validators, generated reports | The repo already contains validation and reporting systems that should continue to support safe growth. |
| Asset pipeline / Blockbench readiness | PARTIAL | assets under `assets/extremecraft`, renderer/model bindings, audit docs | The project is placeholder-safe and replacement-ready, but not final-art-complete. |
| Top-level non-`src` architecture folders | LEGACY / REFERENCE-ONLY | N/A | Useful for direction and planning, not live gameplay truth. |

### Domain Notes That Matter

#### Progression

Current canonical runtime progression ladder:

- `PRIMITIVE`
- `ENERGY`
- `INDUSTRIAL`
- `ADVANCED`
- `ENDGAME`

This is the current live truth in `ProgressionStage`. `AUTOMATION` exists only as a legacy alias mapped back to `INDUSTRIAL`.

#### Machines

Current live machine owner chain:

1. `MachineCatalog` defines machine ids and tier/category metadata
2. `TechBlocks` / `future.registry` register the actual blocks and items
3. `TechMachineBlockEntity` drives runtime machine behavior
4. `machine.recipe` and live datapack recipes determine processing inputs/outputs
5. `ProgressionGate` and stage data determine access

The `data/extremecraft/machines` folder is not the current runtime owner for machine behavior.

#### Quests

Current live quest owner chain:

1. `QuestManager`
2. `GuildQuestRewardService`
3. `data/extremecraft/extremecraft_quests`

The older `data/extremecraft/quests` path is not the canonical first-release quest authority.

#### Contamination

Current live contamination owner chain:

1. `RadiationService` tracks exposure and contamination release
2. `ChunkContaminationService` stores and decays chunk contamination
3. `ContaminationTerrainService` applies terrain mutation and cleanup hooks
4. `RadiationProtectionService` controls protection and cleanup effectiveness

This pipeline already exists. It should be extended, not replaced.

#### Entity Visuals

Current live visual owner chain:

1. entity classes in `com.extremecraft.entity`
2. renderer classes in `com.extremecraft.client.render.entity`
3. model classes in `com.extremecraft.client.model.entity`

`assets/extremecraft/entities` and `assets/extremecraft/models/entity` are support/handoff metadata, not the authoritative runtime bind point.

## 3. Canonical Gameplay Pillars

### Progression And Stage System

What it does:

- defines the player's macro advancement
- gates machines, recipes, unlocks, and some content expectations
- gives the rest of the mod a single ladder to talk to

What it should feel like:

- every new stage should change what the player can build, not just add bigger numbers
- stage advancement should feel earned through infrastructure and milestones
- content should not silently unlock from hidden one-off logic when a stage gate already exists

Canonical rule:

All major machine and content access should route through the stage system first, then optionally layer skill/class/quest requirements on top.

### Skill Progression

What it does:

- tracks player expertise in repeatable play domains such as mining, combat, engineering, and arcane use
- creates specialization pressure without replacing the main stage ladder
- makes the player's activity history matter

What it should feel like:

- skill growth should reward sustained use of a domain
- skills should improve efficiency, stability, survival, or utility
- skills should not become a second hidden progression ladder that bypasses stage gating

Canonical role:

Skills are "how good the player is at a domain," not "whether the player is globally allowed to skip tech tiers."

### Class Progression

What it does:

- defines the player's chosen identity and action style
- controls access to class-bound abilities and some spell/ability combinations
- gives guild/quest progression a meaningful reward beyond raw items

What it should feel like:

- choosing a class should meaningfully bias the player's playstyle
- classes should not hard-lock the entire mod away from the player
- advanced hybrid classes should feel earned, not selected at spawn

Canonical first-release role:

Classes should shape combat, support, casting, and efficiency patterns. They should not replace the universal stage ladder for infrastructure unlocks.

### Guild / Quest Progression

What it does:

- gives authored direction inside a sandbox mod
- rewards exploration, crafting, combat, and milestone completion
- grants class access, points, and sometimes stage-related rewards

What it should feel like:

- a structured path for players who want clear goals
- a reinforcement layer for existing systems, not a full replacement for sandbox discovery
- a way to tie factions, guilds, and role identity into gameplay

Canonical first-release role:

Guild quests are the main authored bridge between player activity and class unlock progression.

### Machine Progression

What it does:

- moves the player from manual processing to automated industry
- turns materials into systems rather than isolated items
- supports resource throughput, crafting scale, and higher-risk production

What it should feel like:

- each machine tier should reduce friction while introducing new dependencies
- better machines should be about chain depth, not just speed multipliers
- advanced machines should be legible in both recipes and progression gates

Canonical first-release role:

Machines are the backbone of the industrial half of the mod. They are the most important bridge between ore acquisition, hazardous materials, power growth, and endgame construction.

### Magic Progression

What it does:

- gives the player direct action tools, mobility, control, healing, and rituals
- creates a second style of infrastructure through Mana and world Aether
- opens the door to hybrid machine-spell systems

What it should feel like:

- responsive and expressive in the moment
- structured and bounded in the long term
- expandable without becoming an unmaintainable spell soup

Canonical first-release role:

Magic should ship as a real, usable progression lane with a spellbook, mana, a world Aether backend, a set of low/mid spells, and a small number of dangerous late rituals.

### Power Progression

What it does:

- drives the industrial machine stack
- creates stage transitions the player can feel in base design
- connects dangerous resource chains to visible output

What it should feel like:

- early power is dirty and limited
- mid power is logistical and scalable
- late power is dangerous, unstable, and spectacular

Canonical first-release role:

Power progression is not just a number curve. It is one of the main storytelling devices of the mod.

### Contamination / Radiation Progression

What it does:

- turns radioactive materials, failed reactor control, and weaponized systems into persistent world consequences
- creates environmental problems the player must manage
- ties danger, cleanup, protection, and material handling together

What it should feel like:

- rising pressure instead of arbitrary punishment
- understandable risk with readable warning signs
- a late-game cost that changes behavior and building practices

Canonical first-release role:

Radiation and contamination are core identity systems, not optional flavor. They should be one coherent hazard pipeline across materials, reactors, spells, and weapons.

### Endgame Power / Forbidden Systems

What it does:

- gives the player access to catastrophic power and reality-bending machinery
- creates a reason for all earlier infrastructure to exist
- provides memorable goals beyond "more ore throughput"

What it should feel like:

- monumental
- intentionally overpowered
- tightly bounded by server-safe execution rules

Canonical first-release role:

The mod should have one late-game forbidden slice, not ten separate unfinished apocalypses. That slice can include catastrophic spells, singularity-level crafting, and an expandable core structure, but it must remain technically responsible.

## 4. First Release Content Pillars

### A. Power Progression

The intended first-release ladder is:

1. primitive fuel power
2. industrial/mechanical power
3. arcane-tech power
4. nuclear/fission power
5. sci-fi/stellar/endgame power
6. expandable energy-core monument

#### Tier 1: Primitive Fuel Power

Player fantasy:

- "I am building dirty but functional survival industry."

Intended machines and generators:

- `coal_generator`
- `crusher`
- `smelter`

Input/output logic:

- consumes simple fuel
- powers basic ore and material processing
- enables the first meaningful throughput increase over furnace-only play

Resource chain:

- stone and iron-era tools
- copper, tin, early lead, coal
- crude processed outputs such as dusts and smelted materials

Expected risks/failure states:

- fuel starvation
- slow throughput
- no meaningful automation buffer yet

UI/readout requirements:

- fuel burn bar
- energy buffer
- active recipe and progress
- tooltip showing stage requirement and machine role

Progression placement:

- this is the first real machine gate after early survival and mining

Current repo grounding:

- this tier is live and already represented in `MachineCatalog`, machine runtime, and primitive stage data

#### Tier 2: Industrial / Mechanical Power

Player fantasy:

- "I am no longer surviving by hand. I am running infrastructure."

Intended machines and generators:

- `steam_generator`
- `solar_generator`
- `industrial_generator`
- `electric_furnace`
- `enrichment_chamber`
- `advanced_pulverizer`
- `fluid_extractor`
- `alloy_furnace`
- `compressor`
- cable network tiers

Input/output logic:

- multi-step ore refinement
- energy generation plus distribution
- higher throughput processing chains
- early byproducts and alloy dependencies

Resource chain:

- copper/gold cable tiers
- steel/bronze/titanium-alloy direction
- fluids and refined dusts
- larger recipe trees and automation loops

Expected risks/failure states:

- network bottlenecks
- underpowered cable tiers
- recipe chain confusion if tooltips are poor

UI/readout requirements:

- input/output slots
- energy in/out and storage
- processing time
- cable tier capacity or tooltip-level throughput information

Progression placement:

- this is the main middle-game foundation and the first major automation threshold

Current repo grounding:

- this tier is live in machine runtime and stage data

#### Tier 3: Arcane-Tech Power

Player fantasy:

- "I am starting to bend machine systems with world magic rather than only combustion and industry."

Intended machines and generators:

- `mana_extractor`
- `rune_infuser`
- `arcane_forge`
- optionally migrated future machines such as `aether_collector`, `rune_dynamo`, `mana_aether_condenser`, `arc_infusion_machine` only if they are fully moved into the live machine chain

Input/output logic:

- dual dependency on machine materials and magical reagents
- conversion between crafted magical ingredients, Mana-adjacent items, and Aether-consuming processes
- hybrid crafting and rune-infused processing

Resource chain:

- `mana_crystal`
- `arcane_dust`
- `ancient_rune`
- `rune_core`
- contaminated or special structure rewards where appropriate

Expected risks/failure states:

- confusing FE vs Mana vs Aether semantics if UI is unclear
- overtuned spell support that bypasses machine progression

UI/readout requirements:

- machine energy buffer
- if applicable, Aether draw/readiness display
- rune/reagent slots
- clear failure messages for missing magical prerequisites

Progression placement:

- this should be the bridge between pure industrial infrastructure and hazardous advanced systems

Current repo grounding:

- parts of this are live now
- several thematic machine ids exist only as metadata and should not be treated as shipped first-release content unless migrated into `MachineCatalog` and the live registry/runtime chain

#### Tier 4: Nuclear / Fission Power

Player fantasy:

- "I can produce enormous power, but now my base design, material handling, and mistakes have consequences."

Intended machines and generators:

- one canonical first-release fission reactor line
- fuel preparation and hazard handling support machines
- reactor control and shielding parts

Input/output logic:

- fuel insertion
- heat generation
- cooling/moderation
- waste accumulation
- radiation leakage and meltdown risk
- high FE-style output when stable

Resource chain:

- uranium
- thorium
- lead
- reactor graphite
- shielding and coolant materials

Expected risks/failure states:

- rising heat and reactivity
- insufficient cooling or shielding
- ambient radiation
- meltdown release
- chunk contamination and terrain mutation

UI/readout requirements:

- heat
- reactivity
- steam/coolant state
- waste
- ambient radiation
- SCRAM state
- structural validity
- meltdown warning

Progression placement:

- late advanced tier
- the major hazard/power inflection point of first release

Current repo grounding:

- reactor backend, fuel logic, safety, meltdown, and destructive caps are real
- first-release identity must converge on one fission line even though the current repo still contains `fusion_reactor` naming in the machine catalog and some services recognize both `fission_reactor` and `fusion_reactor`

Canonical first-release instruction:

Ship one reactor family with one name and one content story. Do not split the first release across parallel fission and fusion trees.

#### Tier 5: Sci-Fi / Stellar / Endgame Power

Player fantasy:

- "I am operating machinery that no longer feels merely industrial."

Intended machines and generators:

- `void_reactor`
- `singularity_compressor`
- `planetary_extractor`
- `dimensional_reactor`

Input/output logic:

- rare materials
- high-tier crafted cores
- extreme throughput or extraction
- world/multiblock validation
- growing instability under sustained output

Resource chain:

- `void_essence`
- `dimensional_core`
- `singularity_core`
- `celestial_engine`
- endgame ores and rare structure/boss rewards

Expected risks/failure states:

- instability
- Aether drain
- catastrophic overcharge events
- failure to meet structure requirements

UI/readout requirements:

- throughput
- instability
- structure validation state
- overcharge
- safety interlock state

Progression placement:

- true endgame
- should exist as a controlled capstone, not as broad mid-game clutter

Current repo grounding:

- the machine ids, blocks, items, recipes, and some validation backends are real
- the gameplay content around them is still partly scaffolded and should remain narrow for first release

#### Tier 6: Expandable Multiblock Energy-Core Monument

Player fantasy:

- "I have built a monument-scale machine that stabilizes impossible power."

Intended form:

- one expandable core centered on `dimensional_reactor`
- staged structure growth
- visible safety architecture such as rings, pylons, shells, and interlocks

Input/output logic:

- consumes Aether and high-tier crafted components
- grows stronger as structure stages validate
- accumulates instability/overcharge if starved or misbuilt

Expected risks/failure states:

- invalid structure
- insufficient Aether supply
- instability buildup
- capped destructive discharge if overcharged

UI/readout requirements:

- active core stage
- structure validity
- Aether consumption
- overcharge
- instability

Progression placement:

- final monumental progression destination of the first release

Current repo grounding:

- `EndgameCoreStructureService` is real runtime backend
- `data/extremecraft/endgame_core/stage_requirements.json` exists but is scaffold-grade
- fallback runtime definitions currently carry much of the real behavior

Canonical first-release instruction:

Treat the endgame core as a narrow, heavily curated capstone system. Do not try to explode it into a whole separate tech tree before the first release is stable.

### B. Nuclear / Radiation / Contamination

This is one of ExtremeCraft's defining systems and it already has enough real backend to be treated as canonical.

#### Intended First-Release Nuclear Material Loop

The first-release nuclear loop should be:

1. discover radioactive ores or dangerous structures
2. refine uranium/thorium with increasingly specialized infrastructure
3. use lead and related shielding/protection materials to survive handling
4. operate one reactor path for major power generation
5. manage waste, contamination, and cleanup
6. optionally weaponize that chain through tightly bounded devices

#### Radioactive Materials And Processing

Grounded materials already present or partially present:

- uranium: live ore/material family and radiation source anchor
- thorium: partially present, recognized by reactor fuel logic, not fully converged in the main ore catalog
- lead: live shielding/protection material
- reactor graphite: present in data/material direction, not fully converged
- irradiated crystal: data-defined bridging concept

Design role:

- uranium should be high-output, high-risk fuel and ordnance material
- thorium should be slower, safer, and more stable, giving a lower-risk reactor path
- lead should be the default shielding/protection material for equipment and cleanup efficiency
- reactor graphite should be the clean moderator/build component in reactor assembly
- irradiated crystal should bridge contamination and advanced arcane-tech crafting

#### Exposure Vs Accumulated Dose

Canonical behavior:

- ambient exposure is the current environmental pressure around the player
- accumulated dose is the player's persistent health risk over time
- contamination is the world's persistent environmental state

Why this matters:

- it lets the mod communicate immediate danger separately from long-term consequences
- it gives cleanup and protective gear a meaningful role

Grounded repo state:

- `RadiationService` already stores and updates player-side radiation state and contamination pressure

#### Chunk Contamination

Canonical behavior:

- chunk contamination is the persistent world-level hazard store
- contamination increases from releases, meltdowns, catastrophic spells, dirty devices, and other hazardous outputs
- contamination decays slowly but should not vanish instantly

Grounded repo state:

- `ChunkContaminationService` already stores chunk contamination as `SavedData`
- decay interval/config exists

#### Contamination Terrain Mutation

Canonical behavior:

- world contamination should not instantly repaint huge areas
- terrain mutation should happen in pulses and profiles
- conversion should be staged, capped, and mechanically meaningful

Grounded repo state:

- `ContaminationTerrainService` already supports profile-driven terrain mutation
- the current pipeline supports contaminated block families and vitrified escalation

First-release terrain family expectation:

- contaminated dirt
- contaminated stone
- contaminated sand
- contaminated grass
- contaminated wood where appropriate

These should remain visually distinct and mechanically readable.

#### Cleanup, Scrubbers, Containment, And Protection

Canonical first-release toolkit:

- hazard armor and especially lead-based protection
- cleanup tools or machines
- radiation-resistant build materials
- localized cleanup and block conversion reversal hooks where appropriate
- hard distinction between "protected handling" and "reckless exposure"

Grounded repo state:

- `RadiationProtectionService` already models armor protection and cleanup effectiveness
- block-break cleanup hooks already exist in contamination terrain logic

#### Meltdown Events

Canonical behavior:

- meltdown is a major failure state, not a common accident
- it should create local destruction, radiation release, contamination, and follow-up gameplay
- it should never run uncapped world destruction

Grounded repo state:

- `ReactorSafetyService` can trigger meltdown
- `RadiationService.releaseMeltdown` exists
- `ECDestructiveEffectService` already uses capped queued destructive pulses
- config already limits radius, budget, and pulse batch size

#### Tactical Nukes, Dirty Bombs, And Superweapons

Design rule:

These systems can feel terrifying and powerful without becoming technically irresponsible.

Implementation guidance:

- dirty bombs should favor contamination spread over huge crater radius
- tactical nukes should use capped blast radii and block budgets
- catastrophic spells and endgame weapons should require explicit setup, rare materials, or staged arming
- post-impact contamination should usually matter more than raw terrain deletion
- weapon effects should route through `ECDestructiveEffectService` plus contamination release, not custom uncontrolled world edits

What first release should ship:

- one dirty-device style hazard weapon
- one tactical nuke style catastrophic weapon
- one truly forbidden endgame effect tied to heavy prerequisites

What first release should not ship:

- a dozen different bomb classes with overlapping code paths
- uncapped terrain wipes
- "secret" destruction systems outside the canonical safety-capped effect service

### C. Magic System

The current repo already contains enough real magic runtime to treat magic as a core first-release pillar.

#### Intended Architecture

Core runtime pieces already present:

- spell definitions in `data/extremecraft/spells`
- `SpellLoader` and `SpellRegistry`
- `SpellCompiler`
- `SpellValidationService`
- `SpellExecutor` and `SpellService`
- `ManaService` and `ManaCapability`
- `AetherNetworkService`
- `RitualService`
- `SpellBookItem`

Canonical first-release architecture:

- spellbooks are the player-facing casting container
- mana is the personal casting resource
- Aether is the world-level magical reserve used by rituals, advanced effects, and some hybrid systems
- spells are data-driven definitions compiled into safe executable behavior
- dangerous spell effects must pass through the same safety-capped destructive pipeline as tech catastrophes

#### Spellbooks

First-release role:

- a spellbook should be the main "I am a caster now" object
- it should let the player cast selected or bound spells reliably
- it does not need a fully in-book visual composition editor for first release

Grounded repo state:

- `SpellBookItem` already supports bound-spell use
- a deeper composition UI is not yet the live reality

#### Mana

Canonical design:

- mana is a personal, regenerating casting resource
- mana governs frequent spell use and short-form class abilities
- mana should remain visually obvious to the player

Grounded repo state:

- `ManaCapability` and `ManaService` already exist

#### World Magic Infrastructure

Canonical design:

- Aether is not "just more mana"
- Aether is world energy, chunk-level magical reserve, and ritual infrastructure fuel
- advanced spellcraft and hybrid tech should stress world Aether, not only the player's blue bar

Grounded repo state:

- `AetherNetworkService` already uses chunk-level reserve/recharge storage
- placeable network-style infrastructure is still more vision than finished feature

#### Spell Schools, Forms, Effects, And Modifiers

Grounded repo state:

- spell definitions already carry school, form, effects, modifiers, particles, sounds, summon targets, range, radius, and catastrophic metadata

Canonical first-release design:

- schools provide identity and filtering
- forms define delivery style such as projectile, blink, summon, ritual, or area release
- effects define outcome
- modifiers tune scale/cost/risk

Important current truth:

- `data/extremecraft/spell_schools/first_release_schools.json` exists as scaffold metadata, but there is no clear active school loader providing gameplay authority today

Therefore:

- spell school presentation can exist in first release
- spell school infrastructure should not be overstated as fully finished data authority until the runtime loader is real

#### Rituals, Summoning, And Forbidden Spells

First-release role:

- rituals should be slower, more infrastructural, and more Aether-dependent than basic spells
- summoning should remain limited and themed
- forbidden spells should be few and memorable

Grounded repo state:

- ritual and summon execution pathways exist
- catastrophic spell support already exists
- examples such as `dirty_bomb_cloud`, `tactical_nuke_protocol`, and `event_horizon` are already present as spell data

#### What First Release Should Ship

- a functional spellbook
- readable mana HUD
- a set of low-tier utility/combat spells
- a set of mid-tier control or mobility spells
- a small number of ritual or catastrophic late spells
- class-bound or class-favored casting access where that improves identity

#### What Should Remain Future-Phase

- a massive combinatorial spellcraft editor
- dozens of half-distinct spell schools without clear runtime authority
- uncontrolled spell chaos that bypasses progression, Aether cost, or safety caps

### D. Hybrid Magic-Tech Systems

ExtremeCraft is at its strongest when magic and tech meet in a controlled way rather than living as isolated mod halves.

#### Canonical Bridge Layer

The bridge tier should revolve around:

- machine systems that consume or manipulate magical resources
- rituals or spells that support industrial infrastructure
- materials that only make sense because machine and magical systems meet
- late systems that unify Aether, FE-style power, and dangerous matter

#### Aether / World Magical Energy

Canonical rule:

- mana is player-level
- Aether is world-level
- dual-input systems should never hide which one they are using

#### Magic-Powered Machines

Current likely first-release candidates:

- `mana_extractor`
- `rune_infuser`
- `arcane_forge`

Potential later migration candidates that are not live owners yet:

- `aether_collector`
- `rune_dynamo`
- `mana_aether_condenser`
- `arc_infusion_machine`

Instruction:

Do not sell metadata-only machine ids as finished content. Promote them into first release only if they are moved into the real runtime chain.

#### Dual-Input FE + Magical Systems

The right first-release pattern:

- FE-like machine energy handles throughput and baseline operation
- Aether or magical reagents gate special operations, infusions, or unstable outputs
- some endgame systems require both

This lets hybrid machines feel special without replacing every ordinary industrial machine.

#### Star-Core Batteries, Singularity Crafting, And Void-Tech

Grounded repo signals already present:

- `singularity_compressor`
- `dimensional_reactor`
- `planetary_extractor`
- `void_reactor`
- `celestial_engine`
- `singularity_core`
- `dimensional_core`

Canonical role:

- these are not just "bigger generators"
- they are the bridge from advanced infrastructure into forbidden, monument-scale systems

### E. Materials And Content Families

The material ecosystem needs to remain honest about what is already real versus what is still conceptual.

| Material or family | State | Intended role in the design |
| --- | --- | --- |
| Uranium | IMPLEMENTED | Primary dangerous fuel, radiation source, high-risk reactor input, late ordnance ingredient, contamination driver. |
| Thorium | PARTIAL | Safer/slower nuclear fuel path, alternative reactor material, should support stable-but-lower-output operation. |
| Lead | IMPLEMENTED | Shielding, hazard gear, cleanup bonuses, contaminated-zone survivability, reactor and ordnance support. |
| Reactor graphite | PARTIAL | Moderator/build component for first-release reactor line; should not remain only a loose data note. |
| Irradiated crystal | PARTIAL | Bridge material between contamination, arcane-tech, and advanced hazard crafting. |
| Mana crystal | IMPLEMENTED | Basic magical battery/catalyst, spell and hybrid machine ingredient. |
| Ancient rune / rune-core family | IMPLEMENTED | Functional stand-in for runestone-style magical components in the current repo; should feed ritual, rune infusion, and hybrid crafting. |
| Aetherium | PARTIAL | Existing high-tier magical/endgame structural material family in runtime fallback/core logic; likely the practical current stand-in for any future "aetherite" naming. |
| Draconium | PARTIAL | Endgame structural/power material used in fallback core validation and high-tier crafting direction. |
| Void essence / void crystal family | PARTIAL | Bridge from dangerous dimensions/structures into void-tech and forbidden systems. |
| Dimensional core | IMPLEMENTED | Endgame machine/core assembly component. |
| Singularity core | IMPLEMENTED | Endgame compression/catastrophe assembly component. |
| Celestial engine | IMPLEMENTED | High-tier crafted endgame component for stellar-scale systems. |
| Radglass | DESIGN-ONLY | Not currently real in the scanned runtime; reserve for future hazard-building, viewing ports, or contamination-safe architecture. |
| Voidsteel | DESIGN-ONLY | Not currently real in the scanned runtime; reserve for future void-machine and armor bridging if it earns a place. |
| Star alloy | DESIGN-ONLY | Not currently real in the scanned runtime; reserve for future stellar-tier expansion, not first-release obligation. |
| Aetherite | DESIGN-ONLY | Not currently real by that name; if introduced later, converge naming with existing Aetherium direction instead of creating parallel near-duplicates. |

Canonical material rule:

First release should rely on materials that are already grounded in runtime catalogs or clearly near-runtime data. Concept-only materials should stay concept-only until they earn a real content chain.

## 5. Exact Player Workflow

This is the intended player journey from a fresh world to late game.

### 1. Early Survival And Crude Extraction

The player begins in ordinary survival conditions but should quickly notice that the world holds unusual ores, strange ruins, and future danger.

Primary activities:

- basic survival and ore gathering
- early exploration for structures and region discovery
- first XP from mining, crafting, combat, and movement through the world

Player feeling:

- "This looks like survival now, but something much larger is ahead."

### 2. Primitive Processing And First Power

The player reaches the `PRIMITIVE` stage and builds:

- `crusher`
- `smelter`
- `coal_generator`

This is the first point where the mod stops being only "items in recipes" and becomes a real system mod.

Player feeling:

- "I can now refine and scale basic resources, but my setup is crude and fragile."

### 3. First Infrastructure And Energy Stage

The player reaches `ENERGY` and begins using:

- cable networks
- `electric_furnace`
- `enrichment_chamber`
- `advanced_pulverizer`
- `steam_generator`
- `solar_generator`
- `industrial_generator`

This is the first real automation threshold.

Player feeling:

- "I have a workshop now, not just a camp."

### 4. Guild Direction, Skills, And The First Identity Split

The player begins to feel authored direction through quests and gains:

- player XP
- player skill points
- class skill points
- possibly a class unlock or stage reward

Meanwhile activity naturally improves domain skills such as:

- mining
- combat
- engineering
- arcane

Player feeling:

- "The game recognizes how I play, and I am starting to specialize."

### 5. Industrial Expansion And Material Depth

The player pushes into alloys, fluids, compression, and larger processing loops.

Typical goals:

- build `alloy_furnace`
- build `compressor`
- deepen ore refinement
- stabilize larger machine arrays

This is where engineering identity should start to matter mechanically, even for players not formally using the engineer class.

Player feeling:

- "My base now has dependencies, throughput, and planning."

### 6. Structure Discovery And Early Advanced Signals

The player discovers themed structures such as:

- `arcane_tower`
- `ancient_research_lab`
- `collapsed_energy_reactor`
- `machine_fortress`
- `void_temple`

Boss or elite entities, danger zones, and loot should start foreshadowing the late game.

Player feeling:

- "The world is teaching me what powers exist beyond my current tier."

### 7. First Serious Magic Access

The player gets a spellbook, a readable mana bar, and a real spell set.

Typical early magical actions:

- combat projectile spell
- blink or movement spell
- healing/support spell
- basic ritual experimentation

Player feeling:

- "Magic is a real lane, not just a gimmick."

### 8. Radiation And Hazard Introduction

The player begins handling:

- uranium
- thorium
- contaminated structure loot
- hazardous machine outputs

The player learns:

- dose is persistent
- contamination is environmental
- lead and protective gear matter
- dirty choices create cleanup work later

Player feeling:

- "Progress now has consequences."

### 9. Reactor Chain And Hazard Management

The player assembles the first canonical reactor path, ideally one fission line, and must manage:

- fuel
- heat
- reactivity
- waste
- radiation
- structural validity
- emergency shutdown

This is the point where tech power becomes truly dangerous.

Player feeling:

- "I can produce immense power, but I am now responsible for containing it."

### 10. Hybrid Magic-Tech Systems

With both industrial and magical infrastructure established, the player enters the bridge layer:

- rune infusion
- mana extraction or magical processing
- Aether-consuming systems
- hybrid crafting

This tier should make the player feel that the two halves of the mod have finally connected.

Player feeling:

- "My machines and magic are no longer separate hobbies."

### 11. Endgame Core, Singularity, And Forbidden Systems

The player assembles monument-scale systems such as:

- `dimensional_reactor`
- `singularity_compressor`
- `planetary_extractor`
- endgame core stages around the dimensional reactor controller

Dangerous endgame spells or weapons become possible, but only with heavy prerequisites and safety-capped execution.

Player feeling:

- "I am now operating things that should not exist."

### 12. Endgame Loop

The endgame loop should be:

- maintain dangerous infrastructure
- solve instability and contamination problems
- pursue singularity-tier crafting and final structures
- defeat endgame threats or secure endgame regions
- choose whether to contain power or weaponize it

That is a complete gameplay arc. First release does not need every future system beyond that.

## 6. System Interaction Map

This section defines how the major systems are meant to interact and what each system is allowed to own.

### Skills Vs Classes

- skills represent earned expertise from repeated play
- classes represent chosen identity and access patterns
- skill gain should come from actions
- class unlocks should come primarily from guild/quest progression
- skills may improve class performance
- classes should not replace broad skill progression

Canonical example:

- an engineer-class player may be better positioned to use engineering-heavy abilities
- engineering skill itself should still come from building and operating the industrial stack

### Quests Vs Classes

- quests should be the main authored path into class access and class advancement rewards
- classes should not be granted by random hidden logic
- if a quest grants a class, that class id must resolve against the canonical class data path

Current repo truth:

- the reward machinery exists
- the class taxonomy currently drifts between old ids and newer data-driven class definitions

### Combat Vs Skills

- combat events grant combat-oriented growth
- combat rewards should not silently unlock industrial tiers
- combat should contribute to the player's build without replacing machine progression

### Machines Vs Progression Stages

- stages gate machine access
- machines in turn generate the outputs needed to reach later stages
- machine unlocks should not be hidden in random side systems if they already belong in the stage/unlock path

### Magic Vs Machines

- magic offers flexibility, mobility, healing, control, and some special crafting
- machines offer throughput, automation, bulk conversion, and infrastructure
- hybrid machines should require both sides to be meaningful
- magic should not trivialize industrial logistics

### Mana Vs Aether / World Infrastructure

- mana is the player's bar
- Aether is world reserve/infrastructure fuel
- the UI must always show which one is being spent
- rituals and endgame hybrid systems should lean harder on Aether than on personal mana

### Radiation Vs World Events

- hazardous structures, reactor accidents, and certain forbidden effects should contribute to contamination
- danger zones should feel persistent and readable
- cleanup should be a system, not just a temporary potion effect response

### Nuclear Systems Vs Contamination

- nuclear systems are one of the main contamination sources
- contamination should not be a totally separate mechanic from radiation
- ordnance, meltdowns, and reactor leakage should all route through the same contamination authority

### Bosses / Events Vs Progression Rewards

- bosses and major events should primarily grant rare materials, components, region flags, or authored milestone rewards
- they can support progression, but should not become a replacement for the main stage system
- if they grant unlocks, those grants should be explicit and documented

### Worldgen Vs Progression

- worldgen provides access to the resources and locations needed for advancement
- worldgen should not itself be the progression owner
- stage gates, quest rewards, class unlocks, and skill thresholds own access logic
- worldgen owns supply and discovery, not permission

### Explicit Ownership Of XP, Unlocks, Gates, And Resources

Grants XP:

- general play events through `ProgressionEvents`
- domain actions through the skill progression path
- quest completions through guild rewards

Grants unlocks:

- `ProgressionGate`
- stage data
- unlock rules
- guild quest rewards for classes or staged advancement

Gates access:

- progression stages
- unlock rules
- class/ability/spell binding checks

Provides resources:

- worldgen
- structures
- bosses
- machine throughput
- contamination cleanup outputs

Canonical rule:

Resources are not permissions. Unlocks are permissions.

## 7. UI / UX / Feedback Design

ExtremeCraft is too system-heavy to survive on "players will figure it out." The UI must carry a large share of the design burden.

### Current Grounded UI Reality

Live player-facing UI pieces already present:

- unified player screen in `ExtremePlayerScreen`
- tabbed sections for player stats, skills, magic, class skills, dual wield, and modules
- wrapper screens that now route into the unified player UI
- ability bar overlay
- mana HUD
- radiation overlay area
- machine GUI with side diagnostics panel

### Minimum Alpha Readability Requirements

These are non-negotiable for a stable alpha.

- every machine tooltip or GUI must clearly communicate what it does
- every major gated machine or unlockable item must expose its required stage
- the player screen must make level, skills, class state, and class skill points legible
- the mana HUD must always be readable while casting
- the radiation HUD must distinguish immediate danger from lingering dose/contamination pressure
- reactor controls must show heat, risk, and emergency shutdown state
- contamination-causing items and blocks must look and read as dangerous
- destructive or catastrophic abilities must telegraph their risk clearly before activation
- error states must use direct wording such as "Requires ADVANCED stage" or "Insufficient Aether"

### Beta Polish Goals

- custom reactor readout screens instead of inferred generic diagnostics
- better warning art, sounds, sirens, and flashing risk states
- stronger icon language for hazardous, magical, contaminated, and endgame items
- better spellbook presentation
- cleaner class and skill tree layout
- more visual identity in particles, world effects, and machine animations

### Machine GUIs

Design requirement:

- a machine GUI must tell the player whether the problem is power, input, output, progression, or hazard state

For first release:

- reuse generic machine screen structure where practical
- add dedicated special readouts only where the generic screen stops being honest, especially for reactors and endgame core controllers

### Progression Screen

The unified player screen already exists and should remain the primary owner for player progression visibility.

First-release requirement:

- it must act as the readable anchor for stage, skill, class, and magic state

### Class GUI And Skill Tree GUI

Design requirement:

- the player should be able to answer "what am I building toward" from the UI, not from the code

Current grounded caution:

- class and skill authorities are not fully converged under one data owner yet, so UI wording must avoid pretending the system is cleaner than it currently is

### Radiation HUD And Danger Warnings

Design requirement:

- radiation warnings must escalate before severe punishment
- contaminated areas should feel visibly wrong and mechanically risky
- reactor failure warnings must be unmissable

Current grounded caution:

- the overlay and storage formats need to stay synchronized; this is a real current risk

### Visual Identity For Dangerous Materials

First-release requirement:

- uranium, contaminated blocks, reactor parts, dirty-device items, and forbidden artifacts should have a coherent warning language
- this can be achieved with placeholder-safe color and icon consistency before final art polish

## 8. Asset And Content Workflow

ExtremeCraft must stay practical. Logic should not wait on final art, and assets should not masquerade as runtime ownership.

### Canonical Workflow Categories

#### Code-First

Use code-first ownership for:

- progression mutation and gates
- machine runtime behavior
- reactor safety and meltdown logic
- radiation storage, contamination release, and terrain mutation
- spell execution logic
- Aether and endgame core backends
- renderer/model bindings for entities

Rule:

If behavior requires server authority or persistent state, code owns it first.

#### Datapack-First

Use datapack-first ownership for:

- stage definitions
- unlock rules
- classes
- class abilities where the runtime hook already exists
- skills
- skill trees
- spells
- quest content
- contamination and terrain profiles
- radiation source profiles
- machine processing recipes
- worldgen placements and structure data
- module content

Rule:

If the behavior is content variation on a stable runtime contract, datapacks should own it.

#### Asset-First

Use asset-first workflow for:

- final textures
- final icons
- final sounds
- polished particles
- final GUI art
- Blockbench model exports once ids and runtime bind points are stable

Rule:

Do not block system implementation on final art. Promote assets to first-class polish once the content contract is stable.

#### Placeholder-Safe

Placeholder-safe content is acceptable for:

- cube-style entity models
- temporary machine textures
- draft GUI panels
- stopgap icons
- basic sound placeholders
- early contaminated block visuals

Rule:

Placeholder assets are acceptable. Fake ownership is not.

#### Beta-Polish-Only

Keep these out of the critical path for stable alpha:

- bespoke animation sets for every entity
- final cinematic particles for every spell
- custom unique UI art for every machine family
- premium sound design on every effect

### Blockbench And Entity Model Workflow

Current runtime truth:

- Java model/render classes are the live owner for entities today
- JSON model files under assets are handoff/support paths

First-class reference entity families already identified:

- zombie-style humanoid: `tech_construct`
- golem-style heavy body: `runic_golem`
- blaze-style floating caster: `arcane_wraith`

Safe replacement workflow:

1. keep the runtime entity id stable
2. keep the renderer bind point stable
3. replace model/texture assets in the agreed path
4. update Java renderer/model hooks only where the runtime truly changes
5. do not move ownership into metadata files that are not read by runtime

### Item / Block / Machine Visual Workflow

For first release:

- block and item ids must stabilize before visual polish scales up
- machine blocks can use placeholder-safe textures while functionality converges
- dangerous materials need stronger visual distinction sooner than ordinary decorative assets

### Spell Visuals And Sounds

First-release priority:

- readable school/effect differentiation
- hazard warnings for forbidden spells
- no dependence on stolen or copied third-party assets

### Contributor Workflow Rule

Before adding a new content family, decide which of these it is:

- code-owned behavior
- datapack-owned variation
- asset-owned presentation
- placeholder-safe temporary work

If the answer is unclear, the feature is probably not ready to be added.

## 9. Current Risks / Gaps / Contradictions

This section matters because ExtremeCraft already contains several cases where the repo can mislead contributors about what is really live.

| Risk or contradiction | Why it matters | First-release impact | Fix timing |
| --- | --- | --- | --- |
| Progression authority overlap between `PlayerProgressData`, `PlayerStatsCapability`, and legacy level mirrors | Multiple sources of truth create invisible bugs and confusing UI/data sync | High | Fix now for first-release clarity |
| Skill-point authority split between canonical progression rewards and skill-tree consumption | Players can receive points in one system and spend from another | High | Fix now |
| Mana authority split between `ManaCapability` and `PlayerStatsCapability.tryConsumeMana` | Spells and class abilities may not agree on resource state | High | Fix now |
| Ability-system overlap between generic abilities and class-ability runtime | Creates duplicate extension paths and unclear ownership | High | Fix now or explicitly freeze one path as primary |
| Class taxonomy drift between quest reward ids and canonical class data files | Class unlock rewards can become misleading or inert | High | Fix now |
| Machine ownership drift across `machine/core`, `machine`, `machines`, and metadata json folders | New content can be added to the wrong place and silently not be live | High | Fix now in docs and gradually in runtime convergence |
| Reactor naming drift between first-release fission intent and current `fusion_reactor` naming | The mod can ship with contradictory reactor identity | High | Fix now in release planning and runtime/data naming |
| Reactor and endgame data relying partly on scaffold/fallback definitions | Makes content appear more finished than it is | Medium | Fix during feature-complete alpha |
| Spell school metadata exists without a clear active loader | Documentation can overstate system completeness | Medium | Fix later unless school-based gating becomes critical |
| UI readout drift, especially radiation overlay vs stored data shape | Player feedback can become misleading in a hazardous system | High | Fix now |
| Quest folder duplication (`extremecraft_quests` vs `quests`) | Content authors may add quests to a non-canonical location | Medium | Fix now in docs; runtime cleanup can follow |
| Structure metadata paths vs live worldgen placement paths | Contributors can add structure data without actual placement | Medium | Fix now in docs |
| Material ownership split across ore catalogs, material data, and generated content | Makes material progression harder to reason about | Medium | Fix during alpha hardening |
| Research exists but is not clearly the first-release gate authority | Overdesign risk and contributor confusion | Medium | Keep narrow for first release; converge later |
| Several metadata-only machine concepts look ready for shipping | Risks overpromising content that is not actually live | Medium | Fix now by honest scoping |

### Which Risks Actually Block First Release

Hard blockers for an honest first release:

- progression/skill/mana authority splits
- class taxonomy drift
- reactor identity drift
- unreadable or incorrect hazard feedback
- misleading ownership of machines and quests

Important but not absolute blockers:

- full research convergence
- final spell-school data authority
- mature addon marketplace vision
- full art and animation polish

## 10. Release Slicing

ExtremeCraft should not try to ship the entire future roadmap in one release.

### Stable Alpha Requirements

This is the smallest stable vertical slice that still feels like ExtremeCraft.

- canonical stage ladder fully enforced
- primitive, energy, and industrial machine loops playable
- one live quest/guild reward path that clearly grants progression and class outcomes
- one clean base class set or class subset with honest unlock behavior
- skill growth readable and spendable through one authority path
- spellbook, mana bar, and a small low/mid spell roster functional
- radiation HUD, exposure, dose, contamination storage, and cleanup functional
- one canonical first-release reactor line functional and safety-capped
- contaminated terrain mutation and cleanup working
- unified player UI readable enough to explain progression state

### Feature-Complete Alpha Requirements

This is the full first-release gameplay identity.

- industrial to advanced machine ladder complete
- hybrid magic-tech layer curated and usable
- one reactor content line with stable fuel, shielding, and meltdown behavior
- one dirty-device weapon
- one tactical catastrophic weapon
- one narrow forbidden spell/device line
- one endgame core/monument system with staged growth
- worldgen, key structures, and danger-zone discovery aligned to progression
- core boss/danger rewards tied into materials or milestone loops
- canonical class lineup and quest rewards reconciled

### Beta Requirements

This is mostly about readability, content confidence, and polish.

- dedicated reactor/core UI improvements
- better hazard feedback and warnings
- more polished icons, textures, and contaminated visuals
- Blockbench replacement pass for key placeholder entities where practical
- improved tooltips and progression explanations
- balancing pass across nuclear, magic, hybrid, and endgame loops
- clearer visual identity for dangerous and forbidden systems

### Future / Post-Release Ambitions

These should not be force-shipped now.

- multiple reactor families beyond the first coherent line
- broad fusion expansion if it is not deliberately migrated as the main reactor identity
- a fully dominant research tree replacing or rivaling current stage/quest progression
- large class catalog expansion beyond the first coherent set
- deep spell-composition complexity beyond the current data-driven runtime
- fully placeable world Aether infrastructure web if the chunk backend is not yet matched by content
- marketplace-grade addon ecosystem and studio tooling maturity
- large concept-only material families without real content chains

## 11. Contributor Guidance

This section defines how contributors should think about the project when adding content or fixing systems.

### Canonical Paths

Add core gameplay logic in:

- `src/main/java/com/extremecraft/...`

Add canonical progression data in:

- `src/main/resources/data/extremecraft/progression`

Add live quest content in:

- `src/main/resources/data/extremecraft/extremecraft_quests`

Add live classes in:

- `src/main/resources/data/extremecraft/classes`

Add live class abilities in:

- `src/main/resources/data/extremecraft/class_abilities`

Add live skills and trees in:

- `src/main/resources/data/extremecraft/skills`
- `src/main/resources/data/extremecraft/skill_trees`

Add live spells in:

- `src/main/resources/data/extremecraft/spells`

Add live machine processing content in:

- `src/main/resources/data/extremecraft/recipes/machine_processing`

Add contamination/radiation profiles in:

- `src/main/resources/data/extremecraft/contamination`
- `src/main/resources/data/extremecraft/contamination_terrain`
- `src/main/resources/data/extremecraft/radiation_sources`

Add live assets in:

- `src/main/resources/assets/extremecraft`

### Paths That Should Not Be Treated As Canonical Runtime Owners

- top-level `core/`, `gameplay/`, `tech/`, `worldgen/`
- `data/extremecraft/machines` for machine runtime behavior
- `data/extremecraft/quests` for first-release quest authority
- entity metadata json folders as if they were the runtime renderer owner

### Systems That Should Not Be Casually Modified

- `ProgressionMutationService`
- `ProgressionFacade`
- `ProgressionGate`
- `RadiationService`
- `ChunkContaminationService`
- `ContaminationTerrainService`
- `MachineCatalog`
- `TechMachineBlockEntity`
- `SpellExecutor`
- `SpellService`
- `ECDestructiveEffectService`

These are foundation systems. Changes here should be deliberate and documented because they affect multiple pillars at once.

### Extend Versus Replace Rules

Extend these systems:

- the existing progression ladder
- the existing hazard pipeline
- the live machine chain
- the unified player UI
- the current spell runtime
- the current entity renderer/model bind path

Do not replace them with:

- a second progression ladder
- a second contamination or corruption system
- a second "real" machine registration path
- a parallel spell execution framework
- asset metadata pretending to be runtime ownership

### Practical Content-Addition Rules

When adding a new machine:

- add or converge its id in `MachineCatalog`
- ensure it is actually registered through the live tech registry path
- add recipes in the live recipe path
- add stage/unlock rules in the canonical progression path
- document it if it is first-release-facing

When adding a new class:

- add the class to canonical class data
- ensure quest or other unlock rewards reference the same id
- bind abilities/spells through the canonical class access path
- do not create a third class framework

When adding a new spell:

- use the existing spell data format first
- only extend runtime execution if a new effect/form cannot fit existing contracts
- route dangerous effects through the same capped hazard/destruction services

When adding a new contaminated material:

- add the block/item content
- connect it through contamination terrain profiles
- make protection and cleanup interactions explicit
- keep it in the existing contamination pipeline

When adding new entity visuals:

- preserve runtime entity ids and renderer ownership
- treat Blockbench exports as asset replacements, not ownership migration

### Contributor Mindset

ExtremeCraft should grow by honest extension of its real systems, not by piling new names into misleading folders.

If a feature only exists as metadata, say that.
If a system is scaffolded, say that.
If a path is legacy, label it.
If a path is canonical, keep it canonical.

## 12. FINAL CANONICAL TRUTHS

- ExtremeCraft is a hybrid RPG, industry, magic, and hazardous-power mod built around controlled escalation.
- The first release is trying to ship one coherent vertical slice from crude survival industry to late-game catastrophic power.
- The systems that define its identity are progression stages, classes and skills, machines and power, magic and Aether, radiation and contamination, and a narrow forbidden endgame.
- The canonical live stage ladder is `PRIMITIVE -> ENERGY -> INDUSTRIAL -> ADVANCED -> ENDGAME`.
- The canonical machine runtime path is the `machine/core` plus `machine/recipe` plus live registry chain, not the metadata-only machine json folder.
- The canonical hazard path is the existing radiation-to-contamination pipeline, not a second corruption system.
- Mana and Aether are not the same thing. Mana is personal. Aether is world infrastructure.
- Quests and guild rewards are supposed to author progression and class access, but the current class id drift must be resolved for first release honesty.
- Skills are expertise growth, not a replacement for stage gating.
- Research exists, but it is not yet the clean first-release authority and should not be forced into that role prematurely.
- One reactor line is enough for first release. Do not split launch identity across multiple half-converged nuclear families.
- Dangerous and overpowered systems are allowed to feel huge only if they remain server-capped, staged, and readable.
- Placeholder art is acceptable for first release. Misleading ownership is not.
- Metadata-only content concepts should not be advertised as shipped features until they are migrated into the real runtime chain.
- ExtremeCraft should not drift into being three separate mods in one jar. Its identity comes from making all of these pillars interact through one honest foundation.
