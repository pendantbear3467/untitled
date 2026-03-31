# progression/capability

Capability providers and APIs for progression-related player state.

Covers:

- capability storage/provider wiring
- API facades used by runtime systems
- capability event registration for attach/sync lifecycle

Guidance:

- avoid direct NBT mutation outside capability/service boundaries
- keep sync behavior explicit for client-facing mirrors
