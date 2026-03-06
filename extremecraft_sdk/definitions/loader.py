from __future__ import annotations

import importlib.util
import json
import re
from pathlib import Path
from types import ModuleType

from extremecraft_sdk.definitions.definition_types import (
    AddonSpec,
    ContentDefinition,
    DependencyGraphSpec,
    VersionedDependency,
)


_DEPENDENCY_RE = re.compile(r"^(?P<id>[a-zA-Z0-9_.\-/]+)\s*(?:@|\s+)?\s*(?P<version>[<>=~^!].+)?$")


class DefinitionLoader:
    """Loads addon definitions from JSON and Python sources."""

    def __init__(self, addons_root: Path) -> None:
        self.addons_root = addons_root

    def load_addon(self, addon_name: str) -> AddonSpec:
        addon_root = self.addons_root / addon_name
        if not addon_root.exists():
            raise FileNotFoundError(f"Addon not found: {addon_root}")

        manifest = self._load_manifest(addon_root)
        definitions = self._load_definitions(addon_root)
        dependency_graph = self._load_dependency_graph(manifest)
        dependencies = [dep.id for dep in dependency_graph.addons]

        return AddonSpec(
            name=manifest.get("name", addon_name),
            namespace=manifest.get("namespace", addon_name),
            version=manifest.get("version", "0.1.0"),
            dependencies=dependencies,
            dependency_graph=dependency_graph,
            compatible_platform=str(manifest.get("compatible_platform", manifest.get("platform_version", "*"))),
            compatible_platform_version=str(manifest.get("compatible_platform_version", manifest.get("platform_version", "*"))),
            definitions=definitions,
            metadata=manifest,
            root=addon_root,
        )

    def _load_manifest(self, addon_root: Path) -> dict:
        manifest_path = addon_root / "addon.json"
        if not manifest_path.exists():
            return {
                "name": addon_root.name,
                "namespace": addon_root.name,
                "version": "0.1.0",
                "dependencies": [],
                "dependency_graph": {},
            }
        return json.loads(manifest_path.read_text(encoding="utf-8"))

    def _load_dependency_graph(self, manifest: dict) -> DependencyGraphSpec:
        payload = manifest.get("dependency_graph")
        if not isinstance(payload, dict):
            payload = {}

        addon_deps = self._parse_dependency_entries(payload.get("addons", []))
        for legacy in manifest.get("dependencies", []):
            parsed = self._parse_dependency_entry(legacy)
            if parsed and parsed.id not in {dep.id for dep in addon_deps}:
                addon_deps.append(parsed)

        api_deps = self._parse_dependency_entries(payload.get("apis", []))
        return DependencyGraphSpec(
            materials=self._parse_string_list(payload.get("materials", [])),
            machines=self._parse_string_list(payload.get("machines", [])),
            addons=addon_deps,
            apis=api_deps,
        )

    def _parse_string_list(self, value: object) -> list[str]:
        if isinstance(value, str):
            return [value.strip()] if value.strip() else []
        if not isinstance(value, list):
            return []
        parsed: list[str] = []
        for entry in value:
            if isinstance(entry, str) and entry.strip():
                parsed.append(entry.strip())
        return parsed

    def _parse_dependency_entries(self, value: object) -> list[VersionedDependency]:
        if isinstance(value, (str, dict)):
            value = [value]
        if not isinstance(value, list):
            return []

        parsed: list[VersionedDependency] = []
        for raw in value:
            dep = self._parse_dependency_entry(raw)
            if dep:
                parsed.append(dep)
        return parsed

    def _parse_dependency_entry(self, value: object) -> VersionedDependency | None:
        if isinstance(value, dict):
            dep_id = str(value.get("id", value.get("name", ""))).strip()
            if not dep_id:
                return None
            version = str(value.get("version", value.get("constraint", "*"))).strip() or "*"
            return VersionedDependency(id=dep_id, version=version)

        if not isinstance(value, str):
            return None

        token = value.strip()
        if not token:
            return None

        match = _DEPENDENCY_RE.match(token)
        if not match:
            return VersionedDependency(id=token, version="*")

        dep_id = str(match.group("id") or "").strip()
        if not dep_id:
            return None
        version = str(match.group("version") or "*").strip() or "*"
        return VersionedDependency(id=dep_id, version=version)

    def _load_definitions(self, addon_root: Path) -> list[ContentDefinition]:
        definitions_root = addon_root / "definitions"
        if not definitions_root.exists():
            definitions_root = addon_root

        definitions: list[ContentDefinition] = []
        for json_path in sorted(definitions_root.rglob("*.json")):
            if json_path.name == "addon.json":
                continue
            payload = json.loads(json_path.read_text(encoding="utf-8"))
            definition_type = str(payload.get("type") or json_path.stem)
            definition_id = str(payload.get("id") or json_path.stem)
            definitions.append(
                ContentDefinition(
                    type=definition_type,
                    id=definition_id,
                    payload=payload,
                    source_path=json_path,
                )
            )

        for py_path in sorted(definitions_root.rglob("*.py")):
            module = self._load_module(py_path)
            extracted = self._extract_python_definitions(module)
            for payload in extracted:
                definition_type = str(payload.get("type"))
                definition_id = str(payload.get("id"))
                definitions.append(
                    ContentDefinition(
                        type=definition_type,
                        id=definition_id,
                        payload=payload,
                        source_path=py_path,
                    )
                )

        return definitions

    def _extract_python_definitions(self, module: ModuleType) -> list[dict]:
        if hasattr(module, "get_definitions") and callable(module.get_definitions):
            data = module.get_definitions()
        elif hasattr(module, "DEFINITIONS"):
            data = module.DEFINITIONS
        else:
            return []

        if isinstance(data, dict):
            return [data]
        if isinstance(data, list):
            return [entry for entry in data if isinstance(entry, dict)]
        return []

    def _load_module(self, path: Path) -> ModuleType:
        spec = importlib.util.spec_from_file_location(f"extremecraft_def_{path.stem}", path)
        if spec is None or spec.loader is None:
            raise RuntimeError(f"Unable to load definitions module: {path}")
        module = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(module)
        return module
