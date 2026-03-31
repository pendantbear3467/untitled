# progression/skilltree

Status: `LIVE_RUNTIME` with `COMPATIBILITY MIRROR` overlap

This package owns live skill-tree loading, unlock checks, and skill-tree state
sync for gameplay.

Runtime-critical files:

- `SkillTreeDataLoader`
- `SkillTreeManager`
- `SkillTreeService`
- `PlayerSkillTreeEvents`

Live data owner:

- `src/main/resources/data/extremecraft/skill_trees/*.json`

Compatibility overlap:

- `SkillTreeRegistry` is a legacy mirror populated from `SkillTreeManager`
- `platform/data/loader/SkillTreeDataLoader` mirrors skill trees for validation
  and client snapshot sync only

Legacy/disconnected data path:

- `src/main/resources/data/extremecraft/skilltrees/*.json`

Guidance:

- Keep unlock prerequisites and point-cost logic centralized here.
- Do not start new gameplay edits in `skilltrees/`.
