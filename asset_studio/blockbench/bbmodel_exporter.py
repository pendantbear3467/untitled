from __future__ import annotations

import json
from pathlib import Path

from asset_studio.blockbench.model_converter import minecraft_json_to_bbmodel
from asset_studio.workspace.workspace_manager import AssetStudioContext


def export_bbmodel(model_id: str, context: AssetStudioContext, kind: str = "block") -> Path:
    path = context.workspace_root / "blockbench" / f"{model_id}.bbmodel"
    model_json_path = context.workspace_root / "assets" / "models" / kind / f"{model_id}.json"
    if model_json_path.exists():
        model_json = json.loads(model_json_path.read_text(encoding="utf-8"))
    else:
        model_json = {"textures": {"all": f"extremecraft:{kind}/{model_id}"}, "elements": []}

    payload = minecraft_json_to_bbmodel(model_json, model_name=model_id)
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(payload, indent=2) + "\n", encoding="utf-8")
    return path
