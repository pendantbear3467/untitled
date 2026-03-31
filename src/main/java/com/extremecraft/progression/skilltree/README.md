# progression/skilltree

Skill tree data model, loaders, runtime services, and sync/interaction packets.

Role:

- load and validate skill tree definitions
- manage unlock/simulation/runtime progression rules
- sync skill tree state to client views

Guidance:

- keep unlock prerequisites and point-cost logic centralized
- validate content inputs before committing unlock changes
