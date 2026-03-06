from __future__ import annotations

from asset_studio.minecraft.blockstate_templates import single_variant_blockstate
from asset_studio.minecraft.model_templates import cube_block_model
from asset_studio.project.workspace_manager import AssetStudioContext


class BlockGenerator:
    def __init__(self, context: AssetStudioContext) -> None:
        self.context = context

    def generate(self, block_name: str, material: str, texture_style: str) -> None:
        texture = self.context.texture_engine.generate_block_texture(material=material, style=texture_style)
        self.context.write_texture(self.context.workspace_root / "assets" / "textures" / "block" / f"{block_name}.png", texture.image)

        self.context.write_json(
            self.context.workspace_root / "assets" / "models" / "block" / f"{block_name}.json",
            cube_block_model(block_name),
        )
        self.context.write_json(
            self.context.workspace_root / "assets" / "models" / "item" / f"{block_name}.json",
            {"parent": f"extremecraft:block/{block_name}"},
        )
        self.context.write_json(
            self.context.workspace_root / "assets" / "blockstates" / f"{block_name}.json",
            single_variant_blockstate(block_name),
        )
