from __future__ import annotations

from collections import defaultdict
from dataclasses import dataclass, field
from pathlib import Path
from typing import Iterable

from asset_studio.code.java_support import JavaAnalysis, analyze_java_source, pascal_case, snake_case
from asset_studio.workspace.index_service import WorkspaceEntry, WorkspaceIndex, WorkspaceIndexService


JAVA_TYPE_SYMBOLS = {"class", "interface", "enum", "record"}
GUI_KINDS = {"gui_source", "gui_runtime"}
MODEL_KINDS = {"model_source", "model_runtime", "item_model", "block_model", "texture_asset"}


@dataclass
class RelationshipTarget:
    relation: str
    kind: str
    path: Path
    label: str
    resource_id: str | None = None
    confidence: str = "possible"
    source: str = "heuristic"
    authoritative: bool = False

    @property
    def state_label(self) -> str:
        return "exact" if self.authoritative else "possible"

    @property
    def navigation_safe(self) -> bool:
        return self.authoritative


@dataclass
class RelationshipRecord:
    path: Path
    kind: str
    resource_id: str | None = None
    targets: list[RelationshipTarget] = field(default_factory=list)
    warnings: list[str] = field(default_factory=list)
    metadata: dict[str, object] = field(default_factory=dict)

    def targets_for(self, relation: str, *, authoritative_only: bool = False) -> list[RelationshipTarget]:
        targets = [target for target in self.targets if target.relation == relation]
        if authoritative_only:
            return [target for target in targets if target.authoritative]
        return targets

    def first_target(self, relation: str) -> RelationshipTarget | None:
        targets = self.targets_for(relation)
        return targets[0] if targets else None

    def preferred_target(self, relation: str | None = None, *, allow_inferred: bool = False) -> RelationshipTarget | None:
        targets = list(self.targets if relation is None else self.targets_for(relation))
        exact = [target for target in targets if target.authoritative]
        if exact:
            return exact[0]
        if not allow_inferred:
            return None
        inferred = [target for target in targets if not target.authoritative]
        if len(inferred) == 1:
            return inferred[0]
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

    def related_targets(self, path: Path, relation: str | None = None) -> list[RelationshipTarget]:
        record = self.resolve_path(path)
        if record is None:
            return []
        return list(record.targets if relation is None else record.targets_for(relation))

    def related_paths(self, path: Path, relation: str | None = None) -> list[Path]:
        return [target.path for target in self.related_targets(path, relation)]

    def first_related_path(self, path: Path, relation: str | None, *, allow_inferred: bool = False) -> Path | None:
        record = self.resolve_path(path)
        if record is None:
            return None
        target = record.preferred_target(relation, allow_inferred=allow_inferred)
        return None if target is None else target.path

    def inspector_payload(self, path: Path) -> dict[str, object]:
        record = self.resolve_path(path)
        if record is None:
            return {}
        return {
            "path": str(record.path),
            "kind": record.kind,
            "resourceId": record.resource_id,
            "warnings": list(record.warnings),
            "metadata": dict(record.metadata),
            "targets": [
                {
                    "relation": target.relation,
                    "kind": target.kind,
                    "path": str(target.path),
                    "label": target.label,
                    "resourceId": target.resource_id,
                    "confidence": target.confidence,
                    "source": target.source,
                    "authoritative": target.authoritative,
                    "state": target.state_label,
                    "navigationSafe": target.navigation_safe,
                }
                for target in record.targets
            ],
        }

    def _build_records(self, index: WorkspaceIndex) -> dict[Path, RelationshipRecord]:
        entries = [entry for entry in index.entries.values() if not entry.is_dir]
        java_analysis: dict[Path, JavaAnalysis] = {}
        class_map: dict[str, list[Path]] = defaultdict(list)
        name_map: dict[str, list[WorkspaceEntry]] = defaultdict(list)
        resource_map: dict[str, list[WorkspaceEntry]] = defaultdict(list)
        reverse_links: dict[Path, list[WorkspaceEntry]] = defaultdict(list)

        for entry in entries:
            normalized_name = _normalized_entry_name(entry.path)
            if normalized_name:
                name_map[normalized_name].append(entry)
            if entry.resource_id:
                resource_map[entry.resource_id].append(entry)
            for target in entry.links.values():
                reverse_links[target.resolve(strict=False)].append(entry)
            if entry.kind == "java_source":
                analysis = self._read_java_analysis(entry.path)
                java_analysis[entry.path] = analysis
                for symbol in analysis.symbols:
                    if symbol.symbol_type in JAVA_TYPE_SYMBOLS:
                        class_map[symbol.name].append(entry.path)

        records: dict[Path, RelationshipRecord] = {}
        for entry in entries:
            record = RelationshipRecord(path=entry.path, kind=entry.kind, resource_id=entry.resource_id)
            record.metadata["badges"] = sorted(entry.badges)
            record.metadata["issues"] = [
                {"severity": issue.severity, "code": issue.code, "message": issue.message, "relatedPath": str(issue.related_path) if issue.related_path else ""}
                for issue in entry.issues
            ]
            record.metadata["links"] = {relation: str(target) for relation, target in sorted(entry.links.items())}
            record.metadata["indexKind"] = entry.kind
            record.warnings.extend(issue.message for issue in entry.issues)
            for relation, target in sorted(entry.links.items()):
                target_entry = index.entry(target)
                record.targets.append(
                    _make_target(
                        relation,
                        target_entry.kind if target_entry is not None else "file",
                        target.resolve(strict=False),
                        target.name,
                        resource_id=target_entry.resource_id if target_entry is not None else None,
                        confidence="exact",
                        source="index",
                        authoritative=True,
                    )
                )

            if entry.kind in GUI_KINDS:
                self._link_gui_record(record, entry, index, java_analysis, class_map, name_map, resource_map)
            elif entry.kind in MODEL_KINDS:
                self._link_model_record(record, entry, index, java_analysis, class_map, name_map, resource_map, reverse_links)
            elif entry.kind == "java_source":
                self._link_java_record(record, entry, index, java_analysis.get(entry.path), name_map, resource_map)
            else:
                self._link_generic_record(record, entry, reverse_links)

            record.targets = _dedupe_targets(record.targets)
            self._annotate_resolution_state(record, java_analysis)
            records[entry.path.resolve(strict=False)] = record
        return records

    def _link_gui_record(
        self,
        record: RelationshipRecord,
        entry: WorkspaceEntry,
        index: WorkspaceIndex,
        java_analysis: dict[Path, JavaAnalysis],
        class_map: dict[str, list[Path]],
        name_map: dict[str, list[WorkspaceEntry]],
        resource_map: dict[str, list[WorkspaceEntry]],
    ) -> None:
        name = _document_name(entry.path)
        if name:
            record.metadata["documentName"] = name
            for candidate in name_map.get(name, []):
                if candidate.path == entry.path or candidate.kind not in GUI_KINDS:
                    continue
                relation = "runtime_export" if candidate.kind == "gui_runtime" else "source_document"
                record.targets.append(
                    _make_target(
                        relation,
                        candidate.kind,
                        candidate.path,
                        candidate.path.name,
                        resource_id=candidate.resource_id,
                        confidence="medium",
                        source="heuristic-name",
                    )
                )
        if entry.resource_id:
            record.metadata["resourceId"] = entry.resource_id
        for class_name in [f"{pascal_case(name)}Screen", f"{pascal_case(name)}Menu", f"{pascal_case(name)}Gui"]:
            for java_path in class_map.get(class_name, []):
                record.targets.append(
                    _make_target(
                        "java_target",
                        "java_source",
                        java_path,
                        java_path.name,
                        confidence="medium",
                        source="heuristic-name",
                    )
                )
        self._link_resource_matches(record, entry.resource_id, resource_map, include_kinds={"java_source"}, relation="java_target")
        if not record.targets_for("source_document", authoritative_only=True) and entry.kind == "gui_runtime":
            explicit = index.related_path(entry.path, "source_definition")
            if explicit is not None:
                target_entry = index.entry(explicit)
                if target_entry is not None:
                    record.targets.append(
                        _make_target(
                            "source_document",
                            target_entry.kind,
                            target_entry.path,
                            target_entry.path.name,
                            resource_id=target_entry.resource_id,
                            confidence="exact",
                            source="index",
                            authoritative=True,
                        )
                    )
        self._annotate_java_metadata(record, java_analysis)

    def _link_model_record(
        self,
        record: RelationshipRecord,
        entry: WorkspaceEntry,
        index: WorkspaceIndex,
        java_analysis: dict[Path, JavaAnalysis],
        class_map: dict[str, list[Path]],
        name_map: dict[str, list[WorkspaceEntry]],
        resource_map: dict[str, list[WorkspaceEntry]],
        reverse_links: dict[Path, list[WorkspaceEntry]],
    ) -> None:
        name = _document_name(entry.path)
        if name:
            record.metadata["documentName"] = name
            for candidate in name_map.get(name, []):
                if candidate.path == entry.path:
                    continue
                if candidate.kind in {"model_source", "model_runtime", "item_model", "block_model"}:
                    relation = _relation_for_candidate(candidate.kind)
                    record.targets.append(
                        _make_target(
                            relation,
                            candidate.kind,
                            candidate.path,
                            candidate.path.name,
                            resource_id=candidate.resource_id,
                            confidence="medium",
                            source="heuristic-name",
                        )
                    )
        if entry.resource_id:
            record.metadata["resourceId"] = entry.resource_id
        for class_name in [
            f"{pascal_case(name)}Block",
            f"{pascal_case(name)}Item",
            f"{pascal_case(name)}Entity",
            f"{pascal_case(name)}Model",
        ]:
            for java_path in class_map.get(class_name, []):
                record.targets.append(
                    _make_target(
                        "java_target",
                        "java_source",
                        java_path,
                        java_path.name,
                        confidence="medium",
                        source="heuristic-name",
                    )
                )
        self._link_resource_matches(record, entry.resource_id, resource_map, include_kinds={"java_source"}, relation="java_target")
        for dependent in reverse_links.get(entry.path.resolve(strict=False), []):
            if dependent.path == entry.path:
                continue
            record.targets.append(
                _make_target(
                    "linked_from",
                    dependent.kind,
                    dependent.path,
                    dependent.path.name,
                    resource_id=dependent.resource_id,
                    confidence="exact",
                    source="index-reverse",
                    authoritative=True,
                )
            )
        self._annotate_java_metadata(record, java_analysis)

    def _link_java_record(
        self,
        record: RelationshipRecord,
        entry: WorkspaceEntry,
        index: WorkspaceIndex,
        analysis: JavaAnalysis | None,
        name_map: dict[str, list[WorkspaceEntry]],
        resource_map: dict[str, list[WorkspaceEntry]],
    ) -> None:
        if analysis is None:
            return
        type_symbols = [symbol for symbol in analysis.symbols if symbol.symbol_type in JAVA_TYPE_SYMBOLS]
        record.metadata.update(
            {
                "package": analysis.package_name or "",
                "imports": list(analysis.imports),
                "types": [symbol.name for symbol in type_symbols],
                "resourceIds": list(analysis.resource_ids),
                "linkedFiles": list(analysis.linked_files),
                "symbolCounts": {
                    "types": len(type_symbols),
                    "fields": len(analysis.by_type("field")),
                    "methods": len(analysis.by_type("method")),
                },
            }
        )
        for diagnostic in analysis.diagnostics:
            record.warnings.append(diagnostic.message)

        for resource_id in analysis.resource_ids:
            for candidate in resource_map.get(resource_id, []):
                if candidate.path == entry.path:
                    continue
                relation = _relation_for_candidate(candidate.kind)
                record.targets.append(
                    _make_target(
                        relation,
                        candidate.kind,
                        candidate.path,
                        candidate.path.name,
                        resource_id=candidate.resource_id,
                        confidence="high",
                        source="heuristic-resource-id",
                    )
                )
                source_path = candidate.links.get("source_definition")
                if source_path is not None:
                    source_entry = index.entry(source_path)
                    if source_entry is not None:
                        record.targets.append(
                            _make_target(
                                "source_document",
                                source_entry.kind,
                                source_entry.path,
                                source_entry.path.name,
                                resource_id=source_entry.resource_id,
                                confidence="high",
                                source="heuristic-resource-id",
                            )
                        )

        for linked_file in analysis.linked_files:
            candidate_path = (self.context.workspace_root / linked_file).resolve(strict=False)
            candidate_entry = index.entry(candidate_path)
            if candidate_entry is not None:
                record.targets.append(
                    _make_target(
                        "source_document",
                        candidate_entry.kind,
                        candidate_entry.path,
                        candidate_entry.path.name,
                        resource_id=candidate_entry.resource_id,
                        confidence="medium",
                        source="heuristic-text-reference",
                    )
                )

        for symbol in type_symbols:
            for base_name in _type_to_candidate_names(symbol.name):
                for candidate in name_map.get(base_name, []):
                    if candidate.path == entry.path:
                        continue
                    relation = _relation_for_candidate(candidate.kind)
                    record.targets.append(
                        _make_target(
                            relation,
                            candidate.kind,
                            candidate.path,
                            candidate.path.name,
                            resource_id=candidate.resource_id,
                            confidence="medium",
                            source="heuristic-name",
                        )
                    )

    def _link_generic_record(self, record: RelationshipRecord, entry: WorkspaceEntry, reverse_links: dict[Path, list[WorkspaceEntry]]) -> None:
        for dependent in reverse_links.get(entry.path.resolve(strict=False), []):
            if dependent.path == entry.path:
                continue
            record.targets.append(
                _make_target(
                    "linked_from",
                    dependent.kind,
                    dependent.path,
                    dependent.path.name,
                    resource_id=dependent.resource_id,
                    confidence="exact",
                    source="index-reverse",
                    authoritative=True,
                )
            )

    def _read_java_analysis(self, path: Path) -> JavaAnalysis:
        try:
            text = path.read_text(encoding="utf-8", errors="ignore")
        except OSError:
            return JavaAnalysis()
        return analyze_java_source(text, path=path)

    def _link_resource_matches(
        self,
        record: RelationshipRecord,
        resource_id: str | None,
        resource_map: dict[str, list[WorkspaceEntry]],
        *,
        include_kinds: set[str],
        relation: str,
    ) -> None:
        if not resource_id:
            return
        for candidate in resource_map.get(resource_id, []):
            if candidate.path == record.path or candidate.kind not in include_kinds:
                continue
            record.targets.append(
                _make_target(
                    relation,
                    candidate.kind,
                    candidate.path,
                    candidate.path.name,
                    resource_id=candidate.resource_id,
                    confidence="high",
                    source="heuristic-resource-id",
                )
            )

    def _annotate_java_metadata(self, record: RelationshipRecord, java_analysis: dict[Path, JavaAnalysis]) -> None:
        java_targets = record.targets_for("java_target")
        record.metadata["javaTargets"] = [str(target.path) for target in java_targets]
        linked_resource_ids: list[str] = []
        for target in java_targets:
            analysis = java_analysis.get(target.path)
            if analysis is None:
                continue
            linked_resource_ids.extend(analysis.resource_ids)
        if linked_resource_ids:
            record.metadata["linkedJavaResourceIds"] = sorted(set(linked_resource_ids))

    def _annotate_resolution_state(self, record: RelationshipRecord, java_analysis: dict[Path, JavaAnalysis]) -> None:
        authoritative_targets = [target for target in record.targets if target.authoritative]
        inferred_targets = [target for target in record.targets if not target.authoritative]
        record.metadata["resolutionCounts"] = {
            "authoritative": len(authoritative_targets),
            "inferred": len(inferred_targets),
        }
        ambiguous_relations: list[str] = []
        for relation in sorted({target.relation for target in record.targets}):
            exact = record.targets_for(relation, authoritative_only=True)
            inferred = [target for target in record.targets_for(relation) if not target.authoritative]
            if len(inferred) > 1 and not exact:
                ambiguous_relations.append(relation)
                record.warnings.append(f"Multiple inferred {relation.replace('_', ' ')} targets are available")
        if ambiguous_relations:
            record.metadata["ambiguousRelations"] = ambiguous_relations


def _make_target(
    relation: str,
    kind: str,
    path: Path,
    label: str,
    *,
    resource_id: str | None = None,
    confidence: str,
    source: str,
    authoritative: bool = False,
) -> RelationshipTarget:
    return RelationshipTarget(
        relation=relation,
        kind=kind,
        path=path.resolve(strict=False),
        label=label,
        resource_id=resource_id,
        confidence=confidence,
        source=source,
        authoritative=authoritative,
    )


def _normalized_entry_name(path: Path) -> str:
    name = path.name
    for suffix in [".gui.json", ".model.json", ".json", ".png", ".java"]:
        if name.endswith(suffix):
            return snake_case(name[: -len(suffix)])
    return snake_case(path.stem)


def _document_name(path: Path) -> str:
    return _normalized_entry_name(path)


def _relation_for_candidate(kind: str) -> str:
    if kind in {"gui_source", "model_source"}:
        return "source_document"
    if kind in {"gui_runtime", "model_runtime", "resource_export", "datapack_export", "generated_artifact"}:
        return "runtime_export"
    if kind == "java_source":
        return "java_target"
    if kind in {"item_model", "block_model", "texture_asset"}:
        return "resource_target"
    return "related_file"


def _type_to_candidate_names(type_name: str) -> list[str]:
    names = [snake_case(type_name)]
    lowered = type_name.lower()
    for suffix in ["screen", "menu", "gui", "block", "item", "entity", "model"]:
        if lowered.endswith(suffix) and len(type_name) > len(suffix):
            names.append(snake_case(type_name[: -len(suffix)]))
    return [name for name in dict.fromkeys(names) if name]


def _dedupe_targets(targets: Iterable[RelationshipTarget]) -> list[RelationshipTarget]:
    selected: dict[tuple[str, Path], RelationshipTarget] = {}
    for target in targets:
        normalized_path = target.path.resolve(strict=False)
        key = (target.relation, normalized_path)
        candidate = _make_target(
            target.relation,
            target.kind,
            normalized_path,
            target.label,
            resource_id=target.resource_id,
            confidence=target.confidence,
            source=target.source,
            authoritative=target.authoritative,
        )
        existing = selected.get(key)
        if existing is None or _target_rank(candidate) < _target_rank(existing):
            selected[key] = candidate
    return sorted(selected.values(), key=lambda target: (target.relation, _target_rank(target), target.label.lower()))


def _target_rank(target: RelationshipTarget) -> tuple[int, int, int, str]:
    confidence_order = {"exact": 0, "high": 1, "medium": 2, "low": 3, "possible": 4}
    source_order = {
        "index": 0,
        "index-reverse": 1,
        "heuristic-resource-id": 2,
        "heuristic-text-reference": 3,
        "heuristic-name": 4,
        "heuristic": 5,
    }
    return (
        0 if target.authoritative else 1,
        confidence_order.get(target.confidence, 9),
        source_order.get(target.source, 9),
        target.path.as_posix(),
    )
