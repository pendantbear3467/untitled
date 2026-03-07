from __future__ import annotations

import argparse

from asset_studio.modpack.modpack_builder import ModpackBuilder


def register_modpack_commands(parser: argparse.ArgumentParser) -> None:
    sub = parser.add_subparsers(dest="modpack_command", required=True)
    build = sub.add_parser("build", help="Build modpack manifest and archive")
    build.add_argument("modpack_name")


def run_modpack_command(args: argparse.Namespace, context) -> int:
    if args.modpack_command != "build":
        raise ValueError(f"Unsupported modpack command: {args.modpack_command}")

    result = ModpackBuilder(context).build(args.modpack_name)
    print(f"Modpack: {result.name}")
    print(f"Manifest: {result.manifest_path}")
    print(f"Archive: {result.archive_path}")
    return 0
