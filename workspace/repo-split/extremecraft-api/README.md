# api

Public integration surface for ExtremeCraft.

Purpose:

- expose stable Java interfaces/contracts for integrations and addon modules
- keep extension APIs separate from runtime-internal implementation details

Typical contents:

- API interfaces and data contracts
- provider/bootstrap access points used by integrations
- registration abstractions for externally-defined content

Build notes:

- this module is included by Gradle configuration and may be consumed by runtime or tooling builds
- keep API signatures backward-compatible where possible

Non-goals:

- gameplay implementation logic should stay in `../src/main/java/com/extremecraft/**`
- generated assets/resources should not be authored here
