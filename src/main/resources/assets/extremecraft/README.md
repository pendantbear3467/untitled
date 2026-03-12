# Asset Root

This folder owns live runtime assets plus a small amount of handoff metadata.

Live runtime owner folders:
- `blockstates/`
- `models/block/`
- `models/item/`
- `textures/`
- `lang/`

Metadata-only handoff folders:
- `entities/`
- `models/entity/`

Important distinction:
- In-game entity rendering is still owned by Java renderer/model classes.
- The entity JSON files here are documentation/handoff metadata so Blockbench replacement work can happen without guessing the runtime path.

Common mistakes:
- Treating every JSON under `assets/` as a runtime consumer input.
- Changing ids here without updating the Java owner path.
