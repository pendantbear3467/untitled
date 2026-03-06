from __future__ import annotations

from dataclasses import dataclass

from asset_studio.graph.graph_nodes import BaseGraphNode, NODE_TYPES


@dataclass
class GraphValidationReport:
    errors: list[str]
    warnings: list[str]


class GraphValidator:
    def validate(self, nodes: list[BaseGraphNode], links: list[dict]) -> GraphValidationReport:
        errors: list[str] = []
        warnings: list[str] = []
        seen: set[str] = set()

        for node in nodes:
            if node.node_id in seen:
                errors.append(f"Duplicate node id: {node.node_id}")
            seen.add(node.node_id)

            if node.node_type not in NODE_TYPES:
                errors.append(f"Unsupported node type: {node.node_type}")

            errors.extend(f"{node.node_id}: {e}" for e in node.validate())

        for link in links:
            src = str(link.get("from", ""))
            dst = str(link.get("to", ""))
            if src not in seen or dst not in seen:
                errors.append(f"Invalid link {src} -> {dst}")
            if src == dst:
                warnings.append(f"Self-link found for {src}")

        return GraphValidationReport(errors=errors, warnings=warnings)
