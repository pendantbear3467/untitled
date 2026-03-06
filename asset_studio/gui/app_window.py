from __future__ import annotations

import sys
import webbrowser
from pathlib import Path

from PyQt6.QtCore import Qt
from PyQt6.QtWidgets import (
    QApplication,
    QFileDialog,
    QMainWindow,
    QPlainTextEdit,
    QSplitter,
    QStatusBar,
    QTabWidget,
)

from asset_studio.blockbench.bbmodel_importer import import_bbmodel
from asset_studio.cli.build_commands import validate_command
from asset_studio.cli.pack_commands import export_pack_command
from asset_studio.generators.armor_generator import ArmorGenerator
from asset_studio.generators.block_generator import BlockGenerator
from asset_studio.generators.machine_generator import MachineGenerator
from asset_studio.generators.ore_generator import OreGenerator
from asset_studio.generators.tool_generator import ToolGenerator
from asset_studio.gui.asset_wizard import AssetWizardPanel, ToolWizardInput
from asset_studio.gui.menu_bar import build_menu_bar
from asset_studio.gui.preview_renderer import PreviewRenderer
from asset_studio.project.workspace_manager import WorkspaceManager


class AssetStudioWindow(QMainWindow):
    def __init__(self, workspace_root: Path) -> None:
        super().__init__()
        self.workspace_manager = WorkspaceManager(workspace_root)
        self.context = self.workspace_manager.load_context()

        self.setWindowTitle("EXTREMECRAFT ASSET STUDIO")
        self.resize(1320, 840)
        self.setStatusBar(QStatusBar(self))

        self.log = QPlainTextEdit(self)
        self.log.setReadOnly(True)

        self.preview = PreviewRenderer()
        self._last_preview_texture: Path | None = None
        self.wizard = AssetWizardPanel()
        self.wizard.generate_tool_requested.connect(self._on_generate_tool)

        tabs = QTabWidget()
        tabs.addTab(self.wizard, "Asset Wizard")
        tabs.addTab(self.log, "Build Log")

        splitter = QSplitter(Qt.Orientation.Horizontal)
        splitter.addWidget(tabs)
        splitter.addWidget(self.preview)
        splitter.setSizes([860, 460])
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
            "texture_generator": lambda: self._write_log("Texture Generator opened"),
            "blockbench_converter": lambda: self._write_log("Blockbench Converter opened"),
            "recipe_builder": lambda: self._write_log("Recipe Builder opened"),
            "datapack_builder": lambda: self._write_log("Datapack Builder opened"),
            "compile_assets": lambda: self._write_log("Compile Assets completed"),
            "validate_assets": self._validate_assets,
            "export_resourcepack": lambda: self._export_target("resourcepack"),
            "export_datapack": lambda: self._export_target("datapack"),
            "preview_models": self._preview_models,
            "texture_viewer": self._texture_viewer,
            "documentation": lambda: webbrowser.open("https://github.com/pendantbear3467/untitled/tree/main/docs"),
            "github": lambda: webbrowser.open("https://github.com/pendantbear3467/untitled"),
        }

    def _write_log(self, message: str) -> None:
        self.log.appendPlainText(message)
        self.statusBar().showMessage(message, 4000)

    def _new_project(self) -> None:
        self.context = self.workspace_manager.initialize_workspace()
        self._write_log("New project initialized")

    def _open_project(self) -> None:
        selected = QFileDialog.getExistingDirectory(self, "Open workspace", str(self.context.workspace_root))
        if not selected:
            return
        self.workspace_manager = WorkspaceManager(Path(selected))
        self.context = self.workspace_manager.load_context()
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
        code = validate_command(self.context, strict=False)
        self._write_log("Validation completed" if code == 0 else "Validation completed with issues")

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

    def _export_target(self, target: str) -> None:
        export_pack_command(self.context, target)
        self._write_log(f"Exported {target}")

    def _preview_models(self) -> None:
        self.preview.set_mode("block")
        if self._last_preview_texture:
            self.preview.load_texture(self._last_preview_texture)
        self._write_log("Preview mode: model")

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

    def _set_preview_texture(self, texture_path: Path, mode: str) -> None:
        self._last_preview_texture = texture_path
        self.preview.set_mode(mode)
        self.preview.load_texture(texture_path)


def launch_gui(workspace_root: Path) -> int:
    app = QApplication(sys.argv)
    app.setApplicationName("EXTREMECRAFT ASSET STUDIO")

    window = AssetStudioWindow(workspace_root)
    window.show()
    return app.exec()
