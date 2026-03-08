from __future__ import annotations

import json
import shutil
import zipfile
from dataclasses import dataclass, field
from pathlib import Path
from typing import TYPE_CHECKING

from asset_studio.compiler.asset_builder import AssetBuilder
from asset_studio.compiler.code_generator import CodeGenerator
from asset_studio.compiler.datapack_builder import DatapackBuilder
from asset_studio.compiler.dependency_resolver import Conflict, DependencyResolver
from asset_studio.compiler.documentation_generator import DocumentationGenerator

if TYPE_CHECKING:
    from asset_studio.workspace.workspace_manager import AssetStudioContext


@dataclass
class ModuleBuildResult:
    addon_name: str
    output_root: Path
    jar_path: Path
    java_source: Path
    conflicts: list[Conflict] = field(default_factory=list)
    generated_paths: list[Path] = field(default_factory=list)
    generated_java_sources: list[Path] = field(default_factory=list)
    dependency_load_order: list[str] = field(default_factory=list)
    documentation_paths: list[Path] = field(default_factory=list)


class ModuleBuilder:
    """Compile addon definitions into a distributable Forge-compatible module layout."""

    def __init__(self, context: "AssetStudioContext", sdk: object) -> None:
        self.context = context
        self.sdk = sdk
        self.code_generator = CodeGenerator()
        self.asset_builder = AssetBuilder()
        self.datapack_builder = DatapackBuilder()
        self.dependency_resolver = DependencyResolver(workspace_root=context.workspace_root)
        self.documentation_generator = DocumentationGenerator(repo_root=context.repo_root)

    def build_expansion(self, addon_name: str) -> ModuleBuildResult:
        addon = self.sdk.load_addon(addon_name)
        validation = self.sdk.validate_addon(addon)
        if validation.errors:
            raise ValueError("Definition validation failed:\n" + "\n".join(validation.errors))

        generated = self.sdk.generate_addon(addon)
        resolution = self.dependency_resolver.resolve(addon)
        module_root = self.context.workspace_root / "build" / "modules" / addon.name
        if module_root.exists():
            shutil.rmtree(module_root)
        module_root.mkdir(parents=True, exist_ok=True)

        java_source = self.code_generator.generate_registry_code(addon, module_root)
        self.asset_builder.build(self.context.workspace_root, module_root)
        datapack_root = self.datapack_builder.build(self.context.workspace_root, module_root, addon=addon)
        docs = self.documentation_generator.generate(addon, module_root, resolution)

        manifest = {
            "name": addon.name,
            "namespace": addon.namespace,
            "version": addon.version,
            "compatible_platform": getattr(addon, "compatible_platform_version", None) or getattr(addon, "compatible_platform", "*"),
            "dependencies": resolution.dependencies,
            "dependency_load_order": resolution.load_order,
            "dependency_graph": {
                "nodes": [node.__dict__ for node in resolution.nodes],
                "edges": [edge.__dict__ for edge in resolution.edges],
            },
            "conflicts": [conflict.__dict__ for conflict in resolution.conflicts],
            "generated_files": [str(path) for path in generated.generated_paths],
            "generated_java_sources": [str(path) for path in self.code_generator.generated_sources],
            "documentation": [str(path) for path in docs],
            "datapack_root": str(datapack_root),
        }
        manifest_path = module_root / "module_manifest.json"
        manifest_path.write_text(json.dumps(manifest, indent=2) + "\n", encoding="utf-8")

        jar_path = self._package_module(module_root, addon.name)
        return ModuleBuildResult(
            addon_name=addon.name,
            output_root=module_root,
            jar_path=jar_path,
            java_source=java_source,
            conflicts=resolution.conflicts,
            generated_paths=generated.generated_paths,
            generated_java_sources=self.code_generator.generated_sources,
            dependency_load_order=resolution.load_order,
            documentation_paths=docs,
        )

    def _package_module(self, module_root: Path, addon_name: str) -> Path:
        jar_dir = self.context.workspace_root / "build" / "modules" / "artifacts"
        jar_dir.mkdir(parents=True, exist_ok=True)
        artifact_id = addon_name.replace("_", "-")
        jar_path = jar_dir / f"extremecraft-{artifact_id}.jar"

        with zipfile.ZipFile(jar_path, "w", compression=zipfile.ZIP_DEFLATED) as archive:
            for file in module_root.rglob("*"):
                if file.is_file():
                    archive.write(file, file.relative_to(module_root))

        return jar_path
