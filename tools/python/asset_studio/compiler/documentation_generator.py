from __future__ import annotations

import re
from pathlib import Path
from typing import TYPE_CHECKING

from asset_studio.compiler.dependency_resolver import DependencyResolution

if TYPE_CHECKING:
    from extremecraft_sdk.definitions.definition_types import AddonSpec


class DocumentationGenerator:
    """Generate addon and API markdown documentation for compiled modules."""

    def __init__(self, repo_root: Path) -> None:
        self.repo_root = repo_root

    def generate(self, addon: "AddonSpec", module_root: Path, resolution: DependencyResolution) -> list[Path]:
        docs_root = module_root / "docs"
        docs_root.mkdir(parents=True, exist_ok=True)

        addon_doc = docs_root / f"{addon.name}_addon.md"
        addon_doc.write_text(self._addon_doc(addon, resolution), encoding="utf-8")

        api_doc = docs_root / "extremecraft_api.md"
        api_doc.write_text(self._api_doc(), encoding="utf-8")

        return [addon_doc, api_doc]

    def _addon_doc(self, addon: "AddonSpec", resolution: DependencyResolution) -> str:
        dependency_graph = getattr(addon, "dependency_graph", None)
        definitions = "\n".join(f"- `{definition.type}` :: `{definition.id}`" for definition in addon.definitions) or "- _No definitions_"
        dependencies = "\n".join(
            f"- addon `{dep.id}` ({dep.version})" for dep in getattr(dependency_graph, "addons", [])
        ) or "- _None_"
        materials = "\n".join(f"- `{material}`" for material in getattr(dependency_graph, "materials", [])) or "- _None_"
        machines = "\n".join(f"- `{machine}`" for machine in getattr(dependency_graph, "machines", [])) or "- _None_"
        apis = "\n".join(f"- `{api.id}` ({api.version})" for api in getattr(dependency_graph, "apis", [])) or "- _None_"
        load_order = "\n".join(f"- `{entry}`" for entry in resolution.load_order) or "- _None_"
        compatible_platform = getattr(addon, "compatible_platform_version", None) or getattr(addon, "compatible_platform", "*")
        conflicts = (
            "\n".join(f"- `{conflict.kind}` {conflict.identifier}: {conflict.message}" for conflict in resolution.conflicts)
            or "- _No conflicts detected_"
        )

        return (
            f"# Addon Documentation: {addon.name}\n\n"
            f"- Namespace: `{addon.namespace}`\n"
            f"- Version: `{addon.version}`\n"
            f"- Compatible platform: `{compatible_platform}`\n\n"
            "## Definitions\n"
            f"{definitions}\n\n"
            "## Dependency Graph\n"
            "### Addons\n"
            f"{dependencies}\n\n"
            "### Materials\n"
            f"{materials}\n\n"
            "### Machines\n"
            f"{machines}\n\n"
            "### APIs\n"
            f"{apis}\n\n"
            "### Resolver Load Order\n"
            f"{load_order}\n\n"
            "## Resolver Conflicts\n"
            f"{conflicts}\n"
        )

    def _api_doc(self) -> str:
        api_root = self.repo_root / "api" / "src" / "main" / "java" / "com" / "extremecraft" / "api"
        if not api_root.exists():
            return "# ExtremeCraft API Surface\n\nAPI source folder not found.\n"

        sections: list[str] = ["# ExtremeCraft API Surface\n"]
        for java_file in sorted(api_root.rglob("*.java")):
            text = java_file.read_text(encoding="utf-8")
            package_line = self._first_match(text, r"package\s+([^;]+);")
            type_name = java_file.stem
            signatures = self._public_signatures(text)

            sections.append(f"## {type_name}\n")
            sections.append(f"- Package: `{package_line}`\n")
            if signatures:
                sections.append("### Public Members\n")
                sections.extend(f"- `{signature}`\n" for signature in signatures)
            else:
                sections.append("- _No public signatures extracted_\n")

            sections.append("\n")

        return "".join(sections)

    def _public_signatures(self, text: str) -> list[str]:
        signatures: list[str] = []
        for line in text.splitlines():
            stripped = line.strip()
            if not stripped.startswith("public"):
                continue
            if stripped.startswith("public class") or stripped.startswith("public interface") or stripped.startswith("public enum"):
                continue
            if not stripped.endswith(";") and "(" not in stripped:
                continue
            signatures.append(re.sub(r"\s+", " ", stripped))
        return signatures

    def _first_match(self, text: str, pattern: str) -> str:
        match = re.search(pattern, text)
        return match.group(1) if match else "unknown"
