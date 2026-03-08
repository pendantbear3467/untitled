"""Canonical access point for Asset Studio desktop components.

The package intentionally uses lazy imports so non-GUI CLI workflows can import
``asset_studio.studio`` without requiring optional Qt dependencies.
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
    "AssetStudioWindow": ("asset_studio.studio.app_window", "AssetStudioWindow"),
    "launch_gui": ("asset_studio.studio.app_window", "launch_gui"),
    "CodeStudioPanel": ("asset_studio.studio.code_studio", "CodeStudioPanel"),
    "StudioCodeEditor": ("asset_studio.studio.code_studio", "StudioCodeEditor"),
    "BuildRunPanel": ("asset_studio.studio.studio_panels", "BuildRunPanel"),
    "GuiStudioPanel": ("asset_studio.studio.studio_panels", "GuiStudioPanel"),
    "ModelStudioPanel": ("asset_studio.studio.studio_panels", "ModelStudioPanel"),
    "StudioSession": ("asset_studio.studio.studio_session", "StudioSession"),
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
