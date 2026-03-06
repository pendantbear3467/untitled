from __future__ import annotations

import sys
from dataclasses import dataclass
from pathlib import Path

from PIL import Image, ImageDraw

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
        self._fallback = False
        repo_root = Path(__file__).resolve().parents[2]
        tools_dir = repo_root / "tools"
        if str(tools_dir) not in sys.path:
            sys.path.append(str(tools_dir))

        try:
            from asset_generator.material_catalog import MaterialDefinition
            from asset_generator.texture_generator import TextureGenerator

            self._material_definition = MaterialDefinition
            self._texture_generator = TextureGenerator(seed=seed)
        except ModuleNotFoundError:
            # Keep the application functional without optional heavy deps (numpy).
            self._material_definition = None
            self._texture_generator = None
            self._fallback = True
        self._resolution = RESOLUTIONS.get(resolution, RESOLUTIONS["32x"]).size

    def _material(self, name: str, style: str):
        if self._fallback:
            return None
        style_def = STYLES.get(style, STYLES["metallic"])
        color = material_hex_color(name, style_def.color_hint)
        return self._material_definition(name=name, color=color, tier=3, glow=style_def.glow)

    def _fallback_texture(self, material: str, style: str, icon: str):
        style_def = STYLES.get(style, STYLES["metallic"])
        rgb = material_hex_color(material, style_def.color_hint).lstrip("#")
        color = tuple(int(rgb[i : i + 2], 16) for i in (0, 2, 4))

        image = Image.new("RGBA", (self._resolution, self._resolution), (*color, 255))
        draw = ImageDraw.Draw(image)
        draw.rectangle((1, 1, self._resolution - 2, self._resolution - 2), outline=(20, 20, 20, 180))
        draw.text((4, 4), icon, fill=(255, 255, 255, 210))

        class _Generated:
            def __init__(self, generated_image):
                self.image = generated_image
                self.emissive = None

        return _Generated(image)

    def generate_ore_texture(self, material: str, style: str):
        if self._fallback:
            return self._fallback_texture(material, style, "Ore")
        return self._texture_generator.generate_ore(self._material(material, style), size=self._resolution)

    def generate_ingot_texture(self, material: str, style: str):
        if self._fallback:
            return self._fallback_texture(material, style, "Ing")
        return self._texture_generator.generate_ingot(self._material(material, style), size=self._resolution)

    def generate_tool_texture(self, material: str, style: str, tool_name: str):
        if self._fallback:
            return self._fallback_texture(material, style, "Tool")
        return self._texture_generator.generate_tool(self._material(material, style), tool_name=tool_name, size=self._resolution)

    def generate_armor_texture(self, material: str, style: str, slot: str):
        if self._fallback:
            return self._fallback_texture(material, style, "Arm")
        return self._texture_generator.generate_armor_icon(self._material(material, style), slot=slot, size=self._resolution)

    def generate_machine_texture(self, material: str, style: str):
        if self._fallback:
            return self._fallback_texture(material, style, "Mac")
        return self._texture_generator.generate_machine_casing(self._material(material, style), size=self._resolution)

    def generate_block_texture(self, material: str, style: str):
        if self._fallback:
            return self._fallback_texture(material, style, "Blk")
        if style in {"crystal", "arcane", "void", "quantum"}:
            return self._texture_generator.generate_ore(self._material(material, style), size=self._resolution)
        return self._texture_generator.generate_metal_block(self._material(material, style), size=self._resolution)
