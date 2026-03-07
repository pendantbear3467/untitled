from __future__ import annotations

from compiler.dependency_resolver import DependencyResolver as CoreDependencyResolver


class DependencyResolver(CoreDependencyResolver):
    """Compatibility wrapper around top-level dependency resolver."""
