from __future__ import annotations

import json
import shutil
import zipfile
from dataclasses import dataclass
from pathlib import Path


@dataclass
class AddonBuildResult:
    name: str
    output_path: Path


@dataclass
class AddonDescriptor:
    name: str
    namespace: str
    version: str
    root: Path


class AddonManager:
    def __init__(self, context) -> None:
        self.context = context
        self.addons_root = context.workspace_root / "addons"
        self.addons_root.mkdir(parents=True, exist_ok=True)

    def install(self, source: Path) -> Path:
        if source.suffix.lower() == ".jar" and source.is_file():
            return self._install_runtime_module_jar(source)
        if source.suffix.lower() == ".zip" and source.is_file():
            return self._install_from_archive(source)
        return self._install_from_directory(source)

    def list_addons(self) -> list[AddonDescriptor]:
        descriptors: list[AddonDescriptor] = []
        for addon_root in sorted(self.addons_root.iterdir()):
            if not addon_root.is_dir():
                continue

            manifest = addon_root / "addon.json"
            if not manifest.exists():
                continue

            payload = json.loads(manifest.read_text(encoding="utf-8"))
            descriptors.append(
                AddonDescriptor(
                    name=str(payload.get("name", addon_root.name)),
                    namespace=str(payload.get("namespace", addon_root.name)),
                    version=str(payload.get("version", "0.1.0")),
                    root=addon_root,
                )
            )
        return descriptors

    def remove(self, addon_name: str) -> Path:
        addon_root = self.addons_root / addon_name
        if not addon_root.exists():
            raise FileNotFoundError(f"Addon not found: {addon_name}")

        shutil.rmtree(addon_root)

        runtime_jar = self.context.repo_root / "run" / "extremecraft" / "modules" / f"extremecraft-{addon_name.replace('_', '-')}.jar"
        if runtime_jar.exists():
            runtime_jar.unlink()

        return addon_root

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

    def build_all(self) -> list[AddonBuildResult]:
        results: list[AddonBuildResult] = []
        for descriptor in self.list_addons():
            results.append(self.build(descriptor.name))
        return results

    def publish(self, addon_name: str) -> Path:
        result = self.build(addon_name)
        publish_root = self.context.workspace_root / "releases" / "addons"
        publish_root.mkdir(parents=True, exist_ok=True)
        published = publish_root / result.output_path.name
        shutil.copy2(result.output_path, published)
        return published

    def _install_from_directory(self, source: Path) -> Path:
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

    def _install_from_archive(self, archive_path: Path) -> Path:
        staging = self.context.workspace_root / "build" / "addon_staging"
        if staging.exists():
            shutil.rmtree(staging)
        staging.mkdir(parents=True, exist_ok=True)

        with zipfile.ZipFile(archive_path, "r") as archive:
            archive.extractall(staging)

        candidates = [path for path in staging.rglob("addon.json")]
        if not candidates:
            raise FileNotFoundError(f"No addon.json found in archive: {archive_path}")

        addon_root = candidates[0].parent
        return self._install_from_directory(addon_root)

    def _install_runtime_module_jar(self, source: Path) -> Path:
        modules_dir = self.context.repo_root / "run" / "extremecraft" / "modules"
        modules_dir.mkdir(parents=True, exist_ok=True)

        target = modules_dir / source.name
        shutil.copy2(source, target)
        return target
