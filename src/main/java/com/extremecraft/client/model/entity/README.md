# Entity Models

Status: `LIVE_RUNTIME`

This folder owns the live Java model layer definitions used by Forge entity rendering.

Runtime-critical files:
- `ECBipedEntityModel`
- Individual `*Model` classes

Current state:
- Most first-release entities are still cube-based Java placeholder models with shared idle/walk/attack animation.
- The asset JSON files under `assets/extremecraft/models/entity` are handoff metadata only.

Blockbench-safe future additions:
1. Preserve the entity id and texture path.
2. Decide whether the replacement stays Java-baked or introduces a new runtime model bridge.
3. Keep the placeholder metadata updated so artists and gameplay contributors see the same ownership story.

Common mistakes:
- Replacing only textures and assuming silhouette/animation changed.
- Adding another parallel model owner without documenting the runtime path.
