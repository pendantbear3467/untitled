# First-Release Gameplay Redesign

Date: 2026-03-12

## 1. What Was Already Present

- Canonical progression mutation already routes through `ProgressionMutationService`, `ProgressionService`, `ProgressionFacade`, and `PlayerProgressData`.
- Stage gating for machines and recipes already exists in `ProgressionGate`, `StageManager`, and `data/extremecraft/progression/stages`.
- Data-driven unlock rules already exist in `UnlockRuleLoader`, but they were still narrow and mostly machine/recipe focused.
- Dual-wield runtime already exists in `PlayerDualWieldData`, `DualWieldService`, `SyncDualWieldDataS2C`, `DwClientHooks`, and `OffhandActionExecutor`.
- The current dual-wield player-facing surface existed only as a tab inside `ExtremePlayerScreen`, plus key-driven cycling.
- Combat routing was recently stabilized around `CombatEngine` and offhand action execution; it should be extended carefully rather than rewritten again.

## 2. What Was Reworked In This Pass

- `PlayerProgressData` now stores generic unlock grants alongside existing class, quest, and progression state.
- `ProgressionService` and `ProgressionFacade` now expose generic unlock-grant mutation paths.
- `UnlockRule` and `UnlockRuleLoader` now support broader requirement sets through `required_unlocks` plus category-specific arrays such as `required_research`, `required_boss`, `required_event`, `required_milestone`, and `required_guild`.
- `UnlockRuleLoader` now resolves mixed unlock sources coherently:
  - stage via `StageManager`
  - class via canonical unlocked classes
  - quest via completed quests
  - research via `ResearchApi`
  - milestone/boss/event/guild via generic unlock grants
- `UnlockAccessService` now exists as the canonical permission helper for unlockable content access. It supports action-specific keys such as `#view`, `#craft`, `#equip`, `#use`, `#cast`, and `#activate`.
- `ProgressionGate` now routes machine and recipe access through `UnlockAccessService` after stage validation.
- Dual-wield now has direct server-authoritative loadout actions:
  - `SelectLoadoutC2S`
  - `SaveLoadoutC2S`
  - `DualWieldService.selectLoadout(...)`
  - `DualWieldService.saveCurrentToLoadout(...)`
- `DualWieldScreen` is now a dedicated standalone screen rather than a wrapper tab alias.
- The inventory flow now exposes a dedicated `DW` entry button through `InventoryButtonInjector`.

## 3. Canonical Unlock Architecture

### Canonical unlock decision owner

- `UnlockAccessService`

Why:

- It is the correct place to answer “can the player view, craft, equip, use, cast, or activate this content?”
- It does not replace stage gating. It composes with it.
- It gives all player-facing systems one permission vocabulary instead of per-feature checks.

### Canonical progression state owner

- `PlayerProgressData`

Why:

- It already owns canonical player progression state.
- It now also owns generic unlock grants for milestone-like progression tokens.
- It is the right long-term place to store boss/event/milestone/guild unlock grants that are not just raw XP or class state.

### Systems that should decide access

- `ProgressionGate`: stage-sensitive machine and recipe access.
- `UnlockAccessService`: action-specific content permissions.
- `UnlockRuleLoader`: data-driven requirement resolution.

### Systems that should grant unlocks, not decide them

- `GuildQuestRewardService`
- research claim/runtime flows
- future boss reward hooks
- future event completion hooks
- milestone infrastructure events

These systems should grant stage upgrades, classes, XP, or generic unlock grants. They should not each invent their own permission logic.

## 4. First-Release Unlock Targets

Support first, in this order:

1. `recipe:*#craft`
2. `machine:*#use`
3. `spell:*#cast`
4. `ability:*#activate`
5. `class_ability:*#activate`
6. `item:*#equip`
7. `item:*#use`
8. `item:*#view`
9. `loadout_slot:*#equip`

Keep first release practical:

- Use stage as the main macro gate.
- Layer skill/class/quest/research on top only when that creates clearer intent.
- Do not hide the physical item from registries or remove content from the game world.
- Do use unlock access to control visibility, equip/use permission, and tooltip truth.

## 5. Dedicated Dual-Wield / Loadout GUI Plan

### Canonical owner chain

1. `PlayerDualWieldData`: stored loadout state
2. `DualWieldService`: server-authoritative mutation and sync
3. `SyncDualWieldDataS2C`: client state replication
4. `DualWieldScreen`: dedicated first-release management surface
5. `InventoryButtonInjector`: inventory-adjacent entry point

### Layout plan

- Keep one compact dedicated screen, separate from the broader unified player UI.
- Show live current hands at the top-left.
- Show three vertically stacked loadout cards.
- Each card shows:
  - loadout label
  - main slot snapshot
  - offhand slot snapshot
  - slot unlocked/locked status
  - `Use` action
  - `Save` action
- Show active loadout clearly with a stronger frame and prefix marker.
- Keep combat key prompts visible in the header.

### Alpha versus beta scope

Alpha shipped in this pass:

- dedicated separate surface
- inventory flow entry button
- 3 visible loadouts
- explicit `Use` and `Save` actions
- truthful active loadout presentation
- lock-state labels sourced from the unified unlock helper

Beta later:

- drag-drop assignment
- slot-type filtering and invalid-item highlighting
- ranged/utility sub-slot treatment
- cleaner inventory-adjacent panel placement and shared inventory chrome

## 6. Curios-Inspired Visual Lessons

Used as inspiration only, not copied:

- Keep extended slots behind one coherent access point near the inventory flow.
- Show slots on-demand rather than flooding the player with dead UI.
- Group slots by role and order rather than by raw count.
- Make hidden or disabled slot states honest and configurable.
- Use icon/slot grouping and active-state framing to show relevance quickly.

ExtremeCraft application:

- three role-focused loadouts instead of a wall of equipment sockets
- explicit active-state emphasis
- practical action buttons instead of decorative paper-doll clutter
- slot lock labels that can later follow real progression rules

## 7. Smart Interaction Priority Model

This pass does not hard-rewrite combat/input resolution again. The live combat path was recently stabilized, so the correct move is to define the next resolver clearly and extend the existing owners.

### Canonical future owner chain

1. `DwClientHooks`: client intent capture
2. new resolver layer under `combat.dualwield` or `combat.input`
3. `OffhandActionExecutor`: explicit offhand execution
4. `CombatEngine`: authoritative damage resolution

### Intent priority matrix

| Target context | Input state | Preferred action | Runtime owner | First-release behavior |
| --- | --- | --- | --- | --- |
| Living hitbox under crosshair | attack/offhand override | combat | `CombatEngine` + offhand executor | prioritize weapon attack |
| Valid block under crosshair with matching tool | use/break intent | tool/block interaction | dual-wield interaction resolver | prioritize mining/tool logic |
| Ranged weapon being drawn or explicit ranged cast/fire intent | ranged input | ranged | ranged weapon or spell path | do not let tool/offhand noise hijack it |
| Entity hover with incompatible tool | normal use input | safe no-op or entity interaction | resolver | suppress nonsense mining/attack fallbacks |
| Empty space / no meaningful target | any passive input | safe no-op | resolver | avoid accidental air actions |

### Conflict rules

- `sword + pickaxe`: entity target means combat, ore block means mining.
- `bow + tool`: draw or ranged-fire intent means ranged, block context means tool.
- `sword + shield`: combat target means weapon/shield logic, empty context means no forced action.
- `spell focus + tool`: spell only on explicit cast input, not passive hover.

### First-release simple vs future advanced

First release:

- context buckets: entity, block, miss
- explicit input routing
- safe no-op when nothing meaningful is targeted
- no giant animation or camera rewrite

Future advanced:

- deeper ranged aim heuristics
- per-weapon stance policies
- richer hover affordances and cooldown overlays

## 8. File-By-File Change Plan

Implemented now:

- `src/main/java/com/extremecraft/progression/PlayerProgressData.java`
- `src/main/java/com/extremecraft/progression/ProgressionService.java`
- `src/main/java/com/extremecraft/progression/ProgressionFacade.java`
- `src/main/java/com/extremecraft/progression/GuildQuestRewardService.java`
- `src/main/java/com/extremecraft/progression/ProgressionGate.java`
- `src/main/java/com/extremecraft/progression/unlock/UnlockRule.java`
- `src/main/java/com/extremecraft/progression/unlock/UnlockRuleLoader.java`
- `src/main/java/com/extremecraft/progression/unlock/UnlockAccessService.java`
- `src/main/java/com/extremecraft/combat/dualwield/PlayerDualWieldData.java`
- `src/main/java/com/extremecraft/combat/dualwield/DualWieldService.java`
- `src/main/java/com/extremecraft/combat/dualwield/SelectLoadoutC2S.java`
- `src/main/java/com/extremecraft/combat/dualwield/SaveLoadoutC2S.java`
- `src/main/java/com/extremecraft/client/gui/player/DualWieldScreen.java`
- `src/main/java/com/extremecraft/client/gui/player/InventoryButtonInjector.java`
- `src/main/java/com/extremecraft/client/gui/player/StandalonePlayerScreen.java`
- `src/main/java/com/extremecraft/network/ModNetwork.java`

Next practical files for the next implementation phase:

- `src/main/java/com/extremecraft/client/DwClientHooks.java`
- `src/main/java/com/extremecraft/combat/dualwield/service/OffhandActionExecutor.java`
- `src/main/java/com/extremecraft/combat/CombatEngine.java`
- spell/class ability entry points that need `UnlockAccessService`
- tooltip and recipe/book UI surfaces that should respect `#view`, `#craft`, `#equip`, and `#use`

## 9. Final Canonical Status

### What is now canonical

- `UnlockAccessService` is the canonical unlock permission surface.
- `PlayerProgressData` is the canonical holder for generic unlock grants.
- `ProgressionGate` remains the canonical stage gate for machines and recipes.
- `DualWieldScreen` is the canonical dedicated first-release loadout management UI.
- `PlayerDualWieldData` + `DualWieldService` remain the canonical loadout runtime owners.

### What remains future-phase

- full action-priority resolver implementation
- tooltip-level lock reason surfacing everywhere
- slot filtering and richer invalid-item handling in the loadout UI
- broad spell/ability/equipment integration onto `UnlockAccessService`
- boss/event/milestone grant producers wired into generic unlock grants

### What still needs manual gameplay testing

- saving and applying loadouts across login, respawn, and dimension change
- dual-wield screen use from the inventory flow under real client latency
- offhand combat and tool use after swapping loadouts rapidly
- spell casting and class ability use while changing loadouts
- future lock-state UI once real loadout slot rules are authored in datapacks