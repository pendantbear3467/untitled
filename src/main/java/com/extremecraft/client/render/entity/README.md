# Entity Render Pipeline

This folder contains the live Forge renderer registration for ExtremeCraft mobs and bosses.

What this folder does:
- Registers entity renderers in `ModEntityRenderers.java`.
- Binds renderer classes to baked model layer definitions.
- Connects entity types to texture paths under `assets/extremecraft/textures/entity/`.

Safe gameplay and art edits:
- Swap renderer class ownership here when changing a mob to a new render/model stack.
- Update texture resource locations in the per-entity renderer classes.
- Keep model classes under `src/main/java/com/extremecraft/client/model/entity/` in sync with the renderer.

Metadata versus live runtime:
- This folder is live runtime render ownership.
- `assets/extremecraft/entities/*.json` is descriptive metadata only and does not control renderer binding.
- `data/extremecraft/entities/*.json` currently reads as design/gameplay metadata; no direct live loader was found during this pass.

Blockbench and future GeckoLib additions:
- Current runtime is classic `MobRenderer` plus custom `EntityModel` classes.
- GeckoLib exists only as optional compat scaffolding today; there is no active GeckoLib entity pipeline wired here yet.
- For Blockbench exports, replace the model class and renderer together rather than editing only the metadata JSON.

Common edit paths:
- Change a mob texture: update the renderer path and corresponding PNG in `assets/extremecraft/textures/entity/`.
- Replace a placeholder cube model with a Blockbench export: add the new model class under `client/model/entity/`, update the renderer, then update `ModEntityRenderers.registerLayerDefinitions`.
- See `docs/ENTITY_PLACEHOLDER_PIPELINE.md` for the current replacement map.