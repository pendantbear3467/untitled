# combat/dualwield

Dual-wield runtime, packet handlers, and validation/service slices.

Role:

- manage dual-wield loadouts and persistence
- handle C2S/S2C dual-wield packet flows
- validate actions before server mutation/execution

Guidance:

- keep validation and execution separated for auditability
- preserve deterministic server authority over offhand actions
