# network/packet

Packet payload definitions and handler entrypoints.

Expected contents:

- C2S request packets for player actions
- S2C sync/result packets for client state updates
- encode/decode/handle routines with defensive validation

Guidance:

- keep packet payloads minimal and explicit
- avoid embedding business logic in packet classes; delegate to domain services
- perform null/direction/rate-limit checks before mutating server state
