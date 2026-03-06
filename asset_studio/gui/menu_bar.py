from __future__ import annotations

from typing import Callable

from PyQt6.QtGui import QAction


def build_menu_bar(window, callbacks: dict[str, Callable[[], None]]) -> None:
    menu_bar = window.menuBar()

    file_menu = menu_bar.addMenu("File")
    _action(file_menu, "New Project", callbacks["new_project"])
    _action(file_menu, "Open Project", callbacks["open_project"])
    _action(file_menu, "Import Blockbench Model", callbacks["import_blockbench"])
    _action(file_menu, "Export Assets", callbacks["export_assets"])
    _action(file_menu, "Save Workspace", callbacks["save_workspace"])

    edit_menu = menu_bar.addMenu("Edit")
    _action(edit_menu, "Undo", callbacks["undo"])
    _action(edit_menu, "Redo", callbacks["redo"])

    assets_menu = menu_bar.addMenu("Assets")
    _action(assets_menu, "Generate Tool", callbacks["generate_tool"])
    _action(assets_menu, "Generate Ore", callbacks["generate_ore"])
    _action(assets_menu, "Generate Armor", callbacks["generate_armor"])
    _action(assets_menu, "Generate Machine", callbacks["generate_machine"])
    _action(assets_menu, "Generate Block", callbacks["generate_block"])

    tools_menu = menu_bar.addMenu("Tools")
    _action(tools_menu, "Texture Generator", callbacks["texture_generator"])
    _action(tools_menu, "Blockbench Converter", callbacks["blockbench_converter"])
    _action(tools_menu, "Recipe Builder", callbacks["recipe_builder"])
    _action(tools_menu, "Datapack Builder", callbacks["datapack_builder"])

    build_menu = menu_bar.addMenu("Build")
    _action(build_menu, "Compile Assets", callbacks["compile_assets"])
    _action(build_menu, "Validate Assets", callbacks["validate_assets"])
    _action(build_menu, "Export ResourcePack", callbacks["export_resourcepack"])
    _action(build_menu, "Export Datapack", callbacks["export_datapack"])

    view_menu = menu_bar.addMenu("View")
    _action(view_menu, "Preview Models", callbacks["preview_models"])
    _action(view_menu, "Texture Viewer", callbacks["texture_viewer"])

    help_menu = menu_bar.addMenu("Help")
    _action(help_menu, "Documentation", callbacks["documentation"])
    _action(help_menu, "GitHub", callbacks["github"])


def _action(menu, title: str, callback: Callable[[], None]) -> QAction:
    action = QAction(title, menu)
    action.triggered.connect(callback)
    menu.addAction(action)
    return action
