from __future__ import annotations

import argparse
import json

from asset_studio.generators.armor_generator import ArmorGenerator
from asset_studio.generators.block_generator import BlockGenerator
from asset_studio.generators.datapack_generator import DatapackGenerator
from asset_studio.generators.item_generator import ItemGenerator
from asset_studio.generators.machine_generator import MachineGenerator
from asset_studio.generators.ore_generator import OreGenerator
from asset_studio.generators.tool_generator import ToolGenerator
from asset_studio.workspace.workspace_manager import AssetStudioContext


def register_generate_commands(parser: argparse.ArgumentParser) -> None:
    sub = parser.add_subparsers(dest="generate_type", required=True)

    material = sub.add_parser("material", help="Generate all content for a material definition")
    material.add_argument("material_name")

    item = sub.add_parser("item", help="Generate item model+texture")
    item.add_argument("item_name")
    item.add_argument("--material", default="mythril")
    item.add_argument("--style", default="metallic")

    tool = sub.add_parser("tool", help="Generate a tool asset bundle")
    tool.add_argument("tool_name")
    tool.add_argument("--material", default="mythril")
    tool.add_argument("--durability", type=int, default=1800)
    tool.add_argument("--attack-damage", type=float, default=7.0)
    tool.add_argument("--mining-speed", type=float, default=9.0)
    tool.add_argument("--tier", type=int, default=4)
    tool.add_argument("--texture-style", default="metallic")

    ore = sub.add_parser("ore", help="Generate ore + datapack worldgen content")
    ore.add_argument("material")
    ore.add_argument("--tier", type=int, default=3)
    ore.add_argument("--style", default="metallic")

    armor = sub.add_parser("armor", help="Generate armor set")
    armor.add_argument("material")
    armor.add_argument("--tier", type=int, default=4)
    armor.add_argument("--style", default="metallic")

    machine = sub.add_parser("machine", help="Generate machine block and assets")
    machine.add_argument("machine_name")
    machine.add_argument("--material", default="steel")
    machine.add_argument("--style", default="industrial")

    block = sub.add_parser("block", help="Generate block texture/model/blockstate")
    block.add_argument("block_name")
    block.add_argument("--material", default="stone")
    block.add_argument("--style", default="industrial")


def run_generate_command(args: argparse.Namespace, context: AssetStudioContext) -> int:
    gtype = args.generate_type

    if gtype == "tool":
        ToolGenerator(context).generate(
            tool_name=args.tool_name,
            material=args.material,
            durability=args.durability,
            attack_damage=args.attack_damage,
            mining_speed=args.mining_speed,
            tier=args.tier,
            texture_style=args.texture_style,
        )
        return 0

    if gtype == "item":
        texture = context.texture_engine.generate_ingot_texture(material=args.material, style=args.style)
        context.write_texture(context.workspace_root / "assets" / "textures" / "item" / f"{args.item_name}.png", texture.image)
        ItemGenerator(context).write_item_model(args.item_name)
        context.add_lang_entry("item.extremecraft." + args.item_name, args.item_name.replace("_", " ").title())
        return 0

    if gtype == "ore":
        OreGenerator(context).generate(material=args.material, tier=args.tier, texture_style=args.style)
        return 0

    if gtype == "armor":
        ArmorGenerator(context).generate(material=args.material, tier=args.tier, texture_style=args.style)
        return 0

    if gtype == "machine":
        MachineGenerator(context).generate(machine_name=args.machine_name, material=args.material, texture_style=args.style)
        return 0

    if gtype == "block":
        BlockGenerator(context).generate(block_name=args.block_name, material=args.material, texture_style=args.style)
        return 0

    if gtype == "material":
        return _generate_from_material_definition(args.material_name, context)

    raise ValueError(f"Unsupported generate command: {gtype}")


def _generate_from_material_definition(material_name: str, context: AssetStudioContext) -> int:
    material_file = context.workspace_root / "materials" / f"{material_name}.json"
    if not material_file.exists():
        default_def = {
            "name": material_name,
            "tier": 4,
            "tools": True,
            "armor": True,
            "ore": True,
            "color": "#3ABEFF",
            "texture_style": "metallic",
        }
        context.write_json(material_file, default_def)

    definition = json.loads(material_file.read_text(encoding="utf-8"))
    style = str(definition.get("texture_style", "metallic"))
    tier = int(definition.get("tier", 3))
    material = str(definition.get("name", material_name))

    if bool(definition.get("ore", True)):
        OreGenerator(context).generate(material=material, tier=tier, texture_style=style)

    if bool(definition.get("tools", True)):
        ToolGenerator(context).generate(
            tool_name=f"{material}_pickaxe",
            material=material,
            durability=1800,
            attack_damage=7.0,
            mining_speed=9.0,
            tier=tier,
            texture_style=style,
        )

    if bool(definition.get("armor", True)):
        ArmorGenerator(context).generate(material=material, tier=tier, texture_style=style)

    DatapackGenerator(context).generate_material_support(material=material, tier=tier)
    return 0
