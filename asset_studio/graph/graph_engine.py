from __future__ import annotations

from pathlib import Path

from asset_studio.graph.graph_executor import GraphExecutor
from asset_studio.graph.graph_nodes import BaseGraphNode, create_node
from asset_studio.graph.graph_serializer import GraphSerializer
from asset_studio.graph.graph_validator import GraphValidator


class GraphEngine:
    def __init__(self, workspace_root: Path) -> None:
        self.workspace_root = workspace_root
        self.graph_root = workspace_root / "graphs"
        self.graph_root.mkdir(parents=True, exist_ok=True)
        self.nodes: list[BaseGraphNode] = []
        self.links: list[dict] = []
        self.name = "untitled_graph"
        self.validator = GraphValidator()
        self.executor = GraphExecutor()

    def add_node(self, node_type: str, parameters: dict[str, object] | None = None, x: float = 0.0, y: float = 0.0) -> BaseGraphNode:
        node_id = f"{node_type.lower()}_{len(self.nodes)+1}"
        node = create_node(node_id=node_id, node_type=node_type, x=x, y=y, parameters=parameters)
        self.nodes.append(node)
        return node

    def add_link(self, src_node_id: str, dst_node_id: str, src_port: str | None = None, dst_port: str | None = None) -> None:
        src_node = self._find_node(src_node_id)
        dst_node = self._find_node(dst_node_id)
        if src_node is None or dst_node is None:
            raise ValueError(f"Invalid link {src_node_id} -> {dst_node_id}: node not found")

        resolved_src_port = src_port or (src_node.outputs[0].name if src_node.outputs else "")
        resolved_dst_port = dst_port or (dst_node.inputs[0].name if dst_node.inputs else "")

        errors, _ = self.validator.validate_link(
            src_node,
            dst_node,
            src_port=resolved_src_port,
            dst_port=resolved_dst_port,
        )
        if errors:
            raise ValueError("; ".join(errors))

        self.links.append(
            {
                "from": src_node_id,
                "to": dst_node_id,
                "from_port": resolved_src_port,
                "to_port": resolved_dst_port,
            }
        )

    def save(self, name: str | None = None) -> Path:
        if name:
            self.name = name
        path = self.graph_root / f"{self.name}.json"
        GraphSerializer.save(path, self.name, self.nodes, self.links)
        return path

    def load(self, name: str) -> Path:
        path = self.graph_root / f"{name}.json"
        self.name, self.nodes, self.links = GraphSerializer.load(path)
        return path

    def validate(self):
        return self.validator.validate(self.nodes, self.links)

    def execute(self, context) -> list[str]:
        report = self.validate()
        if report.errors:
            raise ValueError("Graph validation failed: " + "; ".join(report.errors))
        return self.executor.execute(self.nodes, self.links, context)

    def _find_node(self, node_id: str) -> BaseGraphNode | None:
        for node in self.nodes:
            if node.node_id == node_id:
                return node
        return None
