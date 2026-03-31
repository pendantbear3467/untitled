# skill_trees

Status: `LIVE_RUNTIME`

This folder owns live skill-tree topology, node costs, and node effects.

Read by:

- `progression/skilltree/SkillTreeDataLoader`
- `progression/skilltree/SkillTreeManager`
- `progression/skilltree/SkillTreeService`

`platform/data/loader/SkillTreeDataLoader` mirrors this folder for validation and
client snapshot sync only.

Do not use `skilltrees/` for live gameplay edits.
