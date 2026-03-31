# Progression Datapack Content

Status: `LIVE_RUNTIME`

This folder is the canonical data owner for the formal progression ladder during this cleanup pass.

Owns:
- `stages/*.json` for stage-to-unlock mapping
- `unlocks/*.json` for explicit unlock rule requirements
- `default_curve.json` for progression curve metadata

Consumed by:
- `StageDataLoader`
- `unlock/UnlockRuleLoader`
- `ProgressionGate`

Current ladder:
- `PRIMITIVE -> ENERGY -> INDUSTRIAL -> ADVANCED -> ENDGAME`

Compatibility notes:
- Code still contains fallback unlock defaults for compatibility, but new work should land here first.
- Legacy `AUTOMATION` is treated as an alias, not a canonical runtime stage.

Common mistakes:
- Adding a nonexistent machine id to a stage file.
- Treating `default_curve.json` as the stage owner.
