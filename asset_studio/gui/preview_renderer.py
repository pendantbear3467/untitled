from __future__ import annotations

from pathlib import Path

from PyQt6.QtCore import QTimer
from PyQt6.QtGui import QColor, QImage, QPainter, QPen, QPixmap
from PyQt6.QtOpenGLWidgets import QOpenGLWidget


class PreviewRenderer(QOpenGLWidget):
    """OpenGL-backed preview renderer for textures and pseudo-3D model cards."""

    def __init__(self) -> None:
        super().__init__()
        self._angle = 0
        self._mode = "texture"
        self._texture_path: Path | None = None
        self._pixmap: QPixmap | None = None

        self._timer = QTimer(self)
        self._timer.timeout.connect(self._tick)
        self._timer.start(33)

    def set_mode(self, mode: str) -> None:
        if mode not in {"texture", "item", "block", "animated"}:
            return
        self._mode = mode
        self.update()

    def load_texture(self, texture_path: Path) -> None:
        self._texture_path = texture_path
        if not texture_path.exists():
            self._pixmap = None
            self.update()
            return

        image = QImage(str(texture_path))
        if image.isNull():
            self._pixmap = None
        else:
            self._pixmap = QPixmap.fromImage(image)
        self.update()

    def _tick(self) -> None:
        self._angle = (self._angle + 3) % 360
        self.update()

    def paintGL(self) -> None:  # noqa: N802
        painter = QPainter(self)
        painter.fillRect(self.rect(), QColor(18, 21, 28))

        self._draw_grid(painter)
        self._draw_model_card(painter)
        self._draw_texture_layer(painter)
        self._draw_hud(painter)

        painter.end()

    def _draw_grid(self, painter: QPainter) -> None:
        painter.save()
        pen = QPen(QColor(44, 50, 63), 1)
        painter.setPen(pen)
        step = 24
        for x in range(0, self.width(), step):
            painter.drawLine(x, 0, x, self.height())
        for y in range(0, self.height(), step):
            painter.drawLine(0, y, self.width(), y)
        painter.restore()

    def _draw_model_card(self, painter: QPainter) -> None:
        center = self.rect().center()
        base = min(self.width(), self.height()) // 3
        wobble = 8 if self._mode == "animated" else 3
        scale = 0.88 + (wobble / 100.0)
        card_w = int(base * scale)
        card_h = int(base * scale)

        painter.save()
        painter.translate(center)
        painter.rotate(self._angle)

        if self._mode == "block":
            painter.setPen(QPen(QColor(110, 203, 252), 2))
            painter.setBrush(QColor(40, 100, 132, 80))
            painter.drawRect(-card_w // 2, -card_h // 2, card_w, card_h)
        else:
            painter.setPen(QPen(QColor(246, 209, 118), 2))
            painter.setBrush(QColor(135, 102, 52, 90))
            painter.drawRoundedRect(-card_w // 2, -card_h // 2, card_w, card_h, 8, 8)
        painter.restore()

    def _draw_texture_layer(self, painter: QPainter) -> None:
        if self._pixmap is None:
            return

        tex_size = min(self.width(), self.height()) // 2
        px = self.rect().center().x() - tex_size // 2
        py = self.rect().center().y() - tex_size // 2

        painter.save()
        if self._mode in {"item", "animated"}:
            painter.translate(self.rect().center())
            painter.rotate(-self._angle)
            painter.translate(-self.rect().center())

        painter.drawPixmap(px, py, tex_size, tex_size, self._pixmap)
        painter.restore()

    def _draw_hud(self, painter: QPainter) -> None:
        painter.save()
        painter.setPen(QPen(QColor(225, 232, 245), 1))
        painter.drawText(12, 24, "Preview Renderer")
        painter.drawText(12, 44, f"mode: {self._mode}")
        painter.drawText(12, 64, f"rotation: {self._angle:03d} deg")
        label = str(self._texture_path) if self._texture_path else "no texture loaded"
        if len(label) > 72:
            label = "..." + label[-69:]
        painter.drawText(12, 84, label)
        painter.restore()
