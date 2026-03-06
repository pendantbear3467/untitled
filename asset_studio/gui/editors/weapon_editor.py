from __future__ import annotations

from asset_studio.gui.editors.definition_editor_base import DefinitionEditorBase


class WeaponEditor(DefinitionEditorBase):
    def __init__(self, context) -> None:
        super().__init__(
            context=context,
            definition_type="weapon",
            fields=[
                ("id", "mythril_blade"),
                ("material", "mythril"),
                ("attack_damage", "12"),
                ("durability", "1700"),
                ("tier", "5"),
                ("texture_style", "metallic"),
            ],
        )
