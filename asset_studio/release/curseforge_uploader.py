from __future__ import annotations

import json
import os
from pathlib import Path
from urllib import error, request


class CurseForgeUploader:
    def __init__(self, context) -> None:
        self.context = context

    def upload(self, artifact: Path, changelog: str, dry_run: bool = True) -> dict:
        payload = {
            "artifact": str(artifact),
            "changelog": changelog,
            "gameVersions": ["1.20.1"],
            "releaseType": "release",
        }

        if dry_run:
            out = self.context.workspace_root / "releases" / f"{artifact.stem}_curseforge_upload_request.json"
            out.parent.mkdir(parents=True, exist_ok=True)
            out.write_text(json.dumps(payload, indent=2) + "\n", encoding="utf-8")
            return {"status": "dry-run", "request": out}

        token = os.getenv("CURSEFORGE_API_TOKEN", "")
        project_id = os.getenv("CURSEFORGE_PROJECT_ID", "")
        if not token or not project_id:
            raise RuntimeError("CURSEFORGE_API_TOKEN and CURSEFORGE_PROJECT_ID are required for publish")

        api_url = f"https://minecraft.curseforge.com/api/projects/{project_id}/upload-file"
        req = request.Request(api_url, data=json.dumps(payload).encode("utf-8"), method="POST")
        req.add_header("x-api-token", token)
        req.add_header("Content-Type", "application/json")

        try:
            with request.urlopen(req, timeout=30) as response:
                return json.loads(response.read().decode("utf-8"))
        except error.URLError as exc:
            raise RuntimeError(f"CurseForge upload failed: {exc}") from exc
