from __future__ import annotations

from dataclasses import dataclass, field


@dataclass
class NodePort:
    name: str
    port_type: str


@dataclass
class BaseGraphNode:
    node_id: str
    node_type: str
    title: str
    x: float = 0.0
    y: float = 0.0
    inputs: list[NodePort] = field(default_factory=list)
    outputs: list[NodePort] = field(default_factory=list)
    parameters: dict[str, object] = field(default_factory=dict)

    def validate(self) -> list[str]:
        errors: list[str] = []
        if not self.node_id:
            errors.append("Missing node_id")
        if not self.node_type:
            errors.append(f"Node '{self.node_id}' missing node_type")
        return errors


NODE_TYPES = {
    "MaterialNode": {"inputs": [], "outputs": [NodePort("material", "material")]},
    "ToolNode": {"inputs": [NodePort("material", "material")], "outputs": [NodePort("tool", "item")]},
    "ArmorNode": {"inputs": [NodePort("material", "material")], "outputs": [NodePort("armor", "item")]},
    "WeaponNode": {"inputs": [NodePort("material", "material")], "outputs": [NodePort("weapon", "item")]},
    "BlockNode": {"inputs": [NodePort("material", "material")], "outputs": [NodePort("block", "block")]},
    "RecipeNode": {"inputs": [NodePort("input", "item")], "outputs": [NodePort("recipe", "json")]},
    "WorldgenNode": {"inputs": [NodePort("ore", "block")], "outputs": [NodePort("worldgen", "json")]},
    "MachineNode": {"inputs": [NodePort("material", "material")], "outputs": [NodePort("machine", "block")]},
    "SkillNode": {"inputs": [NodePort("input", "skill")], "outputs": [NodePort("skill", "skill")]},
    "QuestNode": {"inputs": [NodePort("input", "quest")], "outputs": [NodePort("quest", "quest")]},
    "SkillNodeGraph": {"inputs": [NodePort("input", "skill")], "outputs": [NodePort("skill", "skill")]},
    "SkillLinkGraph": {"inputs": [NodePort("a", "skill"), NodePort("b", "skill")], "outputs": [NodePort("link", "skill_link")]},
    "SkillModifierNode": {"inputs": [NodePort("skill", "skill")], "outputs": [NodePort("skill", "skill")]},
}


def create_node(node_id: str, node_type: str, x: float, y: float, parameters: dict[str, object] | None = None) -> BaseGraphNode:
    if node_type not in NODE_TYPES:
        raise ValueError(f"Unsupported node type: {node_type}")
    definition = NODE_TYPES[node_type]
    return BaseGraphNode(
        node_id=node_id,
        node_type=node_type,
        title=node_type,
        x=x,
        y=y,
        inputs=list(definition["inputs"]),
        outputs=list(definition["outputs"]),
        parameters=parameters or {},
    )
