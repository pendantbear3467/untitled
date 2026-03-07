from __future__ import annotations

from asset_studio.gui.editors.definition_editor_base import DefinitionEditorBase


class QuestEditor(DefinitionEditorBase):
    def __init__(self, context) -> None:
        super().__init__(
            context=context,
            definition_type="quest",
            fields=[
                ("id", "first_steps"),
                ("title", "First Steps"),
                ("description", "Craft your first machine"),
            ],
        )
