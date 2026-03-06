from __future__ import annotations

from collections import defaultdict, deque

from asset_studio.graph.graph_nodes import BaseGraphNode


class GraphLayout:
    def auto_arrange(self, nodes: list[BaseGraphNode], links: list[dict], algorithm: str = "layered") -> None:
        if algorithm == "dag":
            self._layout_layered(nodes, links, x_step=280, y_step=120)
            return
        if algorithm == "force":
            self._layout_force(nodes)
            return
        self._layout_layered(nodes, links, x_step=240, y_step=110)

    def _layout_layered(self, nodes: list[BaseGraphNode], links: list[dict], *, x_step: int, y_step: int) -> None:
        indegree: dict[str, int] = {n.node_id: 0 for n in nodes}
        graph: dict[str, list[str]] = defaultdict(list)
        for link in links:
            src = str(link.get("from", ""))
            dst = str(link.get("to", ""))
            if src in indegree and dst in indegree:
                graph[src].append(dst)
                indegree[dst] += 1

        q = deque([nid for nid, deg in indegree.items() if deg == 0])
        level: dict[str, int] = {nid: 0 for nid in q}

        while q:
            nid = q.popleft()
            for nxt in graph.get(nid, []):
                level[nxt] = max(level.get(nxt, 0), level.get(nid, 0) + 1)
                indegree[nxt] -= 1
                if indegree[nxt] == 0:
                    q.append(nxt)

        buckets: dict[int, list[BaseGraphNode]] = defaultdict(list)
        for node in nodes:
            buckets[level.get(node.node_id, 0)].append(node)

        for layer in sorted(buckets.keys()):
            group = buckets[layer]
            for row, node in enumerate(group):
                node.x = float(80 + layer * x_step)
                node.y = float(80 + row * y_step)

    def _layout_force(self, nodes: list[BaseGraphNode]) -> None:
        # Lightweight force-like spacing for interactive use.
        cols = 6
        for i, node in enumerate(nodes):
            col = i % cols
            row = i // cols
            node.x = float(100 + col * 220)
            node.y = float(100 + row * 140)
