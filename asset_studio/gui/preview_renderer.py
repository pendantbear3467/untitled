from __future__ import annotations

from PyQt6.QtCore import QTimer
from PyQt6.QtGui import QPainter, QPen
from PyQt6.QtOpenGLWidgets import QOpenGLWidget


class PreviewRenderer(QOpenGLWidget):
    """Simple OpenGL-backed preview widget with animated rotation indicator."""

    def __init__(self) -> None:
        super().__init__()
        self._angle = 0
        self._timer = QTimer(self)
        self._timer.timeout.connect(self._tick)
        self._timer.start(33)

    def _tick(self) -> None:
        self._angle = (self._angle + 3) % 360
        self.update()

    def paintGL(self) -> None:  # noqa: N802
        painter = QPainter(self)
        painter.fillRect(self.rect(), self.palette().window())

        center = self.rect().center()
        radius = min(self.width(), self.height()) // 3
        painter.setPen(QPen(self.palette().highlight().color(), 2))
        painter.drawEllipse(center, radius, radius)

        painter.setPen(QPen(self.palette().text().color(), 2))
        painter.drawText(12, 24, "3D Preview (OpenGL)")
        painter.drawText(12, 44, f"rotation: {self._angle:03d} deg")
        painter.end()
