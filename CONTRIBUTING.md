# Open Source Contribution Guide

## Workflow

1. Fork the repository.
2. Create a feature branch.
3. Keep PRs focused and small.
4. Include tests or validation steps when applicable.
5. Open a pull request with a clear impact summary.

## Coding expectations

- Keep systems modular and data-driven.
- Prefer extension points over hardcoded behavior.
- Avoid broad refactors without architectural notes in `docs/`.
- Follow package boundaries (`api`, `platform`, domain systems).

## Module submissions

For external module contributions, include:

- compatibility target (`apiVersion`, `protocolVersion`)
- datapack schema documentation
- migration notes for breaking changes
- test scenario for startup/load correctness
