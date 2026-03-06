from __future__ import annotations

import importlib.util
from dataclasses import dataclass, field
from pathlib import Path
from types import ModuleType


@dataclass
class PluginRegistry:
    generators: dict[str, object] = field(default_factory=dict)
    templates: dict[str, object] = field(default_factory=dict)
    textures: dict[str, object] = field(default_factory=dict)
    exporters: dict[str, object] = field(default_factory=dict)


def load_plugins(plugins_dir: Path) -> PluginRegistry:
    registry = PluginRegistry()
    if not plugins_dir.exists():
        return registry

    for plugin_file in plugins_dir.glob("*.py"):
        module = _load_module(plugin_file)
        register_fn = getattr(module, "register", None)
        if callable(register_fn):
            register_fn(registry)
    return registry


def _load_module(path: Path) -> ModuleType:
    spec = importlib.util.spec_from_file_location(f"assetstudio_plugin_{path.stem}", path)
    if spec is None or spec.loader is None:
        raise RuntimeError(f"Cannot load plugin file: {path}")
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module
