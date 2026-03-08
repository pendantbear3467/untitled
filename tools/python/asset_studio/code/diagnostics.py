from __future__ import annotations

from dataclasses import dataclass, field
from pathlib import Path


@dataclass(frozen=True)
class CodeDiagnostic:
    severity: str
    message: str
    line: int = 1
    column: int = 1
    code: str | None = None
    source: str | None = None
    path: Path | None = None


@dataclass
class ProblemsModel:
    _issues: dict[str, list[CodeDiagnostic]] = field(default_factory=dict)

    def set_document_issues(self, document_id: str, diagnostics: list[CodeDiagnostic]) -> None:
        self._issues[document_id] = list(diagnostics)

    def add_issue(self, document_id: str, diagnostic: CodeDiagnostic) -> None:
        self._issues.setdefault(document_id, []).append(diagnostic)

    def clear_document(self, document_id: str) -> None:
        self._issues.pop(document_id, None)

    def issues_for(self, document_id: str) -> list[CodeDiagnostic]:
        return list(self._issues.get(document_id, []))

    def all_issues(self) -> list[CodeDiagnostic]:
        issues: list[CodeDiagnostic] = []
        for diagnostics in self._issues.values():
            issues.extend(diagnostics)
        return issues
