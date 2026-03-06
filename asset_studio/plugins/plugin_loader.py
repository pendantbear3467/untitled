from __future__ import annotations

import importlib.util
import json
import re
from dataclasses import dataclass
from pathlib import Path
from types import ModuleType

from asset_studio.plugins.plugin_api import PluginAPI, PluginMetadata


_VERSION_TOKEN_RE = re.compile(r"[0-9]+")


@dataclass(frozen=True)
class _PluginCandidate:
    path: Path
    metadata: PluginMetadata


def load_plugins(plugins_dir: Path) -> PluginAPI:
    registry = PluginAPI()
    if not plugins_dir.exists():
        return registry

    platform_version = _load_platform_version(plugins_dir)
    candidates: list[_PluginCandidate] = []
    for plugin_file in sorted(plugins_dir.glob("*.py")):
        if plugin_file.name.startswith("_"):
            continue

        try:
            module = _load_module(plugin_file)
            metadata = _extract_metadata(plugin_file, module)
            if not _version_matches(platform_version, metadata.compatible_platform_version):
                continue
            candidates.append(_PluginCandidate(path=plugin_file, metadata=metadata))
        except Exception:  # noqa: BLE001
            continue

    loaded_plugins: set[str] = set()
    pending = list(candidates)

    while pending:
        next_pending: list[_PluginCandidate] = []
        progressed = False

        for candidate in pending:
            unresolved = [dep for dep in candidate.metadata.dependencies if dep not in loaded_plugins]
            if unresolved:
                next_pending.append(candidate)
                continue

            try:
                module = _load_module(candidate.path)
                register_fn = getattr(module, "register", None)
                if callable(register_fn):
                    register_fn(registry)
                registry.register_plugin(candidate.metadata)
                loaded_plugins.add(candidate.metadata.name)
                progressed = True
            except Exception:  # noqa: BLE001
                continue

        if not progressed:
            break
        pending = next_pending

    return registry


def _extract_metadata(plugin_file: Path, module: ModuleType) -> PluginMetadata:
    sidecar = plugin_file.with_suffix(".plugin.json")
    payload: dict = {}

    if sidecar.exists():
        payload = json.loads(sidecar.read_text(encoding="utf-8"))

    module_payload = getattr(module, "PLUGIN_METADATA", None)
    if isinstance(module_payload, dict):
        payload.update(module_payload)

    metadata_fn = getattr(module, "plugin_metadata", None)
    if callable(metadata_fn):
        dynamic_payload = metadata_fn()
        if isinstance(dynamic_payload, dict):
            payload.update(dynamic_payload)

    name = str(payload.get("name", plugin_file.stem))
    version = str(payload.get("version", "0.1.0"))
    dependencies = tuple(_normalize_dependencies(payload.get("dependencies", [])))
    compatible_platform = str(payload.get("compatible_platform_version", payload.get("compatible_platform", "*")))
    entrypoint = str(payload.get("entrypoint", f"{plugin_file.stem}:register"))

    return PluginMetadata(
        name=name,
        version=version,
        dependencies=dependencies,
        compatible_platform_version=compatible_platform,
        entrypoint=entrypoint,
    )


def _normalize_dependencies(value: object) -> list[str]:
    if isinstance(value, str):
        return [value]
    if not isinstance(value, list):
        return []
    return [str(entry) for entry in value if str(entry).strip()]


def _load_module(path: Path) -> ModuleType:
    spec = importlib.util.spec_from_file_location(f"assetstudio_plugin_{path.stem}", path)
    if spec is None or spec.loader is None:
        raise RuntimeError(f"Cannot load plugin file: {path}")
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def _load_platform_version(plugins_dir: Path) -> str:
    repo_root = plugins_dir.parent
    build_file = repo_root / "build.gradle"
    if not build_file.exists():
        return "0.0.0"

    text = build_file.read_text(encoding="utf-8")
    match = re.search(r"^\s*version\s*=\s*['\"]([^'\"]+)['\"]", text, flags=re.MULTILINE)
    return match.group(1) if match else "0.0.0"


def _version_matches(version: str, constraint: str | None) -> bool:
    token = (constraint or "*").strip()
    if not token or token == "*":
        return True

    checks = [entry.strip() for entry in token.split(",") if entry.strip()]
    for check in checks:
        if check.startswith(">=") and _compare_versions(version, check[2:]) < 0:
            return False
        elif check.startswith("<=") and _compare_versions(version, check[2:]) > 0:
            return False
        elif check.startswith(">") and _compare_versions(version, check[1:]) <= 0:
            return False
        elif check.startswith("<") and _compare_versions(version, check[1:]) >= 0:
            return False
        elif check.startswith("==") and _compare_versions(version, check[2:]) != 0:
            return False
        elif check.startswith("=") and _compare_versions(version, check[1:]) != 0:
            return False
        elif check.startswith("^"):
            base = check[1:].strip()
            if _compare_versions(version, base) < 0 or _major(version) != _major(base):
                return False
        elif check.startswith("~"):
            base = check[1:].strip()
            if _compare_versions(version, base) < 0 or _major_minor(version) != _major_minor(base):
                return False
        elif _compare_versions(version, check) != 0:
            return False

    return True


def _compare_versions(left: str, right: str) -> int:
    lparts = [int(token) for token in _VERSION_TOKEN_RE.findall(left)] or [0]
    rparts = [int(token) for token in _VERSION_TOKEN_RE.findall(right)] or [0]
    width = max(len(lparts), len(rparts))

    for index in range(width):
        lval = lparts[index] if index < len(lparts) else 0
        rval = rparts[index] if index < len(rparts) else 0
        if lval < rval:
            return -1
        if lval > rval:
            return 1
    return 0


def _major(version: str) -> int:
    parts = [int(token) for token in _VERSION_TOKEN_RE.findall(version)] or [0]
    return parts[0]


def _major_minor(version: str) -> tuple[int, int]:
    parts = [int(token) for token in _VERSION_TOKEN_RE.findall(version)] or [0]
    major = parts[0]
    minor = parts[1] if len(parts) > 1 else 0
    return major, minor
