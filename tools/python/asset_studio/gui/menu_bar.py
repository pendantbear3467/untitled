from __future__ import annotations

from typing import Callable

from PyQt6.QtGui import QAction


def build_menu_bar(window, callbacks: dict[str, Callable[[], None]]) -> dict[str, QAction]:
    menu_bar = window.menuBar()
    actions: dict[str, QAction] = {}

    file_menu = menu_bar.addMenu("File")
    actions["new_project"] = _action(file_menu, "New Project", callbacks["new_project"], "Ctrl+N", "Create and initialize a new workspace")
    actions["open_project"] = _action(file_menu, "Open Project", callbacks["open_project"], "Ctrl+O", "Open an existing workspace folder")
    actions["import_blockbench"] = _action(file_menu, "Import Blockbench Model", callbacks["import_blockbench"], None, "Import .bbmodel and generate project assets")
    actions["export_assets"] = _action(file_menu, "Export Assets", callbacks["export_assets"], None, "Export both resourcepack and datapack outputs")
    actions["save_workspace"] = _action(file_menu, "Save Workspace", callbacks["save_workspace"], "Ctrl+S", "Save current workspace state")

    edit_menu = menu_bar.addMenu("Edit")
    actions["undo"] = _action(edit_menu, "Undo", callbacks["undo"], "Ctrl+Z", "Undo latest operation in active tool")
    actions["redo"] = _action(edit_menu, "Redo", callbacks["redo"], "Ctrl+Y", "Redo previously undone operation")

    assets_menu = menu_bar.addMenu("Assets")
    actions["generate_tool"] = _action(assets_menu, "Generate Tool", callbacks["generate_tool"], None, "Open quick flow to generate a tool bundle")
    actions["generate_ore"] = _action(assets_menu, "Generate Ore", callbacks["generate_ore"], None, "Generate ore asset pack from defaults")
    actions["generate_armor"] = _action(assets_menu, "Generate Armor", callbacks["generate_armor"], None, "Generate armor asset pack from defaults")
    actions["generate_machine"] = _action(assets_menu, "Generate Machine", callbacks["generate_machine"], None, "Generate machine block and related assets")
    actions["generate_block"] = _action(assets_menu, "Generate Block", callbacks["generate_block"], None, "Generate block textures/models/blockstates")
    actions["generate_material_set"] = _action(assets_menu, "Generate Material Set", callbacks["generate_material_set"], None, "Generate complete material content set")

    tools_menu = menu_bar.addMenu("Tools")
    actions["texture_generator"] = _action(tools_menu, "Texture Generator", callbacks["texture_generator"], None, "Open procedural texture generation tools")
    actions["blockbench_converter"] = _action(tools_menu, "Blockbench Converter", callbacks["blockbench_converter"], None, "Convert Blockbench assets for project use")
    actions["recipe_builder"] = _action(tools_menu, "Recipe Builder", callbacks["recipe_builder"], None, "Create recipe JSON with helper workflow")
    actions["datapack_builder"] = _action(tools_menu, "Datapack Builder", callbacks["datapack_builder"], None, "Assemble datapack content bundles")
    actions["repair_assets"] = _action(tools_menu, "Auto Repair Assets", callbacks["repair_assets"], None, "Run non-destructive repair passes on known asset issues")

    build_menu = menu_bar.addMenu("Build")
    actions["compile_assets"] = _action(build_menu, "Compile Assets", callbacks["compile_assets"], None, "Compile generated assets and metadata")
    actions["compile_expansion"] = _action(build_menu, "Compile Expansion", callbacks["compile_expansion"], None, "Build an addon expansion jar and generated outputs")
    actions["validate_assets"] = _action(build_menu, "Validate Assets", callbacks["validate_assets"], "Ctrl+Shift+V", "Run full asset/datapack validation pipeline")
    actions["export_resourcepack"] = _action(build_menu, "Export ResourcePack", callbacks["export_resourcepack"], None, "Export resourcepack structure")
    actions["export_datapack"] = _action(build_menu, "Export Datapack", callbacks["export_datapack"], None, "Export datapack structure")
    actions["release_build"] = _action(build_menu, "Build Release", callbacks["release_build"], None, "Create release artifact bundle")
    actions["modpack_build"] = _action(build_menu, "Build Modpack", callbacks["modpack_build"], None, "Create modpack distribution archive")

    view_menu = menu_bar.addMenu("View")
    actions["preview_models"] = _action(view_menu, "Preview Models", callbacks["preview_models"], None, "Switch preview renderer to model mode")
    actions["preview_animations"] = _action(view_menu, "Preview Animations", callbacks["preview_animations"], None, "Switch preview renderer to animation mode")
    actions["texture_viewer"] = _action(view_menu, "Texture Viewer", callbacks["texture_viewer"], None, "Load a texture into the preview renderer")

    help_menu = menu_bar.addMenu("Help")
    actions["documentation"] = _action(help_menu, "Documentation", callbacks["documentation"], "F1", "Open project documentation")
    actions["github"] = _action(help_menu, "GitHub", callbacks["github"], None, "Open repository on GitHub")

    return actions


def _action(menu, title: str, callback: Callable[[], None], shortcut: str | None, description: str) -> QAction:
    action = QAction(title, menu)
    if shortcut:
        action.setShortcut(shortcut)
    action.setStatusTip(description)
    action.setToolTip(description)
    action.triggered.connect(callback)
    menu.addAction(action)
    return action
