from __future__ import annotations

import argparse
from pathlib import Path

from asset_studio.addons.addon_manager import AddonManager


def register_addon_commands(parser: argparse.ArgumentParser) -> None:
    sub = parser.add_subparsers(dest="addon_command", required=True)

    install = sub.add_parser("install", help="Install addon from folder or zip package")
    install.add_argument("source")

    build = sub.add_parser("build", help="Build addon package")
    build.add_argument("addon_name")

    sub.add_parser("build-all", help="Build packages for all installed addons")
    sub.add_parser("list", help="List installed addons")

    remove = sub.add_parser("remove", help="Remove installed addon")
    remove.add_argument("addon_name")

    publish = sub.add_parser("publish", help="Publish addon package into releases folder")
    publish.add_argument("addon_name")


def run_addon_command(args: argparse.Namespace, context) -> int:
    manager = AddonManager(context)

    if args.addon_command == "install":
        installed = manager.install(Path(args.source))
        print(f"Addon installed: {installed}")
        return 0

    if args.addon_command == "list":
        addons = manager.list_addons()
        if not addons:
            print("No addons installed.")
            return 0

        print("Installed addons:")
        for addon in addons:
            print(f"- {addon.name} ({addon.version}) namespace={addon.namespace} path={addon.root}")
        return 0

    if args.addon_command == "build":
        try:
            result = manager.build(args.addon_name)
        except FileNotFoundError as exc:
            print(str(exc))
            return 1
        print(f"Addon build: {result.output_path}")
        return 0

    if args.addon_command == "build-all":
        results = manager.build_all()
        if not results:
            print("No addons installed.")
            return 0

        print("Built addon packages:")
        for result in results:
            print(f"- {result.name}: {result.output_path}")
        return 0

    if args.addon_command == "remove":
        try:
            removed = manager.remove(args.addon_name)
        except FileNotFoundError as exc:
            print(str(exc))
            return 1
        print(f"Addon removed: {removed}")
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
