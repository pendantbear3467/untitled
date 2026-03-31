# tools

Repository tooling lives here.

- `python/`: Asset Studio, SDK, compiler, plugin runtime code.
- `scripts/`: Compatibility launchers for contributor workflows.
- `generators/`: Reserved generator-facing scripts/modules.
- Root `tools/*.py`: compatibility launchers and validation scripts used by Gradle.

Important:

- `content_completion.py` is invoked by Gradle checks and must remain reachable at `tools/content_completion.py`.
- `tools/scripts/assetstudio.py` and `tools/scripts/main.py` are shell launchers for `asset_studio.main`.
- `tools/scripts/generate_assets.py` and `tools/scripts/Python-Generator.py` wrap the maintained implementation at `tools/generate_assets.py`.
