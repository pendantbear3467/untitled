from __future__ import annotations

import argparse
from pathlib import Path

from asset_studio.cli.cli_commands import register_subcommands, run_cli


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        prog="assetstudio",
        description="EXTREMECRAFT PLATFORM STUDIO - SDK, compiler, and ecosystem tooling",
    )
    parser.add_argument(
        "--workspace",
        default="workspace",
        help="Workspace directory (default: workspace)",
    )
    parser.add_argument(
        "--gui",
        action="store_true",
        help="Launch the desktop GUI application",
    )

    subparsers = parser.add_subparsers(dest="command")
    register_subcommands(subparsers)
    return parser


def main(argv: list[str] | None = None) -> int:
    parser = build_parser()
    args = parser.parse_args(argv)
    workspace = Path(args.workspace)

    if args.gui:
        from asset_studio.gui.app_window import launch_gui

        return launch_gui(workspace)

    if not args.command:
        parser.print_help()
        return 0

    return run_cli(args, workspace)


if __name__ == "__main__":
    raise SystemExit(main())
