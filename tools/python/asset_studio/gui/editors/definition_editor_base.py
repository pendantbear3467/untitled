from __future__ import annotations

from typing import Any

from PyQt6.QtWidgets import (
    QFormLayout,
    QGroupBox,
    QHBoxLayout,
    QLabel,
    QLineEdit,
    QPushButton,
    QVBoxLayout,
    QWidget,
)


class DefinitionEditorBase(QWidget):
    """Reusable editor surface for JSON-backed definitions."""

    def __init__(self, context, definition_type: str, fields: list[tuple[str, str]]) -> None:
        super().__init__()
        self.context = context
        self.definition_type = definition_type
        self.fields = fields
        self.inputs: dict[str, QLineEdit] = {}

        root = QVBoxLayout(self)
        root.addWidget(QLabel(f"{definition_type.replace('_', ' ').title()} Editor"))

        box = QGroupBox("Definition")
        form = QFormLayout(box)
        for key, default in fields:
            line = QLineEdit(default)
            self.inputs[key] = line
            form.addRow(key, line)

        actions = QHBoxLayout()
        self.save_btn = QPushButton("Save Definition")
        self.save_btn.clicked.connect(self.save_definition)
        actions.addWidget(self.save_btn)
        actions.addStretch(1)

        root.addWidget(box)
        root.addLayout(actions)
        root.addStretch(1)

    def payload(self) -> dict[str, Any]:
        result: dict[str, Any] = {
            "type": self.definition_type,
        }
        for key, line in self.inputs.items():
            result[key] = self._coerce(line.text())
        return result

    def save_definition(self) -> None:
        payload = self.payload()
        definition_id = str(payload.get("id", "")).strip()
        if not definition_id:
            return
        target = self.context.workspace_root / "definitions" / self.definition_type / f"{definition_id}.json"
        self.context.write_json(target, payload)

    def _coerce(self, value: str):
        cleaned = value.strip()
        if cleaned.isdigit():
            return int(cleaned)
        if cleaned.lower() in {"true", "false"}:
            return cleaned.lower() == "true"
        try:
            return float(cleaned)
        except ValueError:
            return cleaned
