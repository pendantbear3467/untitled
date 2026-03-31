# combat/dualwield/validation

Validation and anti-spam guards for offhand action packets.

Role:

- enforce target/reach/state preconditions
- apply lane-based cooldown and pacing checks
- produce normalized validation results used by execution services

Guidance:

- keep policy checks explicit and side-effect free
- isolate timing logic from gameplay execution behavior
