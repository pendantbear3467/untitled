from __future__ import annotations

import base64
import json
import shutil
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
    _attach_animation_data(payload, model_id, context)
    _bake_texture_data(payload, model_id, kind, context)

    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(payload, indent=2) + "\n", encoding="utf-8")
    return path


def _attach_animation_data(payload: dict, model_id: str, context: AssetStudioContext) -> None:
    animation_path = context.workspace_root / "blockbench" / "animations" / f"{model_id}.json"
    if not animation_path.exists():
        return

    animation_payload = json.loads(animation_path.read_text(encoding="utf-8"))
    animations = animation_payload.get("animations") if isinstance(animation_payload, dict) else None
    if isinstance(animations, list):
        payload["animations"] = animations


def _bake_texture_data(payload: dict, model_id: str, kind: str, context: AssetStudioContext) -> None:
    texture_path = context.workspace_root / "assets" / "textures" / kind / f"{model_id}.png"
    if not texture_path.exists():
        return

    encoded = base64.b64encode(texture_path.read_bytes()).decode("ascii")
    textures = payload.get("textures") if isinstance(payload.get("textures"), list) else []
    if not textures:
        textures = [{"name": "texture", "id": "0"}]
        payload["textures"] = textures

    first = textures[0]
    first["path"] = f"textures/{kind}/{model_id}.png"
    first["source"] = f"data:image/png;base64,{encoded}"

    export_texture = context.workspace_root / "blockbench" / "exported_textures" / f"{model_id}.png"
    export_texture.parent.mkdir(parents=True, exist_ok=True)
    shutil.copy2(texture_path, export_texture)
