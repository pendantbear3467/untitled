from __future__ import annotations

import importlib
from dataclasses import dataclass, field
from typing import Any, Callable


@dataclass
class EditorDescriptor:
    id: str
    label: str
    category: str
    area: str = "tab"
    order: int = 100
    factory_ref: str | None = None
    factory: Callable[..., Any] | None = None
    context_mode: str = "asset_context"
    document_types: tuple[str, ...] = ()
    tags: tuple[str, ...] = ()
    metadata: dict[str, Any] = field(default_factory=dict)


class EditorRegistry:
    def __init__(self) -> None:
        self._descriptors: dict[str, EditorDescriptor] = {}

    def register(self, descriptor: EditorDescriptor) -> None:
        self._descriptors[descriptor.id] = descriptor

    def get(self, editor_id: str) -> EditorDescriptor | None:
        return self._descriptors.get(editor_id)

    def all(self, area: str | None = None) -> list[EditorDescriptor]:
        descriptors = self._descriptors.values()
        if area is not None:
            descriptors = [descriptor for descriptor in descriptors if descriptor.area == area]
        return sorted(descriptors, key=lambda descriptor: (descriptor.area, descriptor.order, descriptor.label.lower(), descriptor.id))

    def unregister_prefix(self, prefix: str) -> None:
        for editor_id in [editor_id for editor_id in self._descriptors if editor_id.startswith(prefix)]:
            self._descriptors.pop(editor_id, None)

    def create(self, editor_id: str, app_context: Any) -> Any:
        descriptor = self.get(editor_id)
        if descriptor is None:
            raise KeyError(f"Unknown editor: {editor_id}")
        factory = descriptor.factory or self._resolve_factory(descriptor.factory_ref)
        if factory is None:
            raise RuntimeError(f"Editor '{editor_id}' is registered without a factory")
        if descriptor.context_mode == "none":
            return factory()
        if descriptor.context_mode == "app_context":
            return factory(app_context)
        return factory(app_context.context)

    def _resolve_factory(self, factory_ref: str | None) -> Callable[..., Any] | None:
        if not factory_ref:
            return None
        module_name, attr_name = factory_ref.split(":", 1)
        module = importlib.import_module(module_name)
        return getattr(module, attr_name)

