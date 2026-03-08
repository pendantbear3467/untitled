from __future__ import annotations

import math
from dataclasses import dataclass, field
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


@dataclass
class PreviewVariant:
    payload: dict | None = None
    metadata: dict[str, object] = field(default_factory=dict)
    issues: list[dict[str, str]] = field(default_factory=list)
    texture_path: Path | None = None
    selection_id: str | None = None


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
        self._preview_variants: dict[str, PreviewVariant] = {}
        self._preview_auto_mode = "texture"
        self._mode_override: str | None = None
        self._zoom = 1.0
        self._lighting = 1.0
        self._rotation_speed = 4
        self._auto_rotate = False
        self._view_preset = "isometric"
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
        normalized = MODE_ALIASES.get(mode, mode)
        self._mode = normalized
        self._mode_override = normalized
        self.update()

    def set_mode_override(self, mode: str | None) -> None:
        normalized = None if mode in {None, "", "auto"} else MODE_ALIASES.get(mode, mode)
        self._mode_override = normalized
        if self._preview_variants:
            self._apply_preview_variant()
        else:
            if normalized is not None:
                self._mode = normalized
            self.update()

    def set_preview_variants(self, auto_mode: str, variants: dict[str, PreviewVariant | dict[str, object]]) -> None:
        normalized_variants: dict[str, PreviewVariant] = {}
        for mode, variant in variants.items():
            normalized_variants[MODE_ALIASES.get(mode, mode)] = self._coerce_variant(variant)
        self._preview_variants = normalized_variants
        self._preview_auto_mode = MODE_ALIASES.get(auto_mode, auto_mode)
        if self._preview_auto_mode not in self._preview_variants and self._preview_variants:
            self._preview_auto_mode = next(iter(self._preview_variants))
        self._apply_preview_variant()

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
        self.set_preview_variants(
            mode,
            {
                mode: PreviewVariant(
                    payload=payload,
                    metadata=dict(metadata or {}),
                    issues=list(issues or []),
                    texture_path=texture_path,
                    selection_id=selection_id,
                )
            },
        )

    def set_metadata(self, metadata: dict[str, object] | None) -> None:
        self._metadata = dict(metadata or {})
        variant = self._current_variant()
        if variant is not None:
            variant.metadata = dict(self._metadata)
        self.update()

    def set_validation_issues(self, issues: list[dict[str, str]] | list[str]) -> None:
        normalized: list[dict[str, str]] = []
        for issue in issues:
            if isinstance(issue, dict):
                normalized.append({"severity": str(issue.get("severity", "warning")), "message": str(issue.get("message", ""))})
            else:
                normalized.append({"severity": "warning", "message": str(issue)})
        self._issues = normalized
        variant = self._current_variant()
        if variant is not None:
            variant.issues = list(normalized)
        self.update()

    def set_selection_id(self, selection_id: str | None) -> None:
        self._selection_id = selection_id
        variant = self._current_variant()
        if variant is not None:
            variant.selection_id = selection_id
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
        self._view_preset = preset if preset in presets else "isometric"
        self.update()

    def reset_camera(self) -> None:
        self._view_preset = "isometric"
        self._yaw = 45.0
        self._pitch = 25.0
        self._pan = QPointF(0.0, 0.0)
        self._zoom = 1.0
        self.update()

    def load_texture(self, texture_path: Path | None) -> None:
        self._set_texture_image(texture_path)
        variant = self._current_variant()
        if variant is not None:
            variant.texture_path = texture_path
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

    def preview_state(self) -> dict[str, object]:
        return {
            "mode": self._mode,
            "modeOverride": self._mode_override or "auto",
            "autoMode": self._preview_auto_mode,
            "availableModes": sorted(self._preview_variants.keys()),
            "autoRotate": self._auto_rotate,
            "viewPreset": self._view_preset,
            "zoom": self._zoom,
            "lighting": self._lighting,
            "selectionId": self._selection_id,
            "texturePath": str(self._texture_path) if self._texture_path is not None else "",
            "issueCount": len(self._issues),
        }

    def _current_variant(self) -> PreviewVariant | None:
        key = self._resolved_mode_key()
        return self._preview_variants.get(key)

    def _resolved_mode_key(self) -> str:
        if self._mode_override is not None and self._mode_override in self._preview_variants:
            return self._mode_override
        if self._preview_auto_mode in self._preview_variants:
            return self._preview_auto_mode
        return next(iter(self._preview_variants), self._mode)

    def _apply_preview_variant(self) -> None:
        if not self._preview_variants:
            self.update()
            return
        mode = self._resolved_mode_key()
        variant = self._preview_variants.get(mode)
        if variant is None:
            self._mode = mode
            self.update()
            return
        self._mode = mode
        self._payload = variant.payload
        self._metadata = dict(variant.metadata)
        self._issues = list(variant.issues)
        self._selection_id = variant.selection_id
        self._set_texture_image(variant.texture_path)
        self.update()

    def _set_texture_image(self, texture_path: Path | None) -> None:
        self._texture_path = texture_path
        if texture_path is None or not texture_path.exists():
            self._pixmap = None
            return
        image = QImage(str(texture_path))
        self._pixmap = None if image.isNull() else QPixmap.fromImage(image)

    def _coerce_variant(self, variant: PreviewVariant | dict[str, object]) -> PreviewVariant:
        if isinstance(variant, PreviewVariant):
            return PreviewVariant(
                payload=variant.payload,
                metadata=dict(variant.metadata),
                issues=list(variant.issues),
                texture_path=variant.texture_path,
                selection_id=variant.selection_id,
            )
        payload = variant.get("payload") if isinstance(variant, dict) else None
        metadata = dict(variant.get("metadata") or {}) if isinstance(variant, dict) else {}
        issues = list(variant.get("issues") or []) if isinstance(variant, dict) else []
        texture_path = variant.get("texture_path") if isinstance(variant, dict) else None
        selection_id = variant.get("selection_id") if isinstance(variant, dict) else None
        return PreviewVariant(payload=payload, metadata=metadata, issues=issues, texture_path=texture_path, selection_id=selection_id)

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

    def _viewport_rect(self):
        return self.rect().adjusted(14, 14, -14, -14)

    def _has_visual_content(self) -> bool:
        if self._pixmap is not None:
            return True
        if self._mode == "runtime_model":
            return True
        if isinstance(self._payload, dict):
            if self._payload.get("cubes"):
                return True
            if self._mode in {"source_gui", "runtime_gui"} and (self._payload.get("widgets") or self._payload.get("canvas")):
                return True
        return False

    def paintGL(self) -> None:  # noqa: N802
        painter = QPainter(self)
        base = int(16 * self._lighting)
        painter.fillRect(self.rect(), QColor(min(255, base), min(255, base + 8), min(255, base + 14)))
        viewport = self._viewport_rect()
        self._draw_viewport_frame(painter, viewport)
        painter.save()
        painter.setClipRect(viewport.adjusted(1, 1, -1, -1))
        self._draw_grid(painter, viewport)
        self._draw_preview_surface(painter, viewport)
        if self._mode == "runtime_model" and self._metadata.get("animated"):
            self._animation_renderer.draw(painter, self.width(), self.height(), self._animation_player.progress, self._animation_player.clip.name)
        if not self._has_visual_content():
            self._draw_empty_state(painter, viewport)
        painter.restore()
        self._draw_hud(painter, viewport)
        self._draw_validation_overlay(painter, viewport)
        painter.end()

    def _draw_viewport_frame(self, painter: QPainter, viewport) -> None:
        painter.save()
        painter.fillRect(viewport, QColor(8, 12, 20, 225))
        painter.setPen(QPen(QColor(41, 56, 80), 2))
        painter.drawRect(viewport)
        inner = viewport.adjusted(6, 6, -6, -6)
        painter.setPen(QPen(QColor(23, 31, 45), 1))
        painter.drawRect(inner)
        painter.restore()

    def _draw_grid(self, painter: QPainter, viewport) -> None:
        painter.save()
        painter.setPen(QPen(QColor(37, 45, 60), 1))
        step = 24
        for x in range(viewport.left(), viewport.right() + 1, step):
            painter.drawLine(x, viewport.top(), x, viewport.bottom())
        for y in range(viewport.top(), viewport.bottom() + 1, step):
            painter.drawLine(viewport.left(), y, viewport.right(), y)
        painter.setPen(QPen(QColor(73, 89, 118), 1))
        painter.drawLine(viewport.center().x(), viewport.top(), viewport.center().x(), viewport.bottom())
        painter.drawLine(viewport.left(), viewport.center().y(), viewport.right(), viewport.center().y())
        painter.restore()

    def _draw_preview_surface(self, painter: QPainter, viewport) -> None:
        if self._mode in {"source_gui", "runtime_gui"}:
            self._draw_gui_payload(painter, viewport)
            return
        self._draw_model_surface(painter, viewport)
        if self._pixmap is not None or self._mode == "texture":
            self._draw_texture_layer(painter, viewport)

    def _draw_model_surface(self, painter: QPainter, viewport) -> None:
        cubes = [] if not isinstance(self._payload, dict) else list(self._payload.get("cubes", []))
        if cubes:
            for index, cube in enumerate(cubes[:24]):
                cube_id = str(cube.get("id", f"cube_{index}"))
                highlight = cube_id == self._selection_id
                self._draw_cube_projection(painter, cube, index=index, highlight=highlight, viewport=viewport)
            return
        if self._mode == "runtime_model":
            self._block_renderer.draw(painter, self.width(), self.height(), int(self._yaw))
        else:
            self._model_renderer.draw(painter, self.width(), self.height(), int(self._yaw))

    def _draw_cube_projection(self, painter: QPainter, cube: dict, *, index: int, highlight: bool, viewport) -> None:
        cube_from = cube.get("from") or [0, 0, 0]
        cube_to = cube.get("to") or [16, 16, 16]
        width = max(2.0, float(cube_to[0]) - float(cube_from[0]))
        height = max(2.0, float(cube_to[1]) - float(cube_from[1]))
        cx = (float(cube_from[0]) + float(cube_to[0])) / 2.0 - 8.0
        cy = (float(cube_from[1]) + float(cube_to[1])) / 2.0 - 8.0
        cz = (float(cube_from[2]) + float(cube_to[2])) / 2.0 - 8.0
        center = self._project_point(cx, cy, cz, viewport)
        scale = 10.0 * self._zoom
        rect_width = max(10.0, width * scale * 0.6)
        rect_height = max(10.0, height * scale * 0.6)
        shade = max(40, 150 - index * 3)
        fill = QColor(124, 163, 255, 190 if highlight else 110)
        outline = QColor(234, 241, 255) if highlight else QColor(shade, shade + 20, min(255, shade + 55))
        painter.save()
        rect_left = int(center.x() - rect_width / 2)
        rect_top = int(center.y() - rect_height / 2)
        if highlight:
            painter.setPen(QPen(QColor(130, 196, 255, 110), 6))
            painter.drawRect(rect_left - 3, rect_top - 3, int(rect_width + 6), int(rect_height + 6))
        painter.setPen(QPen(outline, 3 if highlight else 1))
        painter.setBrush(fill)
        painter.drawRect(rect_left, rect_top, int(rect_width), int(rect_height))
        painter.setPen(QColor("#eaf1ff"))
        painter.drawText(rect_left, rect_top - 6, str(cube.get("id", f"cube_{index}")))
        painter.restore()

    def _project_point(self, x: float, y: float, z: float, viewport) -> QPointF:
        yaw = math.radians(self._yaw)
        pitch = math.radians(self._pitch)
        rx = x * math.cos(yaw) - z * math.sin(yaw)
        rz = x * math.sin(yaw) + z * math.cos(yaw)
        ry = y * math.cos(pitch) - rz * math.sin(pitch)
        scale = min(viewport.width(), viewport.height()) / 18.0 * self._zoom
        center = QPointF(viewport.center()) + self._pan
        return QPointF(center.x() + rx * scale, center.y() - ry * scale)

    def _draw_gui_payload(self, painter: QPainter, viewport) -> None:
        canvas = {"width": 176, "height": 166}
        widgets = []
        if isinstance(self._payload, dict):
            canvas = dict(self._payload.get("canvas") or canvas)
            widgets = list(self._payload.get("widgets", []))
        width = int(float(canvas.get("width", 176)) * max(1.2, self._zoom * 1.6))
        height = int(float(canvas.get("height", 166)) * max(1.2, self._zoom * 1.6))
        left = int(viewport.center().x() - width / 2 + self._pan.x())
        top = int(viewport.center().y() - height / 2 + self._pan.y())
        painter.save()
        painter.setPen(QPen(QColor("#c9dbff"), 2))
        painter.setBrush(QColor("#223246"))
        painter.drawRect(left, top, width, height)
        for widget in widgets[:48]:
            bounds = widget.get("bounds") or {}
            widget_left = left + int(float(bounds.get("x", 0)) * width / max(1.0, float(canvas.get("width", 176))))
            widget_top = top + int(float(bounds.get("y", 0)) * height / max(1.0, float(canvas.get("height", 166))))
            widget_width = max(8, int(float(bounds.get("width", 12)) * width / max(1.0, float(canvas.get("width", 176)))))
            widget_height = max(8, int(float(bounds.get("height", 12)) * height / max(1.0, float(canvas.get("height", 166)))))
            selected = str(widget.get("id", "")) == self._selection_id
            if selected:
                painter.setPen(QPen(QColor("#ffffff"), 3))
                painter.setBrush(QColor("#4a678f", 200))
            else:
                painter.setPen(QPen(QColor("#90a8d8"), 1))
                painter.setBrush(QColor("#3b4f70", 120))
            painter.drawRect(widget_left, widget_top, widget_width, widget_height)
            painter.drawText(widget_left + 4, widget_top + 14, str(widget.get("label") or widget.get("id") or widget.get("type", "widget")))
        painter.restore()

    def _draw_texture_layer(self, painter: QPainter, viewport) -> None:
        if self._pixmap is None:
            return
        tex_size = int(min(viewport.width(), viewport.height()) // 2 * self._zoom)
        center = viewport.center() + self._pan.toPoint()
        px = int(center.x() - tex_size // 2)
        py = int(center.y() - tex_size // 2)
        painter.save()
        painter.translate(QPointF(center))
        painter.rotate(-self._yaw if self._mode != "texture" else 0)
        painter.translate(-QPointF(center))
        self._texture_renderer.draw(painter, self._pixmap, px, py, tex_size)
        painter.restore()

    def _draw_hud(self, painter: QPainter, viewport) -> None:
        painter.save()
        lines = [
            "Focused Preview",
            f"mode: {self._mode}",
            f"camera: yaw {self._yaw:03.0f} pitch {self._pitch:03.0f} preset {self._view_preset}",
            f"zoom: {self._zoom:.2f}  light: {self._lighting:.2f}  auto: {'on' if self._auto_rotate else 'off'}",
            "controls: orbit / pan / zoom / reset",
        ]
        if self._selection_id:
            lines.append(f"selection: {self._selection_id}")
        panel_width = min(420, max(280, viewport.width() // 2))
        panel_height = 22 + len(lines) * 18
        panel_left = viewport.left() + 12
        panel_top = viewport.top() + 12
        painter.fillRect(panel_left, panel_top, panel_width, panel_height, QColor(15, 20, 30, 220))
        painter.setPen(QPen(QColor("#334762"), 1))
        painter.drawRect(panel_left, panel_top, panel_width, panel_height)
        painter.setPen(QColor("#f4f7ff"))
        for index, line in enumerate(lines):
            painter.drawText(panel_left + 12, panel_top + 24 + index * 18, line)

        source_label = str(self._metadata.get("sourcePath") or self._texture_path or "No source loaded")
        if len(source_label) > 68:
            source_label = "..." + source_label[-65:]
        meta_lines = [source_label]
        resource_id = self._metadata.get("resourceId")
        if resource_id:
            meta_lines.append(f"resource: {resource_id}")
        runtime_path = self._metadata.get("runtimePath")
        if runtime_path:
            meta_lines.append(f"runtime: {Path(str(runtime_path)).name}")
        meta_width = min(360, max(220, viewport.width() // 3))
        meta_height = 20 + len(meta_lines) * 18
        meta_left = viewport.right() - meta_width - 12
        meta_top = viewport.top() + 12
        painter.fillRect(meta_left, meta_top, meta_width, meta_height, QColor(15, 20, 30, 210))
        painter.setPen(QPen(QColor("#334762"), 1))
        painter.drawRect(meta_left, meta_top, meta_width, meta_height)
        painter.setPen(QColor("#d7deed"))
        for index, line in enumerate(meta_lines):
            painter.drawText(meta_left + 10, meta_top + 22 + index * 18, line)
        painter.restore()

    def _draw_empty_state(self, painter: QPainter, viewport) -> None:
        painter.save()
        width = min(420, int(viewport.width() * 0.58))
        height = 108
        left = int(viewport.center().x() - width / 2)
        top = int(viewport.center().y() - height / 2)
        painter.fillRect(left, top, width, height, QColor(14, 18, 27, 232))
        painter.setPen(QPen(QColor("#334762"), 1))
        painter.drawRect(left, top, width, height)
        painter.setPen(QColor("#f4f7ff"))
        painter.drawText(left + 14, top + 28, "Nothing to preview yet")
        hint = (
            "Open a GUI/model source, runtime export,
or texture asset to populate the focused preview."
            if self._mode != "texture"
            else "Load a texture or select a linked asset
to inspect it here."
        )
        painter.setPen(QColor("#9db6e0"))
        painter.drawText(left + 14, top + 54, width - 28, 44, Qt.AlignmentFlag.AlignLeft | Qt.AlignmentFlag.AlignTop, hint)
        painter.restore()

    def _draw_validation_overlay(self, painter: QPainter, viewport) -> None:
        if not self._issues:
            return
        overlay_width = min(460, viewport.width() - 24)
        overlay_height = min(34 + len(self._issues[:6]) * 18, 168)
        left = viewport.left() + 12
        top = viewport.bottom() - overlay_height - 12
        painter.save()
        painter.fillRect(left, top, overlay_width, overlay_height, QColor(17, 23, 34, 228))
        painter.setPen(QPen(QColor("#334762"), 1))
        painter.drawRect(left, top, overlay_width, overlay_height)
        painter.setPen(QColor("#f4f7ff"))
        painter.drawText(left + 12, top + 22, "Validation")
        for index, issue in enumerate(self._issues[:6]):
            severity = str(issue.get("severity", "warning"))
            color = {"error": QColor("#ff8e8e"), "warning": QColor("#f6c177"), "info": QColor("#8bd5ca")}.get(severity, QColor("#f4f7ff"))
            painter.setPen(color)
            painter.drawText(left + 12, top + 46 + index * 18, f"[{severity.upper()}] {issue.get('message', '')}")
        painter.restore()
