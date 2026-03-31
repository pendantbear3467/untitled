# Entity Gameplay Metadata

This folder contains gameplay-facing JSON notes for entity ids, phases, behaviors, and loot references.

Current status from this scan:
- The files are structured like gameplay metadata.
- No direct loader or runtime consumer for this folder was found in the live Java pass.
- Treat this folder as design/reference metadata unless a loader is wired later.

Live owners instead:
- Entity type registration: `src/main/java/com/extremecraft/entity/ModEntities.java`
- Spawn wiring: `src/main/resources/data/extremecraft/forge/biome_modifier/`
- Mob behavior/runtime code: `src/main/java/com/extremecraft/entity/mob/` and `src/main/java/com/extremecraft/entity/boss/`

Future additions should follow:
- Keep ids aligned with actual entity registry ids.
- If a real loader is added later, document it here instead of assuming these files are already authoritative.