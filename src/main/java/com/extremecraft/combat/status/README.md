# combat/status

Status effect interfaces and managers that modify combat context.

Role:

- apply transient status-driven modifiers before damage resolution
- keep status logic decoupled from packet and UI layers

Guidance:

- prefer additive status hooks over direct engine rewrites
- keep status math transparent and testable
