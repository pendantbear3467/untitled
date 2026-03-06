from __future__ import annotations

import shutil
from pathlib import Path

from asset_studio.workspace.workspace_manager import AssetStudioContext, WorkspaceManager


class ProjectManager:
    def __init__(self, context: AssetStudioContext) -> None:
        self.context = context

    def init_project(self, name: str) -> Path:
        payload = {
            "name": name,
            "version": 1,
            "modid": "extremecraft",
        }
        self.context.write_json(self.context.workspace_root / "project.json", payload)
        return self.context.workspace_root / "project.json"

    def open_project(self, workspace_path: Path) -> Path:
        manager = WorkspaceManager(workspace_path, repo_root=self.context.repo_root)
        manager.initialize_workspace()
        return workspace_path

    def build_project(self, target: str) -> Path:
        build_root = self.context.workspace_root / "build" / target
        build_root.mkdir(parents=True, exist_ok=True)

        if target in {"assets", "resourcepack"}:
            source = self.context.workspace_root / "assets"
            destination = build_root / "assets"
            if destination.exists():
                shutil.rmtree(destination)
            if source.exists():
                shutil.copytree(source, destination)

        if target in {"datapack", "assets"}:
            source = self.context.workspace_root / "data"
            destination = build_root / "data"
            if destination.exists():
                shutil.rmtree(destination)
            if source.exists():
                shutil.copytree(source, destination)

        return build_root
