# Modpack Support

ExtremeCraft provides modpack-facing levers through datapacks and config overlays.

## Recommended controls

- XP multipliers
- machine power cost multipliers
- module slot tuning
- unlock gate tightening/relaxing
- quest progression pacing

## Suggested override strategy

1. Start with vanilla ExtremeCraft datapacks.
2. Override values in a pack-specific datapack namespace.
3. Keep overrides grouped by system (`skills`, `modules`, `machines`, `quests`).
4. Use `/ec reload` for rapid iteration in dev servers.
