from __future__ import annotations

import importlib.util
import json
from pathlib import Path
from types import ModuleType

from extremecraft_sdk.definitions.definition_types import AddonSpec, ContentDefinition


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
        return AddonSpec(
            name=manifest.get("name", addon_name),
            namespace=manifest.get("namespace", addon_name),
            version=manifest.get("version", "0.1.0"),
            dependencies=list(manifest.get("dependencies", [])),
            definitions=definitions,
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
            }
        return json.loads(manifest_path.read_text(encoding="utf-8"))

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
