from __future__ import annotations

from PyQt6.QtCore import Qt
from PyQt6.QtWidgets import (
    QComboBox,
    QFormLayout,
    QFrame,
    QGridLayout,
    QGroupBox,
    QHBoxLayout,
    QLabel,
    QListWidget,
    QPushButton,
    QSplitter,
    QTabWidget,
    QTextEdit,
    QVBoxLayout,
    QWidget,
)


class _PanelHeader(QFrame):
    def __init__(self, title: str, help_text: str) -> None:
        super().__init__()
        row = QHBoxLayout(self)
        row.setContentsMargins(8, 6, 8, 6)
        label = QLabel(title)
        label.setObjectName("panelHeaderTitle")
        label.setToolTip(help_text)
        row.addWidget(label)
        hint = QLabel("What does this do?")
        hint.setObjectName("panelHelpHint")
        hint.setToolTip(help_text)
        row.addWidget(hint)
        row.addStretch(1)


class GuiStudioPanel(QWidget):
    def __init__(self) -> None:
        super().__init__()
        root = QVBoxLayout(self)
        root.addWidget(_PanelHeader("GUI Studio", "Design in-game UI screens with palettes, hierarchy, properties, and preview."))

        split = QSplitter(Qt.Orientation.Horizontal)
        left = self._left_panel()
        center = self._center_panel()
        right = self._right_panel()

        split.addWidget(left)
        split.addWidget(center)
        split.addWidget(right)
        split.setSizes([260, 760, 360])
        root.addWidget(split)

    def _left_panel(self) -> QWidget:
        box = QWidget()
        layout = QVBoxLayout(box)

        palette = QGroupBox("Component Palette")
        palette_layout = QVBoxLayout(palette)
        comp = QListWidget()
        comp.addItems(["Panel", "Label", "Button", "Image", "Progress Bar", "Inventory Grid", "Tooltip Region"])
        palette_layout.addWidget(comp)

        hierarchy = QGroupBox("Hierarchy / Outliner")
        hierarchy_layout = QVBoxLayout(hierarchy)
        tree = QListWidget()
        tree.addItems(["ScreenRoot", "  HeaderBar", "  ContentPanel", "  FooterHints"])
        hierarchy_layout.addWidget(tree)

        layout.addWidget(palette)
        layout.addWidget(hierarchy)
        return box

    def _center_panel(self) -> QWidget:
        box = QWidget()
        layout = QVBoxLayout(box)

        controls = QHBoxLayout()
        screen_selector = QComboBox()
        screen_selector.addItems(["Inventory Screen", "Skill Tree Overlay", "Machine UI", "Quest Journal"])
        theme_selector = QComboBox()
        theme_selector.addItems(["Extreme Dark", "Arcane Brass", "Industrial Slate"])
        resolution_selector = QComboBox()
        resolution_selector.addItems(["1280x720", "1600x900", "1920x1080", "2560x1440"])

        controls.addWidget(QLabel("Screen"))
        controls.addWidget(screen_selector)
        controls.addWidget(QLabel("Theme"))
        controls.addWidget(theme_selector)
        controls.addWidget(QLabel("Resolution"))
        controls.addWidget(resolution_selector)

        canvas = QTextEdit()
        canvas.setReadOnly(True)
        canvas.setPlainText(
            "GUI Canvas Placeholder\n\n"
            "- Drag components from palette\n"
            "- Use alignment tools and spacing guides\n"
            "- Tooltip preview appears for selected component"
        )

        align = QGridLayout()
        for col, name in enumerate(["Align Left", "Center", "Align Right", "Top", "Middle", "Bottom"]):
            btn = QPushButton(name)
            align.addWidget(btn, col // 3, col % 3)

        layout.addLayout(controls)
        layout.addWidget(canvas)
        layout.addLayout(align)
        return box

    def _right_panel(self) -> QWidget:
        panel = QTabWidget()

        inspector = QWidget()
        form = QFormLayout(inspector)
        form.addRow("ID", QLabel("component_id"))
        form.addRow("Position", QLabel("x: 0, y: 0"))
        form.addRow("Size", QLabel("w: 160, h: 24"))
        form.addRow("Anchors", QLabel("Top Left"))
        form.addRow("Tooltip Preview", QLabel("Shows on hover in-game"))

        preview = QWidget()
        preview_layout = QVBoxLayout(preview)
        preview_layout.addWidget(QLabel("Live Preview"))
        preview_text = QTextEdit()
        preview_text.setReadOnly(True)
        preview_text.setPlainText("Preview surface connected to backend renderer contract.")
        preview_layout.addWidget(preview_text)

        panel.addTab(inspector, "Inspector")
        panel.addTab(preview, "Preview")
        return panel


class ModelStudioPanel(QWidget):
    def __init__(self) -> None:
        super().__init__()
        root = QVBoxLayout(self)
        root.addWidget(_PanelHeader("Model Studio", "Edit Minecraft-first model structures, transforms, textures, and viewport previews."))

        split = QSplitter(Qt.Orientation.Horizontal)

        outliner = QGroupBox("Outliner / Part List")
        out_layout = QVBoxLayout(outliner)
        part_list = QListWidget()
        part_list.addItems(["root", " body", " arm_left", " arm_right", " head", " accessory_slot"])
        out_layout.addWidget(part_list)

        center = QWidget()
        center_layout = QVBoxLayout(center)
        toolbar = QHBoxLayout()
        for label in ["Translate", "Rotate", "Scale", "Pivot", "Reset"]:
            toolbar.addWidget(QPushButton(label))
        center_layout.addLayout(toolbar)
        viewport = QTextEdit()
        viewport.setReadOnly(True)
        viewport.setPlainText(
            "Model Viewport Placeholder\n\n"
            "- Orbit camera\n"
            "- Toggle wireframe\n"
            "- Display normals and bounds"
        )
        center_layout.addWidget(viewport)

        right = QTabWidget()
        props = QWidget()
        props_form = QFormLayout(props)
        props_form.addRow("Position", QLabel("0, 0, 0"))
        props_form.addRow("Rotation", QLabel("0, 0, 0"))
        props_form.addRow("Scale", QLabel("1, 1, 1"))
        props_form.addRow("Material", QLabel("default"))

        uv = QWidget()
        uv_layout = QVBoxLayout(uv)
        uv_layout.addWidget(QLabel("Texture / UV"))
        uv_note = QTextEdit()
        uv_note.setReadOnly(True)
        uv_note.setPlainText("UV editor shell ready. Backend UV operations are attached via model contracts.")
        uv_layout.addWidget(uv_note)

        timeline = QWidget()
        timeline_layout = QVBoxLayout(timeline)
        timeline_layout.addWidget(QLabel("Timeline"))
        t_note = QTextEdit()
        t_note.setReadOnly(True)
        t_note.setPlainText("Timeline placeholder for staged rollout once animation backend contracts are available.")
        timeline_layout.addWidget(t_note)

        right.addTab(props, "Inspector")
        right.addTab(uv, "Texture/UV")
        right.addTab(timeline, "Timeline")

        split.addWidget(outliner)
        split.addWidget(center)
        split.addWidget(right)
        split.setSizes([260, 760, 340])
        root.addWidget(split)


class BuildRunPanel(QWidget):
    def __init__(self, callbacks: dict[str, object]) -> None:
        super().__init__()
        root = QVBoxLayout(self)
        root.addWidget(_PanelHeader("Build/Run Studio", "Run validation, compile/export pipelines, and inspect runtime logs and reports."))

        actions = QHBoxLayout()
        for title, key in [
            ("Validate", "validate_assets"),
            ("Compile Expansion", "compile_expansion"),
            ("Build Release", "release_build"),
            ("Build Modpack", "modpack_build"),
            ("Export Datapack", "export_datapack"),
            ("Export Resourcepack", "export_resourcepack"),
        ]:
            btn = QPushButton(title)
            callback = callbacks.get(key)
            if callable(callback):
                btn.clicked.connect(callback)
            actions.addWidget(btn)

        root.addLayout(actions)

        details = QTabWidget()

        task_status = QTextEdit()
        task_status.setReadOnly(True)
        task_status.setPlainText("Task Status\n\nUse the action buttons to run studio tasks.\nLive state is shown in the bottom logs and notifications docks.")

        validation = QTextEdit()
        validation.setReadOnly(True)
        validation.setPlainText("Validation Report Viewer\n\nRuns through existing validation backend and surfaces friendly summaries.")

        tests = QTextEdit()
        tests.setReadOnly(True)
        tests.setPlainText("Test Results\n\nAttach test run backend contract for full test explorer integration.")

        details.addTab(task_status, "Task Status")
        details.addTab(validation, "Validation Reports")
        details.addTab(tests, "Tests")

        root.addWidget(details)
