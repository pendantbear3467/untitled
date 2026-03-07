from __future__ import annotations

from asset_studio.gui.editors.definition_editor_base import DefinitionEditorBase


class WorldgenEditor(DefinitionEditorBase):
    def __init__(self, context) -> None:
        super().__init__(
            context=context,
            definition_type="worldgen",
            fields=[
                ("id", "mythril"),
                ("vein_size", "7"),
                ("min_y", "-48"),
                ("max_y", "32"),
            ],
        )
