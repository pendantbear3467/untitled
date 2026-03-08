"""Compatibility desktop UI namespace for ExtremeCraft Studio.

Use ``asset_studio.gui`` for canonical live UI/runtime imports. This package is
kept as a lazy compatibility shim for older call sites.
"""

from importlib import import_module

__all__ = [
    "AssetStudioWindow",
    "BuildRunPanel",
    "CodeStudioPanel",
    "GuiStudioPanel",
    "ModelStudioPanel",
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
