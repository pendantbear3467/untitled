# materials

Status: `METADATA / VALIDATION MIRROR`

`platform/data/loader/MaterialDataLoader` reads this folder for material
metadata, sync snapshots, and validation use.

Current live ore/item/block registration still starts in:

- `machine/material/OreMaterialCatalog`
- `future/registry/TechBlocks`
- `future/registry/TechItems`

Current live world placement still comes from `worldgen/` and `forge/`.
