from __future__ import annotations

import hashlib
from dataclasses import dataclass

import numpy as np
from PIL import Image, ImageDraw

from .color_palettes import RgbColor, clamp, hex_to_rgb, lerp, scale, shift
from .material_catalog import MaterialDefinition
from .noise_algorithms import fractal_noise, radial_falloff, stable_seed


@dataclass(frozen=True)
class GeneratedTexture:
    image: Image.Image
    emissive: Image.Image | None = None


class TextureGenerator:
    def __init__(self, seed: int) -> None:
        self.seed = seed

    def _rng(self, key: str) -> np.random.Generator:
        return np.random.default_rng(stable_seed(self.seed, key))

    def _to_rgba(self, rgb: np.ndarray) -> np.ndarray:
        alpha = np.full((rgb.shape[0], rgb.shape[1], 1), 255, dtype=np.uint8)
        return np.concatenate([rgb.astype(np.uint8), alpha], axis=2)

    def _gradient(self, base: RgbColor, size: int, key: str) -> np.ndarray:
        top = scale(base, 1.20)
        bot = scale(base, 0.80)
        rng = self._rng(key)
        n = fractal_noise(size, size, self.seed, key=f"{key}:grad", octaves=3)

        y = np.linspace(0.0, 1.0, size, dtype=np.float32)
        yy = np.repeat(y[:, None], size, axis=1)
        mixed = yy * 0.85 + n * 0.15

        r = top.r + (bot.r - top.r) * mixed
        g = top.g + (bot.g - top.g) * mixed
        b = top.b + (bot.b - top.b) * mixed

        jitter = rng.normal(loc=0.0, scale=5.0, size=(size, size, 3))
        rgb = np.stack([r, g, b], axis=2) + jitter
        return np.clip(rgb, 0, 255).astype(np.uint8)

    def generate_ore(self, material: MaterialDefinition, size: int = 32) -> GeneratedTexture:
        base = hex_to_rgb(material.color)
        stone = RgbColor(96, 98, 102)
        stone_rgb = self._gradient(stone, size, key=f"{material.name}:stone")

        veins = fractal_noise(size, size, self.seed, key=f"{material.name}:veins", octaves=5)
        cracks = fractal_noise(size, size, self.seed, key=f"{material.name}:cracks", octaves=4)
        crystal = fractal_noise(size, size, self.seed, key=f"{material.name}:crystal", octaves=6)

        ore_mask = (veins > 0.58) | ((cracks > 0.64) & (crystal > 0.50))
        ore_rgb = self._gradient(base, size, key=f"{material.name}:ore_core")
        highlight = self._gradient(shift(base, 35), size, key=f"{material.name}:ore_highlight")

        blend = np.where((crystal > 0.7)[:, :, None], highlight, ore_rgb)
        final_rgb = np.where(ore_mask[:, :, None], blend, stone_rgb)

        image = Image.fromarray(self._to_rgba(final_rgb), mode="RGBA")

        emissive = None
        if material.glow:
            glow_intensity = np.clip((crystal - 0.72) * 4.0, 0.0, 1.0)
            glow = np.zeros((size, size, 4), dtype=np.uint8)
            glow[:, :, 0] = (base.r * glow_intensity).astype(np.uint8)
            glow[:, :, 1] = (base.g * glow_intensity).astype(np.uint8)
            glow[:, :, 2] = (base.b * glow_intensity).astype(np.uint8)
            glow[:, :, 3] = (255 * glow_intensity).astype(np.uint8)
            emissive = Image.fromarray(glow, mode="RGBA")

        return GeneratedTexture(image=image, emissive=emissive)

    def generate_ingot(self, material: MaterialDefinition, size: int = 32) -> GeneratedTexture:
        base = hex_to_rgb(material.color)
        image = Image.new("RGBA", (size, size), (0, 0, 0, 0))
        draw = ImageDraw.Draw(image)

        fill = shift(base, 8).as_tuple()
        outline = shift(base, -45).as_tuple()
        draw.rounded_rectangle((4, 11, 27, 21), radius=3, fill=(*fill, 255), outline=(*outline, 255), width=1)

        for i in range(4, 27):
            draw.point((i, 12), fill=(*shift(base, 30).as_tuple(), 255))

        return GeneratedTexture(image=image)

    def generate_nugget(self, material: MaterialDefinition, size: int = 32) -> GeneratedTexture:
        base = hex_to_rgb(material.color)
        image = Image.new("RGBA", (size, size), (0, 0, 0, 0))
        draw = ImageDraw.Draw(image)
        rng = self._rng(f"{material.name}:nugget")

        points = []
        for _ in range(9):
            points.append((int(rng.integers(8, 24)), int(rng.integers(8, 24))))
        draw.polygon(points, fill=(*base.as_tuple(), 255), outline=(*shift(base, -35).as_tuple(), 255))
        return GeneratedTexture(image=image)

    def generate_metal_block(self, material: MaterialDefinition, size: int = 32) -> GeneratedTexture:
        base = hex_to_rgb(material.color)
        rgb = self._gradient(base, size, key=f"{material.name}:metal_block")
        falloff = radial_falloff(size, size)

        rgb[:, :, 0] = np.clip(rgb[:, :, 0] + 24 * falloff, 0, 255)
        rgb[:, :, 1] = np.clip(rgb[:, :, 1] + 24 * falloff, 0, 255)
        rgb[:, :, 2] = np.clip(rgb[:, :, 2] + 24 * falloff, 0, 255)

        for i in range(0, size, 8):
            rgb[i : i + 1, :, :] = np.clip(rgb[i : i + 1, :, :] * 0.80, 0, 255)
            rgb[:, i : i + 1, :] = np.clip(rgb[:, i : i + 1, :] * 0.80, 0, 255)

        return GeneratedTexture(image=Image.fromarray(self._to_rgba(rgb), mode="RGBA"))

    def generate_machine_casing(self, material: MaterialDefinition, size: int = 32) -> GeneratedTexture:
        base = hex_to_rgb(material.color)
        steel = lerp(base, RgbColor(110, 112, 118), 0.65)
        rgb = self._gradient(steel, size, key=f"{material.name}:machine")

        image = Image.fromarray(self._to_rgba(rgb), mode="RGBA")
        draw = ImageDraw.Draw(image)
        draw.rectangle((1, 1, size - 2, size - 2), outline=(35, 37, 42, 255))
        draw.rectangle((4, 4, size - 5, size - 5), outline=(*shift(base, 20).as_tuple(), 200))
        draw.rectangle((8, 8, size - 9, size - 9), outline=(*shift(base, -20).as_tuple(), 140))
        return GeneratedTexture(image=image)

    def generate_tool(self, material: MaterialDefinition, tool_name: str, size: int = 32) -> GeneratedTexture:
        base = hex_to_rgb(material.color)
        image = Image.new("RGBA", (size, size), (0, 0, 0, 0))
        draw = ImageDraw.Draw(image)

        handle_color = (92, 66, 43, 255)
        head_color = (*shift(base, 12).as_tuple(), 255)
        edge_color = (*shift(base, 36).as_tuple(), 255)

        if tool_name.endswith("_sword"):
            draw.rectangle((14, 6, 17, 26), fill=head_color)
            draw.rectangle((13, 24, 18, 27), fill=(130, 102, 75, 255))
            draw.rectangle((14, 27, 17, 30), fill=handle_color)
            draw.line((14, 6, 17, 6), fill=edge_color)
        else:
            draw.rectangle((14, 12, 17, 29), fill=handle_color)
            draw.rectangle((10, 8, 21, 13), fill=head_color)
            draw.line((10, 8, 21, 8), fill=edge_color)

        return GeneratedTexture(image=image)

    def generate_armor_icon(self, material: MaterialDefinition, slot: str, size: int = 32) -> GeneratedTexture:
        base = hex_to_rgb(material.color)
        image = Image.new("RGBA", (size, size), (0, 0, 0, 0))
        draw = ImageDraw.Draw(image)

        if slot == "helmet":
            draw.pieslice((6, 5, 25, 23), start=180, end=360, fill=(*base.as_tuple(), 255), outline=(*shift(base, -35).as_tuple(), 255))
            draw.rectangle((9, 14, 22, 21), fill=(*base.as_tuple(), 255))
        elif slot == "chestplate":
            draw.rectangle((8, 6, 23, 25), fill=(*base.as_tuple(), 255), outline=(*shift(base, -35).as_tuple(), 255))
            draw.rectangle((11, 10, 20, 15), fill=(*shift(base, 22).as_tuple(), 255))
        elif slot == "leggings":
            draw.rectangle((10, 7, 21, 16), fill=(*base.as_tuple(), 255), outline=(*shift(base, -35).as_tuple(), 255))
            draw.rectangle((10, 16, 14, 27), fill=(*base.as_tuple(), 255))
            draw.rectangle((17, 16, 21, 27), fill=(*base.as_tuple(), 255))
        else:
            draw.rectangle((10, 10, 21, 22), fill=(*base.as_tuple(), 255), outline=(*shift(base, -35).as_tuple(), 255))

        return GeneratedTexture(image=image)

    def generate_damage_overlay(self, tool_name: str, size: int = 32) -> GeneratedTexture:
        image = Image.new("RGBA", (size, size), (0, 0, 0, 0))
        draw = ImageDraw.Draw(image)
        seed = int(hashlib.sha256(f"{self.seed}:{tool_name}:damage".encode("utf-8")).hexdigest()[:8], 16)
        rng = np.random.default_rng(seed)
        for _ in range(20):
            x0 = int(rng.integers(0, size - 4))
            y0 = int(rng.integers(0, size - 4))
            x1 = min(size - 1, x0 + int(rng.integers(2, 6)))
            y1 = min(size - 1, y0 + int(rng.integers(1, 3)))
            draw.line((x0, y0, x1, y1), fill=(36, 16, 12, 120), width=1)
        return GeneratedTexture(image=image)
