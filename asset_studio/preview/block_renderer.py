from __future__ import annotations

from PyQt6.QtGui import QColor, QPainter, QPen


class BlockRenderer:
    def draw(self, painter: QPainter, width: int, height: int, angle: int) -> None:
        center_x = width // 2
        center_y = height // 2
        size = min(width, height) // 4

        painter.save()
        painter.translate(center_x, center_y)
        painter.rotate(angle)
        painter.setPen(QPen(QColor(246, 209, 118), 2))
        painter.setBrush(QColor(135, 102, 52, 90))
        painter.drawRoundedRect(-size, -size, size * 2, size * 2, 8, 8)
        painter.restore()
