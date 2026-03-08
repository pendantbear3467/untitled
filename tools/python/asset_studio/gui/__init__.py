"""Canonical desktop UI namespace for ExtremeCraft Studio.

This package is the authoritative import surface for the live desktop shell and
editor widgets. Imports are resolved lazily so CLI callers can safely import the
package without forcing optional Qt dependencies to load.
"""

from importlib import import_module

__all__ = [
    "AssetStudioWindow",
    "BuildRunPanel",
    "CodeStudioPanel",
    "GuiStudioPanel",
    "ModelStudioPanel",
    "PreviewRenderer",
    "ProjectBrowser",
    "StudioCodeEditor",
    "StudioSession",
    "launch_gui",
]

_EXPORT_MAP = {
    "AssetStudioWindow": ("asset_studio.gui.app_window", "AssetStudioWindow"),
    "launch_gui": ("asset_studio.gui.app_window", "launch_gui"),
    "CodeStudioPanel": ("asset_studio.gui.code_studio", "CodeStudioPanel"),
    "StudioCodeEditor": ("asset_studio.gui.code_studio", "StudioCodeEditor"),
    "BuildRunPanel": ("asset_studio.gui.studio_panels", "BuildRunPanel"),
    "GuiStudioPanel": ("asset_studio.gui.studio_panels", "GuiStudioPanel"),
    "ModelStudioPanel": ("asset_studio.gui.studio_panels", "ModelStudioPanel"),
    "PreviewRenderer": ("asset_studio.gui.preview_renderer", "PreviewRenderer"),
    "ProjectBrowser": ("asset_studio.gui.project_browser", "ProjectBrowser"),
    "StudioSession": ("asset_studio.core.studio_session", "StudioSession"),
}


def __getattr__(name: str):
    target = _EXPORT_MAP.get(name)
    if target is None:
        raise AttributeError(name)
    module_name, attribute_name = target
    module = import_module(module_name)
    value = getattr(module, attribute_name)
    globals()[name] = value
    return value
