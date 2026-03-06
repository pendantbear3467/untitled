from __future__ import annotations

from asset_studio.gui.editors.definition_editor_base import DefinitionEditorBase


class MachineEditor(DefinitionEditorBase):
    def __init__(self, context) -> None:
        super().__init__(
            context=context,
            definition_type="machine",
            fields=[
                ("id", "mythril_crusher"),
                ("material", "mythril"),
                ("texture_style", "industrial"),
                ("power_per_tick", "120"),
            ],
        )
