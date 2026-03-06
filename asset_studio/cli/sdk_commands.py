from __future__ import annotations

import argparse
import shutil
from pathlib import Path

from extremecraft_sdk.api.sdk import ExtremeCraftSDK


def register_sdk_commands(parser: argparse.ArgumentParser) -> None:
    sub = parser.add_subparsers(dest="sdk_command", required=True)

    init_cmd = sub.add_parser("init-addon", help="Create addon scaffold with SDK definition templates")
    init_cmd.add_argument("addon_name")

    validate_cmd = sub.add_parser("validate", help="Validate addon definitions")
    validate_cmd.add_argument("addon_name")

    generate_cmd = sub.add_parser("generate", help="Generate workspace assets from addon definitions")
    generate_cmd.add_argument("addon_name")


def run_sdk_command(args: argparse.Namespace, context) -> int:
    sdk = ExtremeCraftSDK(
        addons_root=context.workspace_root / "addons",
        context=context,
        plugin_api=context.plugins,
    )

    if args.sdk_command == "init-addon":
        return _init_addon(args.addon_name, context)

    addon = sdk.load_addon(args.addon_name)
    report = sdk.validate_addon(addon)

    if args.sdk_command == "validate":
        print(f"Addon: {addon.name}")
        print(f"Definitions: {len(addon.definitions)}")
        for warning in report.warnings:
            print(f"[warning] {warning}")
        for error in report.errors:
            print(f"[error] {error}")
        return 0 if report.ok else 1

    if args.sdk_command == "generate":
        if report.errors:
            print("Validation failed; cannot generate:")
            for error in report.errors:
                print(f"- {error}")
            return 1
        result = sdk.generate_addon(addon)
        print(f"Generated {len(result.generated_paths)} paths for addon '{addon.name}'")
        return 0

    raise ValueError(f"Unsupported sdk command: {args.sdk_command}")


def _init_addon(addon_name: str, context) -> int:
    addon_root = context.workspace_root / "addons" / addon_name
    definitions_root = addon_root / "definitions"
    definitions_root.mkdir(parents=True, exist_ok=True)

    manifest = {
        "name": addon_name,
        "namespace": addon_name,
        "version": "0.1.0",
        "dependencies": ["extremecraft-core"],
    }
    context.write_json(addon_root / "addon.json", manifest)

    templates_root = context.repo_root / "extremecraft_sdk" / "templates"
    if templates_root.exists():
        for template in templates_root.glob("*.json"):
            destination = definitions_root / template.name
            if not destination.exists():
                shutil.copy2(template, destination)

    print(f"Initialized addon scaffold: {addon_root}")
    return 0
