from __future__ import annotations

from collections import defaultdict, deque
from concurrent.futures import ThreadPoolExecutor
from hashlib import sha1

from asset_studio.graph.graph_debugger import GraphDebugSession
from asset_studio.graph.graph_node_registry import get_registry
from asset_studio.graph.graph_nodes import BaseGraphNode


class GraphExecutor:
    def __init__(self) -> None:
        self._cache: dict[str, list[str]] = {}

    def execute(
        self,
        nodes: list[BaseGraphNode],
        links: list[dict],
        context,
        debug_session: GraphDebugSession | None = None,
    ) -> list[str]:
        node_map = {n.node_id: n for n in nodes}
        pruned_nodes = self._prune_nodes(nodes, links)
        pruned_ids = {n.node_id for n in pruned_nodes}
        pruned_links = [
            l
            for l in links
            if str(l.get("from", "")) in pruned_ids and str(l.get("to", "")) in pruned_ids
        ]

        layers = self._topological_layers(pruned_nodes, pruned_links)
        outputs_by_port: dict[tuple[str, str], object] = {}
        generated: list[str] = []

        for layer in layers:
            with ThreadPoolExecutor(max_workers=max(1, min(4, len(layer)))) as pool:
                futures = []
                for node_id in layer:
                    node = node_map[node_id]
                    futures.append(pool.submit(self._execute_node, node, pruned_links, outputs_by_port, context, debug_session))
                for fut in futures:
                    node_id, node_outputs, port_outputs = fut.result()
                    for port_name, value in port_outputs.items():
                        outputs_by_port[(node_id, port_name)] = value
                    generated.extend(node_outputs)

        return generated

    def _execute_node(
        self,
        node: BaseGraphNode,
        links: list[dict],
        outputs_by_port: dict[tuple[str, str], object],
        context,
        debug_session: GraphDebugSession | None,
    ) -> tuple[str, list[str], dict[str, object]]:
        incoming = [l for l in links if str(l.get("to", "")) == node.node_id]
        inputs: dict[str, object] = {}
        for link in incoming:
            src = str(link.get("from", ""))
            src_port = str(link.get("from_port", ""))
            dst_port = str(link.get("to_port", ""))
            if (src, src_port) in outputs_by_port:
                inputs[dst_port] = outputs_by_port[(src, src_port)]

        cache_key = self._cache_key(node, inputs)
        if cache_key in self._cache:
            node.execution_state = "success"
            node.last_error = None
            if debug_session is not None:
                debug_session.add_event(node.node_id, "success", "cache hit")
            definition = get_registry().get(node.node_type)
            cached_outputs = list(self._cache[cache_key])
            port_outputs: dict[str, object] = {}
            if definition is not None and definition.outputs:
                value = cached_outputs[-1] if cached_outputs else node.node_id
                for port in definition.outputs:
                    port_outputs[port.name] = value
            return node.node_id, cached_outputs, port_outputs

        definition = get_registry().get(node.node_type)
        if definition is None:
            node.execution_state = "error"
            node.last_error = f"No node definition registered for {node.node_type}"
            if debug_session is not None:
                debug_session.add_event(node.node_id, "error", node.last_error)
            raise ValueError(node.last_error)

        node.execution_state = "executing"
        if debug_session is not None:
            debug_session.add_event(node.node_id, "executing", f"Executing {node.node_type}")

        try:
            node_outputs = definition.execute(node, inputs, context)
        except Exception as exc:  # noqa: BLE001
            node.execution_state = "error"
            node.last_error = str(exc)
            if debug_session is not None:
                debug_session.add_event(node.node_id, "error", str(exc))
            raise

        node.execution_state = "success"
        node.last_error = None
        if debug_session is not None:
            debug_session.add_event(node.node_id, "success", f"Generated {len(node_outputs)} outputs")

        self._cache[cache_key] = list(node_outputs)

        port_outputs: dict[str, object] = {}
        if definition.outputs:
            first_output = node_outputs[-1] if node_outputs else node.node_id
            for port in definition.outputs:
                port_outputs[port.name] = first_output

        return node.node_id, node_outputs, port_outputs

    def _topological_layers(self, nodes: list[BaseGraphNode], links: list[dict]) -> list[list[str]]:
        indegree: dict[str, int] = {n.node_id: 0 for n in nodes}
        graph: dict[str, list[str]] = defaultdict(list)

        for link in links:
            src = str(link.get("from", ""))
            dst = str(link.get("to", ""))
            if src in indegree and dst in indegree:
                graph[src].append(dst)
                indegree[dst] += 1

        queue = deque([nid for nid, deg in indegree.items() if deg == 0])
        layers: list[list[str]] = []

        while queue:
            layer_size = len(queue)
            layer: list[str] = []
            for _ in range(layer_size):
                nid = queue.popleft()
                layer.append(nid)
                for nxt in graph.get(nid, []):
                    indegree[nxt] -= 1
                    if indegree[nxt] == 0:
                        queue.append(nxt)
            layers.append(layer)

        unresolved = [nid for nid, deg in indegree.items() if deg > 0]
        if unresolved:
            layers.append(unresolved)
        return layers

    def _prune_nodes(self, nodes: list[BaseGraphNode], links: list[dict]) -> list[BaseGraphNode]:
        outgoing: dict[str, list[str]] = defaultdict(list)
        incoming: dict[str, list[str]] = defaultdict(list)

        for link in links:
            src = str(link.get("from", ""))
            dst = str(link.get("to", ""))
            outgoing[src].append(dst)
            incoming[dst].append(src)

        sinks = [n.node_id for n in nodes if not outgoing.get(n.node_id)]
        keep: set[str] = set(sinks)
        queue = deque(sinks)
        while queue:
            nid = queue.popleft()
            for parent in incoming.get(nid, []):
                if parent not in keep:
                    keep.add(parent)
                    queue.append(parent)

        if not keep:
            return nodes
        return [n for n in nodes if n.node_id in keep]

    @staticmethod
    def _cache_key(node: BaseGraphNode, inputs: dict[str, object]) -> str:
        payload = f"{node.node_type}|{node.parameters}|{inputs}"
        return sha1(payload.encode("utf-8")).hexdigest()
