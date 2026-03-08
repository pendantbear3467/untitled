from __future__ import annotations

from collections import deque

from asset_studio.skilltree.models import (
    DEFAULT_CATEGORIES,
    DEFAULT_CLASSES,
    ProgressionDocument,
    ValidationIssue,
    ValidationReport,
)


class ProgressionValidator:
    def __init__(
        self,
        *,
        allowed_categories: tuple[str, ...] = DEFAULT_CATEGORIES,
        known_classes: tuple[str, ...] = DEFAULT_CLASSES,
        cost_range: tuple[int, int] = (1, 999),
        level_range: tuple[int, int] = (1, 999),
        modifier_range: tuple[float, float] = (-100000.0, 100000.0),
    ) -> None:
        self.allowed_categories = set(allowed_categories)
        self.known_classes = set(known_classes)
        self.cost_range = cost_range
        self.level_range = level_range
        self.modifier_range = modifier_range

    def validate(self, document: ProgressionDocument) -> ValidationReport:
        report = ValidationReport(source=document.name)
        document = document.clone()
        document.sync_links_and_requires(prefer="requires")

        if not document.name.strip():
            report.add("warning", "missing-tree-name", "Skill tree document has no name.")
        if not document.nodes:
            report.add("error", "empty-tree", "Skill tree contains no nodes.")

        if document.class_id and self.known_classes and document.class_id not in self.known_classes:
            report.add(
                "warning",
                "unknown-tree-class",
                f"Tree default class '{document.class_id}' is not in the known class registry.",
                field="class_id",
            )

        for node_id, node in sorted(document.nodes.items()):
            if node.id != node_id:
                report.add(
                    "error",
                    "node-id-mismatch",
                    f"Node key '{node_id}' does not match embedded id '{node.id}'.",
                    node_id=node_id,
                    field="id",
                )
            if not node.id.strip():
                report.add("error", "missing-node-id", "Node id is required.", node_id=node_id, field="id")
            if not node.display_name.strip():
                report.add(
                    "warning",
                    "missing-display-name",
                    f"Node '{node_id}' has an empty display name.",
                    node_id=node_id,
                    field="display_name",
                )
            if node.category not in self.allowed_categories:
                report.add(
                    "warning",
                    "unknown-category",
                    f"Node '{node_id}' uses unknown category '{node.category}'.",
                    node_id=node_id,
                    field="category",
                )
            if not (self.cost_range[0] <= node.cost <= self.cost_range[1]):
                report.add(
                    "error",
                    "invalid-cost-range",
                    f"Node '{node_id}' cost {node.cost} is outside {self.cost_range[0]}..{self.cost_range[1]}.",
                    node_id=node_id,
                    field="cost",
                )
            if not (self.level_range[0] <= node.required_level <= self.level_range[1]):
                report.add(
                    "error",
                    "invalid-level-range",
                    f"Node '{node_id}' required level {node.required_level} is outside {self.level_range[0]}..{self.level_range[1]}.",
                    node_id=node_id,
                    field="required_level",
                )
            if node.required_class and self.known_classes and node.required_class not in self.known_classes:
                report.add(
                    "error",
                    "invalid-class-reference",
                    f"Node '{node_id}' references unknown class '{node.required_class}'.",
                    node_id=node_id,
                    field="required_class",
                )
            for req_id in node.normalized_requires():
                if req_id not in document.nodes:
                    report.add(
                        "error",
                        "missing-dependency",
                        f"Node '{node_id}' depends on missing node '{req_id}'.",
                        node_id=node_id,
                        field="requires",
                        details={"requiredNodeId": req_id},
                    )
            for modifier in node.modifiers:
                if not modifier.type.strip():
                    report.add(
                        "error",
                        "invalid-modifier-type",
                        f"Node '{node_id}' has a modifier with no type.",
                        node_id=node_id,
                        field="modifiers",
                    )
                if not (self.modifier_range[0] <= modifier.value <= self.modifier_range[1]):
                    report.add(
                        "error",
                        "invalid-modifier-range",
                        f"Node '{node_id}' modifier '{modifier.type}' value {modifier.value} is outside {self.modifier_range[0]}..{self.modifier_range[1]}.",
                        node_id=node_id,
                        field="modifiers",
                    )

        seen_links: set[tuple[str, str, str]] = set()
        for link in document.normalized_links():
            link_key = link.key()
            if link_key in seen_links:
                report.add(
                    "warning",
                    "duplicate-link",
                    f"Duplicate link detected for {link.source} -> {link.target} ({link.link_type}).",
                    details={"source": link.source, "target": link.target},
                )
                continue
            seen_links.add(link_key)

            if not link.source or not link.target:
                report.add(
                    "error",
                    "invalid-link",
                    "Link entries must include both source and target ids.",
                    details={"source": link.source, "target": link.target},
                )
                continue
            if link.source == link.target:
                report.add(
                    "error",
                    "self-cycle",
                    f"Node '{link.source}' cannot depend on itself.",
                    node_id=link.source,
                )
            if link.source not in document.nodes:
                report.add(
                    "error",
                    "missing-link-source",
                    f"Link source '{link.source}' does not exist.",
                    details={"source": link.source, "target": link.target},
                )
            if link.target not in document.nodes:
                report.add(
                    "error",
                    "missing-link-target",
                    f"Link target '{link.target}' does not exist.",
                    details={"source": link.source, "target": link.target},
                )

        report.extend(self._cycle_issues(document))
        report.extend(self._reachability_issues(document))
        report.extend(self._reference_issues(document))
        return report

    def validate_link(self, document: ProgressionDocument, source_id: str, target_id: str) -> ValidationReport:
        report = ValidationReport(source=document.name)
        if source_id == target_id:
            report.add("error", "self-cycle", "A node cannot link to itself.", node_id=source_id)
            return report
        if source_id not in document.nodes:
            report.add("error", "missing-link-source", f"Link source '{source_id}' does not exist.")
            return report
        if target_id not in document.nodes:
            report.add("error", "missing-link-target", f"Link target '{target_id}' does not exist.")
            return report

        existing = {(link.source, link.target) for link in document.normalized_links()}
        if (source_id, target_id) in existing:
            report.add("warning", "duplicate-link", f"Link '{source_id}' -> '{target_id}' already exists.")

        clone = document.clone()
        clone.nodes[target_id].requires = sorted({*clone.nodes[target_id].requires, source_id})
        clone.sync_links_and_requires(prefer="requires")
        cycle_issues = self._cycle_issues(clone)
        if cycle_issues:
            report.extend(cycle_issues)
        return report

    def _cycle_issues(self, document: ProgressionDocument) -> list[ValidationIssue]:
        issues: list[ValidationIssue] = []
        visiting: set[str] = set()
        visited: set[str] = set()
        outgoing = document.outgoing_links()

        def dfs(node_id: str, stack: list[str]) -> None:
            if node_id in visiting:
                cycle_start = stack.index(node_id) if node_id in stack else 0
                cycle = stack[cycle_start:] + [node_id]
                issues.append(
                    ValidationIssue(
                        severity="error",
                        code="cycle-detected",
                        message=f"Circular dependency detected: {' -> '.join(cycle)}.",
                        node_id=node_id,
                        details={"cycle": cycle},
                    )
                )
                return
            if node_id in visited:
                return
            visiting.add(node_id)
            stack.append(node_id)
            for link in sorted(outgoing.get(node_id, []), key=lambda item: item.target):
                if link.target in document.nodes:
                    dfs(link.target, stack)
            stack.pop()
            visiting.remove(node_id)
            visited.add(node_id)

        for node_id in sorted(document.nodes):
            dfs(node_id, [])

        deduped: dict[tuple[str, str], ValidationIssue] = {}
        for issue in issues:
            key = (issue.code, issue.message)
            deduped[key] = issue
        return list(deduped.values())

    def _reachability_issues(self, document: ProgressionDocument) -> list[ValidationIssue]:
        issues: list[ValidationIssue] = []
        roots = document.roots()
        if not roots and document.nodes:
            issues.append(
                ValidationIssue(
                    severity="warning",
                    code="no-root-nodes",
                    message="Tree has no root nodes. Every node has at least one prerequisite.",
                )
            )
            return issues

        seen: set[str] = set()
        queue = deque(roots)
        outgoing = document.outgoing_links()
        while queue:
            node_id = queue.popleft()
            if node_id in seen:
                continue
            seen.add(node_id)
            for link in outgoing.get(node_id, []):
                queue.append(link.target)

        for node_id in sorted(document.nodes):
            if node_id not in seen:
                issues.append(
                    ValidationIssue(
                        severity="warning",
                        code="unreachable-node",
                        message=f"Node '{node_id}' is unreachable from any root node.",
                        node_id=node_id,
                    )
                )
        return issues

    def _reference_issues(self, document: ProgressionDocument) -> list[ValidationIssue]:
        issues: list[ValidationIssue] = []
        known_node_ids = set(document.nodes)
        for region in document.regions:
            for node_id in region.node_ids:
                if node_id not in known_node_ids:
                    issues.append(
                        ValidationIssue(
                            severity="warning",
                            code="invalid-region-reference",
                            message=f"Region '{region.id}' references missing node '{node_id}'.",
                            details={"regionId": region.id, "nodeId": node_id},
                        )
                    )
        for annotation in document.annotations:
            for node_id in annotation.node_ids:
                if node_id not in known_node_ids:
                    issues.append(
                        ValidationIssue(
                            severity="warning",
                            code="invalid-annotation-reference",
                            message=f"Annotation '{annotation.id}' references missing node '{node_id}'.",
                            details={"annotationId": annotation.id, "nodeId": node_id},
                        )
                    )
        for bookmark in document.bookmarks:
            for node_id in bookmark.node_ids:
                if node_id not in known_node_ids:
                    issues.append(
                        ValidationIssue(
                            severity="warning",
                            code="invalid-bookmark-reference",
                            message=f"Bookmark '{bookmark.id}' references missing node '{node_id}'.",
                            details={"bookmarkId": bookmark.id, "nodeId": node_id},
                        )
                    )
        return issues
