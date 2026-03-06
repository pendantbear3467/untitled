from __future__ import annotations

from pathlib import Path

from extremecraft_sdk.definitions.definition_types import AddonSpec


class CodeGenerator:
    """Generates Java registry source for compiled addon modules."""

    def generate_registry_code(self, addon: AddonSpec, output_root: Path) -> Path:
        package_name = f"com.extremecraft.generated.{addon.namespace}"
        java_dir = output_root / "src" / "main" / "java" / Path(package_name.replace(".", "/"))
        java_dir.mkdir(parents=True, exist_ok=True)

        class_name = f"{self._to_pascal_case(addon.name)}Registries"
        source_path = java_dir / f"{class_name}.java"

        registrations = "\n".join(
            f'        // {definition.type}:{definition.id}' for definition in addon.definitions
        )

        source = (
            "package " + package_name + ";\n\n"
            "public final class " + class_name + " {\n"
            "    private " + class_name + "() {}\n\n"
            "    public static void registerAll() {\n"
            + (registrations or "        // no generated registrations")
            + "\n"
            "    }\n"
            "}\n"
        )

        source_path.write_text(source, encoding="utf-8")
        return source_path

    def _to_pascal_case(self, value: str) -> str:
        return "".join(part.capitalize() for part in value.replace("-", "_").split("_") if part)
