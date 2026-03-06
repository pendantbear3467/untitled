from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path

from PIL import Image, ImageDraw

from .color_palettes import clamp


@dataclass
class GuiResult:
    generated: int


class GuiGenerator:
    def __init__(self, gui_dir: Path, dry_run: bool = False, force: bool = False) -> None:
        self.gui_dir = gui_dir
        self.dry_run = dry_run
        self.force = force

    def _save(self, path: Path, image: Image.Image) -> bool:
        if path.exists() and not self.force:
            return False
        if self.dry_run:
            return True
        path.parent.mkdir(parents=True, exist_ok=True)
        image.save(path, format="PNG")
        return True

    def _panel(self, width: int, height: int) -> Image.Image:
        image = Image.new("RGBA", (width, height), (22, 28, 36, 255))
        pixels = image.load()
        for y in range(height):
            shade = int(26 * (y / max(1, height - 1)))
            for x in range(width):
                edge = abs((x / max(1, width - 1)) - 0.5)
                vignette = int(edge * 30)
                pixels[x, y] = (clamp(44 - vignette + shade), clamp(52 - vignette + shade), clamp(64 - vignette + shade), 255)

        draw = ImageDraw.Draw(image)
        draw.rectangle((0, 0, width - 1, height - 1), outline=(15, 19, 24, 255))
        draw.rectangle((2, 2, width - 3, height - 3), outline=(96, 110, 124, 150))
        return image

    def _icon(self, size: int) -> Image.Image:
        image = Image.new("RGBA", (size, size), (0, 0, 0, 0))
        draw = ImageDraw.Draw(image)
        draw.ellipse((1, 1, size - 2, size - 2), fill=(90, 112, 132, 255), outline=(180, 220, 248, 200))
        draw.polygon([(size // 2, 3), (size - 4, size // 2), (size // 2, size - 3), (3, size // 2)], fill=(130, 190, 250, 220))
        return image

    def _animated_slot(self, width: int, height: int, frames: int) -> Image.Image:
        strip = Image.new("RGBA", (width * frames, height), (0, 0, 0, 0))
        for frame in range(frames):
            phase = frame / max(1, frames - 1)
            brightness = 0.8 + phase * 0.4
            tile = Image.new("RGBA", (width, height), (36, 42, 50, 255))
            draw = ImageDraw.Draw(tile)
            draw.rectangle((0, 0, width - 1, height - 1), outline=(22, 26, 30, 255))
            draw.rectangle((1, 1, width - 2, height - 2), outline=(80, 96, 112, 160))
            glow = (clamp(45 * brightness), clamp(130 * brightness), clamp(220 * brightness), 210)
            draw.ellipse((5, 5, width - 6, height - 6), outline=glow)
            strip.paste(tile, (frame * width, 0))
        return strip

    def generate(self) -> GuiResult:
        generated = 0
        generated += int(self._save(self.gui_dir / "extreme_player_menu.png", self._panel(306, 206)))
        generated += int(self._save(self.gui_dir / "extremecraft_icon.png", self._icon(20)))
        generated += int(self._save(self.gui_dir / "magic_slot.png", self._animated_slot(22, 22, 4)))
        return GuiResult(generated=generated)
