from __future__ import annotations

import json
import re
from collections import defaultdict, deque
from dataclasses import dataclass, field
from pathlib import Path
from typing import Any, TYPE_CHECKING

if TYPE_CHECKING:
    from extremecraft_sdk.definitions.definition_types import AddonSpec


@dataclass
class Conflict:
    kind: str
    identifier: str
    message: str


@dataclass(frozen=True)
class DependencyNode:
    key: str
    kind: str
    version: str
    constraint: str = "*"


@dataclass(frozen=True)
class DependencyEdge:
    source: str
    target: str


@dataclass
class DependencyResolution:
    dependencies: list[str] = field(default_factory=list)
    conflicts: list[Conflict] = field(default_factory=list)
    nodes: list[DependencyNode] = field(default_factory=list)
    edges: list[DependencyEdge] = field(default_factory=list)
    load_order: list[str] = field(default_factory=list)


@dataclass(frozen=True)
class DependencySpec:
    id: str
    version: str = "*"


@dataclass(frozen=True)
class _AddonManifest:
    name: str
    version: str
    dependencies: tuple[DependencySpec, ...]


_VERSION_TOKEN_RE = re.compile(r"[0-9]+")
_DEPENDENCY_RE = re.compile(r"^(?P<id>[a-zA-Z0-9_.\\-/]+)\s*(?:@|\s+)?\s*(?P<version>[<>=~^!].+)?$")


class DependencyResolver:
    """Resolve graph dependencies and validate compatibility for addon modules."""

    def __init__(self, workspace_root: Path, platform_version: str | None = None) -> None:
        self.workspace_root = workspace_root
        self.platform_version = platform_version or self._load_platform_version()

    def resolve(self, addon: "AddonSpec") -> DependencyResolution:
        resolution = DependencyResolution(dependencies=list(getattr(addon, "dependencies", [])))
        known_ids = self._load_existing_ids()

        self._check_definition_conflicts(addon, known_ids, resolution)
        self._check_platform_compatibility(addon, resolution)
        self._check_graph_resources(addon, resolution)
        self._check_api_dependencies(addon, resolution)
        self._resolve_addon_dependency_graph(addon, resolution)
        return resolution

    def _check_definition_conflicts(self, addon: "AddonSpec", known_ids: set[str], resolution: DependencyResolution) -> None:
        for definition in addon.definitions:
            key = f"{definition.type}:{definition.id}"
            if key in known_ids:
                resolution.conflicts.append(
                    Conflict(
                        kind="duplicate_id",
                        identifier=key,
                        message=f"Definition '{key}' conflicts with existing registry entries",
                    )
                )
            known_ids.add(key)

    def _check_platform_compatibility(self, addon: "AddonSpec", resolution: DependencyResolution) -> None:
        constraint = getattr(addon, "compatible_platform_version", None) or getattr(addon, "compatible_platform", None)
        if _version_matches(self.platform_version, constraint):
            return

        resolution.conflicts.append(
            Conflict(
                kind="platform_version_incompatible",
                identifier=f"platform:{self.platform_version}",
                message=(
                    f"Addon '{addon.name}' requires platform version '{constraint}', "
                    f"but current platform is '{self.platform_version}'"
                ),
            )
        )

    def _check_graph_resources(self, addon: "AddonSpec", resolution: DependencyResolution) -> None:
        asset_db = self.workspace_root / "asset_database.json"
        payload: dict[str, Any] = {}
        if asset_db.exists():
            payload = json.loads(asset_db.read_text(encoding="utf-8"))

        materials = set(payload.get("materials", {}).keys())
        machines = set(payload.get("machines", {}).keys())

        snapshot = self._load_registry_snapshot()
        materials.update(snapshot.get("material_ids", []))
        machines.update(snapshot.get("machine_ids", []))

        materials.update(definition.id for definition in addon.definitions if definition.type == "material")
        machines.update(definition.id for definition in addon.definitions if definition.type == "machine")

        dependency_graph = getattr(addon, "dependency_graph", None)
        for material in getattr(dependency_graph, "materials", []):
            if material in materials:
                continue
            resolution.conflicts.append(
                Conflict(
                    kind="missing_material_dependency",
                    identifier=f"material:{material}",
                    message=f"Required material dependency '{material}' is not available",
                )
            )

        for machine in getattr(dependency_graph, "machines", []):
            if machine in machines:
                continue
            resolution.conflicts.append(
                Conflict(
                    kind="missing_machine_dependency",
                    identifier=f"machine:{machine}",
                    message=f"Required machine dependency '{machine}' is not available",
                )
            )

    def _check_api_dependencies(self, addon: "AddonSpec", resolution: DependencyResolution) -> None:
        dependency_graph = getattr(addon, "dependency_graph", None)
        api_version, protocol_version = self._load_runtime_api_versions()

        for dep in getattr(dependency_graph, "apis", []):
            dep_id = str(getattr(dep, "id", "")).lower()
            dep_version = str(getattr(dep, "version", "*"))
            if dep_id in {"extremecraft-api", "api", "extremecraft_api"}:
                if not _version_matches(api_version, dep_version):
                    resolution.conflicts.append(
                        Conflict(
                            kind="api_version_incompatible",
                            identifier=f"api:{api_version}",
                            message=f"Addon requires API version '{dep_version}' but runtime API is '{api_version}'",
                        )
                    )
                continue

            if dep_id in {"extremecraft-protocol", "protocol", "extremecraft_protocol"}:
                if not _version_matches(protocol_version, dep_version):
                    resolution.conflicts.append(
                        Conflict(
                            kind="protocol_version_incompatible",
                            identifier=f"protocol:{protocol_version}",
                            message=(
                                f"Addon requires protocol version '{dep_version}' "
                                f"but runtime protocol is '{protocol_version}'"
                            ),
                        )
                    )
                continue

            resolution.conflicts.append(
                Conflict(
                    kind="unknown_api_dependency",
                    identifier=f"api:{getattr(dep, 'id', '')}",
                    message=f"Unknown API dependency '{getattr(dep, 'id', '')}' declared in dependency graph",
                )
            )

    def _resolve_addon_dependency_graph(self, addon: "AddonSpec", resolution: DependencyResolution) -> None:
        manifests = self._load_addon_manifests()
        root_key = f"addon:{addon.name}"
        dependency_graph = getattr(addon, "dependency_graph", None)

        nodes: dict[str, DependencyNode] = {
            root_key: DependencyNode(key=root_key, kind="addon", version=addon.version, constraint="self")
        }
        edges: set[DependencyEdge] = set()

        queue: deque[tuple[str, DependencySpec]] = deque(
            (root_key, dep)
            for dep in (
                self._coerce_dependency(entry)
                for entry in getattr(dependency_graph, "addons", [])
            )
            if dep is not None
        )
        processed: set[str] = set()

        while queue:
            parent_key, dep = queue.popleft()
            dep_key = f"addon:{dep.id}"

            nodes.setdefault(dep_key, DependencyNode(key=dep_key, kind="addon", version="unknown", constraint=dep.version))
            edges.add(DependencyEdge(source=dep_key, target=parent_key))

            manifest = manifests.get(dep.id)
            if manifest is None and dep.id == "extremecraft-core":
                manifest = _AddonManifest(name=dep.id, version=self.platform_version, dependencies=tuple())
            if manifest is None:
                resolution.conflicts.append(
                    Conflict(
                        kind="missing_addon_dependency",
                        identifier=dep.id,
                        message=f"Addon dependency '{dep.id}' is not installed in workspace/addons",
                    )
                )
                continue

            nodes[dep_key] = DependencyNode(key=dep_key, kind="addon", version=manifest.version, constraint=dep.version)
            if not _version_matches(manifest.version, dep.version):
                resolution.conflicts.append(
                    Conflict(
                        kind="addon_version_incompatible",
                        identifier=dep.id,
                        message=(
                            f"Addon dependency '{dep.id}' version '{manifest.version}' "
                            f"does not satisfy '{dep.version}'"
                        ),
                    )
                )

            if dep.id in processed:
                continue
            processed.add(dep.id)

            for sub_dep in manifest.dependencies:
                queue.append((dep_key, sub_dep))

        resolution.nodes = [nodes[key] for key in sorted(nodes.keys())]
        resolution.edges = sorted(edges, key=lambda edge: (edge.source, edge.target))
        resolution.load_order = self._topological_order(nodes.keys(), edges, root_key, resolution)

    def _topological_order(
        self,
        node_keys: set[str] | list[str] | tuple[str, ...],
        edges: set[DependencyEdge],
        root_key: str,
        resolution: DependencyResolution,
    ) -> list[str]:
        graph: dict[str, set[str]] = defaultdict(set)
        indegree: dict[str, int] = {key: 0 for key in node_keys}

        for edge in edges:
            if edge.target in graph[edge.source]:
                continue
            graph[edge.source].add(edge.target)
            indegree[edge.target] = indegree.get(edge.target, 0) + 1
            indegree.setdefault(edge.source, 0)

        ready = sorted(key for key, degree in indegree.items() if degree == 0)
        ordered: list[str] = []

        while ready:
            current = ready.pop(0)
            ordered.append(current)
            for next_node in sorted(graph.get(current, set())):
                indegree[next_node] -= 1
                if indegree[next_node] == 0:
                    ready.append(next_node)
            ready.sort()

        if len(ordered) != len(indegree):
            cycle_nodes = sorted(key for key, degree in indegree.items() if degree > 0)
            resolution.conflicts.append(
                Conflict(
                    kind="addon_dependency_cycle",
                    identifier=",".join(cycle_nodes),
                    message=f"Addon dependency graph contains a cycle: {', '.join(cycle_nodes)}",
                )
            )

        return [node.removeprefix("addon:") for node in ordered if node.startswith("addon:") and node != root_key]

    def _load_addon_manifests(self) -> dict[str, _AddonManifest]:
        addons_dir = self.workspace_root / "addons"
        if not addons_dir.exists():
            return {}

        manifests: dict[str, _AddonManifest] = {}
        for addon_root in addons_dir.iterdir():
            if not addon_root.is_dir():
                continue

            manifest_path = addon_root / "addon.json"
            if not manifest_path.exists():
                continue

            raw = json.loads(manifest_path.read_text(encoding="utf-8"))
            name = str(raw.get("name", addon_root.name))
            version = str(raw.get("version", "0.1.0"))
            dependencies = tuple(self._parse_addon_dependencies(raw))
            manifests[name] = _AddonManifest(name=name, version=version, dependencies=dependencies)

        return manifests

    def _parse_addon_dependencies(self, manifest: dict[str, Any]) -> list[DependencySpec]:
        graph = manifest.get("dependency_graph") if isinstance(manifest.get("dependency_graph"), dict) else {}
        dependencies: list[DependencySpec] = []

        for entry in graph.get("addons", []):
            dep = self._coerce_dependency(entry)
            if dep and dep.id not in {item.id for item in dependencies}:
                dependencies.append(dep)

        for entry in manifest.get("dependencies", []):
            dep = self._coerce_dependency(entry)
            if dep and dep.id not in {item.id for item in dependencies}:
                dependencies.append(dep)

        return dependencies

    def _coerce_dependency(self, value: object) -> DependencySpec | None:
        if value is None:
            return None
        if isinstance(value, dict):
            dep_id = str(value.get("id", value.get("name", ""))).strip()
            if not dep_id:
                return None
            constraint = str(value.get("version", value.get("constraint", "*"))).strip() or "*"
            return DependencySpec(id=dep_id, version=constraint)

        dep_id = str(getattr(value, "id", "")).strip()
        dep_version = str(getattr(value, "version", "")).strip()
        if dep_id:
            return DependencySpec(id=dep_id, version=dep_version or "*")

        if not isinstance(value, str):
            return None

        token = value.strip()
        if not token:
            return None

        match = _DEPENDENCY_RE.match(token)
        if not match:
            return DependencySpec(id=token, version="*")

        parsed_id = str(match.group("id") or "").strip()
        if not parsed_id:
            return None

        constraint = str(match.group("version") or "*").strip() or "*"
        return DependencySpec(id=parsed_id, version=constraint)

    def _load_existing_ids(self) -> set[str]:
        payload = self._load_registry_snapshot()
        existing: set[str] = set()
        for key, definition_type in [
            ("item_ids", "item"),
            ("block_ids", "block"),
            ("machine_ids", "machine"),
            ("material_ids", "material"),
            ("ore_ids", "worldgen"),
        ]:
            for entry_id in payload.get(key, []):
                existing.add(f"{definition_type}:{entry_id}")
        return existing

    def _load_registry_snapshot(self) -> dict[str, Any]:
        snapshot_path = self.workspace_root / "registry_snapshot.json"
        if not snapshot_path.exists():
            return {}
        return json.loads(snapshot_path.read_text(encoding="utf-8"))

    def _load_platform_version(self) -> str:
        build_gradle = self.workspace_root.parent / "build.gradle"
        if not build_gradle.exists():
            build_gradle = self.workspace_root / "build.gradle"
        if not build_gradle.exists():
            return "0.0.0"

        pattern = re.compile(r"^\s*version\s*=\s*['\"]([^'\"]+)['\"]", re.MULTILINE)
        match = pattern.search(build_gradle.read_text(encoding="utf-8"))
        return match.group(1).strip() if match else "0.0.0"

    def _load_runtime_api_versions(self) -> tuple[str, str]:
        source = (
            self.workspace_root.parent
            / "api"
            / "src"
            / "main"
            / "java"
            / "com"
            / "extremecraft"
            / "api"
            / "ExtremeCraftApiVersions.java"
        )
        if not source.exists():
            source = (
                self.workspace_root
                / "api"
                / "src"
                / "main"
                / "java"
                / "com"
                / "extremecraft"
                / "api"
                / "ExtremeCraftApiVersions.java"
            )
        if not source.exists():
            return ("1", "1")

        text = source.read_text(encoding="utf-8")
        api_match = re.search(r"EXTREMECRAFT_API_VERSION\s*=\s*([0-9]+)", text)
        protocol_match = re.search(r"EXTREMECRAFT_PROTOCOL_VERSION\s*=\s*([0-9]+)", text)
        return (
            api_match.group(1) if api_match else "1",
            protocol_match.group(1) if protocol_match else "1",
        )


def _version_matches(version: str, constraint: str | None) -> bool:
    token = (constraint or "*").strip()
    if not token or token == "*":
        return True

    checks = [entry.strip() for entry in token.split(",") if entry.strip()]
    if not checks:
        return True

    for check in checks:
        if check.startswith("^"):
            base = check[1:].strip() or "0"
            if _compare_versions(version, base) < 0:
                return False
            if _major(version) != _major(base):
                return False
            continue

        if check.startswith("~"):
            base = check[1:].strip() or "0"
            if _compare_versions(version, base) < 0:
                return False
            if _major_minor(version) != _major_minor(base):
                return False
            continue

        if check.startswith(">="):
            if _compare_versions(version, check[2:].strip() or "0") < 0:
                return False
            continue

        if check.startswith("<="):
            if _compare_versions(version, check[2:].strip() or "0") > 0:
                return False
            continue

        if check.startswith(">"):
            if _compare_versions(version, check[1:].strip() or "0") <= 0:
                return False
            continue

        if check.startswith("<"):
            if _compare_versions(version, check[1:].strip() or "0") >= 0:
                return False
            continue

        if check.startswith("=="):
            if _compare_versions(version, check[2:].strip() or "0") != 0:
                return False
            continue

        if check.startswith("="):
            if _compare_versions(version, check[1:].strip() or "0") != 0:
                return False
            continue

        if _compare_versions(version, check) != 0:
            return False

    return True


def _compare_versions(left: str, right: str) -> int:
    left_parts = _parse_version(left)
    right_parts = _parse_version(right)
    width = max(len(left_parts), len(right_parts))

    for index in range(width):
        lvalue = left_parts[index] if index < len(left_parts) else 0
        rvalue = right_parts[index] if index < len(right_parts) else 0
        if lvalue < rvalue:
            return -1
        if lvalue > rvalue:
            return 1
    return 0


def _parse_version(version: str) -> tuple[int, ...]:
    parts = [int(token) for token in _VERSION_TOKEN_RE.findall(version)]
    return tuple(parts or [0])


def _major(version: str) -> int:
    return _parse_version(version)[0]


def _major_minor(version: str) -> tuple[int, int]:
    parts = _parse_version(version)
    major = parts[0]
    minor = parts[1] if len(parts) > 1 else 0
    return major, minor
