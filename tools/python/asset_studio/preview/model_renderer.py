from __future__ import annotations

from PyQt6.QtGui import QColor, QPainter, QPen


class ModelRenderer:
    def draw(self, painter: QPainter, width: int, height: int, angle: int) -> None:
        center_x = width // 2
        center_y = height // 2
        card = min(width, height) // 3

        painter.save()
        painter.translate(center_x, center_y)
        painter.rotate(angle)
        painter.setPen(QPen(QColor(110, 203, 252), 2))
        painter.setBrush(QColor(40, 100, 132, 80))
        painter.drawRect(-card // 2, -card // 2, card, card)
        painter.restore()
