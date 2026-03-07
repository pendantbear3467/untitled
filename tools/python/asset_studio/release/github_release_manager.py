from __future__ import annotations

import json
import os
from pathlib import Path
from urllib import error, request


class GitHubReleaseManager:
    def __init__(self, context) -> None:
        self.context = context

    def create_release(self, tag: str, title: str, body: str, artifact: Path, dry_run: bool = True) -> dict:
        payload = {
            "tag_name": tag,
            "name": title,
            "body": body,
            "draft": False,
            "prerelease": False,
            "artifact": str(artifact),
        }

        if dry_run:
            out = self.context.workspace_root / "releases" / f"{tag}_github_release_request.json"
            out.parent.mkdir(parents=True, exist_ok=True)
            out.write_text(json.dumps(payload, indent=2) + "\n", encoding="utf-8")
            return {"status": "dry-run", "request": out}

        token = os.getenv("GITHUB_TOKEN", "")
        repo = os.getenv("GITHUB_REPOSITORY", "")
        if not token or not repo:
            raise RuntimeError("GITHUB_TOKEN and GITHUB_REPOSITORY are required for publish")

        api_url = f"https://api.github.com/repos/{repo}/releases"
        req = request.Request(api_url, data=json.dumps(payload).encode("utf-8"), method="POST")
        req.add_header("Authorization", f"Bearer {token}")
        req.add_header("Accept", "application/vnd.github+json")
        req.add_header("Content-Type", "application/json")

        try:
            with request.urlopen(req, timeout=30) as response:
                return json.loads(response.read().decode("utf-8"))
        except error.URLError as exc:
            raise RuntimeError(f"GitHub release publish failed: {exc}") from exc
