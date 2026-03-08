from __future__ import annotations

from PyQt6.QtWidgets import QPlainTextEdit


class ConsolePanel(QPlainTextEdit):
    def __init__(self) -> None:
        super().__init__()
        self.setReadOnly(True)
        self.setPlaceholderText("Build and runtime logs appear here.")

    def append_line(self, text: str) -> None:
        self.appendPlainText(text)

    def clear_logs(self) -> None:
        self.clear()
