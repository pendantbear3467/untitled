from __future__ import annotations

import json
import zipfile
from dataclasses import dataclass
from datetime import datetime
from pathlib import Path

from asset_studio.release.changelog_generator import ChangelogGenerator
from asset_studio.release.curseforge_uploader import CurseForgeUploader
from asset_studio.release.github_release_manager import GitHubReleaseManager


@dataclass
class ReleaseBuildResult:
    release_name: str
    artifact: Path
    changelog: Path


class ReleaseManager:
    """Builds and publishes release artifacts for ExtremeCraft modules."""

    def __init__(self, context) -> None:
        self.context = context
        self.github = GitHubReleaseManager(context)
        self.curseforge = CurseForgeUploader(context)
        self.changelog = ChangelogGenerator(context)

    def build(self, release_name: str | None = None) -> ReleaseBuildResult:
        timestamp = datetime.utcnow().strftime("%Y%m%d-%H%M%S")
        name = release_name or f"extremecraft-{timestamp}"

        release_root = self.context.workspace_root / "releases"
        release_root.mkdir(parents=True, exist_ok=True)
        artifact = release_root / f"{name}.jar"

        with zipfile.ZipFile(artifact, "w", compression=zipfile.ZIP_DEFLATED) as archive:
            for source, target_root in [
                (self.context.repo_root / "src/main/resources", Path("src/main/resources")),
                (self.context.workspace_root / "assets", Path("workspace/assets")),
                (self.context.workspace_root / "data", Path("workspace/data")),
            ]:
                if not source.exists():
                    continue
                for file in source.rglob("*"):
                    if file.is_file():
                        archive.write(file, target_root / file.relative_to(source))

        changelog_path = self.changelog.build_changelog(name)
        return ReleaseBuildResult(release_name=name, artifact=artifact, changelog=changelog_path)

    def publish(self, release: ReleaseBuildResult, dry_run: bool = True) -> dict:
        changelog_body = release.changelog.read_text(encoding="utf-8")
        github_response = self.github.create_release(
            tag=release.release_name,
            title=release.release_name,
            body=changelog_body,
            artifact=release.artifact,
            dry_run=dry_run,
        )
        curseforge_response = self.curseforge.upload(
            artifact=release.artifact,
            changelog=changelog_body,
            dry_run=dry_run,
        )

        summary = {
            "release": release.release_name,
            "artifact": str(release.artifact),
            "changelog": str(release.changelog),
            "github": github_response,
            "curseforge": curseforge_response,
        }
        out = self.context.workspace_root / "releases" / f"{release.release_name}_publish_summary.json"
        out.write_text(json.dumps(summary, indent=2) + "\n", encoding="utf-8")
        return summary
