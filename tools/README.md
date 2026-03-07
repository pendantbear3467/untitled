# tools

Repository tooling lives here.

- `python/`: Asset Studio, SDK, compiler, plugin runtime code.
- `generators/`: Reserved generator-facing scripts/modules.
- Root `tools/*.py`: compatibility launchers and validation scripts used by Gradle.

Important:

- `content_completion.py` is invoked by Gradle checks and must remain reachable at `tools/content_completion.py`.
