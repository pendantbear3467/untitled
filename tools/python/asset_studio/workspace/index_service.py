from __future__ import annotations

import json
import re
from collections import defaultdict
from dataclasses import dataclass, field
from pathlib import Path
from typing import Any

from asset_studio.runtime.task_results import utc_now


_REGISTER_PATTERN = re.compile(r'\.register\("([a-z0-9_]+)"')
_ITEM_HINT = re.compile(r"DeferredRegister<Item>|RegistryObject<Item>")
_BLOCK_HINT = re.compile(r"DeferredRegister<Block>|RegistryObject<Block>")
_RESOURCE_ID_RE = re.compile(r'"resourceId"\s*:\s*"([^"]+)"')
_MACHINE_TOKENS = ("machine", "generator", "crusher", "assembler", "reactor", "smelter")


@dataclass
class WorkspaceIssue:
    severity: str
    code: str
    message: str
    related_path: Path | None = None


@dataclass
class WorkspaceEntry:
    path: Path
    kind: str
    is_dir: bool
    badges: set[str] = field(default_factory=set)
    links: dict[str, Path] = field(default_factory=dict)
    issues: list[WorkspaceIssue] = field(default_factory=list)
    resource_id: str | None = None
    metadata: dict[str, Any] = field(default_factory=dict)

    @property
    def search_text(self) -> str:
        return " ".join(
            part
            for part in [
                str(self.path),
                self.path.name,
                self.kind,
                self.resource_id or "",
                " ".join(sorted(self.badges)),
                " ".join(issue.message for issue in self.issues),
            ]
            if part
        ).lower()


@dataclass
class WorkspaceIndex:
    workspace_root: Path
    repo_root: Path
    generated_at: str
    entries: dict[Path, WorkspaceEntry]
    children: dict[Path, list[Path]]
    issues: list[WorkspaceIssue]
    duplicate_resource_ids: dict[str, list[Path]] = field(default_factory=dict)

    def entry(self, path: Path) -> WorkspaceEntry | None:
        return self.entries.get(path.resolve(strict=False))

    def children_of(self, path: Path) -> list[WorkspaceEntry]:
        normalized = path.resolve(strict=False)
        return [self.entries[child] for child in self.children.get(normalized, []) if child in self.entries]

    def related_path(self, path: Path, relation: str) -> Path | None:
        entry = self.entry(path)
        if entry is None:
            return None
        return entry.links.get(relation)


class WorkspaceIndexService:
    def __init__(self, context) -> None:
        self.context = context
        self._index: WorkspaceIndex | None = None

    def set_context(self, context) -> None:
        self.context = context
        self._index = None

    def refresh(self) -> WorkspaceIndex:
        self._index = self._build_index()
        return self._index

    def snapshot(self) -> WorkspaceIndex:
        return self._index or self.refresh()

    def entry(self, path: Path) -> WorkspaceEntry | None:
        return self.snapshot().entry(path)

    def related_path(self, path: Path, relation: str) -> Path | None:
        return self.snapshot().related_path(path, relation)

    def _build_index(self) -> WorkspaceIndex:
        workspace_root = self.context.workspace_root.resolve(strict=False)
        repo_root = self.context.repo_root.resolve(strict=False)
        entries: dict[Path, WorkspaceEntry] = {}
        children: dict[Path, set[Path]] = defaultdict(set)
        issues: list[WorkspaceIssue] = []
        resource_id_map: dict[str, list[Path]] = defaultdict(list)
        modid = self._load_modid(workspace_root)

        self._ensure_entry(entries, children, workspace_root, "workspace_root", is_dir=True)
        self._add_workspace_entries(workspace_root, repo_root, entries, children, modid)
        self._add_repo_context_entries(repo_root, workspace_root, entries, children)
        java_targets = self._scan_java_targets(repo_root, workspace_root, entries, children)

        for entry in list(entries.values()):
            if entry.is_dir:
                continue
            self._populate_links(entry, entries, workspace_root, repo_root, modid, java_targets)
            if entry.resource_id:
                resource_id_map[entry.resource_id].append(entry.path)
            issues.extend(entry.issues)

        duplicates = {resource_id: paths for resource_id, paths in resource_id_map.items() if len(paths) > 1}
        for resource_id, paths in duplicates.items():
            for path in paths:
                entry = entries[path]
                related = next((candidate for candidate in paths if candidate != path), None)
                self._add_issue(
                    entry,
                    "warning",
                    "duplicate_resource_id",
                    f"Duplicate resource id detected: {resource_id}",
                    related_path=related,
                )
                self._apply_badges(entry)
                issues.append(entry.issues[-1])

        finalized_children = {
            parent: sorted(
                items,
                key=lambda item: (entries[item].kind != "folder", item.name.lower()),
            )
            for parent, items in children.items()
            if parent in entries
        }
        return WorkspaceIndex(
            workspace_root=workspace_root,
            repo_root=repo_root,
            generated_at=utc_now(),
            entries=entries,
            children=finalized_children,
            issues=issues,
            duplicate_resource_ids=duplicates,
        )

    def _add_workspace_entries(
        self,
        workspace_root: Path,
        repo_root: Path,
        entries: dict[Path, WorkspaceEntry],
        children: dict[Path, set[Path]],
        modid: str,
    ) -> None:
        ignored = {".git", "__pycache__", ".pytest_cache"}
        for path in sorted(workspace_root.rglob("*")):
            if any(part in ignored for part in path.parts):
                continue
            kind = self._classify_workspace_path(path, workspace_root, modid)
            entry = self._ensure_entry(entries, children, path, kind, is_dir=path.is_dir())
            entry.metadata["scope"] = "workspace"

    def _add_repo_context_entries(
        self,
        repo_root: Path,
        workspace_root: Path,
        entries: dict[Path, WorkspaceEntry],
        children: dict[Path, set[Path]],
    ) -> None:
        for relative in [Path("build"), Path("config"), Path("docs"), Path("tests")]:
            candidate = repo_root / relative
            if not candidate.exists():
                continue
            for path in [candidate, *sorted(candidate.rglob("*"))]:
                kind = f"repo_{relative.name}" if path.is_dir() else relative.name.rstrip("s")
                entry = self._ensure_entry(entries, children, path, kind, is_dir=path.is_dir())
                entry.metadata["scope"] = "repo"

    def _scan_java_targets(
        self,
        repo_root: Path,
        workspace_root: Path,
        entries: dict[Path, WorkspaceEntry],
        children: dict[Path, set[Path]],
    ) -> dict[str, dict[str, list[Path]]]:
        java_root = repo_root / "src" / "main" / "java"
        targets: dict[str, dict[str, list[Path]]] = {
            "item": defaultdict(list),
            "block": defaultdict(list),
            "machine": defaultdict(list),
        }
        if not java_root.exists():
            return targets

        for path in sorted(java_root.rglob("*.java")):
            entry = self._ensure_entry(entries, children, path, "java_source", is_dir=False)
            entry.metadata["scope"] = "repo"
            text = path.read_text(encoding="utf-8", errors="ignore")
            is_item = bool(_ITEM_HINT.search(text))
            is_block = bool(_BLOCK_HINT.search(text))
            for target_id in _REGISTER_PATTERN.findall(text):
                if is_item:
                    targets["item"][target_id].append(path)
                if is_block:
                    targets["block"][target_id].append(path)
                if any(token in target_id for token in _MACHINE_TOKENS):
                    targets["machine"][target_id].append(path)
        return targets

    def _populate_links(
        self,
        entry: WorkspaceEntry,
        entries: dict[Path, WorkspaceEntry],
        workspace_root: Path,
        repo_root: Path,
        modid: str,
        java_targets: dict[str, dict[str, list[Path]]],
    ) -> None:
        if not entry.path.is_relative_to(workspace_root):
            self._apply_badges(entry)
            return
        relative = entry.path.relative_to(workspace_root)

        if entry.kind == "gui_source":
            name = entry.path.name.removesuffix(".gui.json")
            runtime_path = workspace_root / "assets" / modid / "studio" / "gui" / f"{name}.json"
            entry.resource_id = f"{modid}:studio/gui/{name}"
            self._link_pair(entry, "runtime_export", runtime_path)
            if not runtime_path.exists():
                self._add_issue(entry, "warning", "missing_runtime_export", f"Runtime export not found: {runtime_path}", related_path=runtime_path)
            elif self._is_stale(entry.path, runtime_path):
                self._add_issue(entry, "warning", "stale_runtime_export", f"Runtime export is older than source definition: {runtime_path}", related_path=runtime_path)
        elif entry.kind == "model_source":
            name = entry.path.name.removesuffix(".model.json")
            runtime_path = workspace_root / "assets" / modid / "studio" / "models" / f"{name}.json"
            entry.resource_id = f"{modid}:studio/models/{name}"
            self._link_pair(entry, "runtime_export", runtime_path)
            if not runtime_path.exists():
                self._add_issue(entry, "warning", "missing_runtime_export", f"Runtime export not found: {runtime_path}", related_path=runtime_path)
            elif self._is_stale(entry.path, runtime_path):
                self._add_issue(entry, "warning", "stale_runtime_export", f"Runtime export is older than source definition: {runtime_path}", related_path=runtime_path)
        elif entry.kind in {"gui_runtime", "model_runtime"}:
            name = entry.path.stem
            source_path = (
                workspace_root / "gui_screens" / f"{name}.gui.json"
                if entry.kind == "gui_runtime"
                else workspace_root / "models" / "studio" / f"{name}.model.json"
            )
            self._link_pair(entry, "source_definition", source_path)
            payload = self._safe_load_json(entry.path)
            if isinstance(payload, dict):
                resource_id = payload.get("resourceId")
                entry.resource_id = str(resource_id) if resource_id else None
            if not source_path.exists():
                self._add_issue(entry, "warning", "missing_linked_source", f"Source definition not found: {source_path}", related_path=source_path)
                self._add_issue(entry, "warning", "orphaned_generated_file", f"Generated runtime export has no source definition: {entry.path}", related_path=source_path)
            elif self._is_stale(source_path, entry.path):
                self._add_issue(entry, "warning", "stale_runtime_export", f"Runtime export is older than source definition: {entry.path}", related_path=source_path)
        elif entry.kind == "datapack_export":
            source_path = workspace_root / relative.relative_to(Path("exports") / "datapack")
            self._link_pair(entry, "source_definition", source_path)
            self._check_generated_pair(entry, source_path, stale_code="stale_export", missing_code="missing_linked_source")
            if not source_path.exists():
                self._add_issue(entry, "warning", "orphaned_generated_file", f"Generated datapack export has no source file: {entry.path}", related_path=source_path)
        elif entry.kind == "resource_export":
            source_path = workspace_root / Path(*relative.parts[2:])
            self._link_pair(entry, "source_definition", source_path)
            self._check_generated_pair(entry, source_path, stale_code="stale_export", missing_code="missing_linked_source")
            if not source_path.exists():
                self._add_issue(entry, "warning", "orphaned_generated_file", f"Generated asset export has no source file: {entry.path}", related_path=source_path)
        elif entry.kind == "generated_artifact":
            graph_name = self._graph_name_from_generated(relative)
            source_path = workspace_root / "graphs" / f"{graph_name}.json"
            self._link_pair(entry, "source_definition", source_path)
            self._check_generated_pair(entry, source_path, stale_code="stale_generated_artifact", missing_code="missing_linked_source")
            if not source_path.exists():
                self._add_issue(entry, "warning", "orphaned_generated_file", f"Generated graph artifact has no source graph: {entry.path}", related_path=source_path)
        elif entry.kind in {"item_model", "block_model"}:
            scope = "item" if entry.kind == "item_model" else "block"
            model_id = entry.path.stem
            entry.resource_id = f"{modid}:{scope}/{model_id}"
            self._link_model_dependencies(entry, workspace_root, modid)
            java_target = self._first_existing(java_targets[scope].get(model_id, []))
            if java_target is None and scope == "block":
                java_target = self._first_existing(java_targets["machine"].get(model_id, []))
            if java_target is None:
                self._add_issue(entry, "warning", "missing_java_target", f"No Java registry target found for '{model_id}'")
            else:
                self._link_pair(entry, "java_target", java_target)
        elif entry.kind == "texture_asset":
            self._link_texture_dependents(entry, workspace_root)
        elif entry.kind == "graph_source":
            graph_name = entry.path.stem
            candidates = [
                workspace_root / "generated" / graph_name / "content_bundle.json",
                workspace_root / "generated" / f"{graph_name}.bundle.json",
            ]
            linked_any = False
            for target in candidates:
                if not target.exists():
                    continue
                linked_any = True
                self._link_pair(entry, "generated_artifact", target)
                if self._is_stale(entry.path, target):
                    self._add_issue(entry, "warning", "stale_generated_artifact", f"Generated artifact is older than graph source: {target}", related_path=target)
            if not linked_any:
                self._add_issue(entry, "warning", "missing_runtime_export", f"Generated artifact not found for graph: {entry.path}")

        self._apply_badges(entry)

    def _link_model_dependencies(self, entry: WorkspaceEntry, workspace_root: Path, modid: str) -> None:
        payload = self._safe_load_json(entry.path)
        if not isinstance(payload, dict):
            self._add_issue(entry, "warning", "invalid_json", f"Could not parse JSON: {entry.path}")
            return

        textures = payload.get("textures")
        if not isinstance(textures, dict):
            return
        for texture_ref in textures.values():
            texture_path = self._resolve_texture_path(str(texture_ref), workspace_root, modid)
            if texture_path is None:
                continue
            self._link_pair(entry, "texture", texture_path)
            if not texture_path.exists():
                self._add_issue(entry, "warning", "missing_texture", f"Missing texture: {texture_path}", related_path=texture_path)

    def _link_texture_dependents(self, entry: WorkspaceEntry, workspace_root: Path) -> None:
        textures_root = workspace_root / "assets" / "textures"
        if not entry.path.is_relative_to(textures_root):
            return
        relative = entry.path.relative_to(textures_root)
        if len(relative.parts) < 2:
            return
        stem = entry.path.stem
        if relative.parts[0] == "item":
            model_path = workspace_root / "assets" / "models" / "item" / f"{stem}.json"
        elif relative.parts[0] == "block":
            model_path = workspace_root / "assets" / "models" / "block" / f"{stem}.json"
        else:
            return
        if model_path.exists():
            self._link_pair(entry, "source_definition", model_path)

    def _check_generated_pair(self, entry: WorkspaceEntry, source_path: Path, *, stale_code: str, missing_code: str) -> None:
        if not source_path.exists():
            self._add_issue(entry, "warning", missing_code, f"Linked source not found: {source_path}", related_path=source_path)
            return
        if self._is_stale(source_path, entry.path):
            self._add_issue(entry, "warning", stale_code, f"Generated file is older than source: {entry.path}", related_path=source_path)

    def _apply_badges(self, entry: WorkspaceEntry) -> None:
        entry.badges.clear()
        if entry.kind in {"gui_runtime", "model_runtime", "datapack_export", "resource_export", "generated_artifact", "build_output"}:
            entry.badges.add("generated")
        if entry.links:
            entry.badges.add("linked")
        if any(issue.code.startswith("stale") for issue in entry.issues):
            entry.badges.add("stale")
        if any(issue.code in {"missing_runtime_export", "missing_linked_source", "missing_texture", "missing_java_target"} for issue in entry.issues):
            entry.badges.add("missing target")
        if entry.issues:
            entry.badges.add("invalid")

    def _load_modid(self, workspace_root: Path) -> str:
        project_file = workspace_root / "project.json"
        if not project_file.exists():
            return "extremecraft"
        try:
            payload = json.loads(project_file.read_text(encoding="utf-8"))
        except json.JSONDecodeError:
            return "extremecraft"
        return str(payload.get("modid", "extremecraft"))

    def _classify_workspace_path(self, path: Path, workspace_root: Path, modid: str) -> str:
        if path.is_dir():
            return "workspace_root" if path.resolve(strict=False) == workspace_root else "folder"

        relative = path.relative_to(workspace_root)
        parts = relative.parts
        name = path.name

        if len(parts) >= 2 and parts[0] == "gui_screens" and name.endswith(".gui.json"):
            return "gui_source"
        if len(parts) >= 3 and parts[0] == "models" and parts[1] == "studio" and name.endswith(".model.json"):
            return "model_source"
        if len(parts) >= 4 and parts[0] == "assets" and parts[1] == modid and parts[2] == "studio" and parts[3] == "gui":
            return "gui_runtime"
        if len(parts) >= 4 and parts[0] == "assets" and parts[1] == modid and parts[2] == "studio" and parts[3] == "models":
            return "model_runtime"
        if len(parts) >= 2 and parts[0] == "exports" and parts[1] == "datapack":
            return "datapack_export"
        if len(parts) >= 2 and parts[0] == "exports" and parts[1].endswith("_assets"):
            return "resource_export"
        if parts and parts[0] == "generated":
            return "generated_artifact"
        if parts[:3] in [("assets", "textures", "item"), ("assets", "textures", "block")]:
            return "texture_asset"
        if parts[:3] == ("assets", "models", "item"):
            return "item_model"
        if parts[:3] == ("assets", "models", "block"):
            return "block_model"
        if parts[:2] == ("assets", "blockstates"):
            return "blockstate"
        if parts and parts[0] == "graphs" and path.suffix == ".json":
            return "graph_source"
        if parts and parts[0] == "build":
            return "build_output"
        if parts and parts[0] == "docs":
            return "doc"
        if parts and parts[0] == "tests":
            return "test"
        if parts and parts[0] == "config":
            return "config"
        if path.suffix == ".java":
            return "java_source"
        if path.suffix == ".json":
            return "json"
        return path.suffix.lstrip(".") or "file"

    def _ensure_entry(
        self,
        entries: dict[Path, WorkspaceEntry],
        children: dict[Path, set[Path]],
        path: Path,
        kind: str,
        *,
        is_dir: bool,
    ) -> WorkspaceEntry:
        normalized = path.resolve(strict=False)
        self._ensure_parent_chain(entries, children, normalized)
        entry = entries.get(normalized)
        if entry is None:
            entry = WorkspaceEntry(path=normalized, kind=kind, is_dir=is_dir)
            entries[normalized] = entry
        else:
            if entry.kind == "folder" or entry.kind == "file":
                entry.kind = kind
            entry.is_dir = is_dir
        if normalized.parent != normalized:
            children[normalized.parent].add(normalized)
        return entry

    def _ensure_parent_chain(self, entries: dict[Path, WorkspaceEntry], children: dict[Path, set[Path]], path: Path) -> None:
        workspace_root = self.context.workspace_root.resolve(strict=False)
        repo_root = self.context.repo_root.resolve(strict=False)
        stops = {workspace_root, repo_root, workspace_root.parent, repo_root.parent}
        parent = path.parent
        while parent != path:
            if parent not in entries:
                kind = "workspace_root" if parent == workspace_root else "folder"
                entries[parent] = WorkspaceEntry(path=parent, kind=kind, is_dir=True)
            if parent.parent != parent:
                children[parent.parent].add(parent)
            if parent in stops:
                break
            path = parent
            parent = parent.parent

    def _safe_load_json(self, path: Path) -> dict[str, Any] | None:
        try:
            text = path.read_text(encoding="utf-8")
        except OSError:
            return None
        try:
            return json.loads(text)
        except json.JSONDecodeError:
            match = _RESOURCE_ID_RE.search(text)
            if match:
                return {"resourceId": match.group(1)}
            return None

    def _resolve_texture_path(self, texture_ref: str, workspace_root: Path, modid: str) -> Path | None:
        ref = texture_ref.strip()
        if not ref or ref.startswith("#"):
            return None
        if ref.startswith("textures/"):
            return workspace_root / "assets" / ref
        namespace = modid
        relative = ref
        if ":" in ref:
            namespace, relative = ref.split(":", 1)
        if namespace not in {modid, "extremecraft"}:
            return None
        return workspace_root / "assets" / "textures" / f"{relative}.png"

    def _graph_name_from_generated(self, relative: Path) -> str:
        parts = relative.parts
        if len(parts) >= 2 and parts[1].endswith(".bundle.json"):
            return Path(parts[1]).stem.replace(".bundle", "")
        if len(parts) >= 3:
            return parts[1]
        return relative.stem.replace(".bundle", "")

    def _link_pair(self, entry: WorkspaceEntry, relation: str, target: Path) -> None:
        entry.links[relation] = target.resolve(strict=False)

    def _add_issue(
        self,
        entry: WorkspaceEntry,
        severity: str,
        code: str,
        message: str,
        *,
        related_path: Path | None = None,
    ) -> None:
        entry.issues.append(
            WorkspaceIssue(
                severity=severity,
                code=code,
                message=message,
                related_path=related_path.resolve(strict=False) if related_path is not None else None,
            )
        )

    def _is_stale(self, source_path: Path, generated_path: Path) -> bool:
        try:
            return source_path.exists() and generated_path.exists() and source_path.stat().st_mtime > generated_path.stat().st_mtime
        except OSError:
            return False

    def _first_existing(self, candidates: list[Path]) -> Path | None:
        for candidate in candidates:
            if candidate.exists():
                return candidate.resolve(strict=False)
        return None
