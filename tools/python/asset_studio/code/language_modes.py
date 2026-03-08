from __future__ import annotations

from pathlib import Path


_EXTENSION_MAP = {
    ".json": "json",
    ".json5": "json",
    ".mcmeta": "json",
    ".py": "python",
    ".java": "java",
    ".kt": "kotlin",
    ".gradle": "groovy",
    ".groovy": "groovy",
    ".md": "markdown",
    ".toml": "toml",
    ".yaml": "yaml",
    ".yml": "yaml",
    ".txt": "text",
    ".lang": "properties",
    ".properties": "properties",
    ".cfg": "ini",
    ".ini": "ini",
    ".xml": "xml",
}


def detect_syntax_mode(path: Path | str | None) -> str:
    if path is None:
        return "text"
    suffix = Path(path).suffix.lower()
    return _EXTENSION_MAP.get(suffix, "text")
