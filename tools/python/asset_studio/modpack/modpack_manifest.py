from __future__ import annotations

from dataclasses import dataclass, field


@dataclass
class ModpackManifest:
    name: str
    version: str = "0.1.0"
    minecraft_version: str = "1.20.1"
    forge_version: str = "47.2.0"
    mods: list[dict] = field(default_factory=list)
    overrides: list[str] = field(default_factory=list)

    def to_dict(self) -> dict:
        return {
            "name": self.name,
            "version": self.version,
            "minecraft": self.minecraft_version,
            "forge": self.forge_version,
            "mods": self.mods,
            "overrides": self.overrides,
        }
