# Entity Renderers

Status: `LIVE_RUNTIME`

This folder owns the live Forge renderer registration and texture binding path for entities.

Runtime-critical files:
- `ModEntityRenderers`
- Individual `*Renderer` classes

Safe replacement workflow:
1. Keep the entity id and texture path stable.
2. Replace the Java model/layer hookup deliberately when a new Blockbench-backed runtime path is introduced.
3. Update the metadata files in `assets/extremecraft/entities` at the same time so placeholder status stays honest.

Common mistakes:
- Editing only `assets/extremecraft/models/entity/*.json` and expecting in-game visuals to change.
- Changing texture ids without updating renderer classes.
