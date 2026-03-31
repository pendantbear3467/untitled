# network/sync

Runtime synchronization services and packet adapters.

Role:

- collect authoritative server state snapshots
- deliver consistent S2C sync updates to clients
- isolate sync cadence/indexing concerns from gameplay services

Guidance:

- treat server state as source-of-truth
- keep sync packets idempotent where possible
- avoid client-only assumptions inside server sync services
