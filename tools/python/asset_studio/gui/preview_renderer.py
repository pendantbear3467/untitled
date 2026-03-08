from __future__ import annotations

import math
from pathlib import Path

from PyQt6.QtCore import QPoint, QPointF, QTimer, Qt
from PyQt6.QtGui import QColor, QImage, QPainter, QPen, QPixmap
from PyQt6.QtOpenGLWidgets import QOpenGLWidget

from asset_studio.preview.animation_player import AnimationPlayer
from asset_studio.preview.animation_renderer import AnimationRenderer
from asset_studio.preview.block_renderer import BlockRenderer
from asset_studio.preview.model_renderer import ModelRenderer
from asset_studio.preview.texture_renderer import TextureRenderer


MODE_ALIASES = {
    "texture": "texture",
    "asset": "texture",
    "item": "source_model",
    "block": "runtime_model",
    "animated": "runtime_model",
    "source_model": "source_model",
    "runtime_model": "runtime_model",
    "source_gui": "source_gui",
    "runtime_gui": "runtime_gui",
}


class PreviewRenderer(QOpenGLWidget):
    """Interactive preview renderer for Minecraft-first source/runtime assets."""

    def __init__(self) -> None:
        super().__init__()
        self._mode = "texture"
        self._texture_path: Path | None = None
        self._pixmap: QPixmap | None = None
        self._payload: dict | None = None
        self._metadata: dict[str, object] = {}
        self._issues: list[dict[str, str]] = []
        self._selection_id: str | None = None
        self._zoom = 1.0
        self._lighting = 1.0
        self._rotation_speed = 4
        self._auto_rotate = False
        self._yaw = 45.0
        self._pitch = 25.0
        self._pan = QPointF(0.0, 0.0)
        self._texture_pool: list[Path] = []
        self._texture_index = -1
        self._drag_button: Qt.MouseButton | None = None
        self._last_pointer = QPoint()

        self._texture_renderer = TextureRenderer()
        self._model_renderer = ModelRenderer()
        self._block_renderer = BlockRenderer()
        self._animation_renderer = AnimationRenderer()
        self._animation_player = AnimationPlayer()

        self._timer = QTimer(self)
        self._timer.timeout.connect(self._tick)
        self._timer.start(33)

    def set_mode(self, mode: str) -> None:
        normalized = MODE_ALIASES.get(mode, self._mode)
        self._mode = normalized
        self.update()

    def set_preview_document(
        self,
        mode: str,
        *,
        payload: dict | None = None,
        metadata: dict[str, object] | None = None,
        issues: list[dict[str, str]] | None = None,
        texture_path: Path | None = None,
        selection_id: str | None = None,
    ) -> None:
        self.set_mode(mode)
        self._payload = payload
        self._metadata = dict(metadata or {})
        self._issues = list(issues or [])
        self._selection_id = selection_id
        if texture_path is not None:
            self.load_texture(texture_path)
        self.update()

    def set_metadata(self, metadata: dict[str, object] | None) -> None:
        self._metadata = dict(metadata or {})
        self.update()

    def set_validation_issues(self, issues: list[dict[str, str]] | list[str]) -> None:
        normalized: list[dict[str, str]] = []
        for issue in issues:
            if isinstance(issue, dict):
                normalized.append({"severity": str(issue.get("severity", "warning")), "message": str(issue.get("message", ""))})
            else:
                normalized.append({"severity": "warning", "message": str(issue)})
        self._issues = normalized
        self.update()

    def set_selection_id(self, selection_id: str | None) -> None:
        self._selection_id = selection_id
        self.update()

    def set_auto_rotate(self, enabled: bool) -> None:
        self._auto_rotate = bool(enabled)
        self.update()

    def set_view_preset(self, preset: str) -> None:
        presets = {
            "front": (0.0, 0.0),
            "side": (90.0, 0.0),
            "top": (0.0, -90.0),
            "isometric": (45.0, 25.0),
        }
        yaw, pitch = presets.get(preset, (45.0, 25.0))
        self._yaw = yaw
        self._pitch = pitch
        self.update()

    def reset_camera(self) -> None:
        self._yaw = 45.0
        self._pitch = 25.0
        self._pan = QPointF(0.0, 0.0)
        self._zoom = 1.0
        self.update()

    def load_texture(self, texture_path: Path) -> None:
        self._texture_path = texture_path
        if not texture_path.exists():
            self._pixmap = None
            self.update()
            return
        image = QImage(str(texture_path))
        self._pixmap = None if image.isNull() else QPixmap.fromImage(image)
        self.update()

    def set_zoom(self, zoom: float) -> None:
        self._zoom = max(0.2, min(4.0, zoom))
        self.update()

    def set_lighting(self, lighting: float) -> None:
        self._lighting = max(0.2, min(2.0, lighting))
        self.update()

    def set_rotation_speed(self, speed: int) -> None:
        self._rotation_speed = max(0, min(20, speed))

    def set_animation_progress(self, progress: float) -> None:
        duration = max(1, self._animation_player.clip.duration_ms)
        self._animation_player.current_ms = int(max(0.0, min(1.0, progress)) * duration)
        self.update()

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
        if self._auto_rotate:
            self._yaw = (self._yaw + self._rotation_speed * 0.35) % 360.0
        self._animation_player.tick(33)
        self.update()

    def mousePressEvent(self, event) -> None:  # noqa: N802
        self._drag_button = event.button()
        self._last_pointer = event.position().toPoint()
        event.accept()

    def mouseMoveEvent(self, event) -> None:  # noqa: N802
        if self._drag_button is None:
            return
        current = event.position().toPoint()
        delta = current - self._last_pointer
        self._last_pointer = current
        if self._drag_button == Qt.MouseButton.LeftButton:
            self._yaw = (self._yaw + delta.x() * 0.7) % 360.0
            self._pitch = max(-89.0, min(89.0, self._pitch - delta.y() * 0.7))
        elif self._drag_button in {Qt.MouseButton.RightButton, Qt.MouseButton.MiddleButton}:
            self._pan += QPointF(delta.x(), delta.y())
        self.update()
        event.accept()

    def mouseReleaseEvent(self, event) -> None:  # noqa: N802
        self._drag_button = None
        super().mouseReleaseEvent(event)

    def mouseDoubleClickEvent(self, event) -> None:  # noqa: N802
        self.reset_camera()
        event.accept()

    def wheelEvent(self, event) -> None:  # noqa: N802
        delta = event.angleDelta().y()
        factor = 1.1 if delta > 0 else 0.9
        self.set_zoom(self._zoom * factor)
        event.accept()

    def paintGL(self) -> None:  # noqa: N802
        painter = QPainter(self)
        base = int(16 * self._lighting)
        painter.fillRect(self.rect(), QColor(min(255, base), min(255, base + 6), min(255, base + 12)))
        self._draw_grid(painter)
        self._draw_preview_surface(painter)
        if self._mode == "runtime_model" and self._metadata.get("animated"):
            self._animation_renderer.draw(painter, self.width(), self.height(), self._animation_player.progress, self._animation_player.clip.name)
        self._draw_hud(painter)
        self._draw_validation_overlay(painter)
        painter.end()

    def _draw_grid(self, painter: QPainter) -> None:
        painter.save()
        painter.setPen(QPen(QColor(44, 50, 63), 1))
        step = 24
        for x in range(0, self.width(), step):
            painter.drawLine(x, 0, x, self.height())
        for y in range(0, self.height(), step):
            painter.drawLine(0, y, self.width(), y)
        painter.restore()

    def _draw_preview_surface(self, painter: QPainter) -> None:
        if self._mode in {"source_gui", "runtime_gui"}:
            self._draw_gui_payload(painter)
            return
        self._draw_model_surface(painter)
        if self._pixmap is not None or self._mode == "texture":
            self._draw_texture_layer(painter)

    def _draw_model_surface(self, painter: QPainter) -> None:
        cubes = [] if not isinstance(self._payload, dict) else list(self._payload.get("cubes", []))
        if cubes:
            for index, cube in enumerate(cubes[:24]):
                cube_id = str(cube.get("id", f"cube_{index}"))
                highlight = cube_id == self._selection_id
                self._draw_cube_projection(painter, cube, index=index, highlight=highlight)
            return
        if self._mode == "runtime_model":
            self._block_renderer.draw(painter, self.width(), self.height(), int(self._yaw))
        else:
            self._model_renderer.draw(painter, self.width(), self.height(), int(self._yaw))

    def _draw_cube_projection(self, painter: QPainter, cube: dict, *, index: int, highlight: bool) -> None:
        cube_from = cube.get("from") or [0, 0, 0]
        cube_to = cube.get("to") or [16, 16, 16]
        width = max(2.0, float(cube_to[0]) - float(cube_from[0]))
        height = max(2.0, float(cube_to[1]) - float(cube_from[1]))
        depth = max(2.0, float(cube_to[2]) - float(cube_from[2]))
        cx = (float(cube_from[0]) + float(cube_to[0])) / 2.0 - 8.0
        cy = (float(cube_from[1]) + float(cube_to[1])) / 2.0 - 8.0
        cz = (float(cube_from[2]) + float(cube_to[2])) / 2.0 - 8.0
        center = self._project_point(cx, cy, cz)
        scale = 10.0 * self._zoom
        rect_width = max(8.0, width * scale * 0.6)
        rect_height = max(8.0, height * scale * 0.6)
        shade = max(40, 150 - index * 3)
        fill = QColor(124, 163, 255, 180 if highlight else 110)
        outline = QColor(220, 235, 255) if highlight else QColor(shade, shade + 20, min(255, shade + 55))
        painter.save()
        painter.setPen(QPen(outline, 3 if highlight else 1))
        painter.setBrush(fill)
        painter.drawRect(int(center.x() - rect_width / 2), int(center.y() - rect_height / 2), int(rect_width), int(rect_height))
        painter.setPen(QColor("#eaf1ff"))
        painter.drawText(int(center.x() - rect_width / 2), int(center.y() - rect_height / 2 - 4), str(cube.get("id", f"cube_{index}")))
        painter.restore()

    def _project_point(self, x: float, y: float, z: float) -> QPointF:
        yaw = math.radians(self._yaw)
        pitch = math.radians(self._pitch)
        rx = x * math.cos(yaw) - z * math.sin(yaw)
        rz = x * math.sin(yaw) + z * math.cos(yaw)
        ry = y * math.cos(pitch) - rz * math.sin(pitch)
        scale = min(self.width(), self.height()) / 18.0 * self._zoom
        center = QPointF(self.width() / 2.0, self.height() / 2.0) + self._pan
        return QPointF(center.x() + rx * scale, center.y() - ry * scale)

    def _draw_gui_payload(self, painter: QPainter) -> None:
        canvas = {"width": 176, "height": 166}
        widgets = []
        if isinstance(self._payload, dict):
            canvas = dict(self._payload.get("canvas") or canvas)
            widgets = list(self._payload.get("widgets", []))
        width = int(float(canvas.get("width", 176)) * max(1.2, self._zoom * 1.6))
        height = int(float(canvas.get("height", 166)) * max(1.2, self._zoom * 1.6))
        left = int((self.width() - width) / 2 + self._pan.x())
        top = int((self.height() - height) / 2 + self._pan.y())
        painter.save()
        painter.setPen(QPen(QColor("#c9dbff"), 2))
        painter.setBrush(QColor("#243345"))
        painter.drawRect(left, top, width, height)
        for widget in widgets[:48]:
            bounds = widget.get("bounds") or {}
            widget_left = left + int(float(bounds.get("x", 0)) * width / max(1.0, float(canvas.get("width", 176))))
            widget_top = top + int(float(bounds.get("y", 0)) * height / max(1.0, float(canvas.get("height", 166))))
            widget_width = max(8, int(float(bounds.get("width", 12)) * width / max(1.0, float(canvas.get("width", 176)))))
            widget_height = max(8, int(float(bounds.get("height", 12)) * height / max(1.0, float(canvas.get("height", 166)))))
            selected = str(widget.get("id", "")) == self._selection_id
            painter.setPen(QPen(QColor("#ffffff") if selected else QColor("#90a8d8"), 2 if selected else 1))
            painter.setBrush(QColor("#3b4f70", 180 if selected else 120))
            painter.drawRect(widget_left, widget_top, widget_width, widget_height)
            painter.drawText(widget_left + 4, widget_top + 14, str(widget.get("label") or widget.get("id") or widget.get("type", "widget")))
        painter.restore()

    def _draw_texture_layer(self, painter: QPainter) -> None:
        if self._pixmap is None:
            return
        tex_size = int(min(self.width(), self.height()) // 2 * self._zoom)
        px = int(self.rect().center().x() - tex_size // 2 + self._pan.x())
        py = int(self.rect().center().y() - tex_size // 2 + self._pan.y())
        painter.save()
        painter.translate(self.rect().center() + self._pan)
        painter.rotate(-self._yaw if self._mode != "texture" else 0)
        painter.translate(-(self.rect().center() + self._pan))
        self._texture_renderer.draw(painter, self._pixmap, px, py, tex_size)
        painter.restore()

    def _draw_hud(self, painter: QPainter) -> None:
        painter.save()
        painter.setPen(QPen(QColor(225, 232, 245), 1))
        lines = [
            "Preview Renderer",
            f"mode: {self._mode}",
            f"camera: yaw {self._yaw:03.0f} pitch {self._pitch:03.0f}",
            f"zoom: {self._zoom:.2f}  light: {self._lighting:.2f}  auto: {'on' if self._auto_rotate else 'off'}",
        ]
        source_label = str(self._metadata.get("sourcePath") or self._texture_path or "no source loaded")
        if len(source_label) > 76:
            source_label = "..." + source_label[-73:]
        lines.append(source_label)
        resource_id = self._metadata.get("resourceId")
        if resource_id:
            lines.append(f"resource: {resource_id}")
        if self._selection_id:
            lines.append(f"selection: {self._selection_id}")
        for index, line in enumerate(lines):
            painter.drawText(12, 24 + index * 20, line)
        painter.restore()

    def _draw_validation_overlay(self, painter: QPainter) -> None:
        if not self._issues:
            return
        overlay_width = min(420, self.width() - 24)
        overlay_height = min(28 + len(self._issues[:6]) * 18, 150)
        painter.save()
        painter.fillRect(12, self.height() - overlay_height - 12, overlay_width, overlay_height, QColor(19, 24, 34, 220))
        painter.setPen(QColor("#f4f7ff"))
        painter.drawText(20, self.height() - overlay_height + 8, overlay_width - 16, 18, Qt.AlignmentFlag.AlignLeft, "Validation")
        for index, issue in enumerate(self._issues[:6]):
            severity = str(issue.get("severity", "warning"))
            color = {"error": QColor("#ff8e8e"), "warning": QColor("#f6c177"), "info": QColor("#8bd5ca")}.get(severity, QColor("#f4f7ff"))
            painter.setPen(color)
            painter.drawText(20, self.height() - overlay_height + 30 + index * 18, f"[{severity.upper()}] {issue.get('message', '')}")
        painter.restore()
