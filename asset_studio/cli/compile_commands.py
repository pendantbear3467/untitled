from __future__ import annotations

import argparse

from compiler.module_builder import ModuleBuilder
from extremecraft_sdk.api.sdk import ExtremeCraftSDK


def register_compile_commands(parser: argparse.ArgumentParser) -> None:
    sub = parser.add_subparsers(dest="compile_target", required=True)

    expansion = sub.add_parser("expansion", help="Compile addon expansion into module artifact")
    expansion.add_argument("addon_name")


def run_compile_command(args: argparse.Namespace, context) -> int:
    if args.compile_target != "expansion":
        raise ValueError(f"Unsupported compile target: {args.compile_target}")

    sdk = ExtremeCraftSDK(
        addons_root=context.workspace_root / "addons",
        context=context,
        plugin_api=context.plugins,
    )
    builder = ModuleBuilder(context=context, sdk=sdk)
    result = builder.build_expansion(args.addon_name)

    print(f"Addon: {result.addon_name}")
    print(f"Java source: {result.java_source}")
    print(f"Artifact: {result.jar_path}")

    if result.dependency_load_order:
        print("Dependency load order:")
        for dependency in result.dependency_load_order:
            print(f"- {dependency}")

    if result.generated_java_sources:
        print("Generated Forge registry classes:")
        for source in result.generated_java_sources:
            print(f"- {source}")

    if result.conflicts:
        print("Conflicts detected:")
        for conflict in result.conflicts:
            print(f"- [{conflict.kind}] {conflict.identifier}: {conflict.message}")
    return 0
