# Quest

Status: `LIVE_RUNTIME`

This folder owns the active runtime quest definition registry used by progression quest tracking and guild-trial reward claiming.

Runtime-critical files:
- `QuestManager`
- `QuestDefinition`
- `QuestType`

Live data owner:
- `data/extremecraft/extremecraft_quests/*.json`

Metadata-only overlap:
- `platform/data/loader/QuestDataLoader` loads `data/extremecraft/quests/*.json` for structured metadata and validation work.

Safe future additions:
1. Add live progression quests to `extremecraft_quests`.
2. Keep reward stages/classes aligned with `ProgressionGate` and class ids.
3. Use `GuildQuestRewardService` for claim logic rather than ad hoc reward code.

Common mistakes:
- Adding a quest to `data/extremecraft/quests` and expecting it to appear in live gameplay.
- Encoding quest progression logic inside unrelated event handlers instead of using `ProgressionEvents`.
