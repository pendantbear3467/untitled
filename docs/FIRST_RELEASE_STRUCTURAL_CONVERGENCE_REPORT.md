# First-Release Structural Convergence Report

Date: 2026-03-12

This pass was a convergence pass, not a feature-add pass. The goal was to reduce authority overlap, preserve the real runtime owners, and make first-release-facing progression, class, mana, machine, reactor, and hazard behavior more truthful.

`.\gradlew.bat compileJava` passed after the changes below.

## 1. Systems already converged

- The macro stage ladder was already converged to `PRIMITIVE -> ENERGY -> INDUSTRIAL -> ADVANCED -> ENDGAME` and remained the canonical progression ladder.
- The real machine runtime owner chain was already converged around `machine/core` plus `machine/recipe` plus `future/registry`.
- The hazard pipeline was already converged around `RadiationService -> ChunkContaminationService -> ContaminationTerrainService`.
- The entity placeholder/model pipeline was already documented and kept separate from live renderer ownership.
- Contamination stayed a persistent world authority with terrain mutation as projection, not a second hazard system.

## 2. Systems partially converged

- Progression state already had a canonical owner in `progression/PlayerProgressData`, but mirrors in stats and older progression capability paths still created contributor confusion.
- Skill-tree runtime was functional, but skill-point authority was split between canonical progression state and stats capability consumers.
- Mana runtime already had `ManaCapability` and `ManaService`, but some action paths still consumed mana through `PlayerStatsCapability`.
- Class data already had a canonical datapack path, but older quest reward ids and older class-name assumptions still leaked into live behavior.
- Reactor runtime already existed, but the first-release identity was still split between a live `fusion_reactor` id and a design intent that wanted one coherent fission-grade line.
- Player-facing hazard UI existed, but it still read the wrong player radiation shape and used fake reactor danger heuristics.

## 3. Confirmed first-release blockers found

- Progression authority overlap: `PlayerProgressData` was canonical, but other mirrored state paths still looked authoritative.
- Skill-point authority split: points were granted through progression state and still consumed from stats capability code paths.
- Mana authority split: spell/class/module actions did not consistently consume mana through the canonical mana service.
- Class taxonomy drift: quest rewards and older aliases still pointed at stale ids such as `fighter`, `miner`, `scientist`, and `trader`.
- Class passive application drift: `ProgressionService.applyAttributes` still depended on the older `progression.PlayerClass` enum instead of the canonical class data path.
- Reactor identity drift: the shipped machine id was `fusion_reactor`, but first-release docs and reward text wanted a single fission-grade reactor identity.
- Hazard UI truth drift: the HUD read flat tags that no longer matched the nested `ec_radiation` runtime state, and reactor alerts were inferred from generic machine energy instead of reactor state.
- Metadata honesty drift: contributor-facing docs still presented `data/extremecraft/materials`, `data/extremecraft/machines`, `data/extremecraft/ec_recipes`, and `data/extremecraft/quests` as live first-release owners.

## 4. What you changed

- Added `progression/classsystem/ClassIdResolver` and routed class-id normalization through progression, quest loading, unlock rules, and class access resolution.
- Normalized legacy class ids in `PlayerProgressData` during set/load/unlock flows so old saves and old reward ids resolve onto canonical first-release class ids.
- Updated quest reward data under `data/extremecraft/extremecraft_quests` so class unlock rewards now point at canonical class ids.
- Reworked class switching and attribute application so `ProgressionService` now resolves passives through canonical class data instead of the older enum path.
- Made canonical progression skill points the spend authority by updating `PlayerStatsService` and skill tree consumers to spend from `PlayerProgressData` and mirror outward only for compatibility.
- Added `PlayerStatsService.syncProgressionMirror(...)` and called it from progression mutation, login sync, quest reward claiming, and direct skill-point grants.
- Marked `PlayerStatsCapability.tryConsumeMana` as a legacy adapter path and moved class-ability and module-runtime mana consumption onto `ManaService`.
- Updated player-facing progression and magic UI so `ExtremePlayerScreen` reads macro level, XP, skill points, and live mana from canonical progression/mana services instead of stats-only mirrors.
- Added `reactor/ReactorIdentity` and normalized first-release reactor machine lookups through that helper without creating a second reactor system.
- Updated `MachineCatalog`, `ProgressionGate`, and `ReactorControlService` to treat the live compatibility id consistently as the first-release reactor line.
- Extended machine sync data so `TechMachineBlockEntity` now includes `max_energy` and server-owned reactor telemetry snapshots.
- Updated `TechMachineScreen` to display translated machine names and real synced reactor telemetry instead of fabricating heat, coolant, waste, and damage from generic machine progress/energy.
- Updated `AbilityBarOverlay` to read the nested radiation state (`ambient`, `dose`, `contamination`) and to raise reactor alerts from real synced reactor state instead of energy-buffer guesses.
- Renamed the player-facing `fusion_reactor` language strings to `Fission Reactor` so UI text matches the first-release identity without changing the live registry id.
- Updated `MachineTooltipHandler` and targeted docs/READMEs so contributors and players are pointed toward crusher-first early progression, the real reactor identity, and the real live data owners.

## 5. What authority paths are now canonical

- Macro progression state, stage state, player XP/level, and canonical player skill points:
  `src/main/java/com/extremecraft/progression/PlayerProgressData`
- Progression mutation and synchronization:
  `src/main/java/com/extremecraft/progression/ProgressionMutationService`
  and
  `src/main/java/com/extremecraft/progression/ProgressionService`
- Stage gates and machine access:
  `src/main/java/com/extremecraft/progression/ProgressionGate`
  plus
  `src/main/resources/data/extremecraft/progression/*`
- Mana state and mana spend/regeneration authority:
  `src/main/java/com/extremecraft/magic/mana/ManaCapability`
  and
  `src/main/java/com/extremecraft/magic/mana/ManaService`
- Canonical class ids and class resolution:
  `src/main/resources/data/extremecraft/classes/*.json`
  plus
  `src/main/java/com/extremecraft/progression/classsystem/ClassIdResolver`
  and
  `src/main/java/com/extremecraft/classsystem/ClassAccessResolver`
- Class abilities as the narrow class-specific action path:
  `src/main/java/com/extremecraft/progression/classsystem/ability/ClassAbilityService`
- Generic spell and ability systems as the extensible action path:
  existing spell/ability runtime under `magic` and shared ability sync/runtime paths
- Tech machine runtime:
  `src/main/java/com/extremecraft/machine/core`
  plus
  `src/main/resources/data/extremecraft/recipes/machine_processing/*.json`
- First-release reactor runtime identity:
  live registry id `fusion_reactor`, normalized through `src/main/java/com/extremecraft/reactor/ReactorIdentity`, presented to players as the first-release fission reactor line
- Radiation and contamination:
  `src/main/java/com/extremecraft/radiation/RadiationService`
  `src/main/java/com/extremecraft/radiation/ChunkContaminationService`
  `src/main/java/com/extremecraft/radiation/ContaminationTerrainService`

## 6. What compatibility shims/aliases remain and why

- `PlayerStatsCapability.skillPoints` remains as a synchronized mirror because existing skill-tree/runtime/UI consumers still read stats capability state.
- Older progression/stat mirrors still exist because removing them outright would be a broader migration than this pass allowed.
- Legacy quest/class aliases such as `fighter -> warrior` and `scientist -> technomancer` remain supported through `ClassIdResolver` for backward compatibility and existing save/data safety.
- The live reactor registry id remains `fusion_reactor` because changing registry ids is higher-risk than normalizing lookups and player-facing text.
- `pulverizer` remains present as compatibility content, but `crusher` stays the canonical early progression machine and `advanced_pulverizer` stays later-tier refinement content.
- The reactor SCRAM button still only reports a client-side request in the screen; this pass made that truth explicit instead of pretending it is server-authoritative control.

## 7. What you intentionally preserved

- The canonical stage ladder and stage-gating role were preserved.
- The existing machine runtime chain was preserved instead of replaced with prettier metadata folders.
- The existing mana system was preserved; only non-canonical consumption paths were rerouted to it.
- The existing radiation/contamination architecture was preserved and clarified rather than duplicated.
- Existing class, spell, and ability content was preserved; this pass reduced overlap and naming drift instead of deleting content.
- The live reactor backend, safety systems, and destructive caps were preserved.

## 8. What remains scaffold/future-phase

- `data/extremecraft/materials/*.json` remains metadata/reference-only, not a live material authority.
- `data/extremecraft/machines/*.json` remains metadata-only, not the machine runtime owner.
- `data/extremecraft/ec_recipes/*.json` remains legacy recipe-id metadata, not the canonical processing loader.
- `data/extremecraft/quests/*.json` remains scaffold/reference content, not the first-release guild/class quest authority.
- `data/extremecraft/reactor_parts/*.json` remains scaffold-only until a dedicated runtime loader exists.
- `data/extremecraft/endgame_core/*.json` remains future-phase content, not honest first-release shipped runtime.
- `data/extremecraft/spell_schools/*.json` remains taxonomy scaffold, not a live first-release ownership path.
- `data/extremecraft/worldgen_weights/*.json` remains tuning scaffold, not a canonical live worldgen owner.
- Multiple endgame and hybrid concepts remain partial or design-led and should not be presented as shipped first-release systems without real runtime migration.

## 9. What still needs manual gameplay validation

- Class unlock quest claiming should be checked in-game to confirm canonical class ids unlock the expected classes on existing and new saves.
- Class switching should be checked in-game to confirm passive application now matches canonical class data instead of older enum expectations.
- Skill-point grant, spend, and UI sync should be tested through actual skill-tree unlock flows, not just compile validation.
- Mana use should be validated across spells, class abilities, and module abilities to confirm there are no remaining hidden stats-capability-only mana reads.
- The hazard HUD should be verified in play with real radiation exposure, accumulated dose, and contaminated chunks to confirm the new thresholds feel readable.
- Reactor telemetry should be checked against a live reactor build to confirm the synced values update as expected in the machine screen.
- Early progression copy should be spot-checked in advancements, recipe unlocks, and tooltips to confirm `crusher` remains the clear first machine.
- Reactor player-facing terminology should be spot-checked anywhere still showing `fusion_reactor` raw ids or older fusion wording.

## 10. What the best next implementation phase should be

- Run an in-game validation phase focused on progression, class rewards, skill spending, mana consumption, reactor telemetry, and hazard feedback.
- After gameplay validation, prune or isolate any remaining read paths that still treat stats mirrors as authorities rather than adapters.
- Decide whether the first-release reactor should gain a real server-authoritative SCRAM interaction path or whether the button should be removed until that path exists.
- Continue the metadata honesty pass only where scaffold folders still appear shipped in UI, docs, or contributor workflows.
- Keep future-phase nuclear, hybrid, and endgame content behind honest scaffold labels until they are migrated into real runtime owners.
