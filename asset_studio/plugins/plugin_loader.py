from __future__ import annotations

import importlib.util
from pathlib import Path
from types import ModuleType

from asset_studio.plugins.plugin_api import PluginAPI


def load_plugins(plugins_dir: Path) -> PluginAPI:
    registry = PluginAPI()
    if not plugins_dir.exists():
        return registry

    for plugin_file in sorted(plugins_dir.glob("*.py")):
        if plugin_file.name.startswith("_"):
            continue
        try:
            module = _load_module(plugin_file)
            register_fn = getattr(module, "register", None)
            if callable(register_fn):
                register_fn(registry)
        except Exception:  # noqa: BLE001
            # Keep the platform resilient when one plugin fails to load.
            continue
    return registry


def _load_module(path: Path) -> ModuleType:
    spec = importlib.util.spec_from_file_location(f"assetstudio_plugin_{path.stem}", path)
    if spec is None or spec.loader is None:
        raise RuntimeError(f"Cannot load plugin file: {path}")
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module
