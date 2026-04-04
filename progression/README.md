# progression

Progression authority source root.

Owns:

- canonical player progression mutation boundaries
- progression read/write facades and services
- stage, unlock, skill-tree, quest reward, and class progression logic

Depends on:

- `:api`
- `:core`

This is an included Gradle subproject in bridge mode.

- The root host build still compiles `progression/src/main/java` directly while host-runtime imports remain.
- The `:progression` project exists to tighten ownership, document coupling, and keep the eventual repo split honest.
- Repo extraction is still deferred until direct host-runtime imports and the host classpath bridge are removed.
