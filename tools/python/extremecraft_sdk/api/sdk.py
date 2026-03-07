from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path

from extremecraft_sdk.definitions.definition_types import AddonSpec
from extremecraft_sdk.definitions.loader import DefinitionLoader
from extremecraft_sdk.generators.content_generators import SDKGenerationResult, SDKGenerator
from extremecraft_sdk.validators.definition_validators import DefinitionValidationReport, DefinitionValidator


@dataclass
class SDKConfig:
    addons_root: Path


class ExtremeCraftSDK:
    """High-level SDK facade for loading, validating, and generating addon content."""

    def __init__(self, addons_root: Path, context, plugin_api) -> None:
        self.config = SDKConfig(addons_root=addons_root)
        self._loader = DefinitionLoader(addons_root)
        self._validator = DefinitionValidator()
        self._generator = SDKGenerator(context=context, plugin_api=plugin_api)

    def load_addon(self, addon_name: str) -> AddonSpec:
        return self._loader.load_addon(addon_name)

    def validate_addon(self, addon: AddonSpec) -> DefinitionValidationReport:
        return self._validator.validate(addon)

    def generate_addon(self, addon: AddonSpec) -> SDKGenerationResult:
        return self._generator.generate_addon(addon)
