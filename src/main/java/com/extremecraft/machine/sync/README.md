# machine/sync

Machine synchronization providers and index/lifecycle helpers.

Role:

- track machine state that must be mirrored to clients
- manage sync registration/lifecycle concerns without polluting processing logic

Guidance:

- keep sync payloads minimal and stable
- preserve server-authoritative state as the source of truth
