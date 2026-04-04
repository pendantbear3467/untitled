# core

Shared contracts and extracted core library module.

Owns:

- shared service contracts and registries
- progression read contracts consumed by non-progression modules
- stable compat hook abstractions intended for addon-facing integration

Depends on:

- no project or Forge dependencies at the moment

Exposes:

- `com.extremecraft.ecosystem.core.*` contracts

Status:

- ready to create as a separate repo now

Must stay internal elsewhere:

- gameplay mutation logic
- progression writes
- domain-specific compat implementations
