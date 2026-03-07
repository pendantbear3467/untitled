from __future__ import annotations

from dataclasses import dataclass


@dataclass(frozen=True)
class MaterialStyle:
    name: str
    color_hint: str
    glow: bool


STYLES = {
    "metallic": MaterialStyle("metallic", "#9aa9b5", False),
    "industrial": MaterialStyle("industrial", "#7f8d96", False),
    "mechanical": MaterialStyle("mechanical", "#5f6c76", False),
    "ancient": MaterialStyle("ancient", "#9f7d44", False),
    "arcane": MaterialStyle("arcane", "#4d7dff", True),
    "crystal": MaterialStyle("crystal", "#70f0ff", True),
    "crystalline": MaterialStyle("crystalline", "#83fff2", True),
    "void": MaterialStyle("void", "#373a52", True),
    "quantum": MaterialStyle("quantum", "#31f5c8", True),
}
