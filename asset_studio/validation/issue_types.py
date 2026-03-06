from __future__ import annotations

from dataclasses import dataclass


@dataclass
class ValidationIssue:
    severity: str
    category: str
    path: str
    message: str
