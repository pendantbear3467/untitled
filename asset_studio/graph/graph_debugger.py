from __future__ import annotations

from dataclasses import dataclass, field
from datetime import datetime, timezone


@dataclass
class DebugEvent:
    timestamp: str
    node_id: str
    state: str
    message: str


@dataclass
class GraphDebugSession:
    graph_name: str
    events: list[DebugEvent] = field(default_factory=list)
    node_states: dict[str, str] = field(default_factory=dict)

    def add_event(self, node_id: str, state: str, message: str) -> None:
        stamp = datetime.now(timezone.utc).isoformat()
        self.events.append(DebugEvent(timestamp=stamp, node_id=node_id, state=state, message=message))
        self.node_states[node_id] = state

    def timeline_lines(self) -> list[str]:
        return [f"{ev.timestamp} | {ev.node_id} | {ev.state} | {ev.message}" for ev in self.events]
