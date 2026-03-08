from __future__ import annotations

import sys
import webbrowser
from pathlib import Path

from PyQt6.QtCore import QSettings, Qt, QTimer
from PyQt6.QtWidgets import (
    QApplication,
    QDockWidget,
    QFileDialog,
    QHBoxLayout,
    QLabel,
    QListWidget,
    QListWidgetItem,
    QMainWindow,
    QMessageBox,
    QPushButton,
    QStyle,
    QSlider,
    QStatusBar,
    QTabWidget,
    QToolBar,
    QVBoxLayout,
    QWidget,
)

from asset_studio.blockbench.bbmodel_importer import import_bbmodel
from asset_studio.cli.validate_commands import run_validate_command
from asset_studio.cli.pack_commands import export_pack_command
from asset_studio.generators.armor_generator import ArmorGenerator
from asset_studio.generators.block_generator import BlockGenerator
from asset_studio.generators.content_pack_generator import ContentPackGenerator
from asset_studio.generators.machine_generator import MachineGenerator
from asset_studio.generators.ore_generator import OreGenerator
from asset_studio.generators.tool_generator import ToolGenerator
from asset_studio.gui.asset_wizard import AssetWizardPanel, ToolWizardInput
from asset_studio.gui.code_studio import CodeStudioPanel
from asset_studio.gui.console_panel import ConsolePanel
from asset_studio.gui.editors import MachineEditor, MaterialEditor, QuestEditor, SkillTreeEditor, WeaponEditor, WorldgenEditor
from asset_studio.gui.graph_editor import GraphEditor
from asset_studio.gui.menu_bar import build_menu_bar
from asset_studio.gui.preview_renderer import PreviewRenderer
from asset_studio.gui.project_browser import ProjectBrowser
from asset_studio.gui.skilltree_designer import SkillTreeDesigner
from asset_studio.gui.studio_panels import BuildRunPanel, GuiStudioPanel, ModelStudioPanel
from asset_studio.gui.timeline_widget import TimelineWidget
from asset_studio.modpack.modpack_builder import ModpackBuilder
from asset_studio.release.release_manager import ReleaseManager
from asset_studio.repair.repair_engine import RepairEngine
from asset_studio.workspace.workspace_manager import WorkspaceManager
from compiler.module_builder import ModuleBuilder
from extremecraft_sdk.api.sdk import ExtremeCraftSDK


class AssetStudioWindow(QMainWindow):
    STUDIO_TAB_CODE = "Code"
    STUDIO_TAB_PROGRESS = "Progression"

    def __init__(self, workspace_root: Path) -> None:
        super().__init__()
        self.workspace_manager = WorkspaceManager(workspace_root=workspace_root, repo_root=Path.cwd())
        self.context = self.workspace_manager.load_context()
        self._settings = QSettings("ExtremeCraft", "ExtremeCraftStudio")
        self._recent_projects: list[str] = []

        self.setWindowTitle("EXTREMECRAFT STUDIO")
        self.resize(1420, 900)
        self.setStatusBar(QStatusBar(self))
        self.statusBar().setSizeGripEnabled(True)
        self._cursor_status = QLabel("Ready")
        self._autosave_status = QLabel("Autosave: idle")
        self.statusBar().addPermanentWidget(self._cursor_status)
        self.statusBar().addPermanentWidget(self._autosave_status)

        self._autosave_timer = QTimer(self)
        self._autosave_timer.setInterval(30000)
        self._autosave_timer.timeout.connect(self._autosave_tick)
        self._autosave_timer.start()

        self.log = ConsolePanel()
        self.notifications = QListWidget()
        self.notifications.setAlternatingRowColors(True)
        self.notifications.setToolTip("User-friendly workflow notifications and failures.")
        self.notifications.addItem(QListWidgetItem("Welcome to ExtremeCraft Studio. Hover controls to learn what they do."))

        self.browser = ProjectBrowser()
        self.browser.load_workspace(self.context.workspace_root)
        self.browser.file_open_requested.connect(self._open_file_from_browser)
        self.browser.setToolTip("Open files in embedded code editor by double-clicking.")

        self.preview = PreviewRenderer()
        self.preview_tab_renderer = PreviewRenderer()
        self._last_preview_texture: Path | None = None

        self.wizard = AssetWizardPanel()
        self.wizard.generate_tool_requested.connect(self._on_generate_tool)

        self.code_studio = CodeStudioPanel(self.context.workspace_root)
        self.code_studio.status_message.connect(self._set_cursor_status)
        self.code_studio.notifications.connect(lambda message: self._notify("info", message))

        self.visual_builder = self._safe_panel("Graph Studio", lambda: GraphEditor(self.context))
        if hasattr(self.visual_builder, "graph_log"):
            self.visual_builder.graph_log.connect(self._write_log)

        self.skilltree_designer = self._safe_panel("Progression Studio", lambda: SkillTreeDesigner(self.context))
        if hasattr(self.skilltree_designer, "log_requested"):
            self.skilltree_designer.log_requested.connect(self._write_log)

        self.gui_studio = self._safe_panel("GUI Studio", GuiStudioPanel)
        self.model_studio = self._safe_panel("Model Studio", ModelStudioPanel)

        self.editor_tabs = self._build_editor_tabs()
        self.studio_tabs = self._build_studio_tabs()
        self.setCentralWidget(self.studio_tabs)

        self._build_docks()
        self._build_toolbar()
        self.menu_actions = build_menu_bar(self, self._callbacks())
        self._configure_recent_projects_menu()
        self._apply_theme()
        self._load_recent_projects()
        self._check_recovery_snapshots()

        self._write_log("Workspace loaded")
        self._write_log(str(self.context.workspace_root))
        self._notify("info", "Studio shell ready. Open files from Project Browser to start coding.")

    def _safe_panel(self, name: str, factory):
        try:
            return factory()
        except Exception as exc:  # noqa: BLE001
            fallback = QWidget()
            layout = QVBoxLayout(fallback)
            label = QLabel(f"{name} failed to load.")
            details = QLabel(str(exc))
            details.setWordWrap(True)
            layout.addWidget(label)
            layout.addWidget(details)
            self._notify("error", f"{name} failed to load")
            self._write_log(f"{name} failed to load: {exc}")
            return fallback

    def _build_studio_tabs(self) -> QTabWidget:
        tabs = QTabWidget()
        tabs.setTabPosition(QTabWidget.TabPosition.North)
        tabs.setDocumentMode(True)
        tabs.tabBar().setExpanding(False)
        tabs.tabBar().setElideMode(Qt.TextElideMode.ElideRight)

        self._add_studio_tab(tabs, self.code_studio, self.STUDIO_TAB_CODE, "Syntax-highlighted coding workspace with tabs, find/replace, and diagnostics.")
        self._add_studio_tab(tabs, self.wizard, "Asset Wizard", "Fast generators for common assets and bundles.")
        self._add_studio_tab(tabs, self.visual_builder, "Graph Studio", "Visual logic and content graph editor.")
        self._add_studio_tab(tabs, self.skilltree_designer, self.STUDIO_TAB_PROGRESS, "Progression and skilltree authoring studio.")
        self._add_studio_tab(tabs, self.gui_studio, "GUI Studio", "Layout and component-focused GUI design shell.")
        self._add_studio_tab(tabs, self.model_studio, "Model Studio", "Minecraft-first model editing shell.")
        self._add_studio_tab(tabs, BuildRunPanel(self._callbacks()), "Build/Run", "Build, validation, and run workflow cockpit.")
        self._add_studio_tab(tabs, self.editor_tabs, "Data Editors", "Structured data editors for materials, machines, and more.")
        self._add_studio_tab(tabs, self.preview_tab_renderer, "Preview", "Quick dedicated preview pane.")

        tabs.currentChanged.connect(self._studio_tab_changed)
        return tabs

    def _add_studio_tab(self, tabs: QTabWidget, widget: QWidget, title: str, help_text: str) -> None:
        idx = tabs.addTab(widget, title)
        tabs.setTabToolTip(idx, help_text)

    def _build_docks(self) -> None:
        project_dock = QDockWidget("Project Browser", self)
        project_dock.setObjectName("dock_project_browser")
        project_dock.setWidget(self.browser)
        project_dock.setFeatures(QDockWidget.DockWidgetFeature.DockWidgetMovable | QDockWidget.DockWidgetFeature.DockWidgetFloatable)
        self.addDockWidget(Qt.DockWidgetArea.LeftDockWidgetArea, project_dock)

        preview_controls = self._build_preview_controls()
        preview_host = QWidget()
        preview_layout = QVBoxLayout(preview_host)
        preview_layout.setContentsMargins(6, 6, 6, 6)
        preview_layout.addWidget(self.preview)
        preview_layout.addWidget(preview_controls)

        preview_dock = QDockWidget("Preview Renderer", self)
        preview_dock.setObjectName("dock_preview")
        preview_dock.setWidget(preview_host)
        preview_dock.setFeatures(QDockWidget.DockWidgetFeature.DockWidgetMovable | QDockWidget.DockWidgetFeature.DockWidgetFloatable)
        self.addDockWidget(Qt.DockWidgetArea.RightDockWidgetArea, preview_dock)

        bottom_tabs = QTabWidget()
        bottom_tabs.addTab(self.log, "Console")
        bottom_tabs.addTab(self.notifications, "Notifications")
        bottom_tabs.setToolTip("Debug and workflow output. Failures are isolated and surfaced here.")

        output_dock = QDockWidget("Output", self)
        output_dock.setObjectName("dock_output")
        output_dock.setWidget(bottom_tabs)
        output_dock.setFeatures(QDockWidget.DockWidgetFeature.DockWidgetMovable | QDockWidget.DockWidgetFeature.DockWidgetFloatable)
        self.addDockWidget(Qt.DockWidgetArea.BottomDockWidgetArea, output_dock)

    def _build_toolbar(self) -> None:
        toolbar = QToolBar("Studio")
        toolbar.setMovable(False)
        toolbar.setToolButtonStyle(Qt.ToolButtonStyle.ToolButtonTextBesideIcon)
        self.addToolBar(Qt.ToolBarArea.TopToolBarArea, toolbar)

        open_action = toolbar.addAction(self.style().standardIcon(QStyle.StandardPixmap.SP_DialogOpenButton), "Open")
        open_action.setToolTip("Open workspace")
        open_action.setStatusTip("Open an existing ExtremeCraft workspace")
        open_action.triggered.connect(self._open_project)

        save_action = toolbar.addAction(self.style().standardIcon(QStyle.StandardPixmap.SP_DialogSaveButton), "Save")
        save_action.setToolTip("Save workspace and open code files")
        save_action.setStatusTip("Save workspace and all code tabs")
        save_action.triggered.connect(self._save_workspace)

        toolbar.addSeparator()

        validate_action = toolbar.addAction("Validate")
        validate_action.setStatusTip("Run validation and show user-friendly status")
        validate_action.triggered.connect(self._validate_assets)

        run_action = toolbar.addAction("Build")
        run_action.setStatusTip("Build release bundle")
        run_action.triggered.connect(self._release_build)

        compile_action = toolbar.addAction("Compile")
        compile_action.setStatusTip("Compile primary addon expansion")
        compile_action.triggered.connect(self._compile_expansion)

        latest_log_action = toolbar.addAction("Latest Log")
        latest_log_action.setStatusTip("Open latest runtime log in code studio")
        latest_log_action.triggered.connect(self._open_latest_log)

        clear_log_action = toolbar.addAction("Clear Logs")
        clear_log_action.setStatusTip("Clear console and notification output panes")
        clear_log_action.triggered.connect(self._clear_logs)

        toolbar.addSeparator()

        code_action = toolbar.addAction("Code")
        code_action.setStatusTip("Switch to code studio")
        code_action.triggered.connect(lambda: self._switch_tab_by_name(self.STUDIO_TAB_CODE))

        progression_action = toolbar.addAction("Progression")
        progression_action.setStatusTip("Switch to progression studio")
        progression_action.triggered.connect(lambda: self._switch_tab_by_name(self.STUDIO_TAB_PROGRESS))

        gui_action = toolbar.addAction("GUI")
        gui_action.setStatusTip("Switch to GUI Studio")
        gui_action.triggered.connect(lambda: self._switch_tab_by_name("GUI Studio"))

        model_action = toolbar.addAction("Model")
        model_action.setStatusTip("Switch to Model Studio")
        model_action.triggered.connect(lambda: self._switch_tab_by_name("Model Studio"))

    def _configure_recent_projects_menu(self) -> None:
        file_menu_action = self.menuBar().actions()[0] if self.menuBar().actions() else None
        if file_menu_action is None:
            return
        file_menu = file_menu_action.menu()
        if file_menu is None:
            return
        self.recent_projects_menu = file_menu.addMenu("Recent Projects")

    def _callbacks(self):
        return {
            "new_project": self._new_project,
            "open_project": self._open_project,
            "import_blockbench": self._import_blockbench,
            "export_assets": self._export_assets,
            "save_workspace": self._save_workspace,
            "undo": lambda: self._write_log("Undo requested"),
            "redo": lambda: self._write_log("Redo requested"),
            "generate_tool": lambda: self._switch_tab_by_name("Asset Wizard"),
            "generate_ore": self._generate_ore,
            "generate_armor": self._generate_armor,
            "generate_machine": self._generate_machine,
            "generate_block": self._generate_block,
            "generate_material_set": self._generate_material_set,
            "texture_generator": lambda: self._write_log("Texture Generator opened"),
            "blockbench_converter": lambda: self._write_log("Blockbench Converter opened"),
            "recipe_builder": lambda: self._write_log("Recipe Builder opened"),
            "datapack_builder": lambda: self._write_log("Datapack Builder opened"),
            "repair_assets": self._repair_assets,
            "compile_assets": lambda: self._write_log("Compile Assets completed"),
            "compile_expansion": self._compile_expansion,
            "validate_assets": self._validate_assets,
            "export_resourcepack": lambda: self._export_target("resourcepack"),
            "export_datapack": lambda: self._export_target("datapack"),
            "release_build": self._release_build,
            "modpack_build": self._modpack_build,
            "preview_models": self._preview_models,
            "preview_animations": self._preview_animations,
            "texture_viewer": self._texture_viewer,
            "documentation": lambda: webbrowser.open("https://github.com/pendantbear3467/untitled/tree/main/docs"),
            "github": lambda: webbrowser.open("https://github.com/pendantbear3467/untitled"),
        }

    def _write_log(self, message: str) -> None:
        self.log.append_line(message)
        self.statusBar().showMessage(message, 4000)

    def _set_cursor_status(self, message: str) -> None:
        self._cursor_status.setText(message)

    def _notify(self, level: str, message: str) -> None:
        item = QListWidgetItem(f"{level.upper()}: {message}")
        self.notifications.insertItem(0, item)

    def _load_recent_projects(self) -> None:
        raw = self._settings.value("recentProjects", [], type=list)
        self._recent_projects = [str(path) for path in raw if Path(str(path)).exists()]
        self._refresh_recent_menu()

    def _remember_project(self, path: Path) -> None:
        value = str(path)
        self._recent_projects = [entry for entry in self._recent_projects if entry != value]
        self._recent_projects.insert(0, value)
        self._recent_projects = self._recent_projects[:8]
        self._settings.setValue("recentProjects", self._recent_projects)
        self._refresh_recent_menu()

    def _refresh_recent_menu(self) -> None:
        if not hasattr(self, "recent_projects_menu"):
            return
        self.recent_projects_menu.clear()
        if not self._recent_projects:
            action = self.recent_projects_menu.addAction("No recent projects")
            action.setEnabled(False)
            return
        for project in self._recent_projects:
            action = self.recent_projects_menu.addAction(project)
            action.setToolTip("Open recent workspace")
            action.triggered.connect(lambda checked=False, p=project: self._open_recent(Path(p)))

    def _open_recent(self, path: Path) -> None:
        if not path.exists():
            self._notify("warning", f"Recent project no longer exists: {path}")
            return
        self._load_workspace(path)
        self._notify("info", f"Opened recent project: {path}")

    def _load_workspace(self, root: Path) -> None:
        self.workspace_manager = WorkspaceManager(workspace_root=root, repo_root=Path.cwd())
        self.context = self.workspace_manager.load_context()
        self.browser.load_workspace(self.context.workspace_root)
        self.code_studio.workspace_root = self.context.workspace_root
        if hasattr(self.visual_builder, "set_context"):
            self.visual_builder.set_context(self.context)
        if hasattr(self.skilltree_designer, "set_context"):
            self.skilltree_designer.set_context(self.context)
        self.editor_tabs = self._build_editor_tabs()
        data_index = self._find_tab_index("Data Editors")
        if data_index is not None:
            self.studio_tabs.removeTab(data_index)
            self._add_studio_tab(self.studio_tabs, self.editor_tabs, "Data Editors", "Structured data editors for materials, machines, and more.")
        self._remember_project(root)

    def _new_project(self) -> None:
        self.context = self.workspace_manager.initialize_workspace()
        self.browser.load_workspace(self.context.workspace_root)
        self.code_studio.workspace_root = self.context.workspace_root
        if hasattr(self.visual_builder, "set_context"):
            self.visual_builder.set_context(self.context)
        if hasattr(self.skilltree_designer, "set_context"):
            self.skilltree_designer.set_context(self.context)
        self._remember_project(self.context.workspace_root)
        self._write_log("New project initialized")
        self._notify("info", "New workspace initialized")

    def _open_project(self) -> None:
        selected = QFileDialog.getExistingDirectory(self, "Open workspace", str(self.context.workspace_root))
        if not selected:
            return
        self._load_workspace(Path(selected))
        self._write_log(f"Opened workspace: {selected}")
        self._notify("info", f"Opened workspace: {selected}")

    def _save_workspace(self) -> None:
        self.code_studio.save_all()
        self.workspace_manager.save_context(self.context)
        self._write_log("Workspace saved")
        self._notify("info", "Workspace saved")

    def _import_blockbench(self) -> None:
        selected, _ = QFileDialog.getOpenFileName(self, "Import Blockbench model", "", "Blockbench (*.bbmodel)")
        if not selected:
            return
        result = import_bbmodel(Path(selected), self.context)
        self._write_log(f"Imported Blockbench model: {result}")
        self._notify("info", f"Imported model: {result}")
        self._set_preview_texture(self.context.workspace_root / "assets" / "textures" / "block" / f"{result}.png", "block")

    def _export_assets(self) -> None:
        export_pack_command(self.context, "resourcepack")
        export_pack_command(self.context, "datapack")
        self._write_log("Assets exported")

    def _validate_assets(self) -> None:
        args = type("ValidateArgs", (), {"strict": False})()
        code = run_validate_command(args, self.context)
        if code == 0:
            self._write_log("Validation completed")
            self._notify("info", "Validation completed with no blocking errors")
            return
        self._write_log("Validation completed with issues")
        self._notify("warning", "Validation reported issues. See console for details.")

    def _repair_assets(self) -> None:
        report = RepairEngine(self.context).repair()
        self._write_log(f"Auto-repair completed with {report.total} actions")
        self._notify("info", f"Auto-repair completed ({report.total} actions)")

    def _compile_expansion(self) -> None:
        addons = sorted((self.context.workspace_root / "addons").glob("*"))
        if not addons:
            self._write_log("No addons found. Create one via: assetstudio sdk init-addon <name>")
            return
        addon_name = addons[0].name

        sdk = ExtremeCraftSDK(
            addons_root=self.context.workspace_root / "addons",
            context=self.context,
            plugin_api=self.context.plugins,
        )
        try:
            result = ModuleBuilder(context=self.context, sdk=sdk).build_expansion(addon_name)
        except Exception as exc:  # noqa: BLE001
            self._write_log(f"Compile failed: {exc}")
            self._notify("error", f"Compile failed: {exc}")
            return

        self._write_log(f"Compiled expansion '{addon_name}' -> {result.jar_path}")
        self._notify("info", f"Compiled expansion: {addon_name}")

    def _release_build(self) -> None:
        result = ReleaseManager(self.context).build()
        self._write_log(f"Release built: {result.artifact.name}")
        self._notify("info", f"Release built: {result.artifact.name}")

    def _modpack_build(self) -> None:
        result = ModpackBuilder(self.context).build("extreme_adventure_pack")
        self._write_log(f"Modpack archive: {result.archive_path}")
        self._notify("info", f"Modpack built: {result.archive_path.name}")

    def _on_generate_tool(self, payload: ToolWizardInput) -> None:
        ToolGenerator(self.context).generate(
            tool_name=payload.tool_name,
            material=payload.material,
            durability=payload.durability,
            attack_damage=payload.attack_damage,
            mining_speed=payload.mining_speed,
            tier=payload.tier,
            texture_style=payload.texture_style,
        )
        self._write_log(f"Generated tool bundle: {payload.tool_name}")
        self._notify("info", f"Generated tool bundle: {payload.tool_name}")
        self._set_preview_texture(
            self.context.workspace_root / "assets" / "textures" / "item" / f"{payload.tool_name}.png",
            "item",
        )

    def _generate_ore(self) -> None:
        material = "mythril"
        OreGenerator(self.context).generate(material=material, tier=4, texture_style="metallic")
        self._write_log("Generated ore bundle: mythril")
        self._notify("info", "Generated ore bundle: mythril")
        self._set_preview_texture(
            self.context.workspace_root / "assets" / "textures" / "block" / f"{material}_ore.png",
            "block",
        )

    def _generate_armor(self) -> None:
        material = "mythril"
        ArmorGenerator(self.context).generate(material=material, tier=4, texture_style="metallic")
        self._write_log("Generated armor bundle: mythril")
        self._notify("info", "Generated armor bundle: mythril")
        self._set_preview_texture(
            self.context.workspace_root / "assets" / "textures" / "item" / f"{material}_helmet.png",
            "item",
        )

    def _generate_machine(self) -> None:
        machine_name = "mythril_crusher"
        MachineGenerator(self.context).generate(machine_name=machine_name, material="mythril", texture_style="industrial")
        self._write_log("Generated machine asset: mythril_crusher")
        self._notify("info", "Generated machine asset: mythril_crusher")
        self._set_preview_texture(
            self.context.workspace_root / "assets" / "textures" / "block" / f"{machine_name}.png",
            "block",
        )

    def _generate_block(self) -> None:
        block_name = "mythril_bricks"
        BlockGenerator(self.context).generate(block_name=block_name, material="mythril", texture_style="ancient")
        self._write_log("Generated block asset: mythril_bricks")
        self._notify("info", "Generated block asset: mythril_bricks")
        self._set_preview_texture(
            self.context.workspace_root / "assets" / "textures" / "block" / f"{block_name}.png",
            "block",
        )

    def _generate_material_set(self) -> None:
        generated = ContentPackGenerator(self.context).generate_material_set("mythril", tier=4, style="metallic")
        self._write_log(f"Generated material set (count={len(generated)})")
        self._notify("info", f"Generated material set ({len(generated)} assets)")

    def _export_target(self, target: str) -> None:
        export_pack_command(self.context, target)
        self._write_log(f"Exported {target}")
        self._notify("info", f"Exported {target}")

    def _preview_models(self) -> None:
        self.preview.set_mode("block")
        if self._last_preview_texture:
            self.preview.load_texture(self._last_preview_texture)
        self._write_log("Preview mode: model")
        self._notify("info", "Preview mode switched to model")

    def _preview_animations(self) -> None:
        self.preview.set_mode("animated")
        if self._last_preview_texture:
            self.preview.load_texture(self._last_preview_texture)
        self._write_log("Preview mode: animated")
        self._notify("info", "Preview mode switched to animation")

    def _texture_viewer(self) -> None:
        selected, _ = QFileDialog.getOpenFileName(
            self,
            "Open texture",
            str(self.context.workspace_root / "assets" / "textures"),
            "PNG (*.png)",
        )
        if not selected:
            return
        self._set_preview_texture(Path(selected), "texture")
        candidates = sorted(Path(selected).parent.glob("*.png"))
        self.preview.set_texture_candidates(candidates)
        self._write_log(f"Texture loaded: {selected}")
        self._notify("info", f"Texture loaded: {Path(selected).name}")

    def _open_file_from_browser(self, path: Path) -> None:
        self.code_studio.open_file(path)
        self._switch_tab_by_name(self.STUDIO_TAB_CODE)

    def _open_latest_log(self) -> None:
        candidate = self.context.repo_root / "run" / "logs" / "latest.log"
        if not candidate.exists():
            self._notify("warning", "No latest.log found under run/logs")
            return
        self.code_studio.open_file(candidate)
        self._switch_tab_by_name(self.STUDIO_TAB_CODE)
        self._notify("info", "Opened latest.log")

    def _clear_logs(self) -> None:
        self.log.clear_logs()
        self.notifications.clear()
        self.notifications.addItem(QListWidgetItem("Logs cleared. New events will appear here."))

    def _find_tab_index(self, tab_name: str) -> int | None:
        for index in range(self.studio_tabs.count()):
            if self.studio_tabs.tabText(index) == tab_name:
                return index
        return None

    def _switch_tab_by_name(self, tab_name: str) -> None:
        index = self._find_tab_index(tab_name)
        if index is not None:
            self.studio_tabs.setCurrentIndex(index)

    def _studio_tab_changed(self, index: int) -> None:
        if index < 0:
            return
        self.statusBar().showMessage(f"Studio: {self.studio_tabs.tabText(index)}", 2000)

    def _set_preview_texture(self, texture_path: Path, mode: str) -> None:
        self._last_preview_texture = texture_path
        self.preview.set_mode(mode)
        self.preview.load_texture(texture_path)
        self.preview_tab_renderer.set_mode(mode)
        self.preview_tab_renderer.load_texture(texture_path)

    def _build_preview_controls(self) -> QWidget:
        holder = QWidget()
        row = QHBoxLayout(holder)

        zoom = QSlider(Qt.Orientation.Horizontal)
        zoom.setRange(20, 300)
        zoom.setValue(100)
        zoom.valueChanged.connect(lambda value: self.preview.set_zoom(value / 100.0))

        lighting = QSlider(Qt.Orientation.Horizontal)
        lighting.setRange(20, 200)
        lighting.setValue(100)
        lighting.valueChanged.connect(lambda value: self.preview.set_lighting(value / 100.0))

        next_texture = QPushButton("Next Texture")
        next_texture.clicked.connect(lambda: self.preview.switch_texture(1))
        next_texture.setToolTip("Switch to next texture candidate")

        prev_texture = QPushButton("Prev Texture")
        prev_texture.clicked.connect(lambda: self.preview.switch_texture(-1))
        prev_texture.setToolTip("Switch to previous texture candidate")

        timeline = TimelineWidget()
        timeline.progress_changed.connect(self.preview.set_animation_progress)
        timeline.setToolTip("Scrub animation preview timeline")

        row.addWidget(prev_texture)
        row.addWidget(next_texture)
        row.addWidget(zoom)
        row.addWidget(lighting)
        row.addWidget(timeline)
        return holder

    def _build_editor_tabs(self) -> QTabWidget:
        tabs = QTabWidget()
        tabs.setDocumentMode(True)
        tabs.addTab(MaterialEditor(self.context), "Materials")
        tabs.addTab(MachineEditor(self.context), "Machines")
        tabs.addTab(WeaponEditor(self.context), "Weapons")
        tabs.addTab(WorldgenEditor(self.context), "Worldgen")
        tabs.addTab(QuestEditor(self.context), "Quests")
        tabs.addTab(SkillTreeEditor(self.context), "Skill Trees")

        for editor_name, editor_factory in self.context.plugins.gui_editors.items():
            if callable(editor_factory):
                try:
                    widget = editor_factory(self.context)
                    tabs.addTab(widget, editor_name)
                except Exception as exc:  # noqa: BLE001
                    self._write_log(f"Plugin editor '{editor_name}' failed: {exc}")
                    self._notify("warning", f"Plugin editor failed: {editor_name}")
        return tabs

    def _autosave_tick(self) -> None:
        saved = self.code_studio.autosave_dirty()
        if saved > 0:
            self._autosave_status.setText(f"Autosave: {saved} file(s)")
            self._notify("info", f"Autosaved {saved} open code file(s)")
            return
        self._autosave_status.setText("Autosave: clean")

    def _check_recovery_snapshots(self) -> None:
        autosave_dir = self.context.workspace_root / ".studio" / "autosave"
        if not autosave_dir.exists():
            return
        snapshots = sorted(autosave_dir.glob("*.autosave"))
        if not snapshots:
            return
        message = f"Found {len(snapshots)} autosave snapshot(s). Open autosave folder now?"
        result = QMessageBox.question(self, "Recovery Available", message)
        if result == QMessageBox.StandardButton.Yes:
            self._notify("info", f"Recovery snapshots at {autosave_dir}")

    def closeEvent(self, event) -> None:  # noqa: N802
        if self.code_studio.has_unsaved():
            result = QMessageBox.question(
                self,
                "Unsaved Changes",
                "You have unsaved code changes. Save all before closing?",
                QMessageBox.StandardButton.Yes | QMessageBox.StandardButton.No | QMessageBox.StandardButton.Cancel,
            )
            if result == QMessageBox.StandardButton.Cancel:
                event.ignore()
                return
            if result == QMessageBox.StandardButton.Yes:
                self.code_studio.save_all()
        self._autosave_tick()
        super().closeEvent(event)

    def _apply_theme(self) -> None:
        self.setStyleSheet(
            """
            QMainWindow {
                background-color: #111723;
            }
            QToolBar {
                spacing: 8px;
                padding: 6px;
                background-color: #1a2232;
                border-bottom: 1px solid #283147;
            }
            QTabWidget::pane {
                border: 1px solid #2a344b;
                background: #141c2b;
            }
            QTabBar::tab {
                background: #1c2536;
                color: #d7deed;
                padding: 7px 12px;
                margin-right: 2px;
            }
            QTabBar::tab:selected {
                background: #28334a;
                color: #ffffff;
            }
            QDockWidget::title {
                background: #1e2738;
                text-align: left;
                padding-left: 8px;
                color: #dbe4f7;
            }
            QListWidget, QTreeWidget, QPlainTextEdit, QTextEdit, QLineEdit, QComboBox, QSpinBox {
                background-color: #161f2f;
                color: #e4ebff;
                border: 1px solid #2d3b56;
                selection-background-color: #31548a;
            }
            QPushButton {
                background-color: #25324a;
                color: #f0f4ff;
                border: 1px solid #384e72;
                padding: 5px 10px;
            }
            QPushButton:hover {
                background-color: #304261;
            }
            QLabel#panelHeaderTitle {
                color: #f0f5ff;
                font-size: 14px;
                font-weight: 600;
            }
            QLabel#panelHelpHint {
                color: #9db6e0;
                font-size: 11px;
            }
            """
        )


def launch_gui(workspace_root: Path) -> int:
    app = QApplication(sys.argv)
    app.setApplicationName("EXTREMECRAFT STUDIO")

    window = AssetStudioWindow(workspace_root)
    window.show()
    return app.exec()
