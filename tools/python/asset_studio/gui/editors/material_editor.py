from __future__ import annotations

from asset_studio.gui.editors.definition_editor_base import DefinitionEditorBase


class MaterialEditor(DefinitionEditorBase):
    def __init__(self, context) -> None:
        super().__init__(
            context=context,
            definition_type="material",
            fields=[
                ("id", "mythril"),
                ("tier", "5"),
                ("durability", "1800"),
                ("mining_speed", "12"),
                ("enchantability", "25"),
                ("color", "#5ec7ff"),
                ("texture_style", "metallic"),
            ],
        )
