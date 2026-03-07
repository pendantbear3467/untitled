from __future__ import annotations

from datetime import datetime
from pathlib import Path

from asset_studio.registry.registry_diff_viewer import RegistryDiffViewer
from asset_studio.registry.registry_history import RegistryHistory


class ChangelogGenerator:
    def __init__(self, context) -> None:
        self.context = context
        self.history = RegistryHistory(context.workspace_root)

    def build_changelog(self, release_name: str) -> Path:
        release_dir = self.context.workspace_root / "releases"
        release_dir.mkdir(parents=True, exist_ok=True)
        changelog_path = release_dir / f"{release_name}_CHANGELOG.md"

        lines: list[str] = []
        lines.append(f"# {release_name} Changelog")
        lines.append("")
        lines.append(f"Generated: {datetime.utcnow().isoformat()}Z")
        lines.append("")

        snapshots = self.history.list_snapshots()
        if len(snapshots) >= 2:
            before = self.history.load_snapshot(snapshots[-2].name)
            after = self.history.load_snapshot(snapshots[-1].name)
            diff = RegistryDiffViewer().diff(before, after)
            lines.append("## Registry Changes")
            lines.append(f"- Items added: {len(diff.added_items)}")
            lines.append(f"- Items removed: {len(diff.removed_items)}")
            lines.append(f"- Blocks added: {len(diff.added_blocks)}")
            lines.append(f"- Blocks removed: {len(diff.removed_blocks)}")
            if diff.renamed_items:
                lines.append("- Potential renames:")
                for rename in diff.renamed_items[:8]:
                    lines.append(f"  - `{rename.old_id}` -> `{rename.new_id}` ({rename.score:.2f})")
            lines.append("")
        else:
            lines.append("## Registry Changes")
            lines.append("- No baseline snapshots available.")
            lines.append("")

        lines.append("## Notes")
        lines.append("- Release built via ExtremeCraft Asset Studio pipeline.")

        changelog_path.write_text("\n".join(lines) + "\n", encoding="utf-8")
        return changelog_path
