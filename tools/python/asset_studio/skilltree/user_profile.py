from __future__ import annotations

import json
from dataclasses import dataclass, field
from pathlib import Path


@dataclass(slots=True)
class SkillTreeEditorPreferences:
    autosave_enabled: bool = True
    autosave_interval_seconds: int = 30
    last_tree: str = ""
    last_search: str = ""
    export_format: str = "workspace"
    bookmarked_nodes: list[str] = field(default_factory=list)

    def to_dict(self) -> dict:
        return {
            "autosave_enabled": self.autosave_enabled,
            "autosave_interval_seconds": self.autosave_interval_seconds,
            "last_tree": self.last_tree,
            "last_search": self.last_search,
            "export_format": self.export_format,
            "bookmarked_nodes": list(self.bookmarked_nodes),
        }

    @classmethod
    def from_dict(cls, payload: dict) -> "SkillTreeEditorPreferences":
        return cls(
            autosave_enabled=bool(payload.get("autosave_enabled", True)),
            autosave_interval_seconds=int(payload.get("autosave_interval_seconds", 30)),
            last_tree=str(payload.get("last_tree", "")),
            last_search=str(payload.get("last_search", "")),
            export_format=str(payload.get("export_format", "workspace")),
            bookmarked_nodes=[str(value) for value in payload.get("bookmarked_nodes", [])],
        )


@dataclass(slots=True)
class UserProfile:
    username: str = "default"
    preferred_class: str = "adventurer"
    favorite_trees: list[str] = field(default_factory=list)
    skilltree_preferences: SkillTreeEditorPreferences = field(default_factory=SkillTreeEditorPreferences)

    def to_dict(self) -> dict:
        return {
            "username": self.username,
            "preferred_class": self.preferred_class,
            "favorite_trees": list(self.favorite_trees),
            "skilltree_preferences": self.skilltree_preferences.to_dict(),
        }

    @classmethod
    def from_dict(cls, payload: dict) -> "UserProfile":
        return cls(
            username=str(payload.get("username", "default")),
            preferred_class=str(payload.get("preferred_class", "adventurer")),
            favorite_trees=[str(value) for value in payload.get("favorite_trees", [])],
            skilltree_preferences=SkillTreeEditorPreferences.from_dict(dict(payload.get("skilltree_preferences", {}))),
        )


def load_or_create_profile(path: Path) -> UserProfile:
    if not path.exists():
        profile = UserProfile()
        save_profile(path, profile)
        return profile
    payload = json.loads(path.read_text(encoding="utf-8"))
    return UserProfile.from_dict(payload)


def save_profile(path: Path, profile: UserProfile) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(profile.to_dict(), indent=2) + "\n", encoding="utf-8")
