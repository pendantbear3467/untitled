from __future__ import annotations

from compiler.module_builder import ModuleBuilder as CoreModuleBuilder


class ModuleBuilder(CoreModuleBuilder):
    """Compatibility wrapper around top-level compiler module builder."""
