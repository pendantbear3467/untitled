"""Sample plugin for EXTREMECRAFT ASSET STUDIO.

This demonstrates how community plugins can register extension points.
"""


class CreateRecipeTemplateProvider:
    name = "create_compat_recipes"


class CreateMachineExporter:
    name = "create_machine_exporter"


def register(registry) -> None:
    registry.templates["create:recipes"] = CreateRecipeTemplateProvider
    registry.exporters["create:machines"] = CreateMachineExporter
