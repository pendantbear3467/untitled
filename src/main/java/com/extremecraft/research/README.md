# Research

Status: `EXPERIMENTAL`

This folder owns the live research definition loader and player research capability, but the broader gameplay loop is only partially wired.

Runtime-critical files:
- `ResearchManager` loads staged research definitions from `data/extremecraft/research`.
- `ResearchCapability`, `ResearchProvider`, and `ResearchCapabilityEvents` store unlocked research state.

What is partially wired:
- Definitions and capability state exist.
- Broad gameplay claiming/consumption hooks are still sparse compared with progression, machines, or quests.

Metadata-only overlap:
- `platform/data/loader/ResearchDataLoader` mirrors the same folder for platform/debug data and future migration.

Safe future additions:
1. Reuse this folder for staged research definitions.
2. Route future research unlock rewards through progression-facing services instead of direct capability writes.
3. Document any new research consumer clearly because the system is not yet fully converged.

Common mistakes:
- Assuming research unlocks automatically gate machines or recipes today.
- Adding a second research registry or capability path.
