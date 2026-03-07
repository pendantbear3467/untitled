from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path


@dataclass
class RepairAction:
    category: str
    path: Path
    message: str


MISSING_TEXTURE = "missing_texture"
MISSING_MODEL = "missing_model"
MISSING_RECIPE = "missing_recipe"
