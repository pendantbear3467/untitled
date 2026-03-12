# Entity Runtime

Status: `LIVE_RUNTIME`

This folder owns entity registration, server-side mob/boss behavior, spawn wiring, and extension hooks.

Runtime-critical files:
- `ModEntities`
- `MobSpawns`
- `MobAttributes`
- `mob/*`
- `boss/*`
- `extension/*`
- `system/*`

Client/runtime split:
- Server/common entity authority stays here.
- Client renderer/model ownership lives under `client/render/entity` and `client/model/entity`.

Safe future additions:
1. Register new entity ids in `ModEntities`.
2. Add server behavior here first.
3. Add renderer/model metadata and docs in the client/assets folders after the entity id is stable.

Common mistakes:
- Treating `assets/extremecraft/entities/*.json` as the runtime entity owner.
- Putting common behavior in client-only packages.
