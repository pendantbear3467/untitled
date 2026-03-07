from __future__ import annotations

from compiler.code_generator import CodeGenerator as CoreCodeGenerator


class CodeGenerator(CoreCodeGenerator):
    """Compatibility wrapper around top-level code generator."""
