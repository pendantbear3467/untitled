# network

Canonical network layer for ExtremeCraft packet registration and sync behavior.

Primary ownership:

- `ModNetwork`: single packet channel owner and packet registration authority
- `packet/`: packet payload models and handlers
- `sync/`: runtime sync services and S2C sync packets
- `security/`: packet throttling and guardrail utilities

Contributor rules:

- register new packets in `ModNetwork.init()` only
- keep C2S handlers server-authoritative and direction-validated
- route sensitive C2S paths through packet limiting in `security/`

Ownership note:

- `packet/` and `sync/` contain live gameplay packets and runtime snapshot sync.
- `platform/data/sync/**` sends metadata/catalog snapshots to clients; those sync
  packets do not become the gameplay owner for similarly named systems.
