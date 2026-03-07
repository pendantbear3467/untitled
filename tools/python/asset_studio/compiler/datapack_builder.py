from __future__ import annotations

from compiler.datapack_builder import DatapackBuilder as CoreDatapackBuilder


class DatapackBuilder(CoreDatapackBuilder):
    """Compatibility wrapper around top-level datapack builder."""
