# Alpha Validation And Feel Report

Date: 2026-03-12

This pass was a post-convergence validation pass. The goal was to check whether the structural cleanup actually produced coherent gameplay-facing behavior, then fix the highest-value remaining feel contradictions without reopening architecture.

`.\gradlew.bat compileJava` passed after the changes in this report.

## 1. What already felt coherent

- Mana authority was already mostly converged. Spell casting, class abilities, module abilities, the mana HUD, and the unified player screen were already reading from `ManaCapability` and `ManaService` instead of split stat-capability mana.
- The unified player screen was already reading macro progression state from canonical progression data instead of older mirrors.
- Reactor telemetry and hazard HUD ownership were already materially improved by the previous convergence pass. The machine screen now reads synced reactor state, and the hazard overlay now reads the nested radiation payload.
- Class unlock reward ids were already converged onto the canonical class taxonomy from the prior pass.
- Crusher-first early machine guidance and first-release reactor identity were already more truthful than before this pass.

## 2. What still felt contradictory

- Offhand attacks still had a dedicated damage path that computed damage manually and then let the generic combat event path reinterpret the hit from the wrong weapon context. In practice that meant offhand combat could bypass the real `CombatEngine` intent and inherit main-hand-biased damage reconciliation.
- The combat engine returned a fresh preview after `target.hurt(...)` instead of the resolved damage result used during the actual hurt-event pipeline. That made returned `DamageResult` values untrustworthy for gameplay follow-up logic.
- Dual-wield loadout cycling existed server-side and on the network, but there was no live client trigger for it. The feature was present in the repo and effectively inaccessible in play.
- The default class ability key and ability-slot 3 key were both bound to `R`, so one player-facing combat action path masked the other.
- The integrated skill-tree panel still computed unlockability from mirrored stats values instead of canonical progression level/skill points. That was a trust issue even if mirrors were usually close.
- The standalone skill-tree screen still allowed “can unlock” presentation without checking canonical player skill points.
- The dual-wield tab told only part of the truth. It did not show the active loadout, did not expose the live cycle input, and did not clearly explain which offhand actions mapped to which input.

## 3. What you changed

- Routed living-target offhand attacks through `CombatEngine` by building a proper offhand `DamageContext` in [OffhandActionExecutor.java](/I:/minecraft%20mods/Extreme%20Craft/untitled/src/main/java/com/extremecraft/combat/dualwield/service/OffhandActionExecutor.java).
- Preserved offhand validation, anti-spam, attack-strength scaling, knockback, sprint knockback, fire aspect, shield disable logic, weapon durability loss, crit particles, and post-damage enchant hooks.
- Updated [CombatEngine.java](/I:/minecraft%20mods/Extreme%20Craft/untitled/src/main/java/com/extremecraft/combat/CombatEngine.java) so weapon-source-aware contexts can contribute the correct weapon damage instead of always assuming main hand.
- Updated [CombatEngine.java](/I:/minecraft%20mods/Extreme%20Craft/untitled/src/main/java/com/extremecraft/combat/CombatEngine.java) so `applyDamage(...)` returns the resolved damage result produced during the actual hurt-event pass instead of re-rolling a second preview afterward.
- Added a live client trigger for loadout cycling by registering `CYCLE_LOADOUT` and sending `CycleLoadoutC2S` from [DwClientHooks.java](/I:/minecraft%20mods/Extreme%20Craft/untitled/src/main/java/com/extremecraft/client/DwClientHooks.java).
- Changed the default class ability key from `R` to `C` in [DwKeybinds.java](/I:/minecraft%20mods/Extreme%20Craft/untitled/src/main/java/com/extremecraft/client/DwKeybinds.java) to remove the direct conflict with ability-slot 3.
- Registered a default cycle-loadout key on `V` in [DwKeybinds.java](/I:/minecraft%20mods/Extreme%20Craft/untitled/src/main/java/com/extremecraft/client/DwKeybinds.java).
- Updated the unified player screen in [ExtremePlayerScreen.java](/I:/minecraft%20mods/Extreme%20Craft/untitled/src/main/java/com/extremecraft/client/gui/player/ExtremePlayerScreen.java) so the magic/class/dual-wield tabs now show live key prompts and synced dual-wield loadout state instead of leaving that behavior implicit.
- Improved the dual-wield tab in [ExtremePlayerScreen.java](/I:/minecraft%20mods/Extreme%20Craft/untitled/src/main/java/com/extremecraft/client/gui/player/ExtremePlayerScreen.java) to show the active loadout and all three saved loadout snapshots.
- Updated [SkillTreeScreenPanel.java](/I:/minecraft%20mods/Extreme%20Craft/untitled/src/main/java/com/extremecraft/client/gui/player/SkillTreeScreenPanel.java), [SkillNodeStateService.java](/I:/minecraft%20mods/Extreme%20Craft/untitled/src/main/java/com/extremecraft/progression/skilltree/service/SkillNodeStateService.java), and [SkillPrerequisiteEvaluator.java](/I:/minecraft%20mods/Extreme%20Craft/untitled/src/main/java/com/extremecraft/progression/skilltree/service/SkillPrerequisiteEvaluator.java) so node readiness now respects canonical progression level and canonical player skill points.
- Improved integrated skill-tree connection readability so unlocked and unlockable lanes are visually distinct instead of using one dull connection color for all states.
- Updated the standalone skill-tree screen in [SkillTreeScreen.java](/I:/minecraft%20mods/Extreme%20Craft/untitled/src/main/java/com/extremecraft/progression/skilltree/SkillTreeScreen.java) so “can unlock” now checks canonical player skill points in addition to level and prerequisites.

## 4. What remains a known limitation

- Offhand attacks against non-living attackable entities still fall back to direct damage because the current `CombatEngine` contract is built around `LivingEntity` targets. Living-target combat is now the canonical offhand path.
- There is still no dedicated offhand cooldown meter or per-loadout HUD. The system is more honest now, but not yet deeply surfaced.
- The dual-wield loadout system is now reachable and visible, but it still relies on key-driven cycling rather than a richer in-menu management flow.
- The unified player screen still reports some first-release-future placeholders honestly, such as Aether backend telemetry pending and backend-safe class switching UI pending.
- This pass did not redesign hit animation, impact FX, or combat camera feel. It tightened routing and trust first.

## 5. What still needs manual playtesting

- Spell casting, class ability use, and module ability use should be tested back-to-back while watching the mana HUD and unified player screen to confirm mana spend/read timing feels consistent in live play.
- Offhand combat should be tested against normal mobs, armored targets, shield-using players, and sprint/jump attacks to confirm charge, crit, enchant, and knockback feel right after the routing change.
- Dual-wield loadout cycling should be tested across login, respawn, dimension change, and active combat to confirm persistence and sync feel trustworthy.
- Offhand block use, offhand item use, tap break, and hold break should be tested under real client/server latency conditions.
- Class unlock quest claiming and skill-point spending should be tested in sequence so the skill tree, class tab, and progression tab all tell the same story after rewards are claimed.
- The integrated skill-tree panel and the standalone skill-tree screen should both be tested to confirm readiness, tooltips, and node-link readability match real unlockability.
- Reactor telemetry and hazard overlays should still be checked in-game to confirm the prior convergence changes remain readable during actual dangerous states.

## 6. What the next best focused implementation phase should be

- Run a manual alpha playtest pass centered on combat and spell feel, not architecture. The priority is confirming the offhand-combat reroute, mana truth, and dual-wield cycle path under real input and latency conditions.
- If manual play confirms the offhand route is stable, the next focused implementation phase should be combat feedback polish: clearer offhand cooldown/readiness cues, hit confirmation, and dual-wield HUD support.
- After that, the next best target is a narrow combat-engine normalization pass for any remaining main-hand or vanilla-adjacent damage paths that still return less trustworthy contextual results than ability-driven combat.
- Do not reopen progression, machine ownership, or contamination architecture in the next phase unless gameplay testing proves a live contradiction there.
