from __future__ import annotations

from asset_studio.gui.editors.definition_editor_base import DefinitionEditorBase


class SkillTreeEditor(DefinitionEditorBase):
    def __init__(self, context) -> None:
        super().__init__(
            context=context,
            definition_type="skill_tree",
            fields=[
                ("id", "engineering"),
                ("display_name", "Engineering"),
                ("root_node", "starter"),
            ],
        )
