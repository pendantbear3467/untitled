from __future__ import annotations

import argparse

from asset_studio.release.release_manager import ReleaseManager


def register_release_commands(parser: argparse.ArgumentParser) -> None:
    sub = parser.add_subparsers(dest="release_command", required=True)

    build = sub.add_parser("build", help="Build release artifact and changelog")
    build.add_argument("--name", default=None)

    publish = sub.add_parser("publish", help="Publish to GitHub and CurseForge")
    publish.add_argument("--name", default=None)
    publish.add_argument("--live", action="store_true", help="Perform network publish instead of dry-run")


def run_release_command(args: argparse.Namespace, context) -> int:
    manager = ReleaseManager(context)

    if args.release_command == "build":
        result = manager.build(release_name=args.name)
        print(f"Release: {result.release_name}")
        print(f"Artifact: {result.artifact}")
        print(f"Changelog: {result.changelog}")
        return 0

    if args.release_command == "publish":
        release = manager.build(release_name=args.name)
        summary = manager.publish(release=release, dry_run=not args.live)
        print(f"Published release workflow: {summary['release']}")
        print(f"Summary: {context.workspace_root / 'releases' / (summary['release'] + '_publish_summary.json')}")
        return 0

    raise ValueError(f"Unsupported release command: {args.release_command}")
