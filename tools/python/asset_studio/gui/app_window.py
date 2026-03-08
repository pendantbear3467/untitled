from __future__ import annotations

import json
import sys
import webbrowser
from pathlib import Path

from PyQt6.QtCore import Qt, QTimer
from PyQt6.QtWidgets import (
    QApplication,
    QCheckBox,
    QComboBox,
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
    QToolButton,
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
from asset_studio.gui.ai_workbench import AIWorkbenchPanel
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
            self.browser.file_open_requested.connect(self._open_path)
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
            self.code_studio.current_file_changed.connect(lambda _path: self._sync_workspace_context())
        if hasattr(self.code_studio, "open_link_requested"):
            self.code_studio.open_link_requested.connect(self._open_path)
        if hasattr(self.code_studio, "current_file"):
            self.browser.set_current_file(self.code_studio.current_file())

        self.ai_workbench = AIWorkbenchPanel(self.session.ai_workbench_service)
        self.ai_workbench.status_message.connect(self._set_cursor_status)
        self.ai_workbench.notifications.connect(lambda message: self._publish_notification("info", "ai", message))
        self.ai_workbench.artifact_apply_requested.connect(self._apply_ai_artifact)

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

        self.gui_studio = self._safe_panel(
            "GUI Studio",
            lambda: GuiStudioPanel(
                self.session.gui_studio_engine,
                relationship_service=self.session.relationship_service,
                migration_service=self.session.document_migration_service,
            ),
        )
        if hasattr(self.gui_studio, "status_message"):
            self.gui_studio.status_message.connect(self._set_cursor_status)
        if hasattr(self.gui_studio, "notifications"):
            self.gui_studio.notifications.connect(lambda message: self._publish_notification("info", "gui", message))
        if hasattr(self.gui_studio, "open_path_requested"):
            self.gui_studio.open_path_requested.connect(self._open_path)

        self.model_studio = self._safe_panel(
            "Model Studio",
            lambda: ModelStudioPanel(
                self.session.model_studio_engine,
                relationship_service=self.session.relationship_service,
                migration_service=self.session.document_migration_service,
            ),
        )
        if hasattr(self.model_studio, "status_message"):
            self.model_studio.status_message.connect(self._set_cursor_status)
        if hasattr(self.model_studio, "notifications"):
            self.model_studio.notifications.connect(lambda message: self._publish_notification("info", "model", message))
        if hasattr(self.model_studio, "open_path_requested"):
            self.model_studio.open_path_requested.connect(self._open_path)
        self.build_run_panel = BuildRunPanel(self.session, self._callbacks())

        self.editor_tabs = self._build_editor_tabs()
        self.main_tabs = self._build_main_tabs()
        self.setCentralWidget(self.main_tabs)

        self.notifications = QListWidget()
        self.notifications.setAlternatingRowColors(True)
        self.notifications.setToolTip("Non-fatal warnings, errors, and workflow updates.")
        self.notifications.setUniformItemSizes(True)

        self._context_actions: list[object] = []
        self._global_toolbar: QToolBar | None = None
        self._context_toolbar: QToolBar | None = None
        self._main_tab_aliases = {
            "Code": "Code",
            "AI Workbench": "AI Workbench",
            "Asset Wizard": "Asset Wizard",
            "Graph Studio": "Graph Studio",
            "Progression": "Progression",
            "GUI Studio": "GUI Studio",
            "Model Studio": "Model Studio",
            "Build/Run": "Build/Run",
            "Data Editors": "Data Editors",
            "Preview": "Preview",
        }

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
        tabs.addTab(self.ai_workbench, "AI Workbench")
        tabs.addTab(self.wizard, "Asset Wizard")
        tabs.addTab(self.visual_builder, "Graph Studio")
        tabs.addTab(self.skilltree_designer, "Progression")
        tabs.addTab(self.gui_studio, "GUI Studio")
        tabs.addTab(self.model_studio, "Model Studio")
        tabs.addTab(self.build_run_panel, "Build/Run")
        tabs.addTab(self.editor_tabs, "Data Editors")
        tabs.addTab(self.preview_tab_renderer, "Preview")
        tabs.currentChanged.connect(self._on_main_tab_changed)
        return tabs

    def _on_main_tab_changed(self, index: int) -> None:
        if index < 0:
            return
        self.statusBar().showMessage(f"Studio: {self.main_tabs.tabText(index)}", 2500)
        self._refresh_context_toolbar()

    def _build_docks(self) -> None:
        self.project_dock = QDockWidget("Project Browser", self)
        self.project_dock.setWidget(self.browser)
        self.project_dock.setMinimumWidth(360)
        self.addDockWidget(Qt.DockWidgetArea.LeftDockWidgetArea, self.project_dock)

        browser_inspector = self.browser.take_inspector_widget() if hasattr(self.browser, "take_inspector_widget") else QWidget()
        self.inspector_dock = QDockWidget("Inspector", self)
        self.inspector_dock.setWidget(browser_inspector)
        self.inspector_dock.setMinimumWidth(340)
        self.addDockWidget(Qt.DockWidgetArea.RightDockWidgetArea, self.inspector_dock)

        preview_host = QWidget()
        preview_layout = QVBoxLayout(preview_host)
        preview_layout.setContentsMargins(6, 6, 6, 6)
        preview_layout.setSpacing(8)
        preview_layout.addWidget(QLabel("Focused Preview"))
        preview_layout.addWidget(self.preview)
        preview_layout.addWidget(self._build_preview_controls())
        self.preview_dock = QDockWidget("Preview", self)
        self.preview_dock.setWidget(preview_host)
        self.preview_dock.setMinimumWidth(360)
        self.addDockWidget(Qt.DockWidgetArea.RightDockWidgetArea, self.preview_dock)
        self.tabifyDockWidget(self.inspector_dock, self.preview_dock)
        self.inspector_dock.raise_()

        self.output_tabs = QTabWidget()
        output_host = QWidget()
        output_layout = QVBoxLayout(output_host)
        output_layout.setContentsMargins(6, 6, 6, 6)
        output_layout.setSpacing(6)
        output_header = QHBoxLayout()
        output_label = QLabel("Run Output")
        output_label.setToolTip("Process logs, runtime stream output, and workflow notifications.")
        output_header.addWidget(output_label)
        output_header.addStretch(1)
        clear_button = QToolButton()
        clear_button.setText("Clear")
        clear_button.setToolTip("Clear in-memory output and notifications.")
        clear_button.clicked.connect(self._clear_logs)
        output_header.addWidget(clear_button)
        output_layout.addLayout(output_header)
        self.output_tabs.addTab(self.log, "Logs")
        self.output_tabs.addTab(self.notifications, "Events")
        self.output_tabs.setToolTip("Runtime output and non-fatal diagnostics")
        output_layout.addWidget(self.output_tabs, 1)
        self.output_dock = QDockWidget("Output", self)
        self.output_dock.setWidget(output_host)
        self.output_dock.setMinimumHeight(210)
        self.addDockWidget(Qt.DockWidgetArea.BottomDockWidgetArea, self.output_dock)

        self.setCorner(Qt.Corner.BottomLeftCorner, Qt.DockWidgetArea.LeftDockWidgetArea)
        self.setCorner(Qt.Corner.BottomRightCorner, Qt.DockWidgetArea.RightDockWidgetArea)

    def _build_toolbar(self) -> None:
        global_toolbar = QToolBar("Studio Global")
        global_toolbar.setMovable(False)
        global_toolbar.setToolButtonStyle(Qt.ToolButtonStyle.ToolButtonTextBesideIcon)
        self.addToolBar(Qt.ToolBarArea.TopToolBarArea, global_toolbar)
        self._global_toolbar = global_toolbar

        self._add_toolbar_action(global_toolbar, "Open", self._open_current_target, "Open a file or studio document for the active page.")
        self._add_toolbar_action(global_toolbar, "Workspace", self._open_project, "Open a different workspace folder.")
        self._add_toolbar_action(global_toolbar, "Save", self._save_workspace, "Save the active page and persist workspace state.")
        self._add_toolbar_action(global_toolbar, "Save All", self._save_all_pages, "Save all supported pages and flush workspace state.")
        global_toolbar.addSeparator()
        self._add_toolbar_action(global_toolbar, "Code", lambda: self._switch_tab("Code"), "Switch to Code Studio.")
        self._add_toolbar_action(global_toolbar, "GUI", lambda: self._switch_tab("GUI Studio"), "Switch to GUI Studio.")
        self._add_toolbar_action(global_toolbar, "Model", lambda: self._switch_tab("Model Studio"), "Switch to Model Studio.")
        self._add_toolbar_action(global_toolbar, "Build/Run", lambda: self._switch_tab("Build/Run"), "Switch to Build/Run Studio.")

        context_toolbar = QToolBar("Workbench Context")
        context_toolbar.setMovable(False)
        context_toolbar.setToolButtonStyle(Qt.ToolButtonStyle.ToolButtonTextBesideIcon)
        self.addToolBar(Qt.ToolBarArea.TopToolBarArea, context_toolbar)
        self._context_toolbar = context_toolbar
        self._refresh_context_toolbar()

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

    def _tab_name(self) -> str:
        if not hasattr(self, "main_tabs") or self.main_tabs.count() == 0:
            return ""
        return self.main_tabs.tabText(self.main_tabs.currentIndex())

    def _context_action_specs(self, tab_name: str) -> list[tuple[str, object, str]]:
        shared_output = [
            ("Latest Log", self._open_latest_log, "Open run/logs/latest.log if it exists."),
            ("Output", self._show_output, "Reveal output and focus logs."),
            ("Events", self._show_notifications, "Reveal output and focus notifications."),
        ]
        per_tab: dict[str, list[tuple[str, object, str]]] = {
            "Code": [
                ("Validate", self._validate_assets, "Run workspace validation and show feedback in output."),
                ("Build Assets", self._compile_assets, "Run the asset build pipeline."),
            ],
            "AI Workbench": [
                ("Validate", self._validate_assets, "Run workspace validation before applying artifacts."),
                ("Build Assets", self._compile_assets, "Build generated or edited assets quickly."),
            ],
            "Asset Wizard": [
                ("Build Assets", self._compile_assets, "Build assets after generation."),
                ("Preview", self._texture_viewer, "Open a texture preview target for quick inspection."),
            ],
            "Graph Studio": [
                ("Validate", self._validate_assets, "Validate graph-generated workspace output."),
                ("Build Assets", self._compile_assets, "Run asset build with graph output included."),
            ],
            "Progression": [
                ("Validate", self._validate_assets, "Validate progression data and outputs."),
                ("Build Assets", self._compile_assets, "Build progression-linked assets."),
            ],
            "GUI Studio": [
                ("Preview", self._preview_models, "Sync preview to GUI/model payload mode."),
                ("Validate", self._validate_assets, "Run workspace validation from GUI authoring context."),
                ("Build Assets", self._compile_assets, "Build assets with current GUI changes."),
            ],
            "Model Studio": [
                ("Preview", self._preview_models, "Switch preview to model payload mode."),
                ("Animate", self._preview_animations, "Switch preview to animated mode."),
                ("Build Assets", self._compile_assets, "Build assets with current model changes."),
            ],
            "Build/Run": [
                ("Validate", self._validate_assets, "Run validation task."),
                ("Build Assets", self._compile_assets, "Run build task."),
                ("Run Client", lambda: self._run_named_configuration("client"), "Run client configuration safely."),
                ("Run Server", lambda: self._run_named_configuration("server"), "Run server configuration safely."),
            ],
            "Data Editors": [
                ("Validate", self._validate_assets, "Validate data editor output."),
                ("Build Assets", self._compile_assets, "Build workspace assets from data edits."),
            ],
            "Preview": [
                ("Texture", self._texture_viewer, "Open texture in preview."),
                ("Model", self._preview_models, "Switch preview to model mode."),
                ("Animate", self._preview_animations, "Switch preview to animated mode."),
            ],
        }
        return per_tab.get(tab_name, []) + shared_output

    def _refresh_context_toolbar(self) -> None:
        if self._context_toolbar is None:
            return
        self._context_toolbar.clear()
        self._context_actions.clear()

        tab_name = self._tab_name()
        specs = self._context_action_specs(tab_name)
        title = self._main_tab_aliases.get(tab_name, "Workbench")
        self._context_toolbar.setWindowTitle(f"{title} Actions")
        for index, (label, callback, description) in enumerate(specs):
            action = self._context_toolbar.addAction(label, self._safe_action(f"context.{label}", callback))
            action.setToolTip(description)
            action.setStatusTip(description)
            self._context_actions.append(action)
            if index in {2, 5} and index < len(specs) - 1:
                self._context_toolbar.addSeparator()

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
        if hasattr(self.gui_studio, "set_services"):
            self.gui_studio.set_services(
                relationship_service=self.session.relationship_service,
                migration_service=self.session.document_migration_service,
            )
        if hasattr(self.model_studio, "set_engine"):
            self.model_studio.set_engine(self.session.model_studio_engine)
        if hasattr(self.model_studio, "set_services"):
            self.model_studio.set_services(
                relationship_service=self.session.relationship_service,
                migration_service=self.session.document_migration_service,
            )
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
        self._sync_workspace_context()

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
        for renderer in self._preview_renderers():
            renderer.set_mode("block")
            if self._last_preview_texture:
                renderer.load_texture(self._last_preview_texture)
        self._write_log("Preview mode: model")

    def _preview_animations(self) -> None:
        for renderer in self._preview_renderers():
            renderer.set_mode("animated")
            if self._last_preview_texture:
                renderer.load_texture(self._last_preview_texture)
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
        self._write_log(f"Texture loaded: {selected}")

    def _open_path(self, path: Path) -> None:
        normalized = path.resolve(strict=False)
        entry = self.session.workspace_index_service.entry(normalized)
        if entry is not None and entry.kind == "gui_source" and hasattr(self.gui_studio, "open_document"):
            self.gui_studio.open_document(normalized)
            self._switch_tab("GUI Studio")
            self._sync_workspace_context()
            return
        if entry is not None and entry.kind == "model_source" and hasattr(self.model_studio, "open_document"):
            self.model_studio.open_document(normalized)
            self._switch_tab("Model Studio")
            self._sync_workspace_context()
            return
        self.code_studio.open_file(normalized)
        self._switch_tab("Code")
        self._sync_workspace_context()

    def _preview_renderers(self) -> list[PreviewRenderer]:
        return [self.preview, self.preview_tab_renderer]

    def _set_preview_mode_choice(self, label: str) -> None:
        mapping = {
            "Auto": None,
            "Source Model": "source_model",
            "Runtime Model": "runtime_model",
            "Source GUI": "source_gui",
            "Runtime GUI": "runtime_gui",
            "Texture / Asset": "texture",
        }
        mode = mapping.get(label)
        for renderer in self._preview_renderers():
            renderer.set_mode_override(mode)

    def _set_preview_variants(self, auto_mode: str, variants: dict[str, dict[str, object]]) -> None:
        for renderer in self._preview_renderers():
            renderer.set_preview_variants(auto_mode, variants)

    def _set_preview_document(
        self,
        mode: str,
        *,
        payload: dict | None = None,
        metadata: dict[str, object] | None = None,
        issues: list[dict[str, str]] | None = None,
        texture_path: Path | None = None,
        selection_id: str | None = None,
    ) -> None:
        self._set_preview_variants(
            mode,
            {
                mode: {
                    "payload": payload,
                    "metadata": metadata or {},
                    "issues": issues or [],
                    "texture_path": texture_path,
                    "selection_id": selection_id,
                }
            },
        )

    def _preview_issues_for_path(self, path: Path | None) -> list[dict[str, str]]:
        if path is None:
            return []
        record = self.session.relationship_service.resolve_path(path)
        if record is None:
            return []
        issues: list[dict[str, str]] = []
        metadata_issues = record.metadata.get("issues") or []
        for issue in metadata_issues[:8]:
            if isinstance(issue, dict):
                issues.append({"severity": str(issue.get("severity", "warning")), "message": str(issue.get("message", ""))})
        return issues

    def _current_code_text(self) -> tuple[Path | None, str]:
        tab = self.code_studio._current_tab() if hasattr(self.code_studio, "_current_tab") else None
        if tab is None:
            return None, ""
        document = self.session.code_editor_service.documents.get(tab.document_id)
        if document is None:
            return tab.path, tab.editor.toPlainText()
        return document.path, document.content

    def _sync_workspace_context(self) -> None:
        current_tab = self.main_tabs.tabText(self.main_tabs.currentIndex()) if hasattr(self, "main_tabs") and self.main_tabs.count() else ""
        if current_tab == "GUI Studio" and getattr(self.gui_studio, "current_document", None) is not None:
            document = self.gui_studio.current_document
            current_path = getattr(self.gui_studio, "current_path", None)
            source_payload = self.session.gui_studio_engine.preview_payload(document)
            runtime_payload = self.session.gui_studio_engine.build_runtime_definition(document)
            runtime_path = self.session.gui_studio_engine.runtime_export_path(document)
            base_metadata = {
                "sourcePath": str(current_path or ""),
                "resourceId": runtime_payload.get("resourceId"),
                "runtimePath": str(runtime_path),
            }
            validation = self.session.gui_studio_engine.validate_document(document)
            issues = [{"severity": issue.severity, "message": issue.message} for issue in validation.issues]
            issues.extend(self._preview_issues_for_path(current_path))
            self._set_preview_variants(
                "source_gui",
                {
                    "source_gui": {
                        "payload": source_payload,
                        "metadata": {**base_metadata, "view": "source"},
                        "issues": issues,
                        "selection_id": getattr(self.gui_studio, "selected_widget_id", None),
                    },
                    "runtime_gui": {
                        "payload": runtime_payload,
                        "metadata": {**base_metadata, "view": "runtime"},
                        "issues": issues,
                        "selection_id": getattr(self.gui_studio, "selected_widget_id", None),
                    },
                },
            )
            self.ai_workbench.set_context(
                current_path=current_path,
                current_text=json.dumps(runtime_payload, indent=2),
                selection=getattr(self.gui_studio, "selected_widget_id", "") or "",
                preview_payload=runtime_payload,
                relationship_context=self.session.relationship_service.inspector_payload(current_path) if current_path is not None else {},
            )
            return
        if current_tab == "Model Studio" and getattr(self.model_studio, "current_document", None) is not None:
            document = self.model_studio.current_document
            current_path = getattr(self.model_studio, "current_path", None)
            source_payload = self.session.model_studio_engine.preview_payload(document)
            runtime_payload = self.session.model_studio_engine.build_runtime_definition(document)
            runtime_path = self.session.model_studio_engine.runtime_export_path(document)
            base_metadata = {
                "sourcePath": str(current_path or ""),
                "resourceId": runtime_payload.get("resourceId"),
                "runtimePath": str(runtime_path),
            }
            validation = self.session.model_studio_engine.validate_document(document)
            issues = [{"severity": issue.severity, "message": issue.message} for issue in validation.issues]
            issues.extend(self._preview_issues_for_path(current_path))
            self._set_preview_variants(
                "source_model",
                {
                    "source_model": {
                        "payload": source_payload,
                        "metadata": {**base_metadata, "view": "source"},
                        "issues": issues,
                        "selection_id": getattr(self.model_studio, "selected_cube_id", None),
                    },
                    "runtime_model": {
                        "payload": runtime_payload,
                        "metadata": {**base_metadata, "view": "runtime"},
                        "issues": issues,
                        "selection_id": getattr(self.model_studio, "selected_cube_id", None),
                    },
                },
            )
            self.ai_workbench.set_context(
                current_path=current_path,
                current_text=json.dumps(runtime_payload, indent=2),
                selection=getattr(self.model_studio, "selected_cube_id", "") or "",
                preview_payload=runtime_payload,
                relationship_context=self.session.relationship_service.inspector_payload(current_path) if current_path is not None else {},
            )
            return
        current_path, current_text = self._current_code_text()
        relationship_context = self.session.relationship_service.inspector_payload(current_path) if current_path is not None else {}
        self.ai_workbench.set_context(current_path=current_path, current_text=current_text, preview_payload=None, relationship_context=relationship_context)
        if current_path is None:
            return
        entry = self.session.workspace_index_service.entry(current_path)
        preview_payload = None
        if current_path.suffix.lower() == ".json":
            try:
                loaded = json.loads(current_text)
            except json.JSONDecodeError:
                loaded = None
            if isinstance(loaded, dict):
                preview_payload = loaded
        issues = self._preview_issues_for_path(current_path)
        if current_path.suffix.lower() == ".png" or (entry is not None and entry.kind == "texture_asset"):
            self._set_preview_variants(
                "texture",
                {
                    "texture": {
                        "texture_path": current_path,
                        "metadata": {"sourcePath": str(current_path), "view": "texture"},
                        "issues": issues,
                    }
                },
            )
            return
        if entry is not None and entry.kind == "gui_runtime":
            self._set_preview_variants(
                "runtime_gui",
                {
                    "runtime_gui": {
                        "payload": preview_payload,
                        "metadata": {"sourcePath": str(current_path), "view": "runtime"},
                        "issues": issues,
                    }
                },
            )
            return
        if entry is not None and entry.kind == "gui_source":
            self._set_preview_variants(
                "source_gui",
                {
                    "source_gui": {
                        "payload": preview_payload,
                        "metadata": {"sourcePath": str(current_path), "view": "source"},
                        "issues": issues,
                    }
                },
            )
            return
        if entry is not None and entry.kind in {"model_runtime", "item_model", "block_model"}:
            self._set_preview_variants(
                "runtime_model",
                {
                    "runtime_model": {
                        "payload": preview_payload,
                        "metadata": {"sourcePath": str(current_path), "view": "runtime"},
                        "issues": issues,
                    }
                },
            )
            return
        if entry is not None and entry.kind == "model_source":
            self._set_preview_variants(
                "source_model",
                {
                    "source_model": {
                        "payload": preview_payload,
                        "metadata": {"sourcePath": str(current_path), "view": "source"},
                        "issues": issues,
                    }
                },
            )

    def _apply_ai_artifact(self, artifact) -> None:
        if getattr(artifact, "validation_blockers", None):
            blocker_text = "; ".join(str(message) for message in artifact.validation_blockers)
            self._publish_notification("warning", "ai", f"Apply blocked by validation: {blocker_text}")
            self._show_notifications()
            return
        if artifact.apply_kind == "replace-current":
            if self.code_studio.apply_text_to_current(artifact.candidate_content):
                self._switch_tab("Code")
                self._sync_workspace_context()
            return
        if artifact.target_kind == "gui" and artifact.candidate_payload is not None and hasattr(self.gui_studio, "load_draft_payload"):
            self.gui_studio.load_draft_payload(artifact.candidate_payload)
            self._switch_tab("GUI Studio")
            self._sync_workspace_context()
            return
        if artifact.target_kind == "model" and artifact.candidate_payload is not None and hasattr(self.model_studio, "load_draft_payload"):
            self.model_studio.load_draft_payload(artifact.candidate_payload)
            self._switch_tab("Model Studio")
            self._sync_workspace_context()
            return
        language = "java" if artifact.target_kind == "java" else "json" if artifact.target_kind in {"gui", "model", "json"} else "text"
        self.code_studio.open_generated_content(artifact.candidate_content, language=language, title=artifact.title)
        self._switch_tab("Code")
        self._sync_workspace_context()

    def _open_latest_log(self) -> None:
        latest = self.session.latest_log_path()
        if latest is None or not latest.exists():
            self._publish_notification("warning", "logs", "No latest log found for this workspace or runtime session")
            self._show_notifications()
            return
        self._open_path(latest)
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
        candidates = sorted(texture_path.parent.glob("*.png")) if texture_path.parent.exists() else []
        for renderer in self._preview_renderers():
            renderer.set_texture_candidates(candidates)
        self._set_preview_variants(
            mode,
            {
                mode: {
                    "texture_path": texture_path,
                    "metadata": {"sourcePath": str(texture_path), "view": mode},
                    "issues": self._preview_issues_for_path(texture_path),
                }
            },
        )

    def _set_preview_zoom(self, value: int) -> None:
        zoom = value / 100.0
        for renderer in self._preview_renderers():
            renderer.set_zoom(zoom)

    def _set_preview_lighting(self, value: int) -> None:
        lighting = value / 100.0
        for renderer in self._preview_renderers():
            renderer.set_lighting(lighting)

    def _toggle_preview_rotation(self, checked: bool) -> None:
        for renderer in self._preview_renderers():
            renderer.set_auto_rotate(checked)

    def _set_preview_preset(self, preset: str) -> None:
        for renderer in self._preview_renderers():
            renderer.set_view_preset(preset)

    def _reset_preview_camera(self) -> None:
        for renderer in self._preview_renderers():
            renderer.reset_camera()

    def _switch_preview_texture(self, step: int) -> None:
        for renderer in self._preview_renderers():
            renderer.switch_texture(step)

    def _set_preview_animation_progress(self, progress: float) -> None:
        for renderer in self._preview_renderers():
            renderer.set_animation_progress(progress)

    def _build_preview_controls(self) -> QWidget:
        holder = QWidget()
        layout = QVBoxLayout(holder)
        layout.setContentsMargins(0, 0, 0, 0)
        layout.setSpacing(6)

        top_row = QHBoxLayout()
        camera_row = QHBoxLayout()
        timeline_row = QHBoxLayout()

        self.preview_mode_selector = QComboBox()
        self.preview_mode_selector.addItems(["Auto", "Source Model", "Runtime Model", "Source GUI", "Runtime GUI", "Texture / Asset"])
        self.preview_mode_selector.setToolTip("Switch the preview between source/runtime/gui/model/texture variants when the current context provides them.")
        self.preview_mode_selector.currentTextChanged.connect(self._set_preview_mode_choice)

        rotate_toggle = QCheckBox("Auto Rotate")
        rotate_toggle.setToolTip("Enable or disable automatic rotation. Off by default for manual inspection.")
        rotate_toggle.toggled.connect(self._toggle_preview_rotation)

        zoom = QSlider(Qt.Orientation.Horizontal)
        zoom.setRange(20, 300)
        zoom.setValue(100)
        zoom.setToolTip("Zoom preview")
        zoom.valueChanged.connect(self._set_preview_zoom)

        lighting = QSlider(Qt.Orientation.Horizontal)
        lighting.setRange(20, 200)
        lighting.setValue(100)
        lighting.setToolTip("Adjust preview lighting")
        lighting.valueChanged.connect(self._set_preview_lighting)

        prev_texture = QPushButton("Prev Texture")
        prev_texture.setToolTip("Cycle to previous texture")
        prev_texture.clicked.connect(lambda: self._switch_preview_texture(-1))

        next_texture = QPushButton("Next Texture")
        next_texture.setToolTip("Cycle to next texture")
        next_texture.clicked.connect(lambda: self._switch_preview_texture(1))

        front = QPushButton("Front")
        front.clicked.connect(lambda: self._set_preview_preset("front"))
        side = QPushButton("Side")
        side.clicked.connect(lambda: self._set_preview_preset("side"))
        top = QPushButton("Top")
        top.clicked.connect(lambda: self._set_preview_preset("top"))
        iso = QPushButton("Iso")
        iso.clicked.connect(lambda: self._set_preview_preset("isometric"))
        reset = QPushButton("Reset")
        reset.clicked.connect(self._reset_preview_camera)

        timeline = TimelineWidget()
        timeline.setToolTip("Scrub animation preview")
        timeline.progress_changed.connect(self._set_preview_animation_progress)

        top_row.addWidget(QLabel("Mode"))
        top_row.addWidget(self.preview_mode_selector)
        top_row.addWidget(rotate_toggle)
        top_row.addWidget(QLabel("Zoom"))
        top_row.addWidget(zoom)
        top_row.addWidget(QLabel("Light"))
        top_row.addWidget(lighting)

        camera_row.addWidget(QLabel("View"))
        camera_row.addWidget(front)
        camera_row.addWidget(side)
        camera_row.addWidget(top)
        camera_row.addWidget(iso)
        camera_row.addWidget(reset)
        camera_row.addSpacing(10)
        camera_row.addWidget(prev_texture)
        camera_row.addWidget(next_texture)
        camera_row.addStretch(1)

        timeline_row.addWidget(QLabel("Timeline"))
        timeline_row.addWidget(timeline, 1)

        layout.addLayout(top_row)
        layout.addLayout(camera_row)
        layout.addLayout(timeline_row)
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
            QToolBar { spacing: 6px; padding: 4px 6px; background: #1a2232; border-bottom: 1px solid #283147; }
            QToolBar QToolButton { margin: 1px; padding: 4px 8px; }
            QTabWidget::pane { border: 1px solid #2a344b; background: #141c2b; }
            QTabBar::tab { background: #1c2536; color: #d7deed; padding: 7px 12px; margin-right: 2px; }
            QTabBar::tab:selected { background: #28334a; color: #ffffff; }
            QDockWidget::title { background: #1e2738; color: #dbe4f7; text-align: left; padding-left: 8px; }
            QHeaderView::section { background: #1e2738; color: #dbe4f7; border: 1px solid #2d3b56; padding: 4px 6px; }
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











