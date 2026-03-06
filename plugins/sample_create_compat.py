"""Sample plugin for EXTREMECRAFT ASSET STUDIO.

This demonstrates extended plugin registration points.
"""

from pathlib import Path


class CreateRecipeTemplateProvider:
    name = "create_compat_recipes"


class CreateMachineExporter:
    name = "create_machine_exporter"


def create_repair_rule(context):
    marker = context.workspace_root / "plugins" / "create_compat_repair.marker"
    marker.parent.mkdir(parents=True, exist_ok=True)
    marker.write_text("create-compat plugin repair executed\n", encoding="utf-8")
    return []


def create_datapack_rule(context, definition, addon):
    if definition.type != "machine":
        return
    path = context.workspace_root / "data" / addon.namespace / "create_compat" / f"{definition.id}.json"
    context.write_json(path, {"machine": definition.id, "integration": "create"})


def register(registry) -> None:
    registry.templates["create:recipes"] = CreateRecipeTemplateProvider
    registry.exporters["create:machines"] = CreateMachineExporter
    registry.register_asset_repair("create_compat", create_repair_rule)
    registry.register_datapack_rule("create_compat", create_datapack_rule)
