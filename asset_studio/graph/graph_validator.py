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
        node_map: dict[str, BaseGraphNode] = {}

        for node in nodes:
            if node.node_id in seen:
                errors.append(f"Duplicate node id: {node.node_id}")
            seen.add(node.node_id)
            node_map[node.node_id] = node

            if node.node_type not in NODE_TYPES:
                errors.append(f"Unsupported node type: {node.node_type}")

            errors.extend(f"{node.node_id}: {e}" for e in node.validate())

        for link in links:
            src = str(link.get("from", ""))
            dst = str(link.get("to", ""))
            src_port = str(link.get("from_port", ""))
            dst_port = str(link.get("to_port", ""))

            if src not in seen or dst not in seen:
                errors.append(f"Invalid link {src} -> {dst}")
                continue

            link_errors, link_warnings = self.validate_link(
                node_map[src],
                node_map[dst],
                src_port=src_port,
                dst_port=dst_port,
            )
            errors.extend(link_errors)
            warnings.extend(link_warnings)

        return GraphValidationReport(errors=errors, warnings=warnings)

    def validate_link(
        self,
        src_node: BaseGraphNode,
        dst_node: BaseGraphNode,
        *,
        src_port: str,
        dst_port: str,
    ) -> tuple[list[str], list[str]]:
        errors: list[str] = []
        warnings: list[str] = []

        if src_node.node_id == dst_node.node_id:
            warnings.append(f"Self-link found for {src_node.node_id}")

        src_type = self._find_port_type(src_node.outputs, src_port)
        dst_type = self._find_port_type(dst_node.inputs, dst_port)

        if src_type is None:
            errors.append(f"{src_node.node_id}: unknown output port '{src_port}'")
        if dst_type is None:
            errors.append(f"{dst_node.node_id}: unknown input port '{dst_port}'")
        if src_type is None or dst_type is None:
            return errors, warnings

        if src_type != "any" and dst_type != "any" and src_type != dst_type:
            errors.append(
                f"Type mismatch {src_node.node_id}.{src_port} ({src_type}) -> "
                f"{dst_node.node_id}.{dst_port} ({dst_type})"
            )

        return errors, warnings

    @staticmethod
    def _find_port_type(ports, port_name: str) -> str | None:
        for port in ports:
            if port.name == port_name:
                return port.port_type
        return None
