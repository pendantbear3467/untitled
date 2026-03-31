# combat/dualwield/service

Execution services for validated dual-wield/offhand actions.

Role:

- execute server-authoritative offhand interactions
- keep action-side effects centralized and deterministic
- separate execution from validation/rate-limiting concerns

Guidance:

- keep gameplay mutation in service methods
- preserve shared combat pipeline usage for living targets
