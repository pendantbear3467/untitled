from __future__ import annotations

from PyQt6.QtCore import Qt, pyqtSignal
from PyQt6.QtWidgets import QHBoxLayout, QLabel, QSlider, QWidget


class TimelineWidget(QWidget):
    progress_changed = pyqtSignal(float)

    def __init__(self) -> None:
        super().__init__()
        row = QHBoxLayout(self)
        row.setContentsMargins(0, 0, 0, 0)

        row.addWidget(QLabel("Timeline"))
        self.slider = QSlider(Qt.Orientation.Horizontal)
        self.slider.setRange(0, 1000)
        self.slider.setValue(0)
        self.slider.valueChanged.connect(self._emit_progress)
        row.addWidget(self.slider)

    def set_progress(self, progress: float) -> None:
        self.slider.blockSignals(True)
        self.slider.setValue(int(max(0.0, min(1.0, progress)) * 1000))
        self.slider.blockSignals(False)

    def _emit_progress(self, value: int) -> None:
        self.progress_changed.emit(value / 1000.0)
