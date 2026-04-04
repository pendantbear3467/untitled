# progression

Progression authority source root.

Owns:

- canonical player progression mutation boundaries
- progression read/write facades and services
- stage, unlock, skill-tree, quest reward, and class progression logic

Depends on:

- `:api`
- `:core`

This is a top-level source root, not a separate Gradle module yet. The root build includes it so progression authority can sit outside the remaining gameplay monolith without forcing a risky module cycle.
