from __future__ import annotations

import json
from dataclasses import dataclass
from pathlib import Path


@dataclass
class UserProfile:
    username: str = "default"
    preferred_class: str = "adventurer"
    favorite_trees: list[str] | None = None

    def to_dict(self) -> dict:
        return {
            "username": self.username,
            "preferred_class": self.preferred_class,
            "favorite_trees": self.favorite_trees or [],
        }

    @classmethod
    def from_dict(cls, payload: dict) -> "UserProfile":
        return cls(
            username=str(payload.get("username", "default")),
            preferred_class=str(payload.get("preferred_class", "adventurer")),
            favorite_trees=[str(v) for v in payload.get("favorite_trees", [])],
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
