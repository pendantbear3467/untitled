from __future__ import annotations

import json
import shutil
from dataclasses import dataclass
from pathlib import Path


@dataclass
class AddonBuildResult:
    name: str
    output_path: Path


class AddonManager:
    def __init__(self, context) -> None:
        self.context = context
        self.addons_root = context.workspace_root / "addons"
        self.addons_root.mkdir(parents=True, exist_ok=True)

    def install(self, source: Path) -> Path:
        manifest = source / "addon.json"
        if not manifest.exists():
            raise FileNotFoundError(f"addon.json not found in {source}")
        payload = json.loads(manifest.read_text(encoding="utf-8"))
        name = str(payload.get("name", source.name))
        target = self.addons_root / name
        if target.exists():
            shutil.rmtree(target)
        shutil.copytree(source, target)
        return target

    def build(self, addon_name: str) -> AddonBuildResult:
        addon_root = self.addons_root / addon_name
        if not addon_root.exists():
            raise FileNotFoundError(f"Addon not found: {addon_name}")

        build_root = self.context.workspace_root / "build" / "addons"
        build_root.mkdir(parents=True, exist_ok=True)
        output = build_root / f"{addon_name}.zip"
        if output.exists():
            output.unlink()
        shutil.make_archive(str(output.with_suffix("")), "zip", addon_root)
        return AddonBuildResult(name=addon_name, output_path=output)

    def publish(self, addon_name: str) -> Path:
        result = self.build(addon_name)
        publish_root = self.context.workspace_root / "releases" / "addons"
        publish_root.mkdir(parents=True, exist_ok=True)
        published = publish_root / result.output_path.name
        shutil.copy2(result.output_path, published)
        return published
