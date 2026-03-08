from __future__ import annotations

import re
from dataclasses import dataclass, field
from pathlib import Path
from typing import Iterable

from asset_studio.code.java_support import JavaAnalysis, analyze_java_source, pascal_case
from asset_studio.workspace.index_service import WorkspaceEntry, WorkspaceIndex, WorkspaceIndexService


@dataclass
class RelationshipTarget:
    relation: str
    kind: str
    path: Path
    label: str
    resource_id: str | None = None


@dataclass
class RelationshipRecord:
    path: Path
    kind: str
    resource_id: str | None = None
    targets: list[RelationshipTarget] = field(default_factory=list)
    warnings: list[str] = field(default_factory=list)
    metadata: dict[str, object] = field(default_factory=dict)

    def targets_for(self, relation: str) -> list[RelationshipTarget]:
        return [target for target in self.targets if target.relation == relation]

    def first_target(self, relation: str) -> RelationshipTarget | None:
        for target in self.targets:
            if target.relation == relation:
                return target
        return None


class RelationshipResolverService:
    def __init__(self, context, workspace_index_service: WorkspaceIndexService) -> None:
        self.context = context
        self.workspace_index_service = workspace_index_service
        self._records: dict[Path, RelationshipRecord] = {}

    def set_context(self, context) -> None:
        self.context = context
        self._records.clear()

    def refresh(self) -> dict[Path, RelationshipRecord]:
        index = self.workspace_index_service.snapshot()
        self._records = self._build_records(index)
        return self._records

    def snapshot(self) -> dict[Path, RelationshipRecord]:
        return self._records or self.refresh()

    def resolve_path(self, path: Path) -> RelationshipRecord | None:
        return self.snapshot().get(path.resolve(strict=False))

    def related_paths(self, path: Path, relation: str | None = None) -> list[Path]:
        record = self.resolve_path(path)
        if record is None:
            return []
        targets = record.targets if relation is None else record.targets_for(relation)
        return [target.path for target in targets]

    def resource_metadata(self, path: Path) -> dict[str, object]:
        record = self.resolve_path(path)
        return {} if record is None else dict(record.metadata)

    def _build_records(self, index: WorkspaceIndex) -> dict[Path, RelationshipRecord]:
        entries = list(index.entries.values())
        runtime_by_stem: dict[str, list[WorkspaceEntry]] = {}
        source_by_stem: dict[str, list[WorkspaceEntry]] = {}
        entry_by_resource: dict[str, list[WorkspaceEntry]] = {}
        java_entries: list[WorkspaceEntry] = []
        java_analysis: dict[Path, JavaAnalysis] = {}
        class_map: dict[str, list[Path]] = {}

        for entry in entries:
            if entry.is_dir:
                continue
            stem = entry.path.stem
            if entry.kind in {"gui_source", "model_source", "item_model", "block_model"}:
                source_by_stem.setdefault(stem, []).append(entry)
            if entry.kind in {"gui_runtime", "model_runtime", "resource_export", "datapack_export", "generated_artifact"}:
                runtime_by_stem.setdefault(stem, []).append(entry)
            if entry.resource_id:
                entry_by_resource.setdefault(entry.resource_id, []).append(entry)
            if entry.kind == "java_source":
                java_entries.append(entry)
                try:
                    analysis = analyze_java_source(entry.path.read_text(encoding="utf-8", errors="ignore"), path=entry.path)
                except OSError:
                    analysis = JavaAnalysis()
                java_analysis[entry.path] = analysis
                for symbol in analysis.symbols:
                    if symbol.symbol_type in {"class", "interface", "enum", "record"}:
                        class_map.setdefault(symbol.name, []).append(entry.path)
                for resource_id in analysis.resource_ids:
                    entry_by_resource.setdefault(resource_id, [])

        records: dict[Path, RelationshipRecord] = {}
        for entry in entries:
            if entry.is_dir:
                continue
            record = RelationshipRecord(path=entry.path, kind=entry.kind, resource_id=entry.resource_id)
            for relation, target in sorted(entry.links.items()):
                target_entry = index.entry(target)
                record.targets.append(
                    RelationshipTarget(
                        relation=relation,
                        kind=target_entry.kind if target_entry is not None else "file",
                        path=target,
                        label=target.name,
                        resource_id=target_entry.resource_id if target_entry is not None else None,
                    )
                )

            if entry.kind in {"gui_source", "gui_runtime"}:
                self._link_gui_relationships(record, entry, index, source_by_stem, runtime_by_stem, class_map, java_analysis)
            elif entry.kind in {"model_source", "model_runtime", "item_model", "block_model", "texture_asset"}:
                self._link_model_relationships(record, entry, index, source_by_stem, runtime_by_stem, class_map, java_analysis)
            elif entry.kind == "java_source":
                self._link_java_relationships(record, entry, index, entry_by_resource, java_analysis.get(entry.path))

            record.targets = _dedupe_targets(record.targets)
            records[entry.path] = record
        return records

    def _link_gui_relationships(
        self,
        record: RelationshipRecord,
        entry: WorkspaceEntry,
        index: WorkspaceIndex,
        source_by_stem: dict[str, list[WorkspaceEntry]],
        runtime_by_stem: dict[str, list[WorkspaceEntry]],
        class_map: dict[str, list[Path]],
        java_analysis: dict[Path, JavaAnalysis],
    ) -> None:
        stem = entry.path.stem.replace(".gui", "")
        for candidate in source_by_stem.get(stem, []):
            if candidate.path != entry.path and candidate.kind == "gui_source":
                record.targets.append(RelationshipTarget("source_document", candidate.kind, candidate.path, candidate.path.name, candidate.resource_id))
        for candidate in runtime_by_stem.get(stem, []):
            if candidate.path != entry.path and candidate.kind == "gui_runtime":
                record.targets.append(RelationshipTarget("runtime_export", candidate.kind, candidate.path, candidate.path.name, candidate.resource_id))
        for class_name in [f"{pascal_case(stem)}Screen", f"{pascal_case(stem)}Menu", f"{pascal_case(stem)}Gui"]:
            for java_path in class_map.get(class_name, []):
                record.targets.append(RelationshipTarget("java_target", "java_source", java_path, java_path.name))
        if not record.targets_for("java_target"):
            for java_path, analysis in java_analysis.items():
                if entry.resource_id and entry.resource_id in analysis.resource_ids:
                    record.targets.append(RelationshipTarget("java_target", "java_source", java_path, java_path.name))

    def _link_model_relationships(
        self,
        record: RelationshipRecord,
        entry: WorkspaceEntry,
        index: WorkspaceIndex,
        source_by_stem: dict[str, list[WorkspaceEntry]],
        runtime_by_stem: dict[str, list[WorkspaceEntry]],
        class_map: dict[str, list[Path]],
        java_analysis: dict[Path, JavaAnalysis],
    ) -> None:
        stem = entry.path.stem.replace(".model", "")
        model_like = [candidate for candidate in source_by_stem.get(stem, []) if candidate.path != entry.path]
        runtime_like = [candidate for candidate in runtime_by_stem.get(stem, []) if candidate.path != entry.path]
        for candidate in model_like:
            relation = "source_document" if candidate.kind in {"model_source", "item_model", "block_model"} else "related_source"
            record.targets.append(RelationshipTarget(relation, candidate.kind, candidate.path, candidate.path.name, candidate.resource_id))
        for candidate in runtime_like:
            if candidate.kind == "model_runtime":
                record.targets.append(RelationshipTarget("runtime_export", candidate.kind, candidate.path, candidate.path.name, candidate.resource_id))
        if entry.kind in {"model_source", "model_runtime"}:
            for candidate in source_by_stem.get(stem, []):
                if candidate.kind in {"item_model", "block_model"}:
                    record.targets.append(RelationshipTarget("resource_target", candidate.kind, candidate.path, candidate.path.name, candidate.resource_id))
        for class_name in [f"{pascal_case(stem)}Block", f"{pascal_case(stem)}Item", f"{pascal_case(stem)}Entity", f"{pascal_case(stem)}Model"]:
            for java_path in class_map.get(class_name, []):
                record.targets.append(RelationshipTarget("java_target", "java_source", java_path, java_path.name))
        if not record.targets_for("java_target"):
            for java_path, analysis in java_analysis.items():
                if entry.resource_id and entry.resource_id in analysis.resource_ids:
                    record.targets.append(RelationshipTarget("java_target", "java_source", java_path, java_path.name))

    def _link_java_relationships(
        self,
        record: RelationshipRecord,
        entry: WorkspaceEntry,
        index: WorkspaceIndex,
        entry_by_resource: dict[str, list[WorkspaceEntry]],
        analysis: JavaAnalysis | None,
    ) -> None:
        if analysis is None:
            return
        record.metadata["package"] = analysis.package_name or ""
        record.metadata["imports"] = list(analysis.imports)
        record.metadata["types"] = [symbol.name for symbol in analysis.symbols if symbol.symbol_type in {"class", "interface", "enum", "record"}]
        record.metadata["resource_ids"] = list(analysis.resource_ids)
        for resource_id in analysis.resource_ids:
            for candidate in entry_by_resource.get(resource_id, []):
                relation = "runtime_export" if candidate.kind in {"gui_runtime", "model_runtime"} else "resource_target"
                record.targets.append(RelationshipTarget(relation, candidate.kind, candidate.path, candidate.path.name, candidate.resource_id))
                for source_relation in ["source_definition", "source_document"]:
                    source_target = candidate.links.get("source_definition")
                    if source_target is not None:
                        source_entry = index.entry(source_target)
                        if source_entry is not None:
                            record.targets.append(RelationshipTarget("source_document", source_entry.kind, source_entry.path, source_entry.path.name, source_entry.resource_id))
        for linked_file in analysis.linked_files:
            candidate = (self.context.workspace_root / linked_file).resolve(strict=False)
            target_entry = index.entry(candidate)
            if target_entry is not None:
                record.targets.append(RelationshipTarget("source_document", target_entry.kind, target_entry.path, target_entry.path.name, target_entry.resource_id))
        if not record.targets and not analysis.resource_ids:
            for symbol_name in analysis.metadata if False else []:
                _ = symbol_name


def _dedupe_targets(targets: Iterable[RelationshipTarget]) -> list[RelationshipTarget]:
    seen: set[tuple[str, Path]] = set()
    ordered: list[RelationshipTarget] = []
    for target in targets:
        key = (target.relation, target.path.resolve(strict=False))
        if key in seen:
            continue
        seen.add(key)
        ordered.append(target)
    return ordered
