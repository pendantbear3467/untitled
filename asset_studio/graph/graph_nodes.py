from __future__ import annotations

from dataclasses import dataclass, field

from asset_studio.graph.graph_ports import GraphPort, PortDirection, PortType


@dataclass
class NodePort:
    name: str
    port_type: str
    direction: str = PortDirection.INPUT.value


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
    execution_state: str = "idle"
    last_error: str | None = None

    def validate(self) -> list[str]:
        errors: list[str] = []
        if not self.node_id:
            errors.append("Missing node_id")
        if not self.node_type:
            errors.append(f"Node '{self.node_id}' missing node_type")
        return errors


NODE_TYPES: dict[str, dict[str, list[NodePort]]] = {
    "MaterialNode": {
        "inputs": [],
        "outputs": [NodePort("output_material", PortType.MATERIAL.value, PortDirection.OUTPUT.value)],
    },
    "ToolNode": {
        "inputs": [NodePort("input_material", PortType.MATERIAL.value, PortDirection.INPUT.value)],
        "outputs": [NodePort("output_tool", PortType.TOOL.value, PortDirection.OUTPUT.value)],
    },
    "ArmorNode": {
        "inputs": [NodePort("input_material", PortType.MATERIAL.value, PortDirection.INPUT.value)],
        "outputs": [NodePort("output_item", PortType.ITEM.value, PortDirection.OUTPUT.value)],
    },
    "ItemNode": {
        "inputs": [NodePort("input_material", PortType.MATERIAL.value, PortDirection.INPUT.value)],
        "outputs": [NodePort("output_item", PortType.ITEM.value, PortDirection.OUTPUT.value)],
    },
    "BlockNode": {
        "inputs": [NodePort("input_material", PortType.MATERIAL.value, PortDirection.INPUT.value)],
        "outputs": [NodePort("output_block", PortType.BLOCK.value, PortDirection.OUTPUT.value)],
    },
    "RecipeNode": {
        "inputs": [NodePort("input_item", PortType.ITEM.value, PortDirection.INPUT.value)],
        "outputs": [NodePort("output_recipe", PortType.RECIPE.value, PortDirection.OUTPUT.value)],
    },
    "LootTableNode": {
        "inputs": [NodePort("input_block", PortType.BLOCK.value, PortDirection.INPUT.value)],
        "outputs": [NodePort("output_loot", PortType.LOOT.value, PortDirection.OUTPUT.value)],
    },
    "TagNode": {
        "inputs": [NodePort("input_item", PortType.ITEM.value, PortDirection.INPUT.value)],
        "outputs": [NodePort("output_tag", PortType.TAG.value, PortDirection.OUTPUT.value)],
    },
    "TextureNode": {
        "inputs": [NodePort("input_material", PortType.MATERIAL.value, PortDirection.INPUT.value)],
        "outputs": [NodePort("output_texture", PortType.TEXTURE.value, PortDirection.OUTPUT.value)],
    },
    "LanguageNode": {
        "inputs": [NodePort("input_item", PortType.ITEM.value, PortDirection.INPUT.value)],
        "outputs": [NodePort("output_datapack", PortType.DATAPACK.value, PortDirection.OUTPUT.value)],
    },
    "SkillNode": {
        "inputs": [],
        "outputs": [NodePort("output_skill", PortType.SKILL_NODE.value, PortDirection.OUTPUT.value)],
    },
    "SkillModifierNode": {
        "inputs": [NodePort("input_skill", PortType.SKILL_NODE.value, PortDirection.INPUT.value)],
        "outputs": [NodePort("output_skill", PortType.SKILL_NODE.value, PortDirection.OUTPUT.value)],
    },
    "SkillUnlockNode": {
        "inputs": [NodePort("input_skill", PortType.SKILL_NODE.value, PortDirection.INPUT.value)],
        "outputs": [NodePort("output_skill", PortType.SKILL_NODE.value, PortDirection.OUTPUT.value)],
    },
    "MachineNode": {
        "inputs": [NodePort("input_material", PortType.MATERIAL.value, PortDirection.INPUT.value)],
        "outputs": [NodePort("output_machine", PortType.MACHINE.value, PortDirection.OUTPUT.value)],
    },
    "EnergyInputNode": {
        "inputs": [NodePort("input_machine", PortType.MACHINE.value, PortDirection.INPUT.value)],
        "outputs": [NodePort("output_machine", PortType.MACHINE.value, PortDirection.OUTPUT.value)],
    },
    "RecipeMachineNode": {
        "inputs": [NodePort("input_machine", PortType.MACHINE.value, PortDirection.INPUT.value)],
        "outputs": [NodePort("output_recipe", PortType.RECIPE.value, PortDirection.OUTPUT.value)],
    },
    "ProcessingNode": {
        "inputs": [NodePort("input_recipe", PortType.RECIPE.value, PortDirection.INPUT.value)],
        "outputs": [NodePort("output_datapack", PortType.DATAPACK.value, PortDirection.OUTPUT.value)],
    },
}


def refresh_node_types_from_registry() -> None:
    # Late import to avoid circular dependency during module initialization.
    from asset_studio.graph.graph_node_registry import get_registry

    registry = get_registry()
    for node_type, definition in registry.all().items():
        NODE_TYPES[node_type] = {
            "inputs": [NodePort(p.name, p.port_type.value, p.direction.value) for p in definition.inputs],
            "outputs": [NodePort(p.name, p.port_type.value, p.direction.value) for p in definition.outputs],
        }


def create_node(node_id: str, node_type: str, x: float, y: float, parameters: dict[str, object] | None = None) -> BaseGraphNode:
    refresh_node_types_from_registry()
    if node_type not in NODE_TYPES:
        raise ValueError(f"Unsupported node type: {node_type}")
    definition = NODE_TYPES[node_type]
    resolved_parameters = dict(parameters or {})

    from asset_studio.graph.graph_node_registry import get_registry

    registry_definition = get_registry().get(node_type)
    if registry_definition is not None:
        for param_name, schema in registry_definition.parameters.items():
            if param_name not in resolved_parameters and schema.default is not None:
                resolved_parameters[param_name] = schema.default

    return BaseGraphNode(
        node_id=node_id,
        node_type=node_type,
        title=node_type,
        x=x,
        y=y,
        inputs=list(definition["inputs"]),
        outputs=list(definition["outputs"]),
        parameters=resolved_parameters,
    )
