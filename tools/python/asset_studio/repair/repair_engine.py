from __future__ import annotations

import base64
import json
from dataclasses import dataclass, field
from pathlib import Path

try:
    from PIL import Image, ImageDraw

    _PILLOW_AVAILABLE = True
except ModuleNotFoundError:  # pragma: no cover - optional dependency fallback.
    Image = None
    ImageDraw = None
    _PILLOW_AVAILABLE = False

from asset_studio.repair.repair_rules import MISSING_MODEL, MISSING_RECIPE, MISSING_TEXTURE, RepairAction


@dataclass
class RepairReport:
    actions: list[RepairAction] = field(default_factory=list)

    @property
    def total(self) -> int:
        return len(self.actions)


class RepairEngine:
    """Detects and repairs missing textures, models, and recipes."""

    def __init__(self, context) -> None:
        self.context = context

    def repair(self) -> RepairReport:
        actions: list[RepairAction] = []
        actions.extend(self._repair_missing_textures())
        actions.extend(self._repair_missing_models())
        actions.extend(self._repair_missing_recipes())

        for handler in self.context.plugins.asset_repairs.values():
            if callable(handler):
                handler_actions = handler(self.context)
                if isinstance(handler_actions, list):
                    actions.extend(handler_actions)

        return RepairReport(actions=actions)

    def _repair_missing_textures(self) -> list[RepairAction]:
        actions: list[RepairAction] = []
        model_root = self.context.workspace_root / "assets" / "models" / "item"
        texture_root = self.context.workspace_root / "assets" / "textures" / "item"

        for model_file in model_root.glob("*.json"):
            payload = self._load_json(model_file)
            texture_ref = str(payload.get("textures", {}).get("layer0", ""))
            if not texture_ref.startswith("extremecraft:item/"):
                continue
            texture_name = texture_ref.split("/")[-1]
            texture_path = texture_root / f"{texture_name}.png"
            if texture_path.exists():
                continue

            self._write_placeholder_texture(texture_path, texture_name)
            actions.append(RepairAction(MISSING_TEXTURE, texture_path, f"Generated placeholder texture for {texture_name}"))

        return actions

    def _repair_missing_models(self) -> list[RepairAction]:
        actions: list[RepairAction] = []
        texture_root = self.context.workspace_root / "assets" / "textures" / "item"
        model_root = self.context.workspace_root / "assets" / "models" / "item"

        for texture in texture_root.glob("*.png"):
            model_file = model_root / f"{texture.stem}.json"
            if model_file.exists():
                continue
            payload = {
                "parent": "minecraft:item/generated",
                "textures": {"layer0": f"extremecraft:item/{texture.stem}"},
            }
            self.context.write_json(model_file, payload)
            actions.append(RepairAction(MISSING_MODEL, model_file, f"Generated default item model for {texture.stem}"))

        return actions

    def _repair_missing_recipes(self) -> list[RepairAction]:
        actions: list[RepairAction] = []
        texture_root = self.context.workspace_root / "assets" / "textures" / "item"
        recipes_root = self.context.workspace_root / "data" / "recipes"

        for pickaxe in texture_root.glob("*_pickaxe.png"):
            recipe_file = recipes_root / f"{pickaxe.stem}.json"
            if recipe_file.exists():
                continue
            material = pickaxe.stem.removesuffix("_pickaxe")
            payload = {
                "type": "minecraft:crafting_shaped",
                "pattern": ["MMM", " S ", " S "],
                "key": {
                    "M": {"item": f"extremecraft:{material}_ingot"},
                    "S": {"item": "minecraft:stick"},
                },
                "result": {"item": f"extremecraft:{pickaxe.stem}", "count": 1},
            }
            self.context.write_json(recipe_file, payload)
            actions.append(RepairAction(MISSING_RECIPE, recipe_file, f"Generated fallback recipe for {pickaxe.stem}"))

        return actions

    def _write_placeholder_texture(self, texture_path: Path, texture_name: str) -> None:
        texture_path.parent.mkdir(parents=True, exist_ok=True)

        if _PILLOW_AVAILABLE:
            image = Image.new("RGBA", (32, 32), (40, 40, 48, 255))
            draw = ImageDraw.Draw(image)
            draw.rectangle((2, 2, 29, 29), outline=(255, 64, 64, 255), width=2)
            draw.line((2, 2, 29, 29), fill=(255, 64, 64, 255), width=2)
            draw.line((2, 29, 29, 2), fill=(255, 64, 64, 255), width=2)
            draw.rectangle((9, 20, 24, 30), fill=(20, 20, 24, 200))
            draw.text((10, 21), texture_name[:3].upper(), fill=(220, 220, 220, 255))
            image.save(texture_path, format="PNG")
            return

        # 1x1 transparent PNG fallback for environments without Pillow.
        tiny_png = base64.b64decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR4nGNgYAAAAAMAASsJTYQAAAAASUVORK5CYII=")
        texture_path.write_bytes(tiny_png)

    def _load_json(self, path: Path) -> dict:
        if not path.exists():
            return {}
        try:
            return json.loads(path.read_text(encoding="utf-8"))
        except json.JSONDecodeError:
            return {}
