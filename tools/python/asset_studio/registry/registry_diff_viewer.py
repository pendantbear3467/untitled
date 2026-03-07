from __future__ import annotations

from dataclasses import dataclass, field
from difflib import SequenceMatcher

from asset_studio.registry.registry_snapshot import RegistrySnapshot


@dataclass
class RegistryRename:
    old_id: str
    new_id: str
    score: float


@dataclass
class RegistryConflict:
    id: str
    reason: str


@dataclass
class RegistryDiffResult:
    added_items: list[str] = field(default_factory=list)
    removed_items: list[str] = field(default_factory=list)
    added_blocks: list[str] = field(default_factory=list)
    removed_blocks: list[str] = field(default_factory=list)
    renamed_items: list[RegistryRename] = field(default_factory=list)
    conflicts: list[RegistryConflict] = field(default_factory=list)


class RegistryDiffViewer:
    """Produces high-signal diffs between two registry snapshots."""

    def diff(self, before: RegistrySnapshot, after: RegistrySnapshot) -> RegistryDiffResult:
        old_items = set(before.item_ids)
        new_items = set(after.item_ids)
        old_blocks = set(before.block_ids)
        new_blocks = set(after.block_ids)

        result = RegistryDiffResult(
            added_items=sorted(new_items - old_items),
            removed_items=sorted(old_items - new_items),
            added_blocks=sorted(new_blocks - old_blocks),
            removed_blocks=sorted(old_blocks - new_blocks),
        )

        result.renamed_items = self._detect_renames(result.removed_items, result.added_items)
        result.conflicts = self._detect_conflicts(after)
        return result

    def _detect_renames(self, removed: list[str], added: list[str]) -> list[RegistryRename]:
        candidates: list[RegistryRename] = []
        for old_id in removed:
            best = RegistryRename(old_id=old_id, new_id="", score=0.0)
            for new_id in added:
                score = SequenceMatcher(None, old_id, new_id).ratio()
                if score > best.score:
                    best = RegistryRename(old_id=old_id, new_id=new_id, score=score)
            if best.new_id and best.score >= 0.66:
                candidates.append(best)
        return sorted(candidates, key=lambda entry: entry.score, reverse=True)

    def _detect_conflicts(self, snapshot: RegistrySnapshot) -> list[RegistryConflict]:
        conflicts: list[RegistryConflict] = []
        item_ids = set(snapshot.item_ids)
        block_ids = set(snapshot.block_ids)
        for shared in sorted(item_ids.intersection(block_ids)):
            conflicts.append(RegistryConflict(id=shared, reason="Identifier exists as both item and block"))
        return conflicts
