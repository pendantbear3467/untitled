from __future__ import annotations

import json
from pathlib import Path

from asset_studio.graph.graph_nodes import BaseGraphNode
from asset_studio.graph.graph_serializer import GraphSerializer


class GraphTemplateManager:
    def __init__(self, workspace_root: Path) -> None:
        self.templates_root = workspace_root / "templates"
        self.templates_root.mkdir(parents=True, exist_ok=True)

    def export_template(
        self,
        template_name: str,
        nodes: list[BaseGraphNode],
        links: list[dict],
        node_ids: set[str] | None = None,
    ) -> Path:
        selected = [n for n in nodes if node_ids is None or n.node_id in node_ids]
        selected_ids = {n.node_id for n in selected}
        selected_links = [
            l
            for l in links
            if str(l.get("from", "")) in selected_ids and str(l.get("to", "")) in selected_ids
        ]

        payload = GraphSerializer.to_dict(template_name, selected, selected_links)
        payload["template"] = True
        path = self.templates_root / f"{template_name}.template.json"
        path.write_text(json.dumps(payload, indent=2) + "\n", encoding="utf-8")
        return path

    def import_template(self, template_name: str) -> tuple[list[BaseGraphNode], list[dict]]:
        path = self.templates_root / f"{template_name}.template.json"
        payload = json.loads(path.read_text(encoding="utf-8"))
        _, nodes, links, _metadata = GraphSerializer.from_dict(payload)
        return nodes, links

    def list_templates(self) -> list[str]:
        return [p.stem.replace(".template", "") for p in sorted(self.templates_root.glob("*.template.json"))]
