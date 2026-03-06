from __future__ import annotations

from pathlib import Path

from PyQt6.QtCore import QTimer
from PyQt6.QtGui import QColor, QImage, QPainter, QPen, QPixmap
from PyQt6.QtOpenGLWidgets import QOpenGLWidget

from asset_studio.preview.block_renderer import BlockRenderer
from asset_studio.preview.model_renderer import ModelRenderer
from asset_studio.preview.texture_renderer import TextureRenderer


class PreviewRenderer(QOpenGLWidget):
    """OpenGL-backed preview renderer for textures and pseudo-3D model cards."""

    def __init__(self) -> None:
        super().__init__()
        self._angle = 0
        self._mode = "texture"
        self._texture_path: Path | None = None
        self._pixmap: QPixmap | None = None
        self._zoom = 1.0
        self._lighting = 1.0
        self._rotation_speed = 3
        self._texture_pool: list[Path] = []
        self._texture_index = -1

        self._texture_renderer = TextureRenderer()
        self._model_renderer = ModelRenderer()
        self._block_renderer = BlockRenderer()

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

    def set_zoom(self, zoom: float) -> None:
        self._zoom = max(0.2, min(4.0, zoom))
        self.update()

    def set_lighting(self, lighting: float) -> None:
        self._lighting = max(0.2, min(2.0, lighting))
        self.update()

    def set_rotation_speed(self, speed: int) -> None:
        self._rotation_speed = max(0, min(20, speed))

    def set_texture_candidates(self, paths: list[Path]) -> None:
        self._texture_pool = [path for path in paths if path.exists()]
        self._texture_index = 0 if self._texture_pool else -1
        if self._texture_index >= 0:
            self.load_texture(self._texture_pool[self._texture_index])

    def switch_texture(self, step: int = 1) -> None:
        if not self._texture_pool:
            return
        self._texture_index = (self._texture_index + step) % len(self._texture_pool)
        self.load_texture(self._texture_pool[self._texture_index])

    def _tick(self) -> None:
        self._angle = (self._angle + self._rotation_speed) % 360
        self.update()

    def wheelEvent(self, event) -> None:  # noqa: N802
        delta = event.angleDelta().y()
        factor = 1.1 if delta > 0 else 0.9
        self.set_zoom(self._zoom * factor)
        event.accept()

    def paintGL(self) -> None:  # noqa: N802
        painter = QPainter(self)
        base = int(18 * self._lighting)
        painter.fillRect(self.rect(), QColor(min(255, base), min(255, base + 3), min(255, base + 10)))

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
        base = int(min(self.width(), self.height()) // 3 * self._zoom)
        wobble = 8 if self._mode == "animated" else 3
        scale = 0.88 + (wobble / 100.0)
        _ = scale

        if self._mode == "block":
            self._model_renderer.draw(painter, self.width(), self.height(), self._angle)
        else:
            self._block_renderer.draw(painter, self.width(), self.height(), self._angle)

    def _draw_texture_layer(self, painter: QPainter) -> None:
        if self._pixmap is None:
            return

        tex_size = int(min(self.width(), self.height()) // 2 * self._zoom)
        px = self.rect().center().x() - tex_size // 2
        py = self.rect().center().y() - tex_size // 2

        painter.save()
        if self._mode in {"item", "animated"}:
            painter.translate(self.rect().center())
            painter.rotate(-self._angle)
            painter.translate(-self.rect().center())

        self._texture_renderer.draw(painter, self._pixmap, px, py, tex_size)
        painter.restore()

    def _draw_hud(self, painter: QPainter) -> None:
        painter.save()
        painter.setPen(QPen(QColor(225, 232, 245), 1))
        painter.drawText(12, 24, "Preview Renderer")
        painter.drawText(12, 44, f"mode: {self._mode}")
        painter.drawText(12, 64, f"rotation: {self._angle:03d} deg")
        painter.drawText(12, 84, f"zoom: {self._zoom:.2f}  light: {self._lighting:.2f}")
        label = str(self._texture_path) if self._texture_path else "no texture loaded"
        if len(label) > 72:
            label = "..." + label[-69:]
        painter.drawText(12, 104, label)
        painter.restore()
