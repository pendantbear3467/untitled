from __future__ import annotations

import json
from pathlib import Path

from PyQt6.QtCore import pyqtSignal
from PyQt6.QtWidgets import (
    QComboBox,
    QHBoxLayout,
    QLabel,
    QListWidget,
    QListWidgetItem,
    QPlainTextEdit,
    QPushButton,
    QSplitter,
    QVBoxLayout,
    QWidget,
)

from asset_studio.ai.workbench_service import AIArtifact, AIRequest, AIWorkbenchService


MODES = [
    "explain current selection",
    "generate into new file",
    "apply to current file",
    "generate diff preview",
    "generate model draft",
    "generate GUI draft",
    "generate java class draft",
    "convert preview to runtime definition",
]


class AIWorkbenchPanel(QWidget):
    status_message = pyqtSignal(str)
    notifications = pyqtSignal(str)
    artifact_apply_requested = pyqtSignal(object)

    def __init__(self, service: AIWorkbenchService) -> None:
        super().__init__()
        self.service = service
        self.current_path: Path | None = None
        self.current_text = ""
        self.current_selection = ""
        self.preview_payload: dict | None = None
        self.relationship_context: dict[str, object] = {}
        self.current_artifact: AIArtifact | None = None

        root = QVBoxLayout(self)
        controls = QHBoxLayout()
        self.provider_selector = QComboBox()
        self.provider_selector.addItems(self.service.provider_names())
        self.mode_selector = QComboBox()
        self.mode_selector.addItems(MODES)
        self.generate_button = QPushButton("Generate")
        self.generate_button.clicked.connect(self._generate)
        self.apply_button = QPushButton("Apply")
        self.apply_button.clicked.connect(self._apply)
        self.apply_button.setEnabled(False)
        for label, widget in [("Provider", self.provider_selector), ("Mode", self.mode_selector)]:
            controls.addWidget(QLabel(label))
            controls.addWidget(widget)
        controls.addWidget(self.generate_button)
        controls.addWidget(self.apply_button)
        root.addLayout(controls)

        self.context_label = QLabel("No active editor context")
        self.context_label.setWordWrap(True)
        root.addWidget(self.context_label)

        split = QSplitter()
        left = QWidget()
        left_layout = QVBoxLayout(left)
        left_layout.addWidget(QLabel("Prompt"))
        self.prompt_input = QPlainTextEdit()
        self.prompt_input.setPlaceholderText("Describe the change, draft, or explanation you want.")
        left_layout.addWidget(self.prompt_input)
        left_layout.addWidget(QLabel("History"))
        self.history = QListWidget()
        self.history.itemClicked.connect(self._load_history_item)
        left_layout.addWidget(self.history)
        split.addWidget(left)

        right = QWidget()
        right_layout = QVBoxLayout(right)
        right_layout.addWidget(QLabel("Preview Summary"))
        self.preview_text = QPlainTextEdit()
        self.preview_text.setReadOnly(True)
        right_layout.addWidget(self.preview_text)
        right_layout.addWidget(QLabel("Diff Preview"))
        self.diff_text = QPlainTextEdit()
        self.diff_text.setReadOnly(True)
        right_layout.addWidget(self.diff_text)
        right_layout.addWidget(QLabel("Validation"))
        self.validation_text = QPlainTextEdit()
        self.validation_text.setReadOnly(True)
        right_layout.addWidget(self.validation_text)
        split.addWidget(right)
        split.setSizes([460, 560])
        root.addWidget(split)
        self._refresh_history()

    def set_context(
        self,
        *,
        current_path: Path | None,
        current_text: str,
        selection: str = "",
        preview_payload: dict | None = None,
        relationship_context: dict[str, object] | None = None,
    ) -> None:
        self.current_path = current_path
        self.current_text = current_text
        self.current_selection = selection
        self.preview_payload = preview_payload
        self.relationship_context = dict(relationship_context or {})
        label = str(current_path) if current_path is not None else "unsaved buffer"
        self.context_label.setText(f"Context: {label}\nSelection: {selection or 'entire document'}")

    def _generate(self) -> None:
        request = AIRequest(
            mode=self.mode_selector.currentText(),
            prompt=self.prompt_input.toPlainText(),
            current_text=self.current_text,
            current_path=self.current_path,
            selection=self.current_selection,
            preview_payload=self.preview_payload,
            relationship_context=self.relationship_context,
        )
        self.current_artifact = self.service.generate_artifact(request, provider_name=self.provider_selector.currentText())
        self.preview_text.setPlainText(json.dumps(self.current_artifact.preview_summary, indent=2))
        self.diff_text.setPlainText(self.current_artifact.diff_preview or "No diff preview for this operation.")
        self.validation_text.setPlainText("\n".join(self.current_artifact.validation_messages))
        can_apply = self.current_artifact.apply_kind in {"open-draft", "replace-current"} and self.current_artifact.ready_to_apply
        self.apply_button.setEnabled(can_apply)
        if self.current_artifact.validation_blockers:
            self.notifications.emit("Artifact blocked by validation. Review Validation panel before applying.")
        self._refresh_history()
        self.status_message.emit(f"AI artifact ready: {self.current_artifact.title}")
        self.notifications.emit(f"Generated {self.current_artifact.title} with {self.current_artifact.provider_name}")

    def _apply(self) -> None:
        if self.current_artifact is None:
            return
        if not self.current_artifact.ready_to_apply:
            self.notifications.emit("Cannot apply artifact until validation blockers are resolved")
            return
        self.artifact_apply_requested.emit(self.current_artifact)
        self.notifications.emit(f"Apply requested for {self.current_artifact.title}")

    def _refresh_history(self) -> None:
        self.history.clear()
        for entry in self.service.history:
            item = QListWidgetItem(f"{entry['mode']} :: {entry['title']}")
            item.setData(256, entry)
            self.history.addItem(item)

    def _load_history_item(self, item: QListWidgetItem) -> None:
        entry = item.data(256) or {}
        if entry:
            self.mode_selector.setCurrentText(str(entry.get("mode", MODES[0])))
            self.prompt_input.setPlainText(str(entry.get("prompt", "")))
