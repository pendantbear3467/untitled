from __future__ import annotations

import importlib.util
import json
from dataclasses import dataclass, field
from pathlib import Path
from types import ModuleType
from typing import Callable

from asset_studio.generators.armor_generator import ArmorGenerator
from asset_studio.generators.block_generator import BlockGenerator
from asset_studio.generators.item_generator import ItemGenerator
from asset_studio.generators.machine_generator import MachineGenerator
from asset_studio.generators.ore_generator import OreGenerator
from asset_studio.generators.tool_generator import ToolGenerator
from asset_studio.graph.graph_nodes import BaseGraphNode
from asset_studio.graph.graph_ports import GraphPort, PortDirection, PortType

NodeExecFn = Callable[[BaseGraphNode, dict[str, object], object], list[str]]


@dataclass
class NodeParameterSchema:
    name: str
    value_type: str
    default: object = None
    enum_values: list[str] = field(default_factory=list)


@dataclass
class GraphNodeDefinition:
    node_type: str
    category: str
    inputs: list[GraphPort]
    outputs: list[GraphPort]
    parameters: dict[str, NodeParameterSchema]
    execute: NodeExecFn


class GraphNodeRegistry:
    def __init__(self) -> None:
        self._definitions: dict[str, GraphNodeDefinition] = {}

    def register(self, definition: GraphNodeDefinition) -> None:
        self._definitions[definition.node_type] = definition

    def get(self, node_type: str) -> GraphNodeDefinition | None:
        return self._definitions.get(node_type)

    def all(self) -> dict[str, GraphNodeDefinition]:
        return dict(self._definitions)

    def categories(self) -> dict[str, list[str]]:
        grouped: dict[str, list[str]] = {}
        for node_type, definition in self._definitions.items():
            grouped.setdefault(definition.category, []).append(node_type)
        for category in grouped:
            grouped[category].sort()
        return grouped

    def discover_plugin_nodes(self, roots: list[Path]) -> None:
        for root in roots:
            if not root.exists():
                continue
            for plugin_file in sorted(root.glob("*.py")):
                if plugin_file.name.startswith("_"):
                    continue
                try:
                    module = _load_module(plugin_file)
                    register_fn = getattr(module, "register_graph_nodes", None)
                    if callable(register_fn):
                        register_fn(self)
                except Exception:
                    continue


_GLOBAL_REGISTRY = GraphNodeRegistry()


def get_registry() -> GraphNodeRegistry:
    return _GLOBAL_REGISTRY


def register_node(
    node_type: str,
    *,
    category: str,
    inputs: list[GraphPort],
    outputs: list[GraphPort],
    parameters: dict[str, NodeParameterSchema] | None = None,
) -> Callable[[NodeExecFn], NodeExecFn]:
    def decorator(fn: NodeExecFn) -> NodeExecFn:
        _GLOBAL_REGISTRY.register(
            GraphNodeDefinition(
                node_type=node_type,
                category=category,
                inputs=inputs,
                outputs=outputs,
                parameters=parameters or {},
                execute=fn,
            )
        )
        return fn

    return decorator


def _material_from_inputs(node: BaseGraphNode, inputs: dict[str, object]) -> str:
    value = inputs.get("material") or node.parameters.get("material") or node.parameters.get("name")
    return str(value or "mythril")


def _to_int(value: object, default: int) -> int:
    try:
        return int(str(value)) if value is not None else default
    except (TypeError, ValueError):
        return default


def _to_float(value: object, default: float) -> float:
    try:
        return float(str(value)) if value is not None else default
    except (TypeError, ValueError):
        return default


@register_node(
    "MaterialNode",
    category="Materials",
    inputs=[],
    outputs=[GraphPort("output_material", PortDirection.OUTPUT, PortType.MATERIAL)],
    parameters={
        "material": NodeParameterSchema("material", "string", "mythril"),
        "tier": NodeParameterSchema("tier", "int", 4),
        "style": NodeParameterSchema("style", "enum", "metallic", ["metallic", "industrial", "ancient"]),
    },
)
def exec_material(node: BaseGraphNode, _: dict[str, object], context) -> list[str]:
    material = str(node.parameters.get("material", "mythril"))
    OreGenerator(context).generate(
        material=material,
        tier=_to_int(node.parameters.get("tier", 4), 4),
        texture_style=str(node.parameters.get("style", "metallic")),
    )
    return [f"material:{material}"]


@register_node(
    "ToolNode",
    category="Tools",
    inputs=[GraphPort("input_material", PortDirection.INPUT, PortType.MATERIAL)],
    outputs=[GraphPort("output_tool", PortDirection.OUTPUT, PortType.TOOL)],
    parameters={
        "tool_name": NodeParameterSchema("tool_name", "string", "mythril_pickaxe"),
        "durability": NodeParameterSchema("durability", "int", 1800),
        "attack_damage": NodeParameterSchema("attack_damage", "float", 7.0),
        "mining_speed": NodeParameterSchema("mining_speed", "float", 9.0),
        "tier": NodeParameterSchema("tier", "int", 4),
        "style": NodeParameterSchema("style", "enum", "metallic", ["metallic", "industrial", "ancient"]),
    },
)
def exec_tool(node: BaseGraphNode, inputs: dict[str, object], context) -> list[str]:
    material = _material_from_inputs(node, inputs)
    tool_name = str(node.parameters.get("tool_name", f"{material}_pickaxe"))
    ToolGenerator(context).generate(
        tool_name=tool_name,
        material=material,
        durability=_to_int(node.parameters.get("durability", 1800), 1800),
        attack_damage=_to_float(node.parameters.get("attack_damage", 7.0), 7.0),
        mining_speed=_to_float(node.parameters.get("mining_speed", 9.0), 9.0),
        tier=_to_int(node.parameters.get("tier", 4), 4),
        texture_style=str(node.parameters.get("style", "metallic")),
    )
    return [f"tool:{tool_name}"]


@register_node(
    "ArmorNode",
    category="Items",
    inputs=[GraphPort("input_material", PortDirection.INPUT, PortType.MATERIAL)],
    outputs=[GraphPort("output_item", PortDirection.OUTPUT, PortType.ITEM)],
    parameters={
        "tier": NodeParameterSchema("tier", "int", 4),
        "style": NodeParameterSchema("style", "enum", "metallic", ["metallic", "industrial", "ancient"]),
    },
)
def exec_armor(node: BaseGraphNode, inputs: dict[str, object], context) -> list[str]:
    material = _material_from_inputs(node, inputs)
    ArmorGenerator(context).generate(
        material=material,
        tier=_to_int(node.parameters.get("tier", 4), 4),
        texture_style=str(node.parameters.get("style", "metallic")),
    )
    return [f"armor:{material}"]


@register_node(
    "ItemNode",
    category="Items",
    inputs=[GraphPort("input_material", PortDirection.INPUT, PortType.MATERIAL)],
    outputs=[GraphPort("output_item", PortDirection.OUTPUT, PortType.ITEM)],
    parameters={
        "item_name": NodeParameterSchema("item_name", "string", "mythril_ingot"),
    },
)
def exec_item(node: BaseGraphNode, inputs: dict[str, object], context) -> list[str]:
    material = _material_from_inputs(node, inputs)
    item_name = str(node.parameters.get("item_name", f"{material}_ingot"))
    ItemGenerator(context).write_item_model(item_name)
    context.add_lang_entry(f"item.extremecraft.{item_name}", item_name.replace("_", " ").title())
    return [f"item:{item_name}"]


@register_node(
    "BlockNode",
    category="Blocks",
    inputs=[GraphPort("input_material", PortDirection.INPUT, PortType.MATERIAL)],
    outputs=[GraphPort("output_block", PortDirection.OUTPUT, PortType.BLOCK)],
    parameters={
        "block_name": NodeParameterSchema("block_name", "string", "mythril_block"),
        "style": NodeParameterSchema("style", "enum", "industrial", ["metallic", "industrial", "ancient"]),
    },
)
def exec_block(node: BaseGraphNode, inputs: dict[str, object], context) -> list[str]:
    material = _material_from_inputs(node, inputs)
    block_name = str(node.parameters.get("block_name", f"{material}_block"))
    BlockGenerator(context).generate(
        block_name=block_name,
        material=material,
        texture_style=str(node.parameters.get("style", "industrial")),
    )
    return [f"block:{block_name}"]


@register_node(
    "RecipeNode",
    category="Recipes",
    inputs=[GraphPort("input_item", PortDirection.INPUT, PortType.ITEM)],
    outputs=[GraphPort("output_recipe", PortDirection.OUTPUT, PortType.RECIPE)],
    parameters={
        "recipe_name": NodeParameterSchema("recipe_name", "string", "mythril_ingot_recipe"),
        "result": NodeParameterSchema("result", "string", "extremecraft:mythril_ingot"),
        "ingredients": NodeParameterSchema("ingredients", "list", ["minecraft:iron_ingot"]),
    },
)
def exec_recipe(node: BaseGraphNode, _: dict[str, object], context) -> list[str]:
    recipe_name = str(node.parameters.get("recipe_name", "recipe"))
    out = context.workspace_root / "data" / "recipes" / f"{recipe_name}.json"
    ingredients = node.parameters.get("ingredients", ["minecraft:iron_ingot"])
    rows = [{"item": str(v)} for v in ingredients] if isinstance(ingredients, list) else [{"item": str(ingredients)}]
    payload = {
        "type": "minecraft:crafting_shapeless",
        "ingredients": rows,
        "result": {"item": str(node.parameters.get("result", "extremecraft:mythril_ingot")), "count": 1},
    }
    context.write_json(out, payload)
    return [f"recipe:{recipe_name}"]


@register_node(
    "LootTableNode",
    category="Recipes",
    inputs=[GraphPort("input_block", PortDirection.INPUT, PortType.BLOCK)],
    outputs=[GraphPort("output_loot", PortDirection.OUTPUT, PortType.LOOT)],
    parameters={
        "table_name": NodeParameterSchema("table_name", "string", "mythril_ore"),
        "drop": NodeParameterSchema("drop", "string", "extremecraft:mythril_raw"),
    },
)
def exec_loot(node: BaseGraphNode, _: dict[str, object], context) -> list[str]:
    name = str(node.parameters.get("table_name", "ore"))
    path = context.workspace_root / "data" / "loot_tables" / "blocks" / f"{name}.json"
    payload = {
        "type": "minecraft:block",
        "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": str(node.parameters.get("drop", "minecraft:stone"))}]}],
    }
    context.write_json(path, payload)
    return [f"loot:{name}"]


@register_node(
    "TagNode",
    category="Utility",
    inputs=[GraphPort("input_item", PortDirection.INPUT, PortType.ITEM)],
    outputs=[GraphPort("output_tag", PortDirection.OUTPUT, PortType.TAG)],
    parameters={
        "tag_path": NodeParameterSchema("tag_path", "string", "data/tags/items/ingots.json"),
        "value": NodeParameterSchema("value", "string", "extremecraft:mythril_ingot"),
    },
)
def exec_tag(node: BaseGraphNode, _: dict[str, object], context) -> list[str]:
    relative = str(node.parameters.get("tag_path", "data/tags/items/default.json"))
    path = context.workspace_root / relative
    context.append_to_tag(path, str(node.parameters.get("value", "minecraft:stone")))
    return [f"tag:{relative}"]


@register_node(
    "TextureNode",
    category="Utility",
    inputs=[GraphPort("input_material", PortDirection.INPUT, PortType.MATERIAL)],
    outputs=[GraphPort("output_texture", PortDirection.OUTPUT, PortType.TEXTURE)],
    parameters={
        "texture_kind": NodeParameterSchema("texture_kind", "enum", "ingot", ["ingot", "ore", "block"]),
        "style": NodeParameterSchema("style", "enum", "metallic", ["metallic", "industrial", "ancient"]),
    },
)
def exec_texture(node: BaseGraphNode, inputs: dict[str, object], context) -> list[str]:
    material = _material_from_inputs(node, inputs)
    kind = str(node.parameters.get("texture_kind", "ingot"))
    style = str(node.parameters.get("style", "metallic"))
    if kind == "ore":
        result = context.texture_engine.generate_ore_texture(material=material, style=style)
        path = context.workspace_root / "assets" / "textures" / "block" / f"{material}_ore.png"
    elif kind == "block":
        result = context.texture_engine.generate_block_texture(material=material, style=style)
        path = context.workspace_root / "assets" / "textures" / "block" / f"{material}_block.png"
    else:
        result = context.texture_engine.generate_ingot_texture(material=material, style=style)
        path = context.workspace_root / "assets" / "textures" / "item" / f"{material}_ingot.png"
    context.write_texture(path, result.image)
    return [f"texture:{path.name}"]


@register_node(
    "LanguageNode",
    category="Utility",
    inputs=[GraphPort("input_item", PortDirection.INPUT, PortType.ITEM)],
    outputs=[GraphPort("output_datapack", PortDirection.OUTPUT, PortType.DATAPACK)],
    parameters={
        "key": NodeParameterSchema("key", "string", "item.extremecraft.mythril_ingot"),
        "value": NodeParameterSchema("value", "string", "Mythril Ingot"),
    },
)
def exec_language(node: BaseGraphNode, _: dict[str, object], context) -> list[str]:
    key = str(node.parameters.get("key", "item.extremecraft.unknown"))
    value = str(node.parameters.get("value", "Unknown"))
    context.add_lang_entry(key, value)
    return [f"lang:{key}"]


@register_node(
    "SkillNode",
    category="Skills",
    inputs=[],
    outputs=[GraphPort("output_skill", PortDirection.OUTPUT, PortType.SKILL_NODE)],
    parameters={
        "id": NodeParameterSchema("id", "string", "combat_mastery"),
        "display_name": NodeParameterSchema("display_name", "string", "Combat Mastery"),
        "description": NodeParameterSchema("description", "string", "Increase combat effectiveness"),
    },
)
def exec_skill_node(node: BaseGraphNode, _: dict[str, object], context) -> list[str]:
    tree_root = context.workspace_root / "skilltrees"
    tree_root.mkdir(parents=True, exist_ok=True)
    skill_id = str(node.parameters.get("id", "skill"))
    path = tree_root / f"{skill_id}.json"
    payload = {
        "id": skill_id,
        "name": str(node.parameters.get("display_name", skill_id.replace("_", " ").title())),
        "description": str(node.parameters.get("description", "")),
    }
    context.write_json(path, payload)
    return [f"skill:{skill_id}"]


@register_node(
    "SkillModifierNode",
    category="Skills",
    inputs=[GraphPort("input_skill", PortDirection.INPUT, PortType.SKILL_NODE)],
    outputs=[GraphPort("output_skill", PortDirection.OUTPUT, PortType.SKILL_NODE)],
    parameters={
        "modifier": NodeParameterSchema("modifier", "enum", "damage", ["damage", "speed", "efficiency"]),
        "value": NodeParameterSchema("value", "float", 0.1),
    },
)
def exec_skill_modifier(node: BaseGraphNode, inputs: dict[str, object], context) -> list[str]:
    _ = context
    src = str(inputs.get("input_skill", node.parameters.get("id", "skill")))
    return [f"skill_modifier:{src}:{node.parameters.get('modifier', 'damage')}={node.parameters.get('value', 0.1)}"]


@register_node(
    "SkillUnlockNode",
    category="Skills",
    inputs=[GraphPort("input_skill", PortDirection.INPUT, PortType.SKILL_NODE)],
    outputs=[GraphPort("output_skill", PortDirection.OUTPUT, PortType.SKILL_NODE)],
    parameters={
        "unlock_level": NodeParameterSchema("unlock_level", "int", 10),
    },
)
def exec_skill_unlock(node: BaseGraphNode, inputs: dict[str, object], context) -> list[str]:
    _ = context
    src = str(inputs.get("input_skill", "skill"))
    return [f"skill_unlock:{src}:level={_to_int(node.parameters.get('unlock_level', 10), 10)}"]


@register_node(
    "MachineNode",
    category="Machines",
    inputs=[GraphPort("input_material", PortDirection.INPUT, PortType.MATERIAL)],
    outputs=[GraphPort("output_machine", PortDirection.OUTPUT, PortType.MACHINE)],
    parameters={
        "machine_name": NodeParameterSchema("machine_name", "string", "mythril_crusher"),
        "style": NodeParameterSchema("style", "enum", "industrial", ["industrial", "ancient"]),
    },
)
def exec_machine(node: BaseGraphNode, inputs: dict[str, object], context) -> list[str]:
    material = _material_from_inputs(node, inputs)
    machine_name = str(node.parameters.get("machine_name", f"{material}_machine"))
    MachineGenerator(context).generate(
        machine_name=machine_name,
        material=material,
        texture_style=str(node.parameters.get("style", "industrial")),
    )
    return [f"machine:{machine_name}"]


@register_node(
    "EnergyInputNode",
    category="Machines",
    inputs=[GraphPort("input_machine", PortDirection.INPUT, PortType.MACHINE)],
    outputs=[GraphPort("output_machine", PortDirection.OUTPUT, PortType.MACHINE)],
    parameters={
        "energy_per_tick": NodeParameterSchema("energy_per_tick", "int", 80),
    },
)
def exec_energy_input(node: BaseGraphNode, inputs: dict[str, object], context) -> list[str]:
    _ = context
    machine = str(inputs.get("input_machine", node.parameters.get("machine_name", "machine")))
    return [f"energy:{machine}:{_to_int(node.parameters.get('energy_per_tick', 80), 80)}"]


@register_node(
    "RecipeMachineNode",
    category="Machines",
    inputs=[GraphPort("input_machine", PortDirection.INPUT, PortType.MACHINE)],
    outputs=[GraphPort("output_recipe", PortDirection.OUTPUT, PortType.RECIPE)],
    parameters={
        "recipe_name": NodeParameterSchema("recipe_name", "string", "machine_recipe"),
        "input": NodeParameterSchema("input", "string", "minecraft:iron_ore"),
        "output": NodeParameterSchema("output", "string", "extremecraft:mythril_dust"),
    },
)
def exec_recipe_machine(node: BaseGraphNode, _: dict[str, object], context) -> list[str]:
    name = str(node.parameters.get("recipe_name", "machine_recipe"))
    path = context.workspace_root / "machines" / f"{name}.json"
    payload = {
        "type": "extremecraft:machine_recipe",
        "input": str(node.parameters.get("input", "minecraft:stone")),
        "output": str(node.parameters.get("output", "minecraft:stone")),
    }
    context.write_json(path, payload)
    return [f"machine_recipe:{name}"]


@register_node(
    "ProcessingNode",
    category="Machines",
    inputs=[GraphPort("input_recipe", PortDirection.INPUT, PortType.RECIPE)],
    outputs=[GraphPort("output_datapack", PortDirection.OUTPUT, PortType.DATAPACK)],
    parameters={
        "duration": NodeParameterSchema("duration", "int", 200),
        "parallel": NodeParameterSchema("parallel", "boolean", False),
    },
)
def exec_processing(node: BaseGraphNode, _: dict[str, object], context) -> list[str]:
    _ = context
    return [
        f"processing:duration={_to_int(node.parameters.get('duration', 200), 200)}:"
        f"parallel={bool(node.parameters.get('parallel', False))}"
    ]


def discover_graph_plugins(workspace_root: Path) -> None:
    repo_candidates = [
        Path.cwd() / "tools" / "python" / "asset_studio" / "plugins" / "graph_nodes",
        Path.cwd() / "asset_studio" / "plugins" / "graph_nodes",
    ]
    repo_graph_nodes = next((candidate for candidate in repo_candidates if candidate.exists()), repo_candidates[0])
    workspace_graph_nodes = workspace_root / "plugins" / "graph_nodes"
    _GLOBAL_REGISTRY.discover_plugin_nodes([repo_graph_nodes, workspace_graph_nodes])


def register_plugin_api_nodes(plugin_api_nodes: dict[str, object]) -> None:
    for node_type, handler in plugin_api_nodes.items():
        if not callable(handler):
            continue

        @register_node(
            node_type,
            category="Utility",
            inputs=[GraphPort("in", PortDirection.INPUT, PortType.ANY)],
            outputs=[GraphPort("out", PortDirection.OUTPUT, PortType.ANY)],
            parameters={
                "payload": NodeParameterSchema("payload", "string", ""),
            },
        )
        def _exec(node: BaseGraphNode, inputs: dict[str, object], context, fn=handler) -> list[str]:
            result = fn(node=node, inputs=inputs, context=context)
            if isinstance(result, list):
                return [str(v) for v in result]
            if result is None:
                return [f"plugin:{node.node_type}"]
            return [str(result)]


def _load_module(path: Path) -> ModuleType:
    spec = importlib.util.spec_from_file_location(f"assetstudio_graph_node_{path.stem}", path)
    if spec is None or spec.loader is None:
        raise RuntimeError(f"Cannot load graph node plugin: {path}")
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module

