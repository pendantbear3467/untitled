from __future__ import annotations

from dataclasses import dataclass, field
from pathlib import Path

from asset_studio.generators.armor_generator import ArmorGenerator
from asset_studio.generators.block_generator import BlockGenerator
from asset_studio.generators.datapack_generator import DatapackGenerator
from asset_studio.generators.item_generator import ItemGenerator
from asset_studio.generators.machine_generator import MachineGenerator
from asset_studio.generators.ore_generator import OreGenerator
from asset_studio.generators.tool_generator import ToolGenerator
from extremecraft_sdk.definitions.definition_types import AddonSpec, ContentDefinition


@dataclass
class SDKGenerationResult:
    generated_paths: list[Path] = field(default_factory=list)


class SDKGenerator:
    def __init__(self, context, plugin_api) -> None:
        self.context = context
        self.plugin_api = plugin_api

    def generate_addon(self, addon: AddonSpec) -> SDKGenerationResult:
        result = SDKGenerationResult()
        for definition in addon.definitions:
            result.generated_paths.extend(self._generate_definition(definition, addon))
        return result

    def _generate_definition(self, definition: ContentDefinition, addon: AddonSpec) -> list[Path]:
        output: list[Path] = []
        definition_type = definition.type
        payload = definition.payload

        if definition_type == "material":
            material_id = definition.id
            tier = int(payload.get("tier", 3))
            style = str(payload.get("style", payload.get("texture_style", "metallic")))
            OreGenerator(self.context).generate(material=material_id, tier=tier, texture_style=style)
            if bool(payload.get("generate_tools", True)):
                for tool in ["pickaxe", "axe", "shovel", "sword", "hoe"]:
                    ToolGenerator(self.context).generate(
                        tool_name=f"{material_id}_{tool}",
                        material=material_id,
                        durability=int(payload.get("durability", 1200)),
                        attack_damage=float(payload.get("attack_damage", 7.0)),
                        mining_speed=float(payload.get("mining_speed", 8.0)),
                        tier=tier,
                        texture_style=style,
                    )
            if bool(payload.get("generate_armor", True)):
                ArmorGenerator(self.context).generate(material=material_id, tier=tier, texture_style=style)
            output.extend(self._collect_material_paths(material_id))

        elif definition_type in {"tool", "weapon"}:
            tool_name = definition.id
            ToolGenerator(self.context).generate(
                tool_name=tool_name,
                material=str(payload.get("material", "mythril")),
                durability=int(payload.get("durability", 900)),
                attack_damage=float(payload.get("attack_damage", 6.0)),
                mining_speed=float(payload.get("mining_speed", 7.0)),
                tier=int(payload.get("tier", 3)),
                texture_style=str(payload.get("texture_style", "metallic")),
            )
            output.append(self.context.workspace_root / "assets" / "models" / "item" / f"{tool_name}.json")

        elif definition_type == "armor":
            ArmorGenerator(self.context).generate(
                material=definition.id,
                tier=int(payload.get("tier", 3)),
                texture_style=str(payload.get("texture_style", "metallic")),
            )
            output.append(self.context.workspace_root / "assets" / "textures" / "item" / f"{definition.id}_helmet.png")

        elif definition_type == "machine":
            MachineGenerator(self.context).generate(
                machine_name=definition.id,
                material=str(payload.get("material", "steel")),
                texture_style=str(payload.get("texture_style", "industrial")),
            )
            output.append(self.context.workspace_root / "assets" / "models" / "block" / f"{definition.id}.json")

        elif definition_type == "block":
            BlockGenerator(self.context).generate(
                block_name=definition.id,
                material=str(payload.get("material", "stone")),
                texture_style=str(payload.get("texture_style", "industrial")),
            )
            output.append(self.context.workspace_root / "assets" / "models" / "block" / f"{definition.id}.json")

        elif definition_type == "item":
            item_name = definition.id
            ItemGenerator(self.context).write_item_model(item_name)
            output.append(self.context.workspace_root / "assets" / "models" / "item" / f"{item_name}.json")

        elif definition_type == "worldgen":
            DatapackGenerator(self.context).generate_ore_bundle(material=definition.id)
            output.append(self.context.workspace_root / "data" / "worldgen" / "configured_feature" / f"{definition.id}_ore.json")

        elif definition_type in {"skill_tree", "quest", "recipe"}:
            target_dir = self.context.workspace_root / "data" / addon.namespace / f"{definition_type}s"
            path = target_dir / f"{definition.id}.json"
            self.context.write_json(path, payload)
            output.append(path)

        custom_generator = self.plugin_api.generators.get(definition_type)
        if callable(custom_generator):
            custom_output = custom_generator(self.context, definition, addon)
            if isinstance(custom_output, list):
                output.extend(path for path in custom_output if isinstance(path, Path))

        for rule in self.plugin_api.datapack_rules.values():
            if callable(rule):
                rule(self.context, definition, addon)

        return output

    def _collect_material_paths(self, material_id: str) -> list[Path]:
        return [
            self.context.workspace_root / "assets" / "textures" / "item" / f"{material_id}_pickaxe.png",
            self.context.workspace_root / "assets" / "textures" / "item" / f"{material_id}_helmet.png",
            self.context.workspace_root / "assets" / "textures" / "block" / f"{material_id}_ore.png",
            self.context.workspace_root / "data" / "worldgen" / "configured_feature" / f"{material_id}_ore.json",
        ]
