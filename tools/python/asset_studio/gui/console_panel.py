from __future__ import annotations

from PyQt6.QtWidgets import QPlainTextEdit


class ConsolePanel(QPlainTextEdit):
    def __init__(self) -> None:
        super().__init__()
        self.setReadOnly(True)
        self.setLineWrapMode(QPlainTextEdit.LineWrapMode.NoWrap)
        self.setMaximumBlockCount(5000)
        self.setPlaceholderText("Logs stream here while builds, validation, and runtime tasks execute.")

    def append_line(self, text: str) -> None:
        self.appendPlainText(text)

    def clear_logs(self) -> None:
        self.clear()
