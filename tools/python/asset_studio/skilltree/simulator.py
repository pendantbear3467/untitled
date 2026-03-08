from __future__ import annotations

from asset_studio.skilltree.models import (
    LockReason,
    ProgressionDocument,
    SimulationNodeState,
    SimulationRequest,
    SimulationResult,
)


class ProgressionSimulator:
    def simulate(self, document: ProgressionDocument, request: SimulationRequest | None = None) -> SimulationResult:
        request = request or SimulationRequest(
            player_level=1,
            skill_points=0,
            selected_class=document.class_id or "adventurer",
        )
        document = document.clone()
        document.sync_links_and_requires(prefer="requires")

        unlocked: set[str] = set()
        traversal_order: list[str] = []
        invalid_paths: list[str] = []
        spent_points = 0

        for node_id in request.pre_unlocked:
            if node_id not in document.nodes:
                invalid_paths.append(f"Pre-unlocked node '{node_id}' does not exist.")
                continue
            reasons = self._lock_reasons(
                document,
                node_id,
                unlocked,
                request,
                spent_points,
                ignore_point_costs=True,
            )
            if reasons:
                invalid_paths.append(f"Pre-unlocked node '{node_id}' is invalid: {', '.join(reason.message for reason in reasons)}")
                continue
            unlocked.add(node_id)
            traversal_order.append(node_id)
            if request.count_pre_unlocked_costs:
                spent_points += document.nodes[node_id].cost

        requested = list(request.requested_unlocks or document.deterministic_node_ids())
        for node_id in requested:
            if node_id in unlocked:
                continue
            if node_id not in document.nodes:
                invalid_paths.append(f"Requested unlock '{node_id}' does not exist.")
                continue
            reasons = self._lock_reasons(document, node_id, unlocked, request, spent_points)
            if reasons:
                invalid_paths.append(f"Cannot unlock '{node_id}': {', '.join(reason.message for reason in reasons)}")
                continue
            unlocked.add(node_id)
            spent_points += 0 if request.ignore_point_costs else document.nodes[node_id].cost
            traversal_order.append(node_id)

        node_states: dict[str, SimulationNodeState] = {}
        for node_id in document.deterministic_node_ids():
            node = document.nodes[node_id]
            reasons = [] if node_id in unlocked else self._lock_reasons(document, node_id, unlocked, request, spent_points)
            node_states[node_id] = SimulationNodeState(
                node_id=node_id,
                unlocked=node_id in unlocked,
                available=(node_id not in unlocked and not reasons),
                reasons=reasons,
                cumulative_cost=self._path_cost_to_node(document, node_id, unlocked),
            )

        cumulative_modifiers: dict[str, float] = {}
        for node_id in sorted(unlocked):
            for modifier in document.nodes[node_id].modifiers:
                cumulative_modifiers.setdefault(modifier.type, 0.0)
                cumulative_modifiers[modifier.type] += modifier.value

        available_nodes = sorted(node_id for node_id, state in node_states.items() if state.available)
        unlocked_nodes = sorted(unlocked)
        remaining_points = request.skill_points if request.ignore_point_costs else request.skill_points - spent_points

        return SimulationResult(
            request=request,
            node_states=node_states,
            unlocked_nodes=unlocked_nodes,
            available_nodes=available_nodes,
            spent_points=spent_points,
            remaining_points=remaining_points,
            cumulative_modifiers=cumulative_modifiers,
            invalid_paths=invalid_paths,
            traversal_order=traversal_order,
        )

    def explain_lock(
        self,
        document: ProgressionDocument,
        node_id: str,
        unlocked_nodes: set[str],
        request: SimulationRequest,
        spent_points: int = 0,
    ) -> list[LockReason]:
        document = document.clone()
        document.sync_links_and_requires(prefer="requires")
        return self._lock_reasons(document, node_id, unlocked_nodes, request, spent_points)

    def _lock_reasons(
        self,
        document: ProgressionDocument,
        node_id: str,
        unlocked_nodes: set[str],
        request: SimulationRequest,
        spent_points: int,
        *,
        ignore_point_costs: bool | None = None,
    ) -> list[LockReason]:
        reasons: list[LockReason] = []
        node = document.nodes.get(node_id)
        if node is None:
            return [LockReason(code="missing-node", message=f"Node '{node_id}' does not exist")]
        if node_id in unlocked_nodes:
            return []

        selected_class = request.selected_class or document.class_id
        if node.required_class and node.required_class != selected_class:
            reasons.append(
                LockReason(
                    code="class-restriction",
                    message=f"Requires class '{node.required_class}'.",
                )
            )
        if request.player_level < node.required_level:
            reasons.append(
                LockReason(
                    code="level-restriction",
                    message=f"Requires level {node.required_level}.",
                )
            )
        for req_id in node.normalized_requires():
            if req_id not in unlocked_nodes:
                reasons.append(
                    LockReason(
                        code="missing-prerequisite",
                        message=f"Requires node '{req_id}'.",
                        blocking_node_id=req_id,
                    )
                )
        ignore_costs = request.ignore_point_costs if ignore_point_costs is None else ignore_point_costs
        remaining_points = request.skill_points if ignore_costs else request.skill_points - spent_points
        if not ignore_costs and node.cost > remaining_points:
            reasons.append(
                LockReason(
                    code="insufficient-points",
                    message=f"Requires {node.cost} points but only {remaining_points} remain.",
                )
            )
        return reasons

    def _path_cost_to_node(self, document: ProgressionDocument, node_id: str, unlocked_nodes: set[str]) -> int:
        node = document.nodes.get(node_id)
        if node is None:
            return 0
        total = node.cost if node_id in unlocked_nodes else 0
        visited = set()
        stack = list(node.normalized_requires())
        while stack:
            req_id = stack.pop()
            if req_id in visited or req_id not in document.nodes:
                continue
            visited.add(req_id)
            if req_id in unlocked_nodes:
                total += document.nodes[req_id].cost
            stack.extend(document.nodes[req_id].normalized_requires())
        return total
