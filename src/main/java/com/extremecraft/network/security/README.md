# network/security

Packet hardening and abuse-prevention helpers.

Current focus:

- per-player request throttling
- per-key minimum tick spacing
- bounded request windows for sensitive C2S operations

Guidance:

- use these guards in every high-frequency or stateful C2S handler
- prefer explicit limiter keys by feature domain
- keep policy values readable and centrally auditable
