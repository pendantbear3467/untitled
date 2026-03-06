from __future__ import annotations

import json
from pathlib import Path

from asset_studio.project.workspace_manager import AssetStudioContext


def export_bbmodel(model_id: str, context: AssetStudioContext, kind: str = "block") -> Path:
    path = context.workspace_root / "blockbench" / f"{model_id}.bbmodel"
    payload = {
        "meta": {"format_version": "4.9", "model_format": "java_block"},
        "name": model_id,
        "kind": kind,
        "textures": [{"name": "texture", "path": f"textures/{kind}/{model_id}.png", "id": "0"}],
        "elements": [],
        "display": {
            "gui": {"rotation": [30, 225, 0], "translation": [0, 0, 0], "scale": [0.625, 0.625, 0.625]},
            "ground": {"rotation": [0, 0, 0], "translation": [0, 3, 0], "scale": [0.25, 0.25, 0.25]},
            "fixed": {"rotation": [0, 180, 0], "translation": [0, 0, 0], "scale": [0.5, 0.5, 0.5]},
        },
    }
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(payload, indent=2) + "\n", encoding="utf-8")
    return path
