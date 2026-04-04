# extremecraft-progression

Progression authority module.

Owns:
- canonical progression mutation authority
- classes/quests/unlocks progression state and rules
- progression sync coordination

Depends on:
- `:api`
- `:extremecraft-core`

Exposes:
- progression facade and read/query surfaces

Must stay internal:
- low-level capability mutation internals
- write-only internals used by progression package services
