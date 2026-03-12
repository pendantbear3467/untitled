# Asset Entity Metadata

This folder contains descriptive asset metadata for entity entries.

What this folder does:
- Stores small JSON notes like `render_hint` and lore text for entity assets.

What this folder does not do:
- It does not bind live renderers.
- It does not select model classes.
- It does not control gameplay stats or spawn behavior.

Live owners instead:
- Renderer ownership: `src/main/java/com/extremecraft/client/render/entity/`
- Model ownership: `src/main/java/com/extremecraft/client/model/entity/`
- Texture ownership: `src/main/resources/assets/extremecraft/textures/entity/`

Future additions should follow:
- Keep ids aligned with renderer/model/texture ids.
- Treat this folder as art handoff metadata until a runtime consumer is introduced.