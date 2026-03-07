# ExtremeCraft

ExtremeCraft is a large-scale Minecraft Forge mod for 1.20.1 focused on technology progression, magic abilities, machine automation, and data-driven gameplay systems.

## Repository Layout

- `src/main/java`: Gameplay/runtime Java code loaded by Forge.
- `src/main/resources`: Runtime assets and built-in datapack content (`assets/`, `data/`, `META-INF/`).
- `api/`: Public Java API module consumed by integrations.
- `tools/python`: Python platform tooling (Asset Studio, SDK, compiler, plugin pipeline).
- `tools/`: Build/validation utility scripts used by Gradle checks.
- `datapacks/`: Contributor-facing external datapack workspace and examples.
- `docs/`: Reference docs and generated validation/report artifacts.
- `examples/`: Example addon/template projects.
- `scripts/`: Optional helper scripts for contributor workflows.

## What ExtremeCraft Includes

- Data-driven machine definitions and processing recipes.
- Ability runtime with cooldown/mana/class requirements.
- Progression, quests, research, classes, and skill trees.
- Entity extension hooks for custom mob/boss behavior.
- Validation and generation tooling for large content sets.

## Development Environment

Prerequisites:

- Java 17 (Forge 1.20.1 toolchain).
- Python 3.10+ (optional but recommended for tooling and content validation).

Run the mod in a Forge client dev session:

```powershell
.\gradlew.bat runClient
```

Run verification checks:

```powershell
.\gradlew.bat check
```

## Build The Mod

Compile Java sources:

```powershell
.\gradlew.bat compileJava
```

Build distributable artifacts:

```powershell
.\gradlew.bat build
```

## How Datapacks Extend ExtremeCraft

ExtremeCraft loads JSON content from built-in resources and external datapacks at reload/startup.

- Built-in content: `src/main/resources/data/extremecraft/...`
- External content workspace: `datapacks/`

Supported extension domains include machines, abilities, skills, quests, tech trees, recipes, and related progression content. See `DATAPACK_FORMAT.md` for schema details and examples.
