# Entity Asset Metadata

Status: `METADATA_ONLY`

This folder documents the current placeholder/runtime ownership state for entities.

Owns:
- Placeholder status
- Runtime owner pointers
- Texture/model metadata used for art handoff and Blockbench planning

Does not own:
- Live renderer registration
- Live model baking
- Animation runtime execution

Safe future workflow:
1. Keep entity ids stable.
2. Update the metadata JSON when the Java runtime owner changes.
3. Use these files to describe which entities are still placeholders versus final art.
