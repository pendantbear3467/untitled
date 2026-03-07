from __future__ import annotations

import re
from dataclasses import dataclass, field

from extremecraft_sdk.definitions.definition_types import AddonSpec, SUPPORTED_DEFINITION_TYPES


_ID_RE = re.compile(r"^[a-z0-9_]+$")


@dataclass
class DefinitionValidationReport:
    errors: list[str] = field(default_factory=list)
    warnings: list[str] = field(default_factory=list)

    @property
    def ok(self) -> bool:
        return not self.errors


class DefinitionValidator:
    REQUIRED_FIELDS: dict[str, set[str]] = {
        "material": {"id", "tier", "durability", "mining_speed", "enchantability"},
        "machine": {"id"},
        "weapon": {"id", "material"},
        "tool": {"id", "material"},
        "armor": {"id"},
        "skill_tree": {"id"},
        "quest": {"id"},
        "worldgen": {"id"},
    }

    def validate(self, addon: AddonSpec) -> DefinitionValidationReport:
        report = DefinitionValidationReport()
        seen_ids: set[str] = set()

        for definition in addon.definitions:
            if definition.type not in SUPPORTED_DEFINITION_TYPES:
                report.warnings.append(
                    f"{definition.source_path}: unknown definition type '{definition.type}' (plugin may handle it)"
                )

            if not _ID_RE.match(definition.id):
                report.errors.append(f"{definition.source_path}: invalid id '{definition.id}'")

            key = f"{definition.type}:{definition.id}"
            if key in seen_ids:
                report.errors.append(f"{definition.source_path}: duplicate definition key '{key}'")
            seen_ids.add(key)

            required = self.REQUIRED_FIELDS.get(definition.type, {"id"})
            missing = sorted(field for field in required if field not in definition.payload)
            if missing:
                report.errors.append(
                    f"{definition.source_path}: missing required fields for {definition.type}: {', '.join(missing)}"
                )

        if not addon.definitions:
            report.warnings.append(f"addon '{addon.name}' has no definitions")

        return report
