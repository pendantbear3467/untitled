# abilities

Status: `LIVE_RUNTIME` shared owner

This folder is intentionally shared by two live systems:

- `ability/AbilityRegistry` for generic ability runtime
- `modules/loader/ModuleAbilityLoader` for module-triggered abilities

Edit here for live generic abilities and shared module ability payloads.

Spell behavior does not live here. Spells are loaded from `spells/` and compiled
into generic ability payloads at cast time by `magic/SpellExecutor`.
