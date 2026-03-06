from __future__ import annotations

import json
from dataclasses import dataclass
from datetime import datetime, timezone
from pathlib import Path


@dataclass
class GraphCompileResult:
    output_dir: Path
    bundle_file: Path
    generated: list[str]


class GraphCompiler:
    def __init__(self, workspace_root: Path) -> None:
        self.workspace_root = workspace_root
        self.generated_root = workspace_root / "generated"
        self.generated_root.mkdir(parents=True, exist_ok=True)

    def compile(self, graph_name: str, generated: list[str]) -> GraphCompileResult:
        out_dir = self.generated_root / graph_name
        out_dir.mkdir(parents=True, exist_ok=True)

        bundle = {
            "graph": graph_name,
            "generated": generated,
            "generated_at": datetime.now(timezone.utc).isoformat(),
        }
        bundle_file = out_dir / "content_bundle.json"
        bundle_file.write_text(json.dumps(bundle, indent=2) + "\n", encoding="utf-8")

        # Keep a top-level marker for external pipeline tooling.
        marker = self.generated_root / f"{graph_name}.bundle.json"
        marker.write_text(json.dumps(bundle, indent=2) + "\n", encoding="utf-8")

        return GraphCompileResult(output_dir=out_dir, bundle_file=bundle_file, generated=generated)
