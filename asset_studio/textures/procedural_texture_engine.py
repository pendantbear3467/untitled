from __future__ import annotations

import sys
from dataclasses import dataclass
from pathlib import Path

from asset_studio.textures.material_styles import STYLES
from asset_studio.textures.palette_generator import material_hex_color


@dataclass(frozen=True)
class TextureResolution:
    size: int


RESOLUTIONS = {
    "16x": TextureResolution(16),
    "32x": TextureResolution(32),
    "64x": TextureResolution(64),
    "128x": TextureResolution(128),
}


class ProceduralTextureEngine:
    def __init__(self, seed: int = 1337, resolution: str = "32x") -> None:
        repo_root = Path(__file__).resolve().parents[2]
        tools_dir = repo_root / "tools"
        if str(tools_dir) not in sys.path:
            sys.path.append(str(tools_dir))

        from asset_generator.material_catalog import MaterialDefinition
        from asset_generator.texture_generator import TextureGenerator

        self._material_definition = MaterialDefinition
        self._texture_generator = TextureGenerator(seed=seed)
        self._resolution = RESOLUTIONS.get(resolution, RESOLUTIONS["32x"]).size

    def _material(self, name: str, style: str):
        style_def = STYLES.get(style, STYLES["metallic"])
        color = material_hex_color(name, style_def.color_hint)
        return self._material_definition(name=name, color=color, tier=3, glow=style_def.glow)

    def generate_ore_texture(self, material: str, style: str):
        return self._texture_generator.generate_ore(self._material(material, style), size=self._resolution)

    def generate_ingot_texture(self, material: str, style: str):
        return self._texture_generator.generate_ingot(self._material(material, style), size=self._resolution)

    def generate_tool_texture(self, material: str, style: str, tool_name: str):
        return self._texture_generator.generate_tool(self._material(material, style), tool_name=tool_name, size=self._resolution)

    def generate_armor_texture(self, material: str, style: str, slot: str):
        return self._texture_generator.generate_armor_icon(self._material(material, style), slot=slot, size=self._resolution)

    def generate_machine_texture(self, material: str, style: str):
        return self._texture_generator.generate_machine_casing(self._material(material, style), size=self._resolution)

    def generate_block_texture(self, material: str, style: str):
        if style in {"crystal", "arcane", "void", "quantum"}:
            return self._texture_generator.generate_ore(self._material(material, style), size=self._resolution)
        return self._texture_generator.generate_metal_block(self._material(material, style), size=self._resolution)
