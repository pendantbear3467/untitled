from __future__ import annotations

import json
from pathlib import Path

from asset_studio.graph.graph_migrations import CURRENT_GRAPH_VERSION, migrate_payload
from asset_studio.graph.graph_nodes import BaseGraphNode, NodePort


class GraphSerializer:
    @staticmethod
    def to_dict(name: str, nodes: list[BaseGraphNode], links: list[dict], metadata: dict | None = None) -> dict:
        return {
            "name": name,
            "graph_version": CURRENT_GRAPH_VERSION,
            "metadata": metadata or {},
            "nodes": [
                {
                    "node_id": node.node_id,
                    "node_type": node.node_type,
                    "title": node.title,
                    "x": node.x,
                    "y": node.y,
                    "inputs": [{"name": p.name, "port_type": p.port_type} for p in node.inputs],
                    "outputs": [{"name": p.name, "port_type": p.port_type} for p in node.outputs],
                    "parameters": node.parameters,
                    "execution_state": node.execution_state,
                    "last_error": node.last_error,
                }
                for node in nodes
            ],
            "links": links,
        }

    @staticmethod
    def from_dict(payload: dict) -> tuple[str, list[BaseGraphNode], list[dict], dict]:
        payload = migrate_payload(payload)
        nodes: list[BaseGraphNode] = []
        for raw in payload.get("nodes", []):
            nodes.append(
                BaseGraphNode(
                    node_id=str(raw.get("node_id", "")),
                    node_type=str(raw.get("node_type", "")),
                    title=str(raw.get("title", raw.get("node_type", "Node"))),
                    x=float(raw.get("x", 0.0)),
                    y=float(raw.get("y", 0.0)),
                    inputs=[
                        NodePort(name=str(p.get("name", "")), port_type=str(p.get("port_type", "any")), direction=str(p.get("direction", "input")))
                        for p in raw.get("inputs", [])
                    ],
                    outputs=[
                        NodePort(name=str(p.get("name", "")), port_type=str(p.get("port_type", "any")), direction=str(p.get("direction", "output")))
                        for p in raw.get("outputs", [])
                    ],
                    parameters=dict(raw.get("parameters", {})),
                    execution_state=str(raw.get("execution_state", "idle")),
                    last_error=raw.get("last_error"),
                )
            )
        metadata = dict(payload.get("metadata", {}))
        return str(payload.get("name", "graph")), nodes, list(payload.get("links", [])), metadata

    @staticmethod
    def save(path: Path, name: str, nodes: list[BaseGraphNode], links: list[dict], metadata: dict | None = None) -> None:
        path.parent.mkdir(parents=True, exist_ok=True)
        payload = GraphSerializer.to_dict(name, nodes, links, metadata=metadata)
        path.write_text(json.dumps(payload, indent=2) + "\n", encoding="utf-8")

    @staticmethod
    def load(path: Path) -> tuple[str, list[BaseGraphNode], list[dict], dict]:
        payload = json.loads(path.read_text(encoding="utf-8"))
        return GraphSerializer.from_dict(payload)
