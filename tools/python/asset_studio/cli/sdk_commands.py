from __future__ import annotations

import argparse
import shutil
from pathlib import Path

from asset_studio.sdk_support import OptionalDependencyUnavailableError, load_sdk_class, sdk_status


def register_sdk_commands(parser: argparse.ArgumentParser) -> None:
    sub = parser.add_subparsers(dest="sdk_command", required=True)

    init_cmd = sub.add_parser("init-addon", help="Create addon scaffold with SDK definition templates")
    init_cmd.add_argument("addon_name")

    validate_cmd = sub.add_parser("validate", help="Validate addon definitions")
    validate_cmd.add_argument("addon_name")

    generate_cmd = sub.add_parser("generate", help="Generate workspace assets from addon definitions")
    generate_cmd.add_argument("addon_name")


def run_sdk_command(args: argparse.Namespace, context) -> int:
    if args.sdk_command == "init-addon":
        return _init_addon(args.addon_name, context)

    status = sdk_status(args.sdk_command)
    if not status.available:
        print(status.message)
        if status.error:
            print(status.error)
        return 2

    try:
        sdk_class = load_sdk_class(f"sdk {args.sdk_command}")
        sdk = sdk_class(
            addons_root=context.workspace_root / "addons",
            context=context,
            plugin_api=context.plugins,
        )
    except OptionalDependencyUnavailableError as exc:
        print(str(exc))
        return 2

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
        for path in result.generated_paths:
            print(f"- {path}")
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
        "compatible_platform_version": ">=1.2.0",
        "dependency_graph": {
            "materials": [],
            "machines": [],
            "addons": [{"id": "extremecraft-core", "version": ">=1.0.0"}],
            "apis": [
                {"id": "extremecraft-api", "version": ">=1"},
                {"id": "extremecraft-protocol", "version": ">=1"},
            ],
        },
    }
    context.write_json(addon_root / "addon.json", manifest)

    templates_root = _sdk_templates_root(context.repo_root)
    copied = 0
    if templates_root is not None and templates_root.exists():
        for template in templates_root.glob("*.json"):
            destination = definitions_root / template.name
            if not destination.exists():
                shutil.copy2(template, destination)
                copied += 1
    else:
        print("SDK templates are unavailable; created addon manifest only.")

    print(f"Initialized addon scaffold: {addon_root}")
    if copied:
        print(f"Copied {copied} template definition(s)")
    return 0


def _sdk_templates_root(repo_root: Path) -> Path | None:
    candidates = [
        repo_root / "tools" / "python" / "extremecraft_sdk" / "templates",
        repo_root / "extremecraft_sdk" / "templates",
    ]
    for candidate in candidates:
        if candidate.exists():
            return candidate
    return None
