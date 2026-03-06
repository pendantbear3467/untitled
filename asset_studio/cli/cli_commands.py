from __future__ import annotations

import argparse
import json
from pathlib import Path

from asset_studio.cli.build_commands import build_command, validate_command
from asset_studio.cli.pack_commands import export_pack_command
from asset_studio.generators.armor_generator import ArmorGenerator
from asset_studio.generators.block_generator import BlockGenerator
from asset_studio.generators.machine_generator import MachineGenerator
from asset_studio.generators.ore_generator import OreGenerator
from asset_studio.generators.tool_generator import ToolGenerator
from asset_studio.project.registry_scanner import scan_registry_files
from asset_studio.project.workspace_manager import AssetStudioContext, WorkspaceManager


def register_subcommands(subparsers: argparse._SubParsersAction[argparse.ArgumentParser]) -> None:
    generate = subparsers.add_parser("generate", help="Generate one or more assets")
    generate_sub = generate.add_subparsers(dest="generate_type", required=True)

    tool = generate_sub.add_parser("tool", help="Generate a tool asset bundle")
    tool.add_argument("tool_name")
    tool.add_argument("--material", default="mythril")
    tool.add_argument("--durability", type=int, default=1800)
    tool.add_argument("--attack-damage", type=float, default=7.0)
    tool.add_argument("--mining-speed", type=float, default=9.0)
    tool.add_argument("--tier", type=int, default=4)
    tool.add_argument("--texture-style", default="metallic")

    ore = generate_sub.add_parser("ore", help="Generate ore + storage block bundle")
    ore.add_argument("material")
    ore.add_argument("--tier", type=int, default=3)
    ore.add_argument("--style", default="metallic")

    armor = generate_sub.add_parser("armor", help="Generate armor set")
    armor.add_argument("material")
    armor.add_argument("--tier", type=int, default=4)
    armor.add_argument("--style", default="metallic")

    machine = generate_sub.add_parser("machine", help="Generate machine block and model")
    machine.add_argument("machine_name")
    machine.add_argument("--material", default="steel")
    machine.add_argument("--style", default="industrial")

    block = generate_sub.add_parser("block", help="Generate block model/texture")
    block.add_argument("block_name")
    block.add_argument("--material", default="stone")
    block.add_argument("--style", default="industrial")

    batch = generate_sub.add_parser("batch", help="Batch generation presets")
    batch.add_argument(
        "preset",
        choices=["50_ores", "full_tool_set", "full_armor_set", "machines"],
        help="Batch preset to run",
    )
    batch.add_argument("--material", default="mythril")

    validate = subparsers.add_parser("validate", help="Validate generated assets")
    validate.add_argument("--strict", action="store_true")

    build = subparsers.add_parser("build", help="Build helper commands")
    build.add_argument("target", choices=["assets", "resourcepack", "datapack"])

    export = subparsers.add_parser("export", help="Export generated packs")
    export.add_argument("target", choices=["resourcepack", "datapack", "forge", "fabric"])

    scan = subparsers.add_parser("scan-registry", help="Scan Java registries")
    scan.add_argument("--source", default="src/main/java")
    scan.add_argument("--output", default="workspace/registry_snapshot.json")


def _run_generate(args: argparse.Namespace, context: AssetStudioContext) -> int:
    gtype = args.generate_type

    if gtype == "tool":
        generator = ToolGenerator(context)
        generator.generate(
            tool_name=args.tool_name,
            material=args.material,
            durability=args.durability,
            attack_damage=args.attack_damage,
            mining_speed=args.mining_speed,
            tier=args.tier,
            texture_style=args.texture_style,
        )
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

    if gtype == "batch":
        if args.preset == "50_ores":
            ore_gen = OreGenerator(context)
            for index in range(1, 51):
                ore_gen.generate(material=f"{args.material}_{index}", tier=2 + (index % 5), texture_style="metallic")
            return 0

        if args.preset == "full_tool_set":
            tool_gen = ToolGenerator(context)
            for tool_kind in ["pickaxe", "axe", "shovel", "hoe", "sword"]:
                tool_gen.generate(
                    tool_name=f"{args.material}_{tool_kind}",
                    material=args.material,
                    durability=1400,
                    attack_damage=6.0,
                    mining_speed=8.0,
                    tier=4,
                    texture_style="metallic",
                )
            return 0

        if args.preset == "full_armor_set":
            ArmorGenerator(context).generate(material=args.material, tier=4, texture_style="metallic")
            return 0

        if args.preset == "machines":
            machine_gen = MachineGenerator(context)
            for suffix in ["crusher", "smelter", "alloy_furnace", "assembler"]:
                machine_gen.generate(machine_name=f"{args.material}_{suffix}", material=args.material, texture_style="industrial")
            return 0

    raise ValueError(f"Unsupported generate command: {gtype}")


def run_cli(args: argparse.Namespace, workspace_path: Path) -> int:
    manager = WorkspaceManager(workspace_path)
    context = manager.load_context()

    if args.command == "generate":
        return _run_generate(args, context)

    if args.command == "validate":
        return validate_command(context, strict=args.strict)

    if args.command == "build":
        return build_command(context, args.target)

    if args.command == "export":
        return export_pack_command(context, args.target)

    if args.command == "scan-registry":
        source = Path(args.source)
        result = scan_registry_files(source)
        output_path = Path(args.output)
        output_path.parent.mkdir(parents=True, exist_ok=True)
        output_path.write_text(json.dumps(result.to_dict(), indent=2) + "\n", encoding="utf-8")
        print(f"Scanned {result.files_scanned} files, found {result.item_count} items and {result.block_count} blocks")
        print(f"Snapshot: {output_path}")
        return 0

    raise ValueError(f"Unsupported command: {args.command}")
