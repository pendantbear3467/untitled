from __future__ import annotations

import hashlib

import numpy as np


def stable_seed(seed: int, key: str) -> int:
    digest = hashlib.sha256(f"{seed}:{key}".encode("utf-8")).digest()
    return int.from_bytes(digest[:8], "big")


def value_noise(width: int, height: int, rng: np.random.Generator) -> np.ndarray:
    grid_w = max(2, width // 4)
    grid_h = max(2, height // 4)
    coarse = rng.random((grid_h, grid_w), dtype=np.float32)

    y = np.linspace(0, grid_h - 1, height, dtype=np.float32)
    x = np.linspace(0, grid_w - 1, width, dtype=np.float32)
    xi, yi = np.meshgrid(x, y)

    x0 = np.floor(xi).astype(np.int32)
    y0 = np.floor(yi).astype(np.int32)
    x1 = np.clip(x0 + 1, 0, grid_w - 1)
    y1 = np.clip(y0 + 1, 0, grid_h - 1)

    sx = xi - x0
    sy = yi - y0

    n00 = coarse[y0, x0]
    n10 = coarse[y0, x1]
    n01 = coarse[y1, x0]
    n11 = coarse[y1, x1]

    nx0 = n00 * (1 - sx) + n10 * sx
    nx1 = n01 * (1 - sx) + n11 * sx
    return nx0 * (1 - sy) + nx1 * sy


def fractal_noise(
    width: int,
    height: int,
    seed: int,
    key: str,
    octaves: int = 4,
    persistence: float = 0.5,
) -> np.ndarray:
    rng = np.random.default_rng(stable_seed(seed, key))
    output = np.zeros((height, width), dtype=np.float32)
    amplitude = 1.0
    normalization = 0.0

    for _ in range(max(1, octaves)):
        output += value_noise(width, height, rng) * amplitude
        normalization += amplitude
        amplitude *= persistence

    output /= max(1e-6, normalization)
    return output


def radial_falloff(width: int, height: int) -> np.ndarray:
    y = np.linspace(-1.0, 1.0, height, dtype=np.float32)
    x = np.linspace(-1.0, 1.0, width, dtype=np.float32)
    xx, yy = np.meshgrid(x, y)
    dist = np.sqrt(xx * xx + yy * yy)
    return np.clip(1.0 - dist, 0.0, 1.0)
