from __future__ import annotations

import argparse
from pathlib import Path

from asset_studio.addons.addon_manager import AddonManager


def register_addon_commands(parser: argparse.ArgumentParser) -> None:
    sub = parser.add_subparsers(dest="addon_command", required=True)

    install = sub.add_parser("install", help="Install addon from folder containing addon.json")
    install.add_argument("source")

    build = sub.add_parser("build", help="Build addon package")
    build.add_argument("addon_name")

    publish = sub.add_parser("publish", help="Publish addon package into releases folder")
    publish.add_argument("addon_name")


def run_addon_command(args: argparse.Namespace, context) -> int:
    manager = AddonManager(context)

    if args.addon_command == "install":
        installed = manager.install(Path(args.source))
        print(f"Addon installed: {installed}")
        return 0

    if args.addon_command == "build":
        try:
            result = manager.build(args.addon_name)
        except FileNotFoundError as exc:
            print(str(exc))
            return 1
        print(f"Addon build: {result.output_path}")
        return 0

    if args.addon_command == "publish":
        try:
            published = manager.publish(args.addon_name)
        except FileNotFoundError as exc:
            print(str(exc))
            return 1
        print(f"Addon published: {published}")
        return 0

    raise ValueError(f"Unsupported addon command: {args.addon_command}")
