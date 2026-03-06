from __future__ import annotations

import json
import shutil
from pathlib import Path

from asset_studio.blockbench.model_converter import bbmodel_to_minecraft_json
from asset_studio.minecraft.blockstate_templates import single_variant_blockstate
from asset_studio.workspace.workspace_manager import AssetStudioContext


def import_bbmodel(path: Path, context: AssetStudioContext) -> str:
    data = json.loads(path.read_text(encoding="utf-8"))
    model_name = str(data.get("name", path.stem)).lower().replace(" ", "_")

    context.write_json(
        context.workspace_root / "assets" / "models" / "block" / f"{model_name}.json",
        bbmodel_to_minecraft_json(data, model_name),
    )
    context.write_json(
        context.workspace_root / "assets" / "blockstates" / f"{model_name}.json",
        single_variant_blockstate(model_name),
    )

    baked_texture = _bake_texture(path, data, context, model_name)
    texture_path = context.workspace_root / "assets" / "textures" / "block" / f"{model_name}.png"
    if not baked_texture and not texture_path.exists():
        auto = context.texture_engine.generate_block_texture(material=model_name, style="industrial")
        context.write_texture(texture_path, auto.image)

    _export_animation_bundle(model_name, data, context)
    context.write_preview_snapshot(model_name)
    return model_name


def _bake_texture(bbmodel_path: Path, bbmodel_data: dict, context: AssetStudioContext, model_name: str) -> bool:
    textures = bbmodel_data.get("textures") if isinstance(bbmodel_data.get("textures"), list) else []
    if not textures:
        return False

    source = None
    for entry in textures:
        if not isinstance(entry, dict):
            continue
        source = _resolve_texture_path(bbmodel_path, entry)
        if source and source.exists():
            break

    if source is None or not source.exists():
        return False

    target = context.workspace_root / "assets" / "textures" / "block" / f"{model_name}.png"
    target.parent.mkdir(parents=True, exist_ok=True)
    shutil.copy2(source, target)
    return True


def _resolve_texture_path(bbmodel_path: Path, entry: dict) -> Path | None:
    for key in ["source", "path", "relative_path"]:
        value = entry.get(key)
        if not isinstance(value, str) or not value.strip():
            continue

        candidate = Path(value)
        if candidate.exists():
            return candidate

        local = bbmodel_path.parent / candidate
        if local.exists():
            return local

    return None


def _export_animation_bundle(model_name: str, bbmodel_data: dict, context: AssetStudioContext) -> None:
    animations = bbmodel_data.get("animations")
    if not isinstance(animations, list):
        return

    path = context.workspace_root / "blockbench" / "animations" / f"{model_name}.json"
    context.write_json(path, {"model": model_name, "animations": animations})
