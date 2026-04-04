# progression/skilltree/service

Focused services for skill-tree evaluation and node-state computations.

Role:

- compute unlock state and prerequisites
- centralize skill tree decision logic consumed by UI/runtime flows
- keep evaluation deterministic across client and server presentation

Guidance:

- avoid duplicating prerequisite logic outside this service layer
- prefer pure computations where possible for easier testing
