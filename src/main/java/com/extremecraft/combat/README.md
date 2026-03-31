# combat

Centralized combat computation and event bridge layer.

Primary ownership:

- damage context assembly and normalization
- pre/post calculation event hooks
- status effect integration and resistance handling
- combat results used by abilities, spells, and vanilla hurt events

Guidance:

- route new combat math through shared engine/services instead of ad-hoc damage edits
- keep deterministic server-side resolution as the default
- document balancing changes with clear rationale
