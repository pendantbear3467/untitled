from __future__ import annotations

import json
from dataclasses import dataclass
from pathlib import Path

from PIL import Image

from asset_studio.plugins.plugin_loader import load_plugins
from asset_studio.textures.procedural_texture_engine import ProceduralTextureEngine
from asset_studio.workspace.asset_database import AssetDatabase


@dataclass
class AssetStudioContext:
    workspace_root: Path
    repo_root: Path
    texture_engine: ProceduralTextureEngine
    asset_db: AssetDatabase
    plugins: object

    def write_json(self, path: Path, payload: dict) -> None:
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(json.dumps(payload, indent=2) + "\n", encoding="utf-8")

    def write_texture(self, path: Path, image: Image.Image) -> None:
        path.parent.mkdir(parents=True, exist_ok=True)
        image.save(path, format="PNG")

    def add_lang_entry(self, key: str, value: str) -> None:
        lang_path = self.workspace_root / "assets" / "lang" / "en_us.json"
        payload = {}
        if lang_path.exists():
            payload = json.loads(lang_path.read_text(encoding="utf-8"))
        payload[key] = value
        self.write_json(lang_path, payload)

    def append_to_tag(self, path: Path, value: str) -> None:
        payload = {"replace": False, "values": []}
        if path.exists():
            payload = json.loads(path.read_text(encoding="utf-8"))
            payload.setdefault("replace", False)
            payload.setdefault("values", [])
        if value not in payload["values"]:
            payload["values"].append(value)
        self.write_json(path, payload)

    def write_preview_snapshot(self, model_name: str) -> None:
        snapshot = self.workspace_root / "previews" / f"{model_name}.txt"
        snapshot.parent.mkdir(parents=True, exist_ok=True)
        snapshot.write_text(f"Preview generated for {model_name}\n", encoding="utf-8")


class WorkspaceManager:
    def __init__(self, workspace_root: Path, repo_root: Path) -> None:
        self.workspace_root = workspace_root
        self.repo_root = repo_root

    def initialize_workspace(self) -> AssetStudioContext:
        for rel in [
            "assets/textures/item",
            "assets/textures/block",
            "assets/models/item",
            "assets/models/block",
            "assets/blockstates",
            "assets/lang",
            "data/recipes",
            "data/loot_tables/blocks",
            "data/tags/tools",
            "data/worldgen/configured_feature",
            "data/worldgen/placed_feature",
            "data/forge/biome_modifier",
            "data/advancements",
            "materials",
            "machines",
            "tools",
            "blockbench",
            "previews",
            "build",
            "exports",
            "addons",
            "definitions",
            "modpacks",
            "releases",
            "registry_history",
            "graphs",
            "addons",
            "workspace_plugins",
        ]:
            (self.workspace_root / rel).mkdir(parents=True, exist_ok=True)

        project_file = self.workspace_root / "project.json"
        if not project_file.exists():
            project_file.write_text(
                json.dumps(
                    {
                        "name": "extremecraft-asset-project",
                        "version": 1,
                        "modid": "extremecraft",
                        "description": "Minecraft Mod Development IDE + Pipeline Tool",
                    },
                    indent=2,
                )
                + "\n",
                encoding="utf-8",
            )

        snapshot_path = self.workspace_root / "registry_snapshot.json"
        if not snapshot_path.exists():
            snapshot_path.write_text(json.dumps({"files_scanned": 0}, indent=2) + "\n", encoding="utf-8")

        plugins = load_plugins(self.repo_root / "plugins")
        workspace_plugins_dir = self.workspace_root / "plugins"
        if workspace_plugins_dir.exists():
            workspace_plugins = load_plugins(workspace_plugins_dir)
            plugins.generators.update(workspace_plugins.generators)
            plugins.validators.update(workspace_plugins.validators)
            plugins.texture_styles.update(workspace_plugins.texture_styles)
            plugins.templates.update(workspace_plugins.templates)
            plugins.exporters.update(workspace_plugins.exporters)
            plugins.asset_repairs.update(workspace_plugins.asset_repairs)
            plugins.gui_editors.update(workspace_plugins.gui_editors)
            plugins.datapack_rules.update(workspace_plugins.datapack_rules)
            plugins.graph_nodes.update(workspace_plugins.graph_nodes)

        return AssetStudioContext(
            workspace_root=self.workspace_root,
            repo_root=self.repo_root,
            texture_engine=ProceduralTextureEngine(seed=1337, resolution="32x"),
            asset_db=AssetDatabase(self.workspace_root / "asset_database.json"),
            plugins=plugins,
        )

    def load_context(self) -> AssetStudioContext:
        return self.initialize_workspace()

    def save_context(self, context: AssetStudioContext) -> None:
        marker = context.workspace_root / "project.json"
        if not marker.exists():
            self.initialize_workspace()
