from __future__ import annotations

import sys
import webbrowser
from pathlib import Path

from PyQt6.QtCore import Qt, QTimer
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
    QSlider,
    QStatusBar,
    QTabWidget,
    QToolBar,
    QVBoxLayout,
    QWidget,
)

from asset_studio.blockbench.bbmodel_importer import import_bbmodel
from asset_studio.core.studio_session import StudioSession
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
from asset_studio.repair.repair_engine import RepairEngine


class AssetStudioWindow(QMainWindow):
    def __init__(self, workspace_root: Path) -> None:
        super().__init__()
        self.session = StudioSession.open(workspace_root=workspace_root, repo_root=Path.cwd())
        self.workspace_manager = self.session.workspace_manager
        self.context = self.session.context
        self._observed_notifications = 0
        self._running_tasks: dict[str, object] = {}
        self._running_processes: dict[str, object] = {}
        self._console_cache: set[str] = set()

        self.setWindowTitle("EXTREMECRAFT STUDIO")
        self.resize(1520, 930)
        self.setStatusBar(QStatusBar(self))
        self._cursor_status = QLabel("Ready")
        self._autosave_status = QLabel("Recovery: active")
        self.statusBar().addPermanentWidget(self._cursor_status)
        self.statusBar().addPermanentWidget(self._autosave_status)

        self.log = self._create_editor("console_panel", ConsolePanel)
        self.browser = self._create_editor("project_browser", ProjectBrowser)
        if hasattr(self.browser, "bind_session"):
            self.browser.bind_session(self.session)
        self.browser.load_workspace(self.context.workspace_root)
        if hasattr(self.browser, "file_open_requested"):
            self.browser.file_open_requested.connect(self._open_file_from_browser)
        if hasattr(self.browser, "notifications"):
            self.browser.notifications.connect(lambda message: self._publish_notification("warning", "browser", message))

        self.preview = self._create_editor("preview_renderer", PreviewRenderer)
        self.preview_tab_renderer = self._create_editor("preview_renderer", PreviewRenderer)
        self._last_preview_texture: Path | None = None

        self.code_studio = CodeStudioPanel(self.session, self.context.workspace_root)
        self.code_studio.status_message.connect(self._set_cursor_status)
        self.code_studio.notifications.connect(lambda message: self._publish_notification("info", "code", message))
        if hasattr(self.code_studio, "current_file_changed"):
            self.code_studio.current_file_changed.connect(self.browser.set_current_file)
        if hasattr(self.code_studio, "current_file"):
            self.browser.set_current_file(self.code_studio.current_file())

        self.wizard = self._create_editor("asset_wizard", AssetWizardPanel)
        self.wizard.generate_tool_requested.connect(self._on_generate_tool)
        self.visual_builder = self._safe_panel("Visual Builder", lambda: self._create_editor("visual_builder", GraphEditor, context_required=True))
        if hasattr(self.visual_builder, "graph_log"):
            self.visual_builder.graph_log.connect(self._write_log)

        self.skilltree_designer = self._safe_panel(
            "Progression Studio",
            lambda: self._create_editor("progression_studio", SkillTreeDesigner, context_required=True),
        )
        if hasattr(self.skilltree_designer, "log_requested"):
            self.skilltree_designer.log_requested.connect(self._write_log)

        self.gui_studio = self._safe_panel("GUI Studio", lambda: GuiStudioPanel(self.session.gui_studio_engine))
        if hasattr(self.gui_studio, "status_message"):
            self.gui_studio.status_message.connect(self._set_cursor_status)
        if hasattr(self.gui_studio, "notifications"):
            self.gui_studio.notifications.connect(lambda message: self._publish_notification("info", "gui", message))

        self.model_studio = self._safe_panel("Model Studio", lambda: ModelStudioPanel(self.session.model_studio_engine))
        if hasattr(self.model_studio, "status_message"):
            self.model_studio.status_message.connect(self._set_cursor_status)
        if hasattr(self.model_studio, "notifications"):
            self.model_studio.notifications.connect(lambda message: self._publish_notification("info", "model", message))
        self.build_run_panel = BuildRunPanel(self.session, self._callbacks())

        self.editor_tabs = self._build_editor_tabs()
        self.main_tabs = self._build_main_tabs()
        self.setCentralWidget(self.main_tabs)

        self.notifications = QListWidget()
        self.notifications.setAlternatingRowColors(True)
        self.notifications.setToolTip("Non-fatal warnings, errors, and workflow updates.")

        self._build_docks()
        self._build_toolbar()
        self.menu_actions = build_menu_bar(self, self._callbacks())
        self._apply_theme()

        self._sync_timer = QTimer(self)
        self._sync_timer.setInterval(500)
        self._sync_timer.timeout.connect(self._sync_runtime_surfaces)
        self._sync_timer.start()

        self._show_recovery_hint()
        self._write_log(f"Workspace loaded: {self.context.workspace_root}")

    def _create_editor(self, editor_id: str, fallback_type, *, context_required: bool = False):
        try:
            return self.session.instantiate_editor(editor_id)
        except Exception as exc:  # noqa: BLE001
            self._write_log(f"Falling back to built-in editor '{editor_id}': {exc}")
            if context_required:
                return fallback_type(self.context)
            return fallback_type()

    def _safe_panel(self, label: str, factory):
        try:
            return factory()
        except Exception as exc:  # noqa: BLE001
            self.session.crash_guard.capture_exception(f"panel.{label}", exc)
            holder = QWidget()
            layout = QVBoxLayout(holder)
            layout.addWidget(QLabel(f"{label} failed to load."))
            detail = QLabel(str(exc))
            detail.setWordWrap(True)
            layout.addWidget(detail)
            self._publish_notification("error", "ui", f"{label} failed to load: {exc}")
            return holder

    def _build_main_tabs(self) -> QTabWidget:
        tabs = QTabWidget()
        tabs.setDocumentMode(True)
        tabs.addTab(self.code_studio, "Code")
        tabs.addTab(self.wizard, "Asset Wizard")
        tabs.addTab(self.visual_builder, "Graph Studio")
        tabs.addTab(self.skilltree_designer, "Progression")
        tabs.addTab(self.gui_studio, "GUI Studio")
        tabs.addTab(self.model_studio, "Model Studio")
        tabs.addTab(self.build_run_panel, "Build/Run")
        tabs.addTab(self.editor_tabs, "Data Editors")
        tabs.addTab(self.preview_tab_renderer, "Preview")
        tabs.currentChanged.connect(lambda i: self.statusBar().showMessage(f"Studio: {tabs.tabText(i)}", 2500) if i >= 0 else None)
        return tabs

    def _build_docks(self) -> None:
        self.project_dock = QDockWidget("Project Browser", self)
        self.project_dock.setWidget(self.browser)
        self.addDockWidget(Qt.DockWidgetArea.LeftDockWidgetArea, self.project_dock)

        preview_host = QWidget()
        preview_layout = QVBoxLayout(preview_host)
        preview_layout.setContentsMargins(6, 6, 6, 6)
        preview_layout.addWidget(self.preview)
        preview_layout.addWidget(self._build_preview_controls())
        self.preview_dock = QDockWidget("Preview", self)
        self.preview_dock.setWidget(preview_host)
        self.addDockWidget(Qt.DockWidgetArea.RightDockWidgetArea, self.preview_dock)

        self.output_tabs = QTabWidget()
        self.output_tabs.addTab(self.log, "Console")
        self.output_tabs.addTab(self.notifications, "Notifications")
        self.output_tabs.setToolTip("Runtime output and non-fatal diagnostics")
        self.output_dock = QDockWidget("Output", self)
        self.output_dock.setWidget(self.output_tabs)
        self.addDockWidget(Qt.DockWidgetArea.BottomDockWidgetArea, self.output_dock)

    def _build_toolbar(self) -> None:
        toolbar = QToolBar("Studio")
        toolbar.setMovable(False)
        toolbar.setToolButtonStyle(Qt.ToolButtonStyle.ToolButtonTextBesideIcon)
        self.addToolBar(Qt.ToolBarArea.TopToolBarArea, toolbar)
        self._add_toolbar_action(toolbar, "Open", self._open_current_target, "Open a file or studio document for the active page.")
        self._add_toolbar_action(toolbar, "Workspace", self._open_project, "Open a different workspace folder.")
        self._add_toolbar_action(toolbar, "Save", self._save_workspace, "Save the active page and persist workspace state.")
        self._add_toolbar_action(toolbar, "Save All", self._save_all_pages, "Save all supported pages and flush workspace state.")
        toolbar.addSeparator()
        self._add_toolbar_action(toolbar, "Code", lambda: self._switch_tab("Code"), "Switch to Code Studio.")
        self._add_toolbar_action(toolbar, "Progression", lambda: self._switch_tab("Progression"), "Switch to Progression Studio.")
        self._add_toolbar_action(toolbar, "GUI", lambda: self._switch_tab("GUI Studio"), "Switch to GUI Studio.")
        self._add_toolbar_action(toolbar, "Model", lambda: self._switch_tab("Model Studio"), "Switch to Model Studio.")
        self._add_toolbar_action(toolbar, "Build/Run", lambda: self._switch_tab("Build/Run"), "Switch to Build/Run Studio.")
        toolbar.addSeparator()
        self._add_toolbar_action(toolbar, "Validate", self._validate_assets, "Run workspace validation and show feedback in Output/Notifications.")
        self._add_toolbar_action(toolbar, "Build Assets", self._compile_assets, "Run the asset build pipeline.")
        self._add_toolbar_action(toolbar, "Run Client", lambda: self._run_named_configuration("client"), "Run the configured client process safely through RunService.")
        self._add_toolbar_action(toolbar, "Run Server", lambda: self._run_named_configuration("server"), "Run the configured server process safely through RunService.")
        toolbar.addSeparator()
        self._add_toolbar_action(toolbar, "Latest Log", self._open_latest_log, "Open run/logs/latest.log if it exists.")
        self._add_toolbar_action(toolbar, "Clear Logs", self._clear_logs, "Clear console and notification history after confirmation.")
        self._add_toolbar_action(toolbar, "Output", self._show_output, "Reveal the output dock and focus console logs.")
        self._add_toolbar_action(toolbar, "Notifications", self._show_notifications, "Reveal the output dock and focus notifications.")

    def _add_toolbar_action(self, toolbar: QToolBar, label: str, callback, description: str) -> None:
        action = toolbar.addAction(label, self._safe_action(label, callback))
        action.setToolTip(description)
        action.setStatusTip(description)

    def _safe_action(self, name: str, callback):
        def runner(*_args, **_kwargs):
            try:
                return callback()
            except Exception as exc:  # noqa: BLE001
                self.session.crash_guard.capture_exception(f"action.{name}", exc)
                message = f"{name} failed: {exc}"
                self.session.log_model.append("studio", "error", message, stream="stderr")
                self._publish_notification("error", "ui", message)
                self._write_log(message)
                self._show_output()
                return None

        return runner

    def _callbacks(self):
        callbacks = {
            "new_project": self._new_project,
            "open_project": self._open_project,
            "import_blockbench": self._import_blockbench,
            "export_assets": self._export_assets,
            "save_workspace": self._save_workspace,
            "undo": lambda: self._write_log("Undo requested"),
            "redo": lambda: self._write_log("Redo requested"),
            "generate_tool": lambda: self._switch_tab("Asset Wizard"),
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
            "compile_assets": self._compile_assets,
            "compile_expansion": self._compile_expansion,
            "validate_assets": self._validate_assets,
            "export_resourcepack": lambda: self._export_target("resourcepack"),
            "export_datapack": lambda: self._export_target("datapack"),
            "release_build": self._release_build,
            "modpack_build": self._modpack_build,
            "run_client": lambda: self._run_named_configuration("client"),
            "run_server": lambda: self._run_named_configuration("server"),
            "latest_log": self._open_latest_log,
            "clear_logs": self._clear_logs,
            "preview_models": self._preview_models,
            "preview_animations": self._preview_animations,
            "texture_viewer": self._texture_viewer,
            "documentation": lambda: webbrowser.open("https://github.com/pendantbear3467/untitled/tree/main/docs"),
            "github": lambda: webbrowser.open("https://github.com/pendantbear3467/untitled"),
        }
        return {key: self._safe_action(key, callback) for key, callback in callbacks.items()}

    def _build_editor_tabs(self) -> QTabWidget:
        tabs = QTabWidget()
        tabs.setDocumentMode(True)
        fallback_types = {
            "material_editor": MaterialEditor,
            "machine_editor": MachineEditor,
            "weapon_editor": WeaponEditor,
            "worldgen_editor": WorldgenEditor,
            "quest_editor": QuestEditor,
            "skill_tree_editor": SkillTreeEditor,
        }
        for descriptor in self.session.editor_registry.all(area="content_editor"):
            try:
                if descriptor.id in fallback_types:
                    widget = self._create_editor(descriptor.id, fallback_types[descriptor.id], context_required=True)
                else:
                    widget = self.session.instantiate_editor(descriptor.id)
                tabs.addTab(widget, descriptor.label)
            except Exception as exc:  # noqa: BLE001
                self._write_log(f"Editor '{descriptor.label}' failed: {exc}")
        return tabs

    def _execute_command(self, command_id: str):
        result = self.session.command_registry.dispatch(command_id)
        self._write_log(result.message)
        if not result.success:
            self._publish_notification("error", command_id, result.message)
        return result

    def _run_task(self, name: str, fn, *args):
        try:
            handle = self.session.task_service.submit(name, fn, *args)
        except Exception as exc:  # noqa: BLE001
            self.session.crash_guard.capture_exception(f"task.{name}", exc)
            self._publish_notification("error", "task", f"{name} failed to start: {exc}")
            self._show_notifications()
            return
        self._running_tasks[handle.task_id] = handle
        self._publish_notification("info", "task", f"Queued: {name}")

    def _sync_runtime_surfaces(self) -> None:
        recent = self.session.notification_service.recent(limit=30)
        self.notifications.clear()
        for notification in reversed(recent):
            self.notifications.addItem(
                QListWidgetItem(
                    f"[{notification.severity.upper()}] {notification.source}: {notification.message}"
                )
            )

        for entry in self.session.log_model.tail(120):
            text = f"[{entry.level}] {entry.source}: {entry.message}"
            if text not in self._console_cache:
                self.log.append_line(text)
                self._console_cache.add(text)

        finished: list[str] = []
        for task_id, handle in self._running_tasks.items():
            if not handle.done():
                continue
            result = handle.result()
            level = self._task_severity(result)
            self._publish_notification(level, "task", result.message)
            self._write_log(result.message)
            self._record_task_report(result)
            finished.append(task_id)
        for task_id in finished:
            self._running_tasks.pop(task_id, None)

        finished_processes: list[str] = []
        for task_id, handle in self._running_processes.items():
            if not handle.done():
                continue
            result = handle.result
            if result is not None:
                level = self._task_severity(result)
                self._publish_notification(level, result.name, result.message)
                self.statusBar().showMessage(result.message, 5000)
                self._record_task_report(result)
            finished_processes.append(task_id)
        for task_id in finished_processes:
            self._running_processes.pop(task_id, None)

    def _write_log(self, message: str) -> None:
        if hasattr(self.log, "append_line"):
            self.log.append_line(message)
        self.statusBar().showMessage(message, 4000)

    def _publish_notification(self, severity: str, source: str, message: str) -> None:
        self.session.notification_service.publish(severity, source, message)

    def _set_cursor_status(self, message: str) -> None:
        self._cursor_status.setText(message)

    def _task_severity(self, result) -> str:
        if getattr(result, "cancelled", False):
            return "warning"
        return "info" if getattr(result, "success", False) else "error"

    def _record_task_report(self, result) -> None:
        report = getattr(result, "report", None)
        if report is None:
            return
        for artifact in getattr(report, "artifacts", []):
            if getattr(artifact, "path", None) is not None:
                self._write_log(f"[{report.category}] {artifact.kind}: {artifact.path}")
        for issue in getattr(report, "issues", []):
            prefix = str(getattr(issue, "path", None) or report.operation)
            self._write_log(f"[{issue.severity}] {prefix}: {issue.message}")
        if getattr(report, "category", "") in {"build", "export", "compile", "validation", "release", "modpack"} and hasattr(self.browser, "refresh_view"):
            self.browser.refresh_view()

    def _show_output(self) -> None:
        self.output_dock.show()
        self.output_dock.raise_()
        self.output_tabs.setCurrentIndex(0)

    def _show_notifications(self) -> None:
        self.output_dock.show()
        self.output_dock.raise_()
        self.output_tabs.setCurrentIndex(1)

    def _switch_tab(self, title: str) -> None:
        for idx in range(self.main_tabs.count()):
            if self.main_tabs.tabText(idx) == title:
                self.main_tabs.setCurrentIndex(idx)
                return

    def _rebind_workspace(self) -> None:
        self.workspace_manager = self.session.workspace_manager
        self.context = self.session.context
        if hasattr(self.browser, "bind_session"):
            self.browser.bind_session(self.session)
        self.browser.load_workspace(self.context.workspace_root)
        if hasattr(self.code_studio, "set_session"):
            self.code_studio.set_session(self.session)
        if hasattr(self.code_studio, "set_workspace_root"):
            self.code_studio.set_workspace_root(self.context.workspace_root)
        else:
            self.code_studio.workspace_root = self.context.workspace_root
        if hasattr(self.code_studio, "current_file"):
            self.browser.set_current_file(self.code_studio.current_file())
        if hasattr(self.build_run_panel, "set_session"):
            self.build_run_panel.set_session(self.session)
        if hasattr(self.gui_studio, "set_engine"):
            self.gui_studio.set_engine(self.session.gui_studio_engine)
        if hasattr(self.model_studio, "set_engine"):
            self.model_studio.set_engine(self.session.model_studio_engine)
        if hasattr(self.visual_builder, "set_context"):
            self.visual_builder.set_context(self.context)
        if hasattr(self.skilltree_designer, "set_context"):
            self.skilltree_designer.set_context(self.context)
        self.editor_tabs = self._build_editor_tabs()
        for idx in range(self.main_tabs.count()):
            if self.main_tabs.tabText(idx) == "Data Editors":
                self.main_tabs.removeTab(idx)
                self.main_tabs.insertTab(idx, self.editor_tabs, "Data Editors")
                break

    def _new_project(self) -> None:
        self.session.reload_workspace(self.context.workspace_root)
        self._rebind_workspace()
        self._write_log("New project initialized")

    def _open_project(self) -> None:
        selected = QFileDialog.getExistingDirectory(self, "Open workspace", str(self.context.workspace_root))
        if not selected:
            self._publish_notification("warning", "workspace", "Workspace open cancelled")
            return
        self.session.reload_workspace(Path(selected))
        self._rebind_workspace()
        self._write_log(f"Opened workspace: {selected}")

    def _save_workspace(self) -> None:
        current_page = self._current_studio_page()
        if current_page is not None and hasattr(current_page, "save_current"):
            current_page.save_current()
        elif self.main_tabs.tabText(self.main_tabs.currentIndex()) == "Code":
            self.code_studio.save_current()
        self._execute_command("workspace.save")
        self._publish_notification("info", "workspace", "Workspace save requested")

    def _save_all_pages(self) -> None:
        self.code_studio.save_all()
        for page in [self.gui_studio, self.model_studio]:
            if hasattr(page, "save_all"):
                page.save_all()
        self._execute_command("workspace.save")
        self._publish_notification("info", "workspace", "Save all completed")

    def _open_current_target(self) -> None:
        current_page = self._current_studio_page()
        if current_page is not None and hasattr(current_page, "open_document_dialog"):
            current_page.open_document_dialog()
            return
        if self.main_tabs.tabText(self.main_tabs.currentIndex()) == "Code":
            self.code_studio.open_file_dialog()
            return
        self._publish_notification("warning", "shell", "Open is not available for this page")

    def _current_studio_page(self):
        current_name = self.main_tabs.tabText(self.main_tabs.currentIndex())
        if current_name == "GUI Studio":
            return self.gui_studio
        if current_name == "Model Studio":
            return self.model_studio
        return None

    def _import_blockbench(self) -> None:
        selected, _ = QFileDialog.getOpenFileName(self, "Import Blockbench model", "", "Blockbench (*.bbmodel)")
        if not selected:
            return
        result = import_bbmodel(Path(selected), self.context)
        self._write_log(f"Imported Blockbench model: {result}")
        self._set_preview_texture(self.context.workspace_root / "assets" / "textures" / "block" / f"{result}.png", "block")

    def _export_assets(self) -> None:
        self._run_task("Export ResourcePack", self.session.build_service.export_pack, "resourcepack")
        self._run_task("Export Datapack", self.session.build_service.export_pack, "datapack")

    def _validate_assets(self) -> None:
        self._run_task("Validate Workspace", self.session.build_service.validate_workspace)

    def _repair_assets(self) -> None:
        self._run_task("Repair Assets", lambda: RepairEngine(self.context).repair())

    def _compile_assets(self) -> None:
        self._run_task("Build Assets", self.session.build_service.build_workspace, "assets")

    def _compile_expansion(self) -> None:
        addons = sorted((self.context.workspace_root / "addons").glob("*"))
        if not addons:
            self._write_log("No addons found. Create one via: assetstudio sdk init-addon <name>")
            return
        self._run_task("Compile Expansion", self.session.build_service.compile_expansion, addons[0].name)

    def _release_build(self) -> None:
        self._run_task("Build Release", self.session.build_service.build_release)

    def _modpack_build(self) -> None:
        self._run_task("Build Modpack", self.session.build_service.build_modpack, "extreme_adventure_pack")

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
        self._set_preview_texture(self.context.workspace_root / "assets" / "textures" / "item" / f"{payload.tool_name}.png", "item")

    def _generate_ore(self) -> None:
        material = "mythril"
        OreGenerator(self.context).generate(material=material, tier=4, texture_style="metallic")
        self._write_log("Generated ore bundle: mythril")
        self._set_preview_texture(self.context.workspace_root / "assets" / "textures" / "block" / f"{material}_ore.png", "block")

    def _generate_armor(self) -> None:
        material = "mythril"
        ArmorGenerator(self.context).generate(material=material, tier=4, texture_style="metallic")
        self._write_log("Generated armor bundle: mythril")
        self._set_preview_texture(self.context.workspace_root / "assets" / "textures" / "item" / f"{material}_helmet.png", "item")

    def _generate_machine(self) -> None:
        machine_name = "mythril_crusher"
        MachineGenerator(self.context).generate(machine_name=machine_name, material="mythril", texture_style="industrial")
        self._write_log("Generated machine asset: mythril_crusher")
        self._set_preview_texture(self.context.workspace_root / "assets" / "textures" / "block" / f"{machine_name}.png", "block")

    def _generate_block(self) -> None:
        block_name = "mythril_bricks"
        BlockGenerator(self.context).generate(block_name=block_name, material="mythril", texture_style="ancient")
        self._write_log("Generated block asset: mythril_bricks")
        self._set_preview_texture(self.context.workspace_root / "assets" / "textures" / "block" / f"{block_name}.png", "block")

    def _generate_material_set(self) -> None:
        generated = ContentPackGenerator(self.context).generate_material_set("mythril", tier=4, style="metallic")
        self._write_log(f"Generated material set (count={len(generated)})")

    def _export_target(self, target: str) -> None:
        self._run_task(f"Export {target}", self.session.build_service.export_pack, target)

    def _preview_models(self) -> None:
        self.preview.set_mode("block")
        if self._last_preview_texture:
            self.preview.load_texture(self._last_preview_texture)
        self._write_log("Preview mode: model")

    def _preview_animations(self) -> None:
        self.preview.set_mode("animated")
        if self._last_preview_texture:
            self.preview.load_texture(self._last_preview_texture)
        self._write_log("Preview mode: animated")

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

    def _open_file_from_browser(self, path: Path) -> None:
        self.code_studio.open_file(path)
        self._switch_tab("Code")

    def _open_latest_log(self) -> None:
        latest = self.session.latest_log_path()
        if latest is None or not latest.exists():
            self._publish_notification("warning", "logs", "No latest log found for this workspace or runtime session")
            self._show_notifications()
            return
        self.code_studio.open_file(latest)
        self._switch_tab("Code")
        self._publish_notification("info", "logs", f"Opened latest log: {latest}")

    def _clear_logs(self) -> None:
        result = QMessageBox.question(
            self,
            "Clear Logs",
            "Clear console output and notification history? This does not delete files on disk.",
            QMessageBox.StandardButton.Yes | QMessageBox.StandardButton.No,
        )
        if result != QMessageBox.StandardButton.Yes:
            self._publish_notification("warning", "logs", "Clear logs cancelled")
            return
        command_result = self._execute_command("logs.clear")
        if hasattr(self.log, "clear_logs"):
            self.log.clear_logs()
        self._console_cache.clear()
        self.notifications.clear()
        if command_result.success:
            self.notifications.addItem(QListWidgetItem("Logs cleared. New messages will appear here."))
            self.statusBar().showMessage("Logs cleared", 3000)
        else:
            self._publish_notification("error", "logs", command_result.message)
            self._show_notifications()

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
        zoom.setToolTip("Zoom preview")
        zoom.valueChanged.connect(lambda value: self.preview.set_zoom(value / 100.0))

        lighting = QSlider(Qt.Orientation.Horizontal)
        lighting.setRange(20, 200)
        lighting.setValue(100)
        lighting.setToolTip("Adjust preview lighting")
        lighting.valueChanged.connect(lambda value: self.preview.set_lighting(value / 100.0))

        next_texture = QPushButton("Next Texture")
        next_texture.setToolTip("Cycle to next texture")
        next_texture.clicked.connect(lambda: self.preview.switch_texture(1))

        prev_texture = QPushButton("Prev Texture")
        prev_texture.setToolTip("Cycle to previous texture")
        prev_texture.clicked.connect(lambda: self.preview.switch_texture(-1))

        timeline = TimelineWidget()
        timeline.setToolTip("Scrub animation preview")
        timeline.progress_changed.connect(self.preview.set_animation_progress)

        row.addWidget(prev_texture)
        row.addWidget(next_texture)
        row.addWidget(zoom)
        row.addWidget(lighting)
        row.addWidget(timeline)
        return holder

    def _show_recovery_hint(self) -> None:
        snapshots = self.session.recovery_service.list_snapshots(document_type="text-document")
        if not snapshots:
            return
        QMessageBox.information(self, "Recovery", f"Detected {len(snapshots)} recovery snapshots from previous sessions.")

    def _run_named_configuration(self, name: str) -> None:
        result = self.session.run_service.run_named(name)
        if hasattr(result, "process") and hasattr(result, "task_id"):
            self._running_processes[result.task_id] = result
            self._publish_notification("info", "run", f"Started {name} configuration")
            self._show_output()
            return
        self.session.log_model.append("run", "warning", result.message)
        self._publish_notification("warning", "run", result.message)
        self._show_notifications()

    def closeEvent(self, event) -> None:  # noqa: N802
        if self.code_studio.has_unsaved():
            result = QMessageBox.question(
                self,
                "Unsaved Changes",
                "Save all open code files before closing?",
                QMessageBox.StandardButton.Yes | QMessageBox.StandardButton.No | QMessageBox.StandardButton.Cancel,
            )
            if result == QMessageBox.StandardButton.Cancel:
                event.ignore()
                return
            if result == QMessageBox.StandardButton.Yes:
                self._save_all_pages()
        self._execute_command("workspace.save")
        self.session.shutdown()
        super().closeEvent(event)

    def _apply_theme(self) -> None:
        self.setStyleSheet(
            """
            QMainWindow { background: #111723; }
            QToolBar { spacing: 8px; padding: 6px; background: #1a2232; border-bottom: 1px solid #283147; }
            QTabWidget::pane { border: 1px solid #2a344b; background: #141c2b; }
            QTabBar::tab { background: #1c2536; color: #d7deed; padding: 7px 12px; margin-right: 2px; }
            QTabBar::tab:selected { background: #28334a; color: #ffffff; }
            QDockWidget::title { background: #1e2738; color: #dbe4f7; text-align: left; padding-left: 8px; }
            QListWidget, QTreeWidget, QPlainTextEdit, QTextEdit, QLineEdit, QComboBox, QSpinBox {
                background-color: #161f2f; color: #e4ebff; border: 1px solid #2d3b56; selection-background-color: #31548a;
            }
            QPushButton { background-color: #25324a; color: #f0f4ff; border: 1px solid #384e72; padding: 5px 10px; }
            QPushButton:hover { background-color: #304261; }
            QLabel#panelHeaderTitle { color: #f0f5ff; font-size: 14px; font-weight: 600; }
            QLabel#panelHelpHint { color: #9db6e0; font-size: 11px; }
            """
        )


def launch_gui(workspace_root: Path) -> int:
    app = QApplication(sys.argv)
    app.setApplicationName("EXTREMECRAFT STUDIO")

    window = AssetStudioWindow(workspace_root)
    window.show()
    return app.exec()


