from __future__ import annotations

from statistics import median

from asset_studio.skilltree.models import (
    BalanceFinding,
    BalanceReport,
    BranchAnalysis,
    HeatmapEntry,
    LevelBandReport,
    NodeEfficiency,
    PathCostEntry,
    ProgressionDocument,
    SimulationRequest,
)
from asset_studio.skilltree.simulator import ProgressionSimulator


class BalanceAnalyzer:
    def __init__(self, simulator: ProgressionSimulator | None = None) -> None:
        self.simulator = simulator or ProgressionSimulator()

    def analyze(
        self,
        document: ProgressionDocument,
        request: SimulationRequest | None = None,
    ) -> BalanceReport:
        document = document.clone()
        document.sync_links_and_requires(prefer="requires")
        report = BalanceReport()

        outgoing = document.outgoing_links()
        roots = document.roots()
        path_costs = self._compute_path_costs(document)
        report.path_costs = path_costs

        efficiency_scores: list[float] = []
        for node_id, node in sorted(document.nodes.items()):
            modifier_totals = {modifier.type: modifier.value for modifier in node.modifiers}
            efficiency = node.power_score / max(node.cost, 1)
            report.node_efficiency[node_id] = NodeEfficiency(
                node_id=node_id,
                power_score=node.power_score,
                efficiency=efficiency,
                cost=node.cost,
                modifiers=modifier_totals,
            )
            efficiency_scores.append(efficiency)

            path_entry = path_costs.get(node_id)
            if path_entry is not None:
                report.cumulative_totals_by_node[node_id] = self._cumulative_modifiers_for_path(document, path_entry.path)

        median_efficiency = median(efficiency_scores) if efficiency_scores else 0.0
        weak_threshold = median_efficiency * 0.5 if median_efficiency else 0.0

        for node_id, efficiency in report.node_efficiency.items():
            if efficiency.power_score <= 0 and node_id not in roots:
                report.findings.append(
                    BalanceFinding(
                        severity="warning",
                        code="weak-node",
                        message=f"Node '{node_id}' contributes no measurable power.",
                        node_id=node_id,
                    )
                )
            elif weak_threshold and efficiency.efficiency < weak_threshold:
                report.findings.append(
                    BalanceFinding(
                        severity="warning",
                        code="weak-node",
                        message=f"Node '{node_id}' efficiency {efficiency.efficiency:.2f} is well below the tree median.",
                        node_id=node_id,
                    )
                )

        reachable = set(path_costs)
        for node_id in sorted(document.nodes):
            if node_id not in reachable:
                report.findings.append(
                    BalanceFinding(
                        severity="warning",
                        code="unreachable-node",
                        message=f"Node '{node_id}' is unreachable from any root branch.",
                        node_id=node_id,
                    )
                )

        for node_id, node in sorted(document.nodes.items()):
            descendants = outgoing.get(node_id, [])
            if not descendants and node.modifiers == [] and "capstone" not in node.normalized_tags():
                report.findings.append(
                    BalanceFinding(
                        severity="warning",
                        code="dead-end",
                        message=f"Node '{node_id}' is a dead end with no capstone marker or modifiers.",
                        node_id=node_id,
                    )
                )

        for root_id in roots:
            branch_nodes = self._descendants(document, root_id)
            total_cost = sum(document.nodes[node_id].cost for node_id in branch_nodes)
            total_power = sum(document.nodes[node_id].power_score for node_id in branch_nodes)
            average_efficiency = total_power / max(total_cost, 1)
            capstones = [
                node_id
                for node_id in branch_nodes
                if not outgoing.get(node_id) or "capstone" in document.nodes[node_id].normalized_tags()
            ]
            report.branches[root_id] = BranchAnalysis(
                root_node_id=root_id,
                node_ids=sorted(branch_nodes),
                total_cost=total_cost,
                total_power=total_power,
                average_efficiency=average_efficiency,
                capstones=sorted(capstones),
            )

        self._append_branch_findings(report)
        report.level_bands = self._build_level_bands(document)
        report.heatmap = self._build_heatmap(report)

        simulation = self.simulator.simulate(document, request) if request else self.simulator.simulate(
            document,
            SimulationRequest(
                player_level=max((node.required_level for node in document.nodes.values()), default=1),
                skill_points=sum(node.cost for node in document.nodes.values()),
                selected_class=document.class_id,
                requested_unlocks=document.deterministic_node_ids(),
            ),
        )
        report.simulated_totals = simulation.cumulative_modifiers
        return report

    def _compute_path_costs(self, document: ProgressionDocument) -> dict[str, PathCostEntry]:
        roots = document.roots()
        if not roots:
            return {}

        ordered = document.deterministic_node_ids()
        best_cost: dict[str, int] = {}
        best_path: dict[str, list[str]] = {}
        best_level: dict[str, int] = {}

        for node_id in ordered:
            node = document.nodes[node_id]
            requirements = [req_id for req_id in node.normalized_requires() if req_id in document.nodes]
            if not requirements:
                best_cost[node_id] = node.cost
                best_path[node_id] = [node_id]
                best_level[node_id] = node.required_level
                continue

            candidate_paths = []
            for req_id in requirements:
                if req_id not in best_cost:
                    continue
                candidate_paths.append(
                    (
                        best_cost[req_id] + node.cost,
                        best_level[req_id] + node.required_level,
                        best_path[req_id] + [node_id],
                    )
                )
            if candidate_paths:
                total_cost, total_level, path = min(candidate_paths, key=lambda item: (item[0], item[2]))
                best_cost[node_id] = total_cost
                best_level[node_id] = total_level
                best_path[node_id] = path

        return {
            node_id: PathCostEntry(
                node_id=node_id,
                path=best_path[node_id],
                total_cost=best_cost[node_id],
                total_level_requirement=best_level[node_id],
            )
            for node_id in sorted(best_path)
        }

    def _cumulative_modifiers_for_path(self, document: ProgressionDocument, path: list[str]) -> dict[str, float]:
        totals: dict[str, float] = {}
        for node_id in path:
            node = document.nodes.get(node_id)
            if node is None:
                continue
            for modifier in node.modifiers:
                totals.setdefault(modifier.type, 0.0)
                totals[modifier.type] += modifier.value
        return totals

    def _descendants(self, document: ProgressionDocument, root_id: str) -> set[str]:
        outgoing = document.outgoing_links()
        seen: set[str] = set()
        stack = [root_id]
        while stack:
            node_id = stack.pop()
            if node_id in seen:
                continue
            seen.add(node_id)
            for link in outgoing.get(node_id, []):
                stack.append(link.target)
        return seen

    def _append_branch_findings(self, report: BalanceReport) -> None:
        if not report.branches:
            return
        average_power = sum(branch.total_power for branch in report.branches.values()) / max(len(report.branches), 1)
        for root_id, branch in report.branches.items():
            if average_power and branch.total_power > average_power * 1.5:
                report.findings.append(
                    BalanceFinding(
                        severity="warning",
                        code="branch-dominance",
                        message=f"Branch rooted at '{root_id}' dominates average branch power ({branch.total_power:.2f}).",
                        branch_root_id=root_id,
                    )
                )
            for capstone_id in branch.capstones:
                path = report.path_costs.get(capstone_id)
                if path is None:
                    continue
                if path.total_cost > max(20, average_power * 2):
                    report.findings.append(
                        BalanceFinding(
                            severity="info",
                            code="capstone-accessibility",
                            message=f"Capstone '{capstone_id}' requires {path.total_cost} total points along its cheapest path.",
                            node_id=capstone_id,
                            branch_root_id=root_id,
                        )
                    )

    def _build_level_bands(self, document: ProgressionDocument) -> list[LevelBandReport]:
        bands: dict[str, list[str]] = {}
        for node_id, node in sorted(document.nodes.items()):
            low = ((max(node.required_level, 1) - 1) // 10) * 10 + 1
            high = low + 9
            label = f"{low}-{high}"
            bands.setdefault(label, []).append(node_id)
        return [
            LevelBandReport(
                label=label,
                node_ids=node_ids,
                total_cost=sum(document.nodes[node_id].cost for node_id in node_ids),
                total_power=sum(document.nodes[node_id].power_score for node_id in node_ids),
            )
            for label, node_ids in sorted(bands.items())
        ]

    def _build_heatmap(self, report: BalanceReport) -> dict[str, HeatmapEntry]:
        if not report.node_efficiency:
            return {}
        max_efficiency = max(entry.efficiency for entry in report.node_efficiency.values()) or 1.0
        return {
            node_id: HeatmapEntry(
                node_id=node_id,
                value=max(0.0, min(1.0, entry.efficiency / max_efficiency)),
                reason="efficiency",
            )
            for node_id, entry in report.node_efficiency.items()
        }
