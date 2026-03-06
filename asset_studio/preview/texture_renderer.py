from __future__ import annotations

from PyQt6.QtGui import QPainter, QPixmap


class TextureRenderer:
    def draw(self, painter: QPainter, pixmap: QPixmap | None, x: int, y: int, size: int) -> None:
        if pixmap is None:
            return
        painter.drawPixmap(x, y, size, size, pixmap)
