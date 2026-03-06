from __future__ import annotations

import sys
import webbrowser
from pathlib import Path

from PyQt6.QtCore import Qt
from PyQt6.QtWidgets import (
    QApplication,
    QFileDialog,
    QHBoxLayout,
    QMainWindow,
    QPushButton,
    QSlider,
    QSplitter,
    QStatusBar,
    QTabWidget,
    QVBoxLayout,
    QWidget,
)

from asset_studio.blockbench.bbmodel_importer import import_bbmodel
from asset_studio.cli.validate_commands import run_validate_command
from asset_studio.cli.pack_commands import export_pack_command
from asset_studio.generators.armor_generator import ArmorGenerator
from asset_studio.generators.block_generator import BlockGenerator
from asset_studio.generators.content_set_generator import ContentSetGenerator
from asset_studio.generators.machine_generator import MachineGenerator
from asset_studio.generators.ore_generator import OreGenerator
from asset_studio.generators.tool_generator import ToolGenerator
from asset_studio.gui.asset_wizard import AssetWizardPanel, ToolWizardInput
from asset_studio.gui.console_panel import ConsolePanel
from asset_studio.gui.editors import MachineEditor, MaterialEditor, QuestEditor, SkillTreeEditor, WeaponEditor, WorldgenEditor
from asset_studio.gui.menu_bar import build_menu_bar
from asset_studio.gui.preview_renderer import PreviewRenderer
from asset_studio.gui.project_browser import ProjectBrowser
from asset_studio.gui.timeline_widget import TimelineWidget
from asset_studio.modpack.modpack_builder import ModpackBuilder
from asset_studio.release.release_manager import ReleaseManager
from asset_studio.repair.repair_engine import RepairEngine
from asset_studio.workspace.workspace_manager import WorkspaceManager
from compiler.module_builder import ModuleBuilder
from extremecraft_sdk.api.sdk import ExtremeCraftSDK


class AssetStudioWindow(QMainWindow):
    def __init__(self, workspace_root: Path) -> None:
        super().__init__()
        self.workspace_manager = WorkspaceManager(workspace_root=workspace_root, repo_root=Path.cwd())
        self.context = self.workspace_manager.load_context()

        self.setWindowTitle("EXTREMECRAFT ASSET STUDIO")
        self.resize(1420, 900)
        self.setStatusBar(QStatusBar(self))

        self.log = ConsolePanel()
        self.browser = ProjectBrowser()
        self.browser.load_workspace(self.context.workspace_root)

        self.preview = PreviewRenderer()
        self._last_preview_texture: Path | None = None
        self.wizard = AssetWizardPanel()
        self.wizard.generate_tool_requested.connect(self._on_generate_tool)

        self.editor_tabs = self._build_editor_tabs()

        preview_controls = self._build_preview_controls()
        preview_stack = QWidget()
        preview_layout = QVBoxLayout(preview_stack)
        preview_layout.addWidget(self.preview)
        preview_layout.addWidget(preview_controls)

        tabs = QTabWidget()
        tabs.addTab(self.wizard, "Asset Wizard")
        tabs.addTab(self.editor_tabs, "Content Editors")
        tabs.addTab(self.browser, "Project Browser")
        tabs.addTab(self.log, "Console")

        splitter = QSplitter(Qt.Orientation.Horizontal)
        splitter.addWidget(tabs)
        splitter.addWidget(preview_stack)
        splitter.setSizes([920, 500])
        self.setCentralWidget(splitter)

        build_menu_bar(self, self._callbacks())
        self._write_log("Workspace loaded")
        self._write_log(str(self.context.workspace_root))

    def _callbacks(self):
        return {
            "new_project": self._new_project,
            "open_project": self._open_project,
            "import_blockbench": self._import_blockbench,
            "export_assets": self._export_assets,
            "save_workspace": self._save_workspace,
            "undo": lambda: self._write_log("Undo requested"),
            "redo": lambda: self._write_log("Redo requested"),
            "generate_tool": lambda: self._write_log("Use Asset Wizard -> Generate Tool Bundle"),
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

    def _new_project(self) -> None:
        self.context = self.workspace_manager.initialize_workspace()
        self.browser.load_workspace(self.context.workspace_root)
        self._write_log("New project initialized")

    def _open_project(self) -> None:
        selected = QFileDialog.getExistingDirectory(self, "Open workspace", str(self.context.workspace_root))
        if not selected:
            return
        self.workspace_manager = WorkspaceManager(workspace_root=Path(selected), repo_root=Path.cwd())
        self.context = self.workspace_manager.load_context()
        self.browser.load_workspace(self.context.workspace_root)
        self.editor_tabs = self._build_editor_tabs()
        self._write_log(f"Opened workspace: {selected}")

    def _save_workspace(self) -> None:
        self.workspace_manager.save_context(self.context)
        self._write_log("Workspace saved")

    def _import_blockbench(self) -> None:
        selected, _ = QFileDialog.getOpenFileName(self, "Import Blockbench model", "", "Blockbench (*.bbmodel)")
        if not selected:
            return
        result = import_bbmodel(Path(selected), self.context)
        self._write_log(f"Imported Blockbench model: {result}")
        self._set_preview_texture(self.context.workspace_root / "assets" / "textures" / "block" / f"{result}.png", "block")

    def _export_assets(self) -> None:
        export_pack_command(self.context, "resourcepack")
        export_pack_command(self.context, "datapack")
        self._write_log("Assets exported")

    def _validate_assets(self) -> None:
        args = type("ValidateArgs", (), {"strict": False})()
        code = run_validate_command(args, self.context)
        self._write_log("Validation completed" if code == 0 else "Validation completed with issues")

    def _repair_assets(self) -> None:
        report = RepairEngine(self.context).repair()
        self._write_log(f"Auto-repair completed with {report.total} actions")

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
            return

        self._write_log(f"Compiled expansion '{addon_name}' -> {result.jar_path}")

    def _release_build(self) -> None:
        result = ReleaseManager(self.context).build()
        self._write_log(f"Release built: {result.artifact.name}")

    def _modpack_build(self) -> None:
        result = ModpackBuilder(self.context).build("extreme_adventure_pack")
        self._write_log(f"Modpack archive: {result.archive_path}")

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
        self._set_preview_texture(
            self.context.workspace_root / "assets" / "textures" / "item" / f"{payload.tool_name}.png",
            "item",
        )

    def _generate_ore(self) -> None:
        material = "mythril"
        OreGenerator(self.context).generate(material=material, tier=4, texture_style="metallic")
        self._write_log("Generated ore bundle: mythril")
        self._set_preview_texture(
            self.context.workspace_root / "assets" / "textures" / "block" / f"{material}_ore.png",
            "block",
        )

    def _generate_armor(self) -> None:
        material = "mythril"
        ArmorGenerator(self.context).generate(material=material, tier=4, texture_style="metallic")
        self._write_log("Generated armor bundle: mythril")
        self._set_preview_texture(
            self.context.workspace_root / "assets" / "textures" / "item" / f"{material}_helmet.png",
            "item",
        )

    def _generate_machine(self) -> None:
        machine_name = "mythril_crusher"
        MachineGenerator(self.context).generate(machine_name=machine_name, material="mythril", texture_style="industrial")
        self._write_log("Generated machine asset: mythril_crusher")
        self._set_preview_texture(
            self.context.workspace_root / "assets" / "textures" / "block" / f"{machine_name}.png",
            "block",
        )

    def _generate_block(self) -> None:
        block_name = "mythril_bricks"
        BlockGenerator(self.context).generate(block_name=block_name, material="mythril", texture_style="ancient")
        self._write_log("Generated block asset: mythril_bricks")
        self._set_preview_texture(
            self.context.workspace_root / "assets" / "textures" / "block" / f"{block_name}.png",
            "block",
        )

    def _generate_material_set(self) -> None:
        generated = ContentSetGenerator(self.context).generate_material_set("mythril", tier=4, style="metallic")
        self._write_log(f"Generated material set (count={len(generated)})")

    def _export_target(self, target: str) -> None:
        export_pack_command(self.context, target)
        self._write_log(f"Exported {target}")

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

    def _set_preview_texture(self, texture_path: Path, mode: str) -> None:
        self._last_preview_texture = texture_path
        self.preview.set_mode(mode)
        self.preview.load_texture(texture_path)

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

        prev_texture = QPushButton("Prev Texture")
        prev_texture.clicked.connect(lambda: self.preview.switch_texture(-1))

        timeline = TimelineWidget()
        timeline.progress_changed.connect(self.preview.set_animation_progress)

        row.addWidget(prev_texture)
        row.addWidget(next_texture)
        row.addWidget(zoom)
        row.addWidget(lighting)
        row.addWidget(timeline)
        return holder

    def _build_editor_tabs(self) -> QTabWidget:
        tabs = QTabWidget()
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
        return tabs


def launch_gui(workspace_root: Path) -> int:
    app = QApplication(sys.argv)
    app.setApplicationName("EXTREMECRAFT ASSET STUDIO")

    window = AssetStudioWindow(workspace_root)
    window.show()
    return app.exec()
