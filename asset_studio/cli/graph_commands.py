from __future__ import annotations

import argparse
import json
import zipfile
from pathlib import Path

from asset_studio.graph.graph_engine import GraphEngine
from asset_studio.workspace.workspace_manager import AssetStudioContext


def register_graph_commands(parser: argparse.ArgumentParser) -> None:
    sub = parser.add_subparsers(dest="graph_action", required=True)

    new_cmd = sub.add_parser("new", help="Create a new graph file")
    new_cmd.add_argument("name", help="Graph name")

    list_cmd = sub.add_parser("list", help="List saved graphs")
    list_cmd.add_argument("--json", action="store_true", help="Print as JSON")

    show_cmd = sub.add_parser("show", help="Show graph summary")
    show_cmd.add_argument("name", help="Graph name")
    show_cmd.add_argument("--json", action="store_true", help="Print graph details as JSON")

    add_cmd = sub.add_parser("add", help="Add node to graph")
    add_cmd.add_argument("name", help="Graph name")
    add_cmd.add_argument("node_type", help="Node type")
    add_cmd.add_argument("--param", action="append", default=[], help="Node parameter in key=value format")
    add_cmd.add_argument("--x", type=float, default=0.0)
    add_cmd.add_argument("--y", type=float, default=0.0)

    link_cmd = sub.add_parser("link", help="Link two nodes in graph")
    link_cmd.add_argument("name", help="Graph name")
    link_cmd.add_argument("src", help="Source node id")
    link_cmd.add_argument("dst", help="Destination node id")
    link_cmd.add_argument("--from-port", default=None)
    link_cmd.add_argument("--to-port", default=None)

    validate_cmd = sub.add_parser("validate", help="Validate graph")
    validate_cmd.add_argument("name", help="Graph name")

    execute_cmd = sub.add_parser("execute", help="Execute graph")
    execute_cmd.add_argument("name", help="Graph name")

    sub.add_parser("execute-all", help="Execute all graphs in workspace")

    arrange_cmd = sub.add_parser("auto-layout", help="Auto arrange graph nodes")
    arrange_cmd.add_argument("name", help="Graph name")
    arrange_cmd.add_argument("--algorithm", choices=["layered", "dag", "force"], default="layered")

    export_cmd = sub.add_parser("export", help="Export graph")
    export_cmd.add_argument("name", help="Graph name")
    export_cmd.add_argument("--format", choices=["json", "yaml", "package"], default="json")

    tpl_export = sub.add_parser("template-export", help="Export current graph as template")
    tpl_export.add_argument("name", help="Graph name")
    tpl_export.add_argument("template_name", help="Template name")

    tpl_import = sub.add_parser("template-import", help="Import template into graph")
    tpl_import.add_argument("name", help="Graph name")
    tpl_import.add_argument("template_name", help="Template name")

    sub.add_parser("template-list", help="List available graph templates")


def run_graph_command(args: argparse.Namespace, context: AssetStudioContext) -> int:
    action = args.graph_action
    plugin_nodes = getattr(getattr(context, "plugins", None), "graph_nodes", {})
    engine = GraphEngine(context.workspace_root, plugin_api_nodes=plugin_nodes)

    if action == "new":
        engine.name = args.name
        path = engine.save(name=args.name)
        print(f"Created graph: {path}")
        return 0

    if action == "list":
        names = [p.stem for p in sorted(engine.graph_root.glob("*.json"))]
        if args.json:
            print(json.dumps({"graphs": names}, indent=2))
        else:
            if not names:
                print("No graphs found")
            else:
                for name in names:
                    print(name)
        return 0

    if action == "show":
        _load_graph(engine, args.name)
        if args.json:
            payload = {
                "name": engine.name,
                "nodes": [
                    {
                        "node_id": n.node_id,
                        "node_type": n.node_type,
                        "x": n.x,
                        "y": n.y,
                        "parameters": n.parameters,
                    }
                    for n in engine.nodes
                ],
                "links": engine.links,
            }
            print(json.dumps(payload, indent=2))
            return 0

        print(f"Graph: {engine.name}")
        print(f"Nodes: {len(engine.nodes)}")
        print(f"Links: {len(engine.links)}")
        for node in engine.nodes:
            print(f"- {node.node_id} [{node.node_type}] params={len(node.parameters)}")
        return 0

    if action == "add":
        _load_graph(engine, args.name)
        params = _parse_params(args.param)
        node = engine.add_node(node_type=args.node_type, parameters=params, x=args.x, y=args.y)
        path = engine.save()
        print(f"Added node {node.node_id} to {engine.name} -> {path}")
        return 0

    if action == "link":
        _load_graph(engine, args.name)
        engine.add_link(args.src, args.dst, src_port=args.from_port, dst_port=args.to_port)
        path = engine.save()
        print(f"Linked {args.src} -> {args.dst} in {engine.name} -> {path}")
        return 0

    if action == "validate":
        _load_graph(engine, args.name)
        report = engine.validate()
        print(f"Validation Report for {engine.name}")
        print(f"- errors: {len(report.errors)}")
        print(f"- warnings: {len(report.warnings)}")
        for error in report.errors:
            print(f"ERROR: {error}")
        for warning in report.warnings:
            print(f"WARN: {warning}")
        return 1 if report.errors else 0

    if action == "execute":
        _load_graph(engine, args.name)
        generated = engine.execute(context)
        print(f"Executed graph {engine.name} ({len(generated)} outputs)")
        for item in generated:
            print(f"- {item}")
        if engine.metadata.get("last_bundle"):
            print(f"Bundle: {engine.metadata['last_bundle']}")
        return 0

    if action == "execute-all":
        graph_files = sorted(engine.graph_root.glob("*.json"))
        if not graph_files:
            print("No graphs found")
            return 0
        failures = 0
        for graph_file in graph_files:
            graph_name = graph_file.stem
            try:
                _load_graph(engine, graph_name)
                generated = engine.execute(context)
                print(f"Executed {graph_name}: {len(generated)} outputs")
            except Exception as exc:  # noqa: BLE001
                failures += 1
                print(f"FAILED {graph_name}: {exc}")
        return 1 if failures else 0

    if action == "auto-layout":
        _load_graph(engine, args.name)
        engine.auto_arrange(algorithm=args.algorithm)
        path = engine.save()
        print(f"Auto arranged {args.name} using {args.algorithm} -> {path}")
        return 0

    if action == "export":
        _load_graph(engine, args.name)
        return _export_graph(engine, args.name, args.format)

    if action == "template-export":
        _load_graph(engine, args.name)
        path = engine.export_template(args.template_name)
        print(f"Template exported: {path}")
        return 0

    if action == "template-import":
        _load_graph(engine, args.name)
        engine.import_template(args.template_name)
        path = engine.save()
        print(f"Template imported into {args.name}: {path}")
        return 0

    if action == "template-list":
        templates = engine.templates.list_templates()
        if not templates:
            print("No templates found")
            return 0
        for template in templates:
            print(template)
        return 0

    raise ValueError(f"Unsupported graph action: {action}")


def _load_graph(engine: GraphEngine, name: str) -> None:
    path = engine.graph_root / f"{name}.json"
    if not path.exists():
        raise FileNotFoundError(f"Graph not found: {name}")
    engine.load(name)


def _parse_params(raw_params: list[str]) -> dict[str, object]:
    result: dict[str, object] = {}
    for raw in raw_params:
        if "=" not in raw:
            raise ValueError(f"Invalid --param value '{raw}'. Expected key=value")
        key, value = raw.split("=", 1)
        key = key.strip()
        if not key:
            raise ValueError("Parameter key cannot be empty")
        result[key] = _coerce_value(value.strip())
    return result


def _coerce_value(raw: str) -> object:
    lowered = raw.lower()
    if lowered in {"true", "false"}:
        return lowered == "true"

    if lowered in {"none", "null"}:
        return None

    try:
        if "." in raw:
            return float(raw)
        return int(raw)
    except ValueError:
        pass

    if (raw.startswith("{") and raw.endswith("}")) or (raw.startswith("[") and raw.endswith("]")):
        try:
            return json.loads(raw)
        except json.JSONDecodeError:
            return raw

    return raw


def _export_graph(engine: GraphEngine, graph_name: str, export_format: str) -> int:
    graph_path = engine.graph_root / f"{graph_name}.json"
    if export_format == "json":
        print(f"Exported JSON: {graph_path}")
        return 0

    if export_format == "yaml":
        payload = json.loads(graph_path.read_text(encoding="utf-8"))
        yaml_path = engine.graph_root / f"{graph_name}.yaml"
        yaml_path.write_text(_to_yaml(payload), encoding="utf-8")
        print(f"Exported YAML: {yaml_path}")
        return 0

    package_path = engine.graph_root / f"{graph_name}.ecgraph.zip"
    with zipfile.ZipFile(package_path, mode="w", compression=zipfile.ZIP_DEFLATED) as zf:
        zf.write(graph_path, arcname=f"{graph_name}.json")
        manifest = {
            "name": graph_name,
            "type": "extremecraft_graph_package",
            "version": 1,
        }
        manifest_path = engine.graph_root / f"{graph_name}.manifest.json"
        manifest_path.write_text(json.dumps(manifest, indent=2) + "\n", encoding="utf-8")
        zf.write(manifest_path, arcname="manifest.json")
        manifest_path.unlink(missing_ok=True)
    print(f"Exported package: {package_path}")
    return 0


def _to_yaml(value, indent: int = 0) -> str:
    pad = " " * indent
    if isinstance(value, dict):
        lines: list[str] = []
        for key, child in value.items():
            if isinstance(child, (dict, list)):
                lines.append(f"{pad}{key}:")
                lines.append(_to_yaml(child, indent + 2))
            else:
                lines.append(f"{pad}{key}: {_yaml_scalar(child)}")
        return "\n".join(lines) + "\n"
    if isinstance(value, list):
        lines = []
        for child in value:
            if isinstance(child, (dict, list)):
                lines.append(f"{pad}-")
                lines.append(_to_yaml(child, indent + 2))
            else:
                lines.append(f"{pad}- {_yaml_scalar(child)}")
        return "\n".join(lines) + "\n"
    return f"{pad}{_yaml_scalar(value)}\n"


def _yaml_scalar(value) -> str:
    if value is None:
        return "null"
    if isinstance(value, bool):
        return "true" if value else "false"
    if isinstance(value, (int, float)):
        return str(value)
    text = str(value).replace("\"", "\\\"")
    return f'"{text}"'