# core

Shared contracts and extracted core library module.

Owns:

- shared service contracts and registries
- progression read contracts consumed by non-progression modules
- stable compat hook abstractions intended for addon-facing integration

Depends on:

- `:api`

Exposes:

- `com.extremecraft.ecosystem.core.*` contracts

Must stay internal elsewhere:

- gameplay mutation logic
- progression writes
- domain-specific compat implementations
