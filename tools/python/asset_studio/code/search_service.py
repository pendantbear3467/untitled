from __future__ import annotations

from dataclasses import dataclass, field
from pathlib import Path

from asset_studio.code.document import TextDocument


@dataclass(frozen=True)
class SearchMatch:
    document_id: str
    line: int
    column: int
    preview: str
    path: Path | None = None


@dataclass
class SearchResults:
    query: str
    matches: list[SearchMatch] = field(default_factory=list)


class SearchService:
    def search_documents(self, query: str, documents: list[TextDocument], *, case_sensitive: bool = False) -> SearchResults:
        matches: list[SearchMatch] = []
        needle = query if case_sensitive else query.lower()
        for document in documents:
            for line_number, line in enumerate(document.content.splitlines(), start=1):
                haystack = line if case_sensitive else line.lower()
                index = haystack.find(needle)
                if index >= 0:
                    matches.append(
                        SearchMatch(
                            document_id=document.document_id,
                            line=line_number,
                            column=index + 1,
                            preview=line.strip(),
                            path=document.path,
                        )
                    )
        return SearchResults(query=query, matches=matches)

    def replace_all(self, document: TextDocument, needle: str, replacement: str, *, case_sensitive: bool = False) -> int:
        if not needle:
            return 0
        source = document.content
        if case_sensitive:
            count = source.count(needle)
            if count:
                document.set_content(source.replace(needle, replacement))
            return count

        lowered = source.lower()
        lowered_needle = needle.lower()
        index = 0
        count = 0
        result_parts: list[str] = []
        while True:
            match_index = lowered.find(lowered_needle, index)
            if match_index < 0:
                result_parts.append(source[index:])
                break
            result_parts.append(source[index:match_index])
            result_parts.append(replacement)
            index = match_index + len(needle)
            count += 1
        if count:
            document.set_content("".join(result_parts))
        return count
