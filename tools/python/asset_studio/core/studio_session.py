from __future__ import annotations

from pathlib import Path

from asset_studio.ai.workbench_service import AIWorkbenchService
from asset_studio.code.editor_service import EditorService
from asset_studio.core.app_context import StudioAppContext
from asset_studio.core.command_registry import CommandRegistry, StudioCommand
from asset_studio.core.crash_guard import CrashGuard
from asset_studio.core.editor_registry import EditorDescriptor, EditorRegistry
from asset_studio.core.help_registry import HelpEntry, HelpRegistry
from asset_studio.core.notification_service import NotificationService
from asset_studio.core.plugin_service import PluginService
from asset_studio.core.process_service import ProcessService
from asset_studio.core.recovery_service import RecoveryService
from asset_studio.core.task_service import TaskService
from asset_studio.gui_studio.engine import GuiStudioEngine
from asset_studio.model_studio.engine import ModelStudioEngine
from asset_studio.runtime.build_service import BuildService
from asset_studio.runtime.log_model import LogStreamModel
from asset_studio.runtime.run_service import RunService
from asset_studio.runtime.task_results import StudioTaskResult, utc_now
from asset_studio.schema.migration_service import DocumentMigrationService
from asset_studio.workspace.index_service import WorkspaceIndexService
from asset_studio.workspace.relationship_service import RelationshipResolverService
from asset_studio.workspace.workspace_manager import AssetStudioContext, WorkspaceManager


class StudioSession:
    def __init__(self, workspace_manager: WorkspaceManager, context: AssetStudioContext) -> None:
        self.workspace_manager = workspace_manager
        self.context = context
        self.app_context = StudioAppContext(context=context, workspace_manager=workspace_manager)

        self.help_registry = HelpRegistry()
        self.notification_service = NotificationService()
        self.recovery_service = RecoveryService(context.workspace_root)
        self.crash_guard = CrashGuard(
            recovery_service=self.recovery_service,
            notification_service=self.notification_service,
        )
        self.command_registry = CommandRegistry(
            help_registry=self.help_registry,
            crash_guard=self.crash_guard,
            notification_service=self.notification_service,
        )
        self.editor_registry = EditorRegistry()
        self.log_model = LogStreamModel()
        self.task_service = TaskService(
            crash_guard=self.crash_guard,
            notification_service=self.notification_service,
        )
        self.process_service = ProcessService(
            crash_guard=self.crash_guard,
            log_model=self.log_model,
            log_directory=context.workspace_root / ".studio" / "logs",
        )
        self.plugin_service = PluginService(context.plugins, crash_guard=self.crash_guard)
        self.code_editor_service = EditorService(recovery_service=self.recovery_service)
        self.ai_workbench_service = AIWorkbenchService()
        self.gui_studio_engine = GuiStudioEngine(context.workspace_root / "gui_screens")
        self.model_studio_engine = ModelStudioEngine(context.workspace_root / "models" / "studio")
        self.build_service = BuildService(context)
        self.run_service = RunService(context, self.process_service, log_model=self.log_model)
        self.workspace_index_service = WorkspaceIndexService(context)
        self.document_migration_service = DocumentMigrationService(context.workspace_root)
        self.relationship_service = RelationshipResolverService(context, self.workspace_index_service)
        self.workspace_index_service.refresh()
        self.relationship_service.refresh()

        for name, service in {
            "help_registry": self.help_registry,
            "notification_service": self.notification_service,
            "recovery_service": self.recovery_service,
            "crash_guard": self.crash_guard,
            "command_registry": self.command_registry,
            "editor_registry": self.editor_registry,
            "task_service": self.task_service,
            "process_service": self.process_service,
            "plugin_service": self.plugin_service,
            "code_editor_service": self.code_editor_service,
            "ai_workbench_service": self.ai_workbench_service,
            "gui_studio_engine": self.gui_studio_engine,
            "model_studio_engine": self.model_studio_engine,
            "build_service": self.build_service,
            "run_service": self.run_service,
            "workspace_index_service": self.workspace_index_service,
            "document_migration_service": self.document_migration_service,
            "relationship_service": self.relationship_service,
            "ai_workbench_service": self.ai_workbench_service,
            "log_model": self.log_model,
        }.items():
            self.app_context.register_service(name, service)

        self.session_id = self.recovery_service.start_session()
        self._register_builtin_help()
        self._register_builtin_commands()
        self._register_builtin_editors()
        self._register_plugin_editors()
        self.restore_code_session_state()

    @classmethod
    def open(cls, workspace_root: Path, repo_root: Path) -> "StudioSession":
        workspace_manager = WorkspaceManager(workspace_root=workspace_root, repo_root=repo_root)
        context = workspace_manager.load_context()
        return cls(workspace_manager, context)

    def reload_workspace(self, workspace_root: Path | None = None) -> None:
        if workspace_root is not None:
            self.workspace_manager = WorkspaceManager(workspace_root=workspace_root, repo_root=self.context.repo_root)
        self.context = self.workspace_manager.load_context()
        self.app_context.context = self.context
        self.recovery_service = RecoveryService(self.context.workspace_root)
        self.crash_guard = CrashGuard(
            recovery_service=self.recovery_service,
            notification_service=self.notification_service,
        )
        self.build_service = BuildService(self.context)
        self.process_service = ProcessService(
            crash_guard=self.crash_guard,
            log_model=self.log_model,
            log_directory=self.context.workspace_root / ".studio" / "logs",
        )
        self.run_service = RunService(self.context, self.process_service, log_model=self.log_model)
        self.workspace_index_service = WorkspaceIndexService(self.context)
        self.document_migration_service = DocumentMigrationService(self.context.workspace_root)
        self.relationship_service = RelationshipResolverService(self.context, self.workspace_index_service)
        self.workspace_index_service.refresh()
        self.relationship_service.refresh()
        self.plugin_service = PluginService(self.context.plugins, crash_guard=self.crash_guard)
        self.gui_studio_engine = GuiStudioEngine(self.context.workspace_root / "gui_screens")
        self.model_studio_engine = ModelStudioEngine(self.context.workspace_root / "models" / "studio")
        for name, service in {
            "recovery_service": self.recovery_service,
            "crash_guard": self.crash_guard,
            "build_service": self.build_service,
            "process_service": self.process_service,
            "run_service": self.run_service,
            "workspace_index_service": self.workspace_index_service,
            "document_migration_service": self.document_migration_service,
            "relationship_service": self.relationship_service,
            "ai_workbench_service": self.ai_workbench_service,
            "plugin_service": self.plugin_service,
            "gui_studio_engine": self.gui_studio_engine,
            "model_studio_engine": self.model_studio_engine,
        }.items():
            self.app_context.register_service(name, service)
        self._register_plugin_editors()
        self.restore_code_session_state()
        self.recovery_service.update_session(workspace=str(self.context.workspace_root))

    def restore_code_session_state(self) -> None:
        payload = self.recovery_service.latest_session_payload(exclude_session_id=self.session_id)
        if not isinstance(payload, dict):
            return
        code_session = payload.get("codeSession")
        if not isinstance(code_session, dict):
            return
        self.code_editor_service.restore_session_state(code_session)
        self.app_context.state["code_session"] = code_session
        self.recovery_service.update_session(restoredFrom=payload.get("sessionId"), openedDocuments=list(code_session.get("open_files", [])))

    def save_workspace(self) -> None:
        self.workspace_manager.save_context(self.context)
        self.sync_code_session_state()
        self.recovery_service.update_session(lastSavedWorkspace=str(self.context.workspace_root))

    def instantiate_editor(self, editor_id: str):
        return self.editor_registry.create(editor_id, self.app_context)

    def shutdown(self) -> None:
        self.task_service.shutdown(wait=False)

    def clear_runtime_state(self) -> StudioTaskResult:
        self.log_model.clear()
        self.notification_service.clear()
        return StudioTaskResult(
            task_id="logs-clear",
            name="Clear Runtime State",
            success=True,
            started_at=utc_now(),
            finished_at=utc_now(),
            message="Cleared in-memory logs and notifications",
        )

    def latest_log_path(self) -> Path | None:
        candidates = [
            self.run_service.latest_log_path(),
            self.context.workspace_root / "logs" / "latest.log",
            self.context.repo_root / "run" / "logs" / "latest.log",
        ]
        for candidate in candidates:
            if candidate is not None and candidate.exists():
                return candidate
        return None

    def sync_code_session_state(self) -> None:
        state = self.code_editor_service.build_session_state()
        open_files = [str(document.path) for document in self.code_editor_service.documents.values() if document.path is not None]
        payload = {
            "open_documents": list(state.open_documents),
            "recent_files": list(state.recent_files),
            "split_layouts": {key: list(value) for key, value in state.split_layouts.items()},
            "open_files": open_files,
        }
        self.app_context.state["code_session"] = payload
        self.recovery_service.update_session(codeSession=payload, openedDocuments=open_files)

    def _register_builtin_help(self) -> None:
        entries = [
            HelpEntry(
                id="studio.shell",
                label="Studio Shell",
                short_tooltip="Workspace-aware shell services for embedded tools.",
                long_description="Provides service lookup, command dispatch, recovery, tasks, and editor registration for ExtremeCraft Studio.",
                category="studio",
                keywords=("workspace", "services", "shell"),
            ),
            HelpEntry(
                id="studio.code",
                label="Code Studio",
                short_tooltip="Open, edit, search, and save text documents with tracked buffers.",
                long_description="The code backend manages text documents, dirty state, diagnostics, recent files, and split editor sessions.",
                category="editors",
                keywords=("code", "editor", "diagnostics"),
            ),
            HelpEntry(
                id="studio.gui",
                label="GUI Studio",
                short_tooltip="Versioned Minecraft GUI documents, preview payloads, validation, and runtime export.",
                long_description="The GUI backend provides authoring documents, inventory-aware widget schemas, preview contracts, export/import, and validation for mod screens.",
                category="editors",
                keywords=("gui", "screen", "widget", "inventory"),
            ),
            HelpEntry(
                id="studio.model",
                label="Model Studio",
                short_tooltip="Cube model documents with hierarchy, UV data, preview payloads, and runtime export.",
                long_description="The model backend provides cube-first Minecraft model authoring contracts for block, item, and entity style assets.",
                category="editors",
                keywords=("model", "cube", "uv", "bone"),
            ),
            HelpEntry(
                id="studio.relationships",
                label="Relationship Graph",
                short_tooltip="Canonical file, source, runtime, and Java linkage across the workspace.",
                long_description="The relationship service resolves editor-to-editor links between Java, GUI sources, model sources, runtime exports, textures, and generated assets.",
                category="studio",
                keywords=("relationships", "links", "java", "runtime"),
            ),
            HelpEntry(
                id="studio.schema",
                label="Schema Migration",
                short_tooltip="Safe schema normalization and diagnostic fallback for studio documents.",
                long_description="The migration service previews and normalizes GUI/model studio documents, backs up on write when needed, and keeps malformed documents editable in diagnostic mode.",
                category="studio",
                keywords=("schema", "migration", "diagnostic", "recovery"),
            ),
        ]
        for entry in entries:
            self.help_registry.register(entry)

    def _register_builtin_commands(self) -> None:
        commands = [
            StudioCommand(
                id="workspace.save",
                label="Save Workspace",
                handler=self.save_workspace,
                category="workspace",
                short_tooltip="Persist the current workspace marker and session state.",
                long_description="Saves workspace metadata and keeps the current Studio session aligned with the loaded workspace.",
            ),
            StudioCommand(
                id="workspace.validate",
                label="Validate Workspace",
                handler=self.build_service.validate_workspace,
                category="build",
                short_tooltip="Run the validation pipeline for the current workspace.",
                long_description="Executes the asset, model, texture, datapack, registry, and plugin validation pipeline for the active workspace.",
            ),
            StudioCommand(
                id="build.export.resourcepack",
                label="Export ResourcePack",
                handler=lambda: self.build_service.export_pack("resourcepack"),
                category="build",
                short_tooltip="Export the current workspace assets into the resource pack export folder.",
                long_description="Copies workspace assets into the deterministic export location used by Asset Studio.",
            ),
            StudioCommand(
                id="build.export.datapack",
                label="Export Datapack",
                handler=lambda: self.build_service.export_pack("datapack"),
                category="build",
                short_tooltip="Export the current workspace data into the datapack export folder.",
                long_description="Copies workspace datapack content into the deterministic export location used by Asset Studio.",
            ),
            StudioCommand(
                id="build.project.assets",
                label="Build Assets",
                handler=lambda: self.build_service.build_workspace("assets"),
                category="build",
                short_tooltip="Build workspace asset outputs.",
                long_description="Creates the standard workspace build tree for assets without changing the existing CLI contract.",
            ),
            StudioCommand(
                id="logs.clear",
                label="Clear Logs",
                handler=self.clear_runtime_state,
                category="runtime",
                short_tooltip="Clear in-memory log and notification buffers.",
                long_description="Clears studio log surfaces without touching project files or build outputs.",
            ),
            StudioCommand(
                id="logs.latest",
                label="Latest Log",
                handler=self.latest_log_path,
                category="runtime",
                short_tooltip="Resolve the newest available runtime log file.",
                long_description="Resolves the newest process or game log file known to the studio.",
            ),
        ]
        for command in commands:
            self.command_registry.register(command)

    def _register_builtin_editors(self) -> None:
        descriptors = [
            EditorDescriptor(
                id="asset_wizard",
                label="Asset Wizard",
                category="tools",
                area="main_tab",
                order=10,
                factory_ref="asset_studio.gui.asset_wizard:AssetWizardPanel",
                context_mode="none",
            ),
            EditorDescriptor(
                id="visual_builder",
                label="Visual Builder",
                category="tools",
                area="main_tab",
                order=20,
                factory_ref="asset_studio.gui.graph_editor:GraphEditor",
                context_mode="asset_context",
            ),
            EditorDescriptor(
                id="progression_studio",
                label="Skill Tree Designer",
                category="editors",
                area="main_tab",
                order=30,
                factory_ref="asset_studio.gui.skilltree_designer:SkillTreeDesigner",
                context_mode="asset_context",
                document_types=("skilltree",),
            ),
            EditorDescriptor(
                id="project_browser",
                label="Project Browser",
                category="panels",
                area="main_tab",
                order=50,
                factory_ref="asset_studio.gui.project_browser:ProjectBrowser",
                context_mode="none",
            ),
            EditorDescriptor(
                id="console_panel",
                label="Console",
                category="panels",
                area="main_tab",
                order=60,
                factory_ref="asset_studio.gui.console_panel:ConsolePanel",
                context_mode="none",
            ),
            EditorDescriptor(
                id="preview_renderer",
                label="Preview Renderer",
                category="panels",
                area="main_tab",
                order=70,
                factory_ref="asset_studio.gui.preview_renderer:PreviewRenderer",
                context_mode="none",
            ),
            EditorDescriptor(
                id="material_editor",
                label="Materials",
                category="content",
                area="content_editor",
                order=100,
                factory_ref="asset_studio.gui.editors:MaterialEditor",
                context_mode="asset_context",
            ),
            EditorDescriptor(
                id="machine_editor",
                label="Machines",
                category="content",
                area="content_editor",
                order=110,
                factory_ref="asset_studio.gui.editors:MachineEditor",
                context_mode="asset_context",
            ),
            EditorDescriptor(
                id="weapon_editor",
                label="Weapons",
                category="content",
                area="content_editor",
                order=120,
                factory_ref="asset_studio.gui.editors:WeaponEditor",
                context_mode="asset_context",
            ),
            EditorDescriptor(
                id="worldgen_editor",
                label="Worldgen",
                category="content",
                area="content_editor",
                order=130,
                factory_ref="asset_studio.gui.editors:WorldgenEditor",
                context_mode="asset_context",
            ),
            EditorDescriptor(
                id="quest_editor",
                label="Quests",
                category="content",
                area="content_editor",
                order=140,
                factory_ref="asset_studio.gui.editors:QuestEditor",
                context_mode="asset_context",
            ),
            EditorDescriptor(
                id="skill_tree_editor",
                label="Skill Trees",
                category="content",
                area="content_editor",
                order=150,
                factory_ref="asset_studio.gui.editors:SkillTreeEditor",
                context_mode="asset_context",
            ),
            EditorDescriptor(
                id="code_studio",
                label="Code Studio",
                category="editors",
                area="registered_backend",
                order=200,
                document_types=("text", "json", "java", "python"),
                tags=("backend", "code"),
            ),
            EditorDescriptor(
                id="gui_studio",
                label="GUI Studio",
                category="editors",
                area="registered_backend",
                order=210,
                document_types=("gui-screen",),
                tags=("backend", "gui"),
            ),
            EditorDescriptor(
                id="model_studio",
                label="Model Studio",
                category="editors",
                area="registered_backend",
                order=220,
                document_types=("cube-model",),
                tags=("backend", "model"),
            ),
            EditorDescriptor(
                id="debug_studio",
                label="Debug Studio",
                category="editors",
                area="registered_backend",
                order=230,
                tags=("backend", "logs"),
            ),
            EditorDescriptor(
                id="build_studio",
                label="Build Studio",
                category="editors",
                area="registered_backend",
                order=240,
                tags=("backend", "build"),
            ),
        ]
        for descriptor in descriptors:
            self.editor_registry.register(descriptor)
            self.help_registry.register(
                HelpEntry(
                    id=f"editor.{descriptor.id}",
                    label=descriptor.label,
                    short_tooltip=f"Open {descriptor.label}.",
                    long_description=f"Registered studio editor '{descriptor.label}' in category '{descriptor.category}'.",
                    category="editors",
                    keywords=descriptor.tags,
                )
            )

    def _register_plugin_editors(self) -> None:
        self.editor_registry.unregister_prefix("plugin.")
        for editor_name, editor_factory in self.context.plugins.gui_editors.items():
            self.editor_registry.register(
                EditorDescriptor(
                    id=f"plugin.{editor_name}",
                    label=editor_name,
                    category="plugins",
                    area="content_editor",
                    order=500,
                    factory=editor_factory,
                    context_mode="asset_context",
                    tags=("plugin",),
                )
            )
            self.help_registry.register(
                HelpEntry(
                    id=f"editor.plugin.{editor_name}",
                    label=editor_name,
                    short_tooltip=f"Plugin-provided editor: {editor_name}.",
                    long_description="This editor is registered by a plugin and loaded through the studio editor registry.",
                    category="plugins",
                    keywords=("plugin", "editor"),
                )
            )




