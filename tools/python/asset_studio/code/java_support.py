from __future__ import annotations

import re
from dataclasses import dataclass, field
from pathlib import Path

from asset_studio.code.diagnostics import CodeDiagnostic


PACKAGE_RE = re.compile(r"^\s*package\s+([A-Za-z_][\w.]*)\s*;")
IMPORT_RE = re.compile(r"^\s*import\s+(static\s+)?([A-Za-z_][\w.*]*)\s*;")
TYPE_RE = re.compile(
    r"^\s*(?:@[A-Za-z_][\w.()\",\s]*)*\s*(?P<mods>(?:(?:public|protected|private|abstract|final|sealed|non-sealed|static)\s+)*)"
    r"(?P<kind>class|interface|enum|record)\s+(?P<name>[A-Za-z_][\w]*)"
)
METHOD_RE = re.compile(
    r"^\s*(?:@[A-Za-z_][\w.()\",\s]*)*\s*(?P<mods>(?:(?:public|protected|private|static|final|abstract|synchronized|native|default|strictfp)\s+)*)"
    r"(?:(?P<return>[A-Za-z_][\w<>,.?\[\]\s]*)\s+)?(?P<name>[A-Za-z_][\w]*)\s*\((?P<params>[^)]*)\)"
    r"\s*(?:throws\s+[A-Za-z_][\w.,\s]*)?\s*(?P<ender>\{|;)?\s*$"
)
FIELD_RE = re.compile(
    r"^\s*(?:@[A-Za-z_][\w.()\",\s]*)*\s*(?P<mods>(?:(?:public|protected|private|static|final|transient|volatile)\s+)*)"
    r"(?P<type>[A-Za-z_][\w<>,.?\[\]\s]*)\s+(?P<name>[A-Za-z_][\w]*)\s*(?:=[^;]*)?;\s*$"
)
RESOURCE_ID_RE = re.compile(r"([a-z0-9_\-.]+:[a-z0-9_\-./]+)")
LINKED_FILE_RE = re.compile(r"([A-Za-z0-9_./-]+\.(?:gui|model)\.json)")


@dataclass
class JavaSymbol:
    symbol_type: str
    name: str
    line: int
    column: int = 1
    signature: str = ""
    container: str | None = None
    modifiers: tuple[str, ...] = ()

    @property
    def display_name(self) -> str:
        if self.signature:
            return self.signature
        return self.name


@dataclass
class JavaAnalysis:
    package_name: str | None = None
    imports: list[str] = field(default_factory=list)
    symbols: list[JavaSymbol] = field(default_factory=list)
    diagnostics: list[CodeDiagnostic] = field(default_factory=list)
    resource_ids: list[str] = field(default_factory=list)
    linked_files: list[str] = field(default_factory=list)

    def by_type(self, symbol_type: str) -> list[JavaSymbol]:
        return [symbol for symbol in self.symbols if symbol.symbol_type == symbol_type]


def analyze_java_source(text: str, *, path: Path | None = None) -> JavaAnalysis:
    analysis = JavaAnalysis()
    brace_depth = 0
    type_stack: list[tuple[str, int]] = []
    method_depths: list[int] = []

    for line_number, raw_line in enumerate(text.splitlines(), start=1):
        stripped = _strip_comments(raw_line).strip()
        if not stripped:
            brace_depth += _brace_delta(raw_line)
            _trim_context(type_stack, method_depths, brace_depth)
            continue

        package_match = PACKAGE_RE.match(stripped)
        if package_match:
            analysis.package_name = package_match.group(1)

        import_match = IMPORT_RE.match(stripped)
        if import_match:
            analysis.imports.append(import_match.group(2))

        current_type = type_stack[-1][0] if type_stack else None
        in_method = bool(method_depths and brace_depth >= method_depths[-1])

        type_match = TYPE_RE.match(stripped)
        if type_match and not in_method:
            mods = tuple(part for part in type_match.group("mods").split() if part)
            symbol = JavaSymbol(
                symbol_type=type_match.group("kind"),
                name=type_match.group("name"),
                line=line_number,
                column=max(1, raw_line.find(type_match.group("name")) + 1),
                signature=f"{type_match.group('kind')} {type_match.group('name')}",
                container=current_type,
                modifiers=mods,
            )
            analysis.symbols.append(symbol)

        if current_type and not in_method:
            method_match = METHOD_RE.match(stripped)
            if method_match and not stripped.startswith(("if ", "for ", "while ", "switch ", "catch ", "return ")):
                name = method_match.group("name")
                return_type = (method_match.group("return") or "").strip()
                if name not in {"if", "for", "while", "switch", "catch", "new", "return"}:
                    params = method_match.group("params").strip()
                    mods = tuple(part for part in method_match.group("mods").split() if part)
                    signature = f"{name}({params})"
                    if return_type and name != current_type:
                        signature = f"{return_type} {signature}"
                    analysis.symbols.append(
                        JavaSymbol(
                            symbol_type="method",
                            name=name,
                            line=line_number,
                            column=max(1, raw_line.find(name) + 1),
                            signature=signature,
                            container=current_type,
                            modifiers=mods,
                        )
                    )
                    if method_match.group("ender") == "{" or "{" in stripped:
                        method_depths.append(brace_depth + 1)
            else:
                field_match = FIELD_RE.match(stripped)
                if field_match and "(" not in stripped:
                    name = field_match.group("name")
                    field_type = field_match.group("type").strip()
                    mods = tuple(part for part in field_match.group("mods").split() if part)
                    analysis.symbols.append(
                        JavaSymbol(
                            symbol_type="field",
                            name=name,
                            line=line_number,
                            column=max(1, raw_line.find(name) + 1),
                            signature=f"{field_type} {name}",
                            container=current_type,
                            modifiers=mods,
                        )
                    )

        brace_depth += _brace_delta(raw_line)
        if type_match and "{" in stripped:
            type_stack.append((type_match.group("name"), brace_depth))
        _trim_context(type_stack, method_depths, brace_depth)

    if brace_depth != 0:
        analysis.diagnostics.append(
            CodeDiagnostic(
                severity="warning",
                message="Brace count is unbalanced",
                line=1,
                source="java",
                path=path,
            )
        )
    if path is not None and path.suffix == ".java" and "src/main/java" in str(path).replace("\\", "/") and not analysis.package_name:
        analysis.diagnostics.append(
            CodeDiagnostic(
                severity="warning",
                message="Java source under src/main/java is missing a package declaration",
                line=1,
                source="java",
                path=path,
            )
        )

    resource_ids = sorted({match.group(1) for match in RESOURCE_ID_RE.finditer(text)})
    linked_files = sorted({match.group(1) for match in LINKED_FILE_RE.finditer(text)})
    analysis.resource_ids.extend(resource_ids)
    analysis.linked_files.extend(linked_files)
    return analysis


def suggest_package_for_path(path: Path, *, repo_root: Path | None = None) -> str:
    normalized = path.resolve(strict=False)
    parts = normalized.parts
    marker = ("src", "main", "java")
    for index in range(len(parts) - len(marker) + 1):
        if parts[index:index + len(marker)] == marker:
            package_parts = [part for part in normalized.parent.parts[index + len(marker):] if part]
            return ".".join(package_parts)
    if repo_root is not None:
        try:
            relative = normalized.relative_to((repo_root / "src" / "main" / "java").resolve(strict=False))
            return ".".join(relative.parent.parts)
        except ValueError:
            return ""
    return ""


def suggest_java_target_path(workspace_root: Path, package_name: str, type_name: str, *, repo_root: Path | None = None) -> Path:
    base = (repo_root or workspace_root) / "src" / "main" / "java"
    package_path = Path(*[part for part in package_name.split(".") if part]) if package_name else Path()
    return (base / package_path / f"{type_name}.java").resolve(strict=False)


def render_java_scaffold(kind: str, type_name: str, *, package_name: str = "", extra: str = "") -> str:
    header = f"package {package_name};\n\n" if package_name else ""
    extra_block = f"\n{extra.strip()}\n" if extra.strip() else ""
    if kind == "class":
        body = f"public class {type_name} {{{extra_block}}}\n"
    elif kind == "interface":
        body = f"public interface {type_name} {{{extra_block}}}\n"
    elif kind == "enum":
        body = f"public enum {type_name} {{\n    SAMPLE\n}}\n"
    elif kind == "record":
        body = f"public record {type_name}(String id) {{{extra_block}}}\n"
    else:
        raise ValueError(f"Unsupported Java scaffold kind: {kind}")
    return header + body


def pascal_case(value: str) -> str:
    return "".join(part.capitalize() for part in re.split(r"[^A-Za-z0-9]+", value) if part)


def snake_case(value: str) -> str:
    parts = [part.lower() for part in re.split(r"[^A-Za-z0-9]+", value) if part]
    return "_".join(parts)


def _strip_comments(line: str) -> str:
    if "//" in line:
        return line.split("//", 1)[0]
    return line


def _brace_delta(line: str) -> int:
    return line.count("{") - line.count("}")


def _trim_context(type_stack: list[tuple[str, int]], method_depths: list[int], brace_depth: int) -> None:
    while method_depths and brace_depth < method_depths[-1]:
        method_depths.pop()
    while type_stack and brace_depth < type_stack[-1][1]:
        type_stack.pop()
