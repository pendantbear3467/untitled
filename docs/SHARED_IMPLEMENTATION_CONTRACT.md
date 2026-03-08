# ExtremeCraft Shared Implementation Contract

Status: Active for Forge 1.20.1 first major release.
Applies to: all runtime/gameplay/content/tooling contributors.

## 1) Project Target And Identity

ExtremeCraft is a hybrid RPG + machine + nuclear + arcane-tech + endgame catastrophe mod.

Release progression target:
- Crude survival power
- Industry and machine infrastructure
- Magic infrastructure with mana and aether domains
- Radiation and fission-risk gameplay
- One expandable monument-scale endgame energy core

## 2) Non-Negotiable Design Rules

These rules are mandatory unless a blocker is documented in PR notes and migration notes.

1. Preserve existing registry IDs unless absolutely necessary.
2. Do not casually rename packages, registries, data IDs, saved fields, or core APIs.
3. Prefer additive/convergent fixes over rewrites.
4. Maintain dedicated-server safety. No client-only classes in common/server logic.
5. Server authority is canonical for:
   - progression
   - machine state
   - mana/aether state
   - radiation state
   - reactor state
   - spell validation
   - destructive effects
6. Skills and classes are separate progression domains:
   - skill XP sources: mob kills, combat, active gameplay only
   - class XP sources: guild quest completion only
   - no implicit or hidden XP crossover
7. Use canonical facades for cross-system interaction. Systems must not mutate each other directly.
8. Enforce technical safety for machines/reactors/radiation/catastrophic spells:
   - capped block budgets
   - cached structure validation
   - interval ticking
   - recipe caching
   - bounded scans
   - config-gated destructive effects
9. Data-drive content wherever reasonable:
   - materials
   - machine definitions
   - machine recipes
   - skill trees
   - class definitions
   - quests
   - spell definitions
   - radiation source values
   - reactor parts
   - worldgen weights
10. Keep first-release scope strict and finite.
11. Keep fictional reactor/nuke/spell logic as game-system design only; do not provide real-world engineering guidance.
12. Mark placeholder art/content clearly so replacement is logic-safe.
13. Choose maintainability over cleverness when uncertain.
14. Implementation output must be production-grade, not brainstorm-grade.

## 3) Canonical Integration Boundaries

The following boundaries apply to all new and modified systems.

- Networking:
  - packet registration ownership remains centralized (see architecture docs)
  - C2S requests are validated and server-authoritative
- Progression:
  - mutations flow through canonical progression mutation services/facades
  - no direct cross-domain XP mutation
- Machines/Reactor/Radiation/Spell destruction:
  - no unbounded scans
  - no per-tick heavy recomputation without caching and interval gates
- Client separation:
  - UI/render code stays client-only
  - common logic must not import or depend on client classes

## 4) First Major Release Required Systems

All systems below are in-scope commitments for the first major release:

- Primitive generators
- Industrial machines
- Mana + aether split
- Spellbook with spell forms/effects/modifiers
- Radiation HUD and contamination
- Uranium/thorium/lead/reactor chain
- One fission reactor
- One tactical nuke
- One dirty bomb
- One catastrophic endgame spell/device with safe caps
- Skill/class split with clean UI
- Machine UI, reactor UI, spellbook UI, progression UI
- One expandable endgame core with stabilizers/rings/pylons/shell logic

## 5) Shared Acceptance Checklist (PR Gate)

A change touching core systems should satisfy the following checklist before merge:

- Registry/data/save compatibility preserved, or migration documented
- Dedicated server run path remains valid (no client class leakage)
- Cross-system writes go through canonical facade/service boundaries
- Skill XP and class XP remain source-isolated
- Machine/reactor/radiation/catastrophic logic uses bounded work + caching
- Config gates exist for destructive/high-risk behaviors
- New content is data-driven where practical and schema-validated
- Placeholder assets/content are explicitly marked
- Tests/validation commands executed or documented if unavailable

## 6) Change Management Rules

When a non-additive change is unavoidable:

- Document why additive migration was not viable.
- Keep old IDs/routes as compatibility aliases when feasible.
- Add explicit migration/remap handlers for saved data and registries.
- Add release note entries for server admins and pack authors.

## 7) Contributor Notes

- Prefer small vertical slices over broad framework rewrites.
- Preserve established ownership boundaries in architecture docs.
- If uncertain between options, choose the one with clearer maintenance and validation paths.
