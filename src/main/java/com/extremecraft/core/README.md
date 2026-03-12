# Core Bootstrap

Status: `LIVE_RUNTIME`

This folder owns Forge bootstrap and global startup orchestration.

Runtime-critical files:
- `ExtremeCraft.java` registers registries, gameplay services, reload listeners, and client bridge wiring.
- `ECConstants.java` and closely related constants/config entrypoints affect runtime IDs and startup behavior.

Metadata-only files:
- None in this folder. Treat everything here as startup-sensitive.

Consumed by:
- Forge mod loading
- All registries under `registry/` and `future/registry/`
- Gameplay event registrations for progression, machines, classes, research, modules, worldgen, radiation, and entity systems

Legacy or adapter notes:
- This folder intentionally boots both canonical systems and compatibility shims. Do not remove registrations without checking save/runtime impact.

Safe future additions:
1. Register new subsystems here only when they need lifecycle hookup.
2. Keep domain logic in the owning package; leave this folder as orchestration only.
3. Prefer comments and explicit registration order over implicit side effects.

Common mistakes:
- Putting gameplay logic directly in `ExtremeCraft`.
- Registering the same reload listener from multiple folders.
- Treating top-level repo folders outside `src/main/java` as part of the runtime source set.
