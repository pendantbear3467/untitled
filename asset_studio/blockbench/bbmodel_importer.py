from __future__ import annotations

import json
from pathlib import Path

from asset_studio.minecraft.model_templates import block_model_from_blockbench
from asset_studio.project.workspace_manager import AssetStudioContext


def import_bbmodel(path: Path, context: AssetStudioContext) -> str:
    data = json.loads(path.read_text(encoding="utf-8"))
    model_name = str(data.get("name", path.stem)).lower().replace(" ", "_")

    context.write_json(
        context.workspace_root / "assets" / "models" / "block" / f"{model_name}.json",
        block_model_from_blockbench(model_name, data),
    )

    texture_path = context.workspace_root / "assets" / "textures" / "block" / f"{model_name}.png"
    if not texture_path.exists():
        auto = context.texture_engine.generate_block_texture(material=model_name, style="industrial")
        context.write_texture(texture_path, auto.image)

    context.write_preview_snapshot(model_name)
    return model_name
