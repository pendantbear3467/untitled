from __future__ import annotations

from dataclasses import dataclass, field


@dataclass(frozen=True)
class HelpEntry:
    id: str
    label: str
    short_tooltip: str
    long_description: str
    category: str = "general"
    docs_ref: str | None = None
    example: str | None = None
    keywords: tuple[str, ...] = ()


@dataclass
class HelpRegistry:
    _entries: dict[str, HelpEntry] = field(default_factory=dict)

    def register(self, entry: HelpEntry) -> None:
        self._entries[entry.id] = entry

    def get(self, entry_id: str) -> HelpEntry | None:
        return self._entries.get(entry_id)

    def all(self) -> list[HelpEntry]:
        return sorted(self._entries.values(), key=lambda entry: (entry.category, entry.label.lower(), entry.id))

    def categories(self) -> list[str]:
        return sorted({entry.category for entry in self._entries.values()})

    def search(self, text: str = "", category: str | None = None) -> list[HelpEntry]:
        needle = text.strip().lower()
        matches: list[HelpEntry] = []
        for entry in self.all():
            if category and entry.category != category:
                continue
            haystack = " ".join(
                [
                    entry.id,
                    entry.label,
                    entry.short_tooltip,
                    entry.long_description,
                    *(entry.keywords or ()),
                ]
            ).lower()
            if not needle or needle in haystack:
                matches.append(entry)
        return matches
