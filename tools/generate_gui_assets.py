#!/usr/bin/env python3
"""Generate placeholder GUI textures for the ExtremeCraft RPG interface."""

from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path

from PIL import Image, ImageDraw

MODID = "extremecraft"
REPO_ROOT = Path(__file__).resolve().parents[1]
GUI_TEXTURE_DIR = REPO_ROOT / "src" / "main" / "resources" / "assets" / MODID / "textures" / "gui"

ICON_PATH = GUI_TEXTURE_DIR / "extremecraft_icon.png"
MENU_PATH = GUI_TEXTURE_DIR / "extreme_player_menu.png"
SLOT_PATH = GUI_TEXTURE_DIR / "magic_slot.png"


@dataclass
class GuiGenerationResult:
    generated: int
    missing: int


def _clamp(value: int) -> int:
    return max(0, min(255, value))


def _lerp_color(a: tuple[int, int, int], b: tuple[int, int, int], t: float) -> tuple[int, int, int]:
    return (
        int(a[0] + (b[0] - a[0]) * t),
        int(a[1] + (b[1] - a[1]) * t),
        int(a[2] + (b[2] - a[2]) * t),
    )


def create_panel_texture(width: int, height: int) -> Image.Image:
    """Create a dark steel panel with rune corners and a neutral center area."""
    image = Image.new("RGBA", (width, height), (0, 0, 0, 0))
    pixels = image.load()

    top = (44, 49, 57)
    bottom = (26, 30, 36)
    for y in range(height):
        yt = y / max(1, height - 1)
        row_color = _lerp_color(top, bottom, yt)
        for x in range(width):
            xt = x / max(1, width - 1)
            edge_fade = abs(xt - 0.5) * 2.0
            shade = int(10 * edge_fade)
            pixels[x, y] = (
                _clamp(row_color[0] - shade),
                _clamp(row_color[1] - shade),
                _clamp(row_color[2] - shade),
                255,
            )

    draw = ImageDraw.Draw(image)

    # Soft steel frame.
    draw.rectangle((0, 0, width - 1, height - 1), outline=(16, 19, 24, 255))
    draw.rectangle((1, 1, width - 2, height - 2), outline=(66, 73, 86, 190))
    draw.rectangle((2, 2, width - 3, height - 3), outline=(25, 30, 37, 255))

    # Neutral center keeps text and overlays readable.
    margin_x = 26
    margin_y = 20
    for y in range(margin_y, height - margin_y):
        neutral_t = (y - margin_y) / max(1, (height - margin_y) - margin_y)
        neutral = _lerp_color((48, 53, 61), (40, 45, 52), neutral_t)
        for x in range(margin_x, width - margin_x):
            pixels[x, y] = (neutral[0], neutral[1], neutral[2], 255)

    draw.rectangle(
        (margin_x - 1, margin_y - 1, width - margin_x, height - margin_y),
        outline=(85, 94, 108, 75),
    )

    # Subtle rune-like corner ornaments.
    rune = (92, 155, 196, 105)
    rune_shadow = (25, 45, 60, 100)
    corner_size = 18
    offset = 8
    corners = [
        (offset, offset, 1, 1),
        (width - offset - corner_size, offset, -1, 1),
        (offset, height - offset - corner_size, 1, -1),
        (width - offset - corner_size, height - offset - corner_size, -1, -1),
    ]
    for cx, cy, sx, sy in corners:
        for i in range(0, corner_size, 3):
            x0 = cx + (i if sx > 0 else corner_size - i)
            y0 = cy + (0 if sy > 0 else corner_size)
            x1 = cx + (0 if sx > 0 else corner_size)
            y1 = cy + (i if sy > 0 else corner_size - i)
            draw.line((x0, y0, x1, y1), fill=rune_shadow, width=2)
            draw.line((x0 - sx, y0 - sy, x1 - sx, y1 - sy), fill=rune, width=1)

    return image


def create_icon_texture(size: int) -> Image.Image:
    """Create a metallic rune icon with a glowing blue core."""
    image = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    pixels = image.load()
    cx = size // 2
    cy = size // 2

    for y in range(size):
        for x in range(size):
            dx = x - cx
            dy = y - cy
            dist2 = dx * dx + dy * dy

            if dist2 <= 90:
                ring = abs(dist2 - 60)
                metallic = _clamp(148 - ring // 2)
                pixels[x, y] = (metallic, metallic + 8, metallic + 14, 255)

            if dist2 <= 28:
                glow = _clamp(220 - dist2 * 5)
                pixels[x, y] = (
                    _clamp(35 + glow // 7),
                    _clamp(95 + glow // 5),
                    _clamp(175 + glow // 3),
                    255,
                )

    draw = ImageDraw.Draw(image)
    # Angular rune shape over the core.
    rune = [(cx, 4), (cx + 4, 8), (cx + 1, 11), (cx + 5, 15), (cx, 17), (cx - 5, 15), (cx - 1, 11), (cx - 4, 8)]
    draw.polygon(rune, fill=(174, 214, 255, 220), outline=(220, 240, 255, 255))

    draw.rectangle((1, 1, size - 2, size - 2), outline=(26, 34, 45, 160))
    draw.rectangle((0, 0, size - 1, size - 1), outline=(12, 17, 24, 230))

    return image


def _draw_slot_frame(width: int, height: int, pulse: float) -> Image.Image:
    frame = Image.new("RGBA", (width, height), (0, 0, 0, 0))
    pixels = frame.load()

    top = (53, 59, 66)
    bottom = (34, 38, 44)
    for y in range(height):
        yt = y / max(1, height - 1)
        color = _lerp_color(top, bottom, yt)
        for x in range(width):
            pixels[x, y] = (color[0], color[1], color[2], 255)

    draw = ImageDraw.Draw(frame)
    draw.rectangle((0, 0, width - 1, height - 1), outline=(15, 18, 22, 255))
    draw.rectangle((1, 1, width - 2, height - 2), outline=(85, 94, 108, 160))

    cx = width // 2
    cy = height // 2
    glow_base = int(145 * pulse)

    for radius in (7, 5, 3):
        alpha = int(120 * pulse) if radius == 7 else int(170 * pulse)
        color = (
            _clamp(25 + glow_base // 5),
            _clamp(80 + glow_base // 3),
            _clamp(150 + glow_base // 2),
            alpha,
        )
        draw.ellipse((cx - radius, cy - radius, cx + radius, cy + radius), outline=color)

    rune_points = [
        (cx, cy - 6),
        (cx + 3, cy - 1),
        (cx + 1, cy + 1),
        (cx + 4, cy + 5),
        (cx, cy + 6),
        (cx - 4, cy + 5),
        (cx - 1, cy + 1),
        (cx - 3, cy - 1),
    ]
    draw.polygon(
        rune_points,
        fill=(
            _clamp(100 + int(60 * pulse)),
            _clamp(170 + int(65 * pulse)),
            _clamp(240 + int(10 * pulse)),
            220,
        ),
        outline=(220, 238, 255, 210),
    )

    return frame


def create_animated_slot(width: int, height: int, frames: int) -> Image.Image:
    """Create a horizontal sprite strip with pulsing rune frames."""
    strip = Image.new("RGBA", (width * frames, height), (0, 0, 0, 0))
    pulse_values = [0.80, 0.95, 1.10, 0.95]

    for frame_index in range(frames):
        pulse = pulse_values[frame_index % len(pulse_values)]
        frame = _draw_slot_frame(width, height, pulse)
        strip.paste(frame, (frame_index * width, 0))

    return strip


def _save_if_allowed(path: Path, image: Image.Image, force: bool) -> bool:
    if path.exists() and not force:
        return False
    path.parent.mkdir(parents=True, exist_ok=True)
    image.save(path, format="PNG")
    return True


def generate_gui_assets(force: bool = False) -> GuiGenerationResult:
    """Generate all required GUI textures and return generation stats."""
    GUI_TEXTURE_DIR.mkdir(parents=True, exist_ok=True)

    generated = 0
    generated += int(_save_if_allowed(ICON_PATH, create_icon_texture(20), force))
    generated += int(_save_if_allowed(MENU_PATH, create_panel_texture(306, 206), force))
    generated += int(_save_if_allowed(SLOT_PATH, create_animated_slot(22, 22, 4), force))

    required = (ICON_PATH, MENU_PATH, SLOT_PATH)
    missing = sum(1 for path in required if not path.exists())

    return GuiGenerationResult(generated=generated, missing=missing)


def main() -> None:
    result = generate_gui_assets(force=False)
    print(f"GUI textures generated: {result.generated}")
    print(f"Missing files: {result.missing}")


if __name__ == "__main__":
    main()
