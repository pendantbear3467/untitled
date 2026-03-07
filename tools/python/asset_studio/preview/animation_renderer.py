from __future__ import annotations

import math

from PyQt6.QtGui import QColor, QPainter, QPen


class AnimationRenderer:
    """Draws a lightweight timeline-driven animation overlay."""

    def draw(self, painter: QPainter, width: int, height: int, progress: float, label: str) -> None:
        painter.save()

        center_x = width // 2
        center_y = height // 2
        radius = int(min(width, height) * 0.22)

        phase = progress * math.tau
        x = center_x + int(math.cos(phase) * radius)
        y = center_y + int(math.sin(phase) * radius)

        painter.setPen(QPen(QColor(110, 190, 255), 3))
        painter.drawEllipse(center_x - radius, center_y - radius, radius * 2, radius * 2)

        painter.setPen(QPen(QColor(255, 210, 110), 4))
        painter.drawLine(center_x, center_y, x, y)

        painter.setPen(QPen(QColor(220, 230, 245), 1))
        painter.drawText(14, height - 18, f"animation: {label} ({progress:.2f})")

        painter.restore()
