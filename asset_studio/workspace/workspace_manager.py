from __future__ import annotations

import json
from dataclasses import dataclass
from pathlib import Path

from PIL import Image

from asset_studio.skilltree.user_profile import UserProfile, load_or_create_profile, save_profile
from asset_studio.textures.procedural_texture_engine import ProceduralTextureEngine
from asset_studio.workspace.asset_database import AssetDatabase


@dataclass
class AssetStudioContext:
    workspace_root: Path
    repo_root: Path
    texture_engine: ProceduralTextureEngine
    asset_db: AssetDatabase

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

    def user_profile_path(self) -> Path:
        return self.workspace_root / "user_profile.json"

    def get_user_profile(self) -> UserProfile:
        return load_or_create_profile(self.user_profile_path())

    def save_user_profile(self, profile: UserProfile) -> None:
        save_profile(self.user_profile_path(), profile)


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
            "skilltrees",
            "previews",
            "build",
            "exports",
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

        profile_path = self.workspace_root / "user_profile.json"
        if not profile_path.exists():
            profile_path.write_text(
                json.dumps(
                    {
                        "username": "default",
                        "preferred_class": "adventurer",
                        "favorite_trees": [],
                    },
                    indent=2,
                )
                + "\n",
                encoding="utf-8",
            )

        return AssetStudioContext(
            workspace_root=self.workspace_root,
            repo_root=self.repo_root,
            texture_engine=ProceduralTextureEngine(seed=1337, resolution="32x"),
            asset_db=AssetDatabase(self.workspace_root / "asset_database.json"),
        )

    def load_context(self) -> AssetStudioContext:
        return self.initialize_workspace()

    def save_context(self, context: AssetStudioContext) -> None:
        marker = context.workspace_root / "project.json"
        if not marker.exists():
            self.initialize_workspace()
