from __future__ import annotations

from dataclasses import dataclass, field
from typing import Any


DEFAULT_GRAPH_TYPE = "skilltree"
DEFAULT_SCHEMA_VERSION = 2
DEFAULT_CATEGORIES = ("combat", "survival", "arcane", "exploration", "technology")
DEFAULT_CLASSES = ("adventurer", "warrior", "mage", "rogue", "engineer")


def _sorted_unique(values: list[str]) -> list[str]:
    return sorted({str(value).strip() for value in values if str(value).strip()})


@dataclass(slots=True)
class Modifier:
    type: str
    value: float
    metadata: dict[str, Any] = field(default_factory=dict)

    def clone(self) -> "Modifier":
        return Modifier(type=self.type, value=self.value, metadata=dict(self.metadata))

    def to_dict(self) -> dict[str, Any]:
        payload = {"type": self.type, "value": self.value}
        if self.metadata:
            payload["metadata"] = dict(self.metadata)
        return payload

    @classmethod
    def from_dict(cls, payload: dict[str, Any]) -> "Modifier":
        return cls(
            type=str(payload.get("type", "")).strip(),
            value=float(payload.get("value", 0.0)),
            metadata=dict(payload.get("metadata", {})),
        )

    @property
    def magnitude(self) -> float:
        return abs(float(self.value))


@dataclass(slots=True)
class ProgressionLink:
    source: str
    target: str
    link_type: str = "prerequisite"
    required: bool = True
    metadata: dict[str, Any] = field(default_factory=dict)

    def clone(self) -> "ProgressionLink":
        return ProgressionLink(
            source=self.source,
            target=self.target,
            link_type=self.link_type,
            required=self.required,
            metadata=dict(self.metadata),
        )

    def key(self) -> tuple[str, str, str]:
        return self.source, self.target, self.link_type

    def to_dict(self) -> dict[str, Any]:
        payload = {
            "source": self.source,
            "target": self.target,
            "type": self.link_type,
            "required": self.required,
        }
        if self.metadata:
            payload["metadata"] = dict(self.metadata)
        return payload

    @classmethod
    def from_dict(cls, payload: dict[str, Any]) -> "ProgressionLink":
        return cls(
            source=str(payload.get("source", payload.get("from", ""))).strip(),
            target=str(payload.get("target", payload.get("to", ""))).strip(),
            link_type=str(payload.get("type", "prerequisite")).strip() or "prerequisite",
            required=bool(payload.get("required", True)),
            metadata=dict(payload.get("metadata", {})),
        )


@dataclass(slots=True)
class ProgressionRegion:
    id: str
    label: str
    node_ids: list[str] = field(default_factory=list)
    color: str = "#406280"
    metadata: dict[str, Any] = field(default_factory=dict)

    def clone(self) -> "ProgressionRegion":
        return ProgressionRegion(
            id=self.id,
            label=self.label,
            node_ids=list(self.node_ids),
            color=self.color,
            metadata=dict(self.metadata),
        )

    def to_dict(self) -> dict[str, Any]:
        payload = {
            "id": self.id,
            "label": self.label,
            "nodeIds": list(self.node_ids),
            "color": self.color,
        }
        if self.metadata:
            payload["metadata"] = dict(self.metadata)
        return payload

    @classmethod
    def from_dict(cls, payload: dict[str, Any]) -> "ProgressionRegion":
        return cls(
            id=str(payload.get("id", "")).strip(),
            label=str(payload.get("label", "")).strip(),
            node_ids=[str(value).strip() for value in payload.get("nodeIds", []) if str(value).strip()],
            color=str(payload.get("color", "#406280")),
            metadata=dict(payload.get("metadata", {})),
        )


@dataclass(slots=True)
class ProgressionAnnotation:
    id: str
    text: str
    x: float = 0.0
    y: float = 0.0
    node_ids: list[str] = field(default_factory=list)
    metadata: dict[str, Any] = field(default_factory=dict)

    def clone(self) -> "ProgressionAnnotation":
        return ProgressionAnnotation(
            id=self.id,
            text=self.text,
            x=self.x,
            y=self.y,
            node_ids=list(self.node_ids),
            metadata=dict(self.metadata),
        )

    def to_dict(self) -> dict[str, Any]:
        payload = {
            "id": self.id,
            "text": self.text,
            "x": self.x,
            "y": self.y,
            "nodeIds": list(self.node_ids),
        }
        if self.metadata:
            payload["metadata"] = dict(self.metadata)
        return payload

    @classmethod
    def from_dict(cls, payload: dict[str, Any]) -> "ProgressionAnnotation":
        return cls(
            id=str(payload.get("id", "")).strip(),
            text=str(payload.get("text", "")).strip(),
            x=float(payload.get("x", 0.0)),
            y=float(payload.get("y", 0.0)),
            node_ids=[str(value).strip() for value in payload.get("nodeIds", []) if str(value).strip()],
            metadata=dict(payload.get("metadata", {})),
        )


@dataclass(slots=True)
class GraphBookmark:
    id: str
    label: str
    node_ids: list[str] = field(default_factory=list)
    viewport: dict[str, float] = field(default_factory=dict)
    metadata: dict[str, Any] = field(default_factory=dict)

    def clone(self) -> "GraphBookmark":
        return GraphBookmark(
            id=self.id,
            label=self.label,
            node_ids=list(self.node_ids),
            viewport=dict(self.viewport),
            metadata=dict(self.metadata),
        )

    def to_dict(self) -> dict[str, Any]:
        payload = {
            "id": self.id,
            "label": self.label,
            "nodeIds": list(self.node_ids),
            "viewport": dict(self.viewport),
        }
        if self.metadata:
            payload["metadata"] = dict(self.metadata)
        return payload

    @classmethod
    def from_dict(cls, payload: dict[str, Any]) -> "GraphBookmark":
        return cls(
            id=str(payload.get("id", "")).strip(),
            label=str(payload.get("label", "")).strip(),
            node_ids=[str(value).strip() for value in payload.get("nodeIds", []) if str(value).strip()],
            viewport={str(key): float(value) for key, value in dict(payload.get("viewport", {})).items()},
            metadata=dict(payload.get("metadata", {})),
        )


@dataclass(slots=True)
class SelectionModel:
    node_ids: list[str] = field(default_factory=list)
    region_ids: list[str] = field(default_factory=list)
    annotation_ids: list[str] = field(default_factory=list)
    bookmark_ids: list[str] = field(default_factory=list)

    def clone(self) -> "SelectionModel":
        return SelectionModel(
            node_ids=list(self.node_ids),
            region_ids=list(self.region_ids),
            annotation_ids=list(self.annotation_ids),
            bookmark_ids=list(self.bookmark_ids),
        )


@dataclass(slots=True)
class NodePaletteEntry:
    id: str
    label: str
    category: str
    defaults: dict[str, Any] = field(default_factory=dict)
    tags: list[str] = field(default_factory=list)

    def clone(self) -> "NodePaletteEntry":
        return NodePaletteEntry(
            id=self.id,
            label=self.label,
            category=self.category,
            defaults=dict(self.defaults),
            tags=list(self.tags),
        )


@dataclass(slots=True)
class ProgressionNode:
    id: str
    display_name: str
    category: str = "combat"
    x: float = 0.0
    y: float = 0.0
    cost: int = 1
    requires: list[str] = field(default_factory=list)
    required_level: int = 1
    required_class: str = ""
    modifiers: list[Modifier] = field(default_factory=list)
    tags: list[str] = field(default_factory=list)
    metadata: dict[str, Any] = field(default_factory=dict)
    note: str = ""
    icon: str = ""

    def clone(self) -> "ProgressionNode":
        return ProgressionNode(
            id=self.id,
            display_name=self.display_name,
            category=self.category,
            x=self.x,
            y=self.y,
            cost=self.cost,
            requires=list(self.requires),
            required_level=self.required_level,
            required_class=self.required_class,
            modifiers=[modifier.clone() for modifier in self.modifiers],
            tags=list(self.tags),
            metadata=dict(self.metadata),
            note=self.note,
            icon=self.icon,
        )

    def normalized_requires(self) -> list[str]:
        return _sorted_unique(self.requires)

    def normalized_tags(self) -> list[str]:
        return _sorted_unique(self.tags)

    def sync_requires(self, links: list[ProgressionLink]) -> None:
        self.requires = sorted(
            {
                link.source
                for link in links
                if link.target == self.id and link.link_type == "prerequisite" and link.source
            }
        )

    @property
    def power_score(self) -> float:
        return sum(modifier.magnitude for modifier in self.modifiers)

    @classmethod
    def from_payload(cls, payload: dict[str, Any]) -> "ProgressionNode":
        modifiers = [
            Modifier.from_dict(raw)
            for raw in payload.get("modifiers", [])
            if isinstance(raw, dict)
        ]
        return cls(
            id=str(payload.get("id", "")).strip(),
            display_name=str(payload.get("displayName", payload.get("display_name", ""))).strip(),
            category=str(payload.get("category", "combat")).strip().lower() or "combat",
            x=float(payload.get("x", 0.0)),
            y=float(payload.get("y", 0.0)),
            cost=int(payload.get("cost", 1)),
            requires=[str(value).strip() for value in payload.get("requires", []) if str(value).strip()],
            required_level=int(payload.get("requiredLevel", payload.get("required_level", 1))),
            required_class=str(payload.get("requiredClass", payload.get("required_class", ""))).strip(),
            modifiers=modifiers,
            tags=[str(value).strip() for value in payload.get("tags", []) if str(value).strip()],
            metadata=dict(payload.get("metadata", {})),
            note=str(payload.get("note", "")).strip(),
            icon=str(payload.get("icon", "")).strip(),
        )


@dataclass(slots=True)
class ProgressionDocument:
    name: str
    owner: str = "default"
    class_id: str = "adventurer"
    graph_type: str = DEFAULT_GRAPH_TYPE
    schema_version: int = DEFAULT_SCHEMA_VERSION
    nodes: dict[str, ProgressionNode] = field(default_factory=dict)
    links: list[ProgressionLink] = field(default_factory=list)
    regions: list[ProgressionRegion] = field(default_factory=list)
    annotations: list[ProgressionAnnotation] = field(default_factory=list)
    bookmarks: list[GraphBookmark] = field(default_factory=list)
    metadata: dict[str, Any] = field(default_factory=dict)
    preferences: dict[str, Any] = field(default_factory=dict)

    @property
    def tree_name(self) -> str:
        return self.name

    @tree_name.setter
    def tree_name(self, value: str) -> None:
        self.name = value

    def clone(self) -> "ProgressionDocument":
        return ProgressionDocument(
            name=self.name,
            owner=self.owner,
            class_id=self.class_id,
            graph_type=self.graph_type,
            schema_version=self.schema_version,
            nodes={node_id: node.clone() for node_id, node in self.nodes.items()},
            links=[link.clone() for link in self.links],
            regions=[region.clone() for region in self.regions],
            annotations=[annotation.clone() for annotation in self.annotations],
            bookmarks=[bookmark.clone() for bookmark in self.bookmarks],
            metadata=dict(self.metadata),
            preferences=dict(self.preferences),
        )

    def deterministic_node_ids(self) -> list[str]:
        indegree = {node_id: 0 for node_id in self.nodes}
        outgoing = {node_id: [] for node_id in self.nodes}

        for link in self.normalized_links():
            if link.source in self.nodes and link.target in self.nodes:
                indegree[link.target] += 1
                outgoing[link.source].append(link.target)

        queue = sorted(node_id for node_id, degree in indegree.items() if degree == 0)
        ordered: list[str] = []
        while queue:
            node_id = queue.pop(0)
            ordered.append(node_id)
            for child in sorted(outgoing[node_id]):
                indegree[child] -= 1
                if indegree[child] == 0:
                    queue.append(child)

        for node_id in sorted(self.nodes):
            if node_id not in ordered:
                ordered.append(node_id)
        return ordered

    def incoming_links(self) -> dict[str, list[ProgressionLink]]:
        incoming: dict[str, list[ProgressionLink]] = {node_id: [] for node_id in self.nodes}
        for link in self.normalized_links():
            if link.target in incoming:
                incoming[link.target].append(link)
        return incoming

    def outgoing_links(self) -> dict[str, list[ProgressionLink]]:
        outgoing: dict[str, list[ProgressionLink]] = {node_id: [] for node_id in self.nodes}
        for link in self.normalized_links():
            if link.source in outgoing:
                outgoing[link.source].append(link)
        return outgoing

    def roots(self) -> list[str]:
        incoming = self.incoming_links()
        return sorted(node_id for node_id, links in incoming.items() if not links)

    def normalized_links(self) -> list[ProgressionLink]:
        links_by_key: dict[tuple[str, str, str], ProgressionLink] = {}
        for link in self.links:
            if not link.source or not link.target:
                continue
            links_by_key[link.key()] = link.clone()

        for node in self.nodes.values():
            for req_id in node.normalized_requires():
                link = ProgressionLink(source=req_id, target=node.id)
                links_by_key[link.key()] = link

        return [links_by_key[key] for key in sorted(links_by_key)]

    def sync_links_and_requires(self, prefer: str = "requires") -> None:
        if prefer == "links":
            for node in self.nodes.values():
                node.sync_requires(self.links)
            self.links = self.normalized_links()
            return

        self.links = self.normalized_links()
        for node in self.nodes.values():
            node.sync_requires(self.links)

    def next_node_id(self, base: str = "node") -> str:
        candidate = str(base).strip() or "node"
        if candidate not in self.nodes:
            return candidate
        index = 2
        while f"{candidate}_{index}" in self.nodes:
            index += 1
        return f"{candidate}_{index}"


@dataclass(slots=True)
class ValidationIssue:
    severity: str
    code: str
    message: str
    node_id: str = ""
    field_name: str = ""
    details: dict[str, Any] = field(default_factory=dict)

    def to_dict(self) -> dict[str, Any]:
        payload = {
            "severity": self.severity,
            "code": self.code,
            "message": self.message,
        }
        if self.node_id:
            payload["nodeId"] = self.node_id
        if self.field_name:
            payload["field"] = self.field_name
        if self.details:
            payload["details"] = dict(self.details)
        return payload


@dataclass(slots=True)
class ValidationReport:
    issues: list[ValidationIssue] = field(default_factory=list)
    source: str = ""

    def add(
        self,
        severity: str,
        code: str,
        message: str,
        *,
        node_id: str = "",
        field_name: str = "",
        details: dict[str, Any] | None = None,
    ) -> None:
        self.issues.append(
            ValidationIssue(
                severity=severity,
                code=code,
                message=message,
                node_id=node_id,
                field_name=field,
                details=dict(details or {}),
            )
        )

    def extend(self, issues: list[ValidationIssue]) -> None:
        self.issues.extend(issues)

    @property
    def errors(self) -> list[str]:
        return [issue.message for issue in self.issues if issue.severity == "error"]

    @property
    def warnings(self) -> list[str]:
        return [issue.message for issue in self.issues if issue.severity == "warning"]

    @property
    def infos(self) -> list[str]:
        return [issue.message for issue in self.issues if issue.severity == "info"]

    @property
    def has_errors(self) -> bool:
        return any(issue.severity == "error" for issue in self.issues)

    def to_dict(self) -> dict[str, Any]:
        return {
            "source": self.source,
            "errors": len(self.errors),
            "warnings": len(self.warnings),
            "infos": len(self.infos),
            "issues": [issue.to_dict() for issue in self.issues],
        }


@dataclass(slots=True)
class LockReason:
    code: str
    message: str
    blocking_node_id: str = ""

    def to_dict(self) -> dict[str, Any]:
        payload = {"code": self.code, "message": self.message}
        if self.blocking_node_id:
            payload["blockingNodeId"] = self.blocking_node_id
        return payload


@dataclass(slots=True)
class SimulationRequest:
    player_level: int = 1
    skill_points: int = 0
    selected_class: str = "adventurer"
    requested_unlocks: list[str] = field(default_factory=list)
    pre_unlocked: list[str] = field(default_factory=list)
    ignore_point_costs: bool = False
    count_pre_unlocked_costs: bool = False


@dataclass(slots=True)
class SimulationNodeState:
    node_id: str
    unlocked: bool
    available: bool
    reasons: list[LockReason] = field(default_factory=list)
    cumulative_cost: int = 0

    def to_dict(self) -> dict[str, Any]:
        return {
            "nodeId": self.node_id,
            "unlocked": self.unlocked,
            "available": self.available,
            "cumulativeCost": self.cumulative_cost,
            "reasons": [reason.to_dict() for reason in self.reasons],
        }


@dataclass(slots=True)
class SimulationResult:
    request: SimulationRequest
    node_states: dict[str, SimulationNodeState]
    unlocked_nodes: list[str]
    available_nodes: list[str]
    spent_points: int
    remaining_points: int
    cumulative_modifiers: dict[str, float]
    invalid_paths: list[str] = field(default_factory=list)
    traversal_order: list[str] = field(default_factory=list)

    def why_locked(self, node_id: str) -> list[LockReason]:
        state = self.node_states.get(node_id)
        if state is None:
            return [LockReason(code="missing-node", message=f"Node '{node_id}' does not exist")]
        return list(state.reasons)

    def to_dict(self) -> dict[str, Any]:
        return {
            "request": {
                "playerLevel": self.request.player_level,
                "skillPoints": self.request.skill_points,
                "selectedClass": self.request.selected_class,
                "requestedUnlocks": list(self.request.requested_unlocks),
                "preUnlocked": list(self.request.pre_unlocked),
                "ignorePointCosts": self.request.ignore_point_costs,
            },
            "unlockedNodes": list(self.unlocked_nodes),
            "availableNodes": list(self.available_nodes),
            "spentPoints": self.spent_points,
            "remainingPoints": self.remaining_points,
            "cumulativeModifiers": dict(self.cumulative_modifiers),
            "invalidPaths": list(self.invalid_paths),
            "traversalOrder": list(self.traversal_order),
            "nodeStates": {node_id: state.to_dict() for node_id, state in self.node_states.items()},
        }


@dataclass(slots=True)
class NodeEfficiency:
    node_id: str
    power_score: float
    efficiency: float
    cost: int
    modifiers: dict[str, float] = field(default_factory=dict)

    def to_dict(self) -> dict[str, Any]:
        return {
            "nodeId": self.node_id,
            "powerScore": self.power_score,
            "efficiency": self.efficiency,
            "cost": self.cost,
            "modifiers": dict(self.modifiers),
        }


@dataclass(slots=True)
class PathCostEntry:
    node_id: str
    path: list[str]
    total_cost: int
    total_level_requirement: int

    def to_dict(self) -> dict[str, Any]:
        return {
            "nodeId": self.node_id,
            "path": list(self.path),
            "totalCost": self.total_cost,
            "totalLevelRequirement": self.total_level_requirement,
        }


@dataclass(slots=True)
class BranchAnalysis:
    root_node_id: str
    node_ids: list[str]
    total_cost: int
    total_power: float
    average_efficiency: float
    capstones: list[str] = field(default_factory=list)

    def to_dict(self) -> dict[str, Any]:
        return {
            "rootNodeId": self.root_node_id,
            "nodeIds": list(self.node_ids),
            "totalCost": self.total_cost,
            "totalPower": self.total_power,
            "averageEfficiency": self.average_efficiency,
            "capstones": list(self.capstones),
        }


@dataclass(slots=True)
class LevelBandReport:
    label: str
    node_ids: list[str]
    total_cost: int
    total_power: float

    def to_dict(self) -> dict[str, Any]:
        return {
            "label": self.label,
            "nodeIds": list(self.node_ids),
            "totalCost": self.total_cost,
            "totalPower": self.total_power,
        }


@dataclass(slots=True)
class HeatmapEntry:
    node_id: str
    value: float
    reason: str

    def to_dict(self) -> dict[str, Any]:
        return {
            "nodeId": self.node_id,
            "value": self.value,
            "reason": self.reason,
        }


@dataclass(slots=True)
class BalanceFinding:
    severity: str
    code: str
    message: str
    node_id: str = ""
    branch_root_id: str = ""

    def to_dict(self) -> dict[str, Any]:
        payload = {
            "severity": self.severity,
            "code": self.code,
            "message": self.message,
        }
        if self.node_id:
            payload["nodeId"] = self.node_id
        if self.branch_root_id:
            payload["branchRootId"] = self.branch_root_id
        return payload


@dataclass(slots=True)
class BalanceReport:
    node_efficiency: dict[str, NodeEfficiency] = field(default_factory=dict)
    branches: dict[str, BranchAnalysis] = field(default_factory=dict)
    path_costs: dict[str, PathCostEntry] = field(default_factory=dict)
    level_bands: list[LevelBandReport] = field(default_factory=list)
    findings: list[BalanceFinding] = field(default_factory=list)
    heatmap: dict[str, HeatmapEntry] = field(default_factory=dict)
    simulated_totals: dict[str, float] = field(default_factory=dict)
    cumulative_totals_by_node: dict[str, dict[str, float]] = field(default_factory=dict)

    def to_dict(self) -> dict[str, Any]:
        return {
            "nodeEfficiency": {node_id: entry.to_dict() for node_id, entry in self.node_efficiency.items()},
            "branches": {node_id: branch.to_dict() for node_id, branch in self.branches.items()},
            "pathCosts": {node_id: path.to_dict() for node_id, path in self.path_costs.items()},
            "levelBands": [band.to_dict() for band in self.level_bands],
            "findings": [finding.to_dict() for finding in self.findings],
            "heatmap": {node_id: entry.to_dict() for node_id, entry in self.heatmap.items()},
            "simulatedTotals": dict(self.simulated_totals),
            "cumulativeTotalsByNode": {
                node_id: dict(totals) for node_id, totals in self.cumulative_totals_by_node.items()
            },
        }


@dataclass(slots=True)
class DocumentDiff:
    added_nodes: list[str] = field(default_factory=list)
    removed_nodes: list[str] = field(default_factory=list)
    changed_nodes: list[str] = field(default_factory=list)
    added_links: list[tuple[str, str]] = field(default_factory=list)
    removed_links: list[tuple[str, str]] = field(default_factory=list)
    changed_metadata: list[str] = field(default_factory=list)

    def to_dict(self) -> dict[str, Any]:
        return {
            "addedNodes": list(self.added_nodes),
            "removedNodes": list(self.removed_nodes),
            "changedNodes": list(self.changed_nodes),
            "addedLinks": list(self.added_links),
            "removedLinks": list(self.removed_links),
            "changedMetadata": list(self.changed_metadata),
        }


SkillNode = ProgressionNode
SkillTree = ProgressionDocument

