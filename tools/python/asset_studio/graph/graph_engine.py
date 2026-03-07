from __future__ import annotations

from pathlib import Path

from asset_studio.graph.graph_compiler import GraphCompiler
from asset_studio.graph.graph_debugger import GraphDebugSession
from asset_studio.graph.graph_executor import GraphExecutor
from asset_studio.graph.graph_layout import GraphLayout
from asset_studio.graph.graph_node_registry import discover_graph_plugins, register_plugin_api_nodes
from asset_studio.graph.graph_nodes import BaseGraphNode, create_node
from asset_studio.graph.graph_preview import GraphPreviewBuilder
from asset_studio.graph.graph_serializer import GraphSerializer
from asset_studio.graph.graph_templates import GraphTemplateManager
from asset_studio.graph.graph_validator import GraphValidator


class GraphEngine:
    def __init__(self, workspace_root: Path, plugin_api_nodes: dict[str, object] | None = None) -> None:
        self.workspace_root = workspace_root
        self.graph_root = workspace_root / "graphs"
        self.graph_root.mkdir(parents=True, exist_ok=True)
        self.nodes: list[BaseGraphNode] = []
        self.links: list[dict] = []
        self.name = "untitled_graph"
        self.metadata: dict[str, object] = {}
        self.validator = GraphValidator()
        self.executor = GraphExecutor()
        self.layout = GraphLayout()
        self.templates = GraphTemplateManager(workspace_root)
        self.preview_builder = GraphPreviewBuilder()
        self.compiler = GraphCompiler(workspace_root)

        discover_graph_plugins(workspace_root)
        if plugin_api_nodes:
            register_plugin_api_nodes(plugin_api_nodes)

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
        GraphSerializer.save(path, self.name, self.nodes, self.links, metadata=self.metadata)
        return path

    def load(self, name: str) -> Path:
        path = self.graph_root / f"{name}.json"
        self.name, self.nodes, self.links, self.metadata = GraphSerializer.load(path)
        return path

    def validate(self):
        return self.validator.validate(self.nodes, self.links)

    def execute(self, context) -> list[str]:
        report = self.validate()
        if report.errors:
            raise ValueError("Graph validation failed: " + "; ".join(report.errors))

        debug = GraphDebugSession(graph_name=self.name)
        generated = self.executor.execute(self.nodes, self.links, context, debug_session=debug)
        compile_result = self.compiler.compile(self.name, generated)
        self.metadata["last_bundle"] = str(compile_result.bundle_file)
        self.metadata["last_debug_events"] = len(debug.events)
        self.save(self.name)
        return generated

    def execute_with_debug(self, context) -> tuple[list[str], GraphDebugSession]:
        report = self.validate()
        if report.errors:
            raise ValueError("Graph validation failed: " + "; ".join(report.errors))
        debug = GraphDebugSession(graph_name=self.name)
        generated = self.executor.execute(self.nodes, self.links, context, debug_session=debug)
        compile_result = self.compiler.compile(self.name, generated)
        self.metadata["last_bundle"] = str(compile_result.bundle_file)
        self.save(self.name)
        return generated, debug

    def auto_arrange(self, algorithm: str = "layered") -> None:
        self.layout.auto_arrange(self.nodes, self.links, algorithm=algorithm)

    def preview(self):
        return self.preview_builder.build(self.workspace_root)

    def export_template(self, template_name: str, node_ids: set[str] | None = None) -> Path:
        return self.templates.export_template(template_name, self.nodes, self.links, node_ids=node_ids)

    def import_template(self, template_name: str, *, x_offset: float = 80.0, y_offset: float = 80.0) -> None:
        nodes, links = self.templates.import_template(template_name)
        id_map: dict[str, str] = {}

        for node in nodes:
            base = f"{node.node_type.lower()}_{len(self.nodes)+1}"
            while self._find_node(base) is not None:
                base = f"{node.node_type.lower()}_{len(self.nodes)+1}"
            id_map[node.node_id] = base
            node.node_id = base
            node.x += x_offset
            node.y += y_offset
            self.nodes.append(node)

        for link in links:
            src = str(link.get("from", ""))
            dst = str(link.get("to", ""))
            if src in id_map and dst in id_map:
                self.links.append(
                    {
                        "from": id_map[src],
                        "to": id_map[dst],
                        "from_port": str(link.get("from_port", "")),
                        "to_port": str(link.get("to_port", "")),
                    }
                )

    def _find_node(self, node_id: str) -> BaseGraphNode | None:
        for node in self.nodes:
            if node.node_id == node_id:
                return node
        return None
