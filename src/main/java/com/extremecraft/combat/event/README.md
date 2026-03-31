# combat/event

Combat pre/post calculation events used to extend or observe damage processing.

Purpose:

- expose structured event hooks around damage calculations
- allow modules/systems to participate without modifying core engine code

Guidance:

- keep event payloads stable and explicit
- avoid side effects that break deterministic damage resolution
