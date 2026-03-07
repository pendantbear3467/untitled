from __future__ import annotations

from asset_studio.minecraft.model_templates import item_model_template
from asset_studio.workspace.workspace_manager import AssetStudioContext


class ItemGenerator:
    def __init__(self, context: AssetStudioContext) -> None:
        self.context = context

    def write_item_model(self, item_id: str) -> None:
        self.context.write_json(
            self.context.workspace_root / "assets" / "models" / "item" / f"{item_id}.json",
            item_model_template(item_id),
        )
