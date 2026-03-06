from __future__ import annotations

from asset_studio.generators.armor_generator import ArmorGenerator
from asset_studio.generators.block_generator import BlockGenerator
from asset_studio.generators.machine_generator import MachineGenerator
from asset_studio.generators.ore_generator import OreGenerator
from asset_studio.generators.tool_generator import ToolGenerator
from asset_studio.graph.graph_nodes import BaseGraphNode


class GraphExecutor:
    def execute(self, nodes: list[BaseGraphNode], links: list[dict], context) -> list[str]:
        _ = links
        generated: list[str] = []

        material_name = "mythril"
        for node in nodes:
            if node.node_type == "MaterialNode":
                material_name = str(node.parameters.get("material", node.parameters.get("name", material_name)))

        for node in nodes:
            if node.node_type == "MaterialNode":
                OreGenerator(context).generate(material=material_name, tier=int(node.parameters.get("tier", 4)), texture_style=str(node.parameters.get("style", "metallic")))
                generated.append(f"material:{material_name}")
            elif node.node_type == "ToolNode":
                tool_name = str(node.parameters.get("tool_name", f"{material_name}_pickaxe"))
                ToolGenerator(context).generate(
                    tool_name=tool_name,
                    material=material_name,
                    durability=int(node.parameters.get("durability", 1800)),
                    attack_damage=float(node.parameters.get("attack_damage", 7.0)),
                    mining_speed=float(node.parameters.get("mining_speed", 9.0)),
                    tier=int(node.parameters.get("tier", 4)),
                    texture_style=str(node.parameters.get("style", "metallic")),
                )
                generated.append(f"tool:{tool_name}")
            elif node.node_type in {"ArmorNode", "SkillNodeGraph"}:
                ArmorGenerator(context).generate(material=material_name, tier=int(node.parameters.get("tier", 4)), texture_style=str(node.parameters.get("style", "metallic")))
                generated.append(f"armor:{material_name}")
            elif node.node_type == "BlockNode":
                block_name = str(node.parameters.get("block_name", f"{material_name}_block"))
                BlockGenerator(context).generate(block_name=block_name, material=material_name, texture_style=str(node.parameters.get("style", "industrial")))
                generated.append(f"block:{block_name}")
            elif node.node_type == "MachineNode":
                machine_name = str(node.parameters.get("machine_name", f"{material_name}_machine"))
                MachineGenerator(context).generate(machine_name=machine_name, material=material_name, texture_style=str(node.parameters.get("style", "industrial")))
                generated.append(f"machine:{machine_name}")

        return generated
