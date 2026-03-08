from __future__ import annotations

from dataclasses import dataclass, field
from pathlib import Path
from typing import Any

from asset_studio.workspace.workspace_manager import AssetStudioContext, WorkspaceManager


@dataclass
class StudioAppContext:
    context: AssetStudioContext
    workspace_manager: WorkspaceManager
    services: dict[str, Any] = field(default_factory=dict)
    state: dict[str, Any] = field(default_factory=dict)

    @property
    def workspace_root(self) -> Path:
        return self.context.workspace_root

    @property
    def repo_root(self) -> Path:
        return self.context.repo_root

    def register_service(self, name: str, service: Any) -> Any:
        self.services[name] = service
        return service

    def get_service(self, name: str, default: Any = None) -> Any:
        return self.services.get(name, default)
