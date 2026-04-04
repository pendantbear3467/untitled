# Agent Collaboration Contract

Applies to split-planning, extraction prep, and migration safety work.

## Shared Rules

1. Non-destructive by default: never delete host-owned runtime folders in split passes.
2. Reuse existing docs/plans before proposing new structure.
3. Prefer additive changes and explicit reports over implicit refactors.
4. Run baseline compile/tests before and after structural changes.
5. If ownership is unclear, defer to existing contracts in `docs/`.

## Role Split

- Claude: architecture reasoning, migration tradeoff analysis, risk narratives.
- Codex: implementation edits, scripts/automation, compile-safe refactors.
- Copilot: orchestration, execution sequencing, validation, final convergence checks.

## Conflict Handling

If two agents propose conflicting edits:

1. Keep the path with lower runtime risk.
2. Keep host bootstrap ownership intact.
3. Require passing compile + boundary tests.
4. Record the decision in the active split runbook/report.
