from __future__ import annotations

from dataclasses import dataclass


@dataclass(frozen=True)
class RgbColor:
    r: int
    g: int
    b: int

    def as_tuple(self) -> tuple[int, int, int]:
        return (self.r, self.g, self.b)


def clamp(value: float) -> int:
    return max(0, min(255, int(value)))


def hex_to_rgb(value: str) -> RgbColor:
    normalized = value.strip().lstrip("#")
    if len(normalized) != 6:
        raise ValueError(f"Expected a 6-digit hex color, got: {value}")
    return RgbColor(
        int(normalized[0:2], 16),
        int(normalized[2:4], 16),
        int(normalized[4:6], 16),
    )


def shift(color: RgbColor, amount: int) -> RgbColor:
    return RgbColor(
        clamp(color.r + amount),
        clamp(color.g + amount),
        clamp(color.b + amount),
    )


def scale(color: RgbColor, factor: float) -> RgbColor:
    return RgbColor(
        clamp(color.r * factor),
        clamp(color.g * factor),
        clamp(color.b * factor),
    )


def lerp(a: RgbColor, b: RgbColor, t: float) -> RgbColor:
    return RgbColor(
        clamp(a.r + (b.r - a.r) * t),
        clamp(a.g + (b.g - a.g) * t),
        clamp(a.b + (b.b - a.b) * t),
    )
