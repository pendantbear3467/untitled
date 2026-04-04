# abilities

Status: `LIVE_RUNTIME`

This folder is the canonical datapack source for active ability casts handled by:

- `ability/AbilityRegistry`
- `ability/AbilityEngine`
- `ability/AbilityExecutor`

Do not put module-trigger payloads here anymore. Module trigger definitions now
live in `module_abilities/`, with `abilities/` kept only as a warned legacy
fallback for older packs that still store module trigger JSON beside generic
abilities.

Spell behavior does not live here. Spells are loaded from `spells/` and compiled
into generic ability payloads at cast time by `magic/SpellExecutor`.

Important overlap warning:
- Some ids intentionally exist in both `abilities/` and `spells/`.
- Editing `abilities/fireball.json` changes the active ability cast.
- Editing `spells/fireball.json` changes the spell cast.
