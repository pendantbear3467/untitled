from __future__ import annotations

from PyQt6.QtWidgets import QPlainTextEdit


class ConsolePanel(QPlainTextEdit):
    def __init__(self) -> None:
        super().__init__()
        self.setReadOnly(True)

    def append_line(self, text: str) -> None:
        self.appendPlainText(text)
