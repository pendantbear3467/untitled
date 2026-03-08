from __future__ import annotations

import difflib
import json
import re
from dataclasses import dataclass, field
from pathlib import Path
from typing import Any, Protocol
from uuid import uuid4

from asset_studio.code.java_support import pascal_case, render_java_scaffold, snake_case


@dataclass
class AIRequest:
    mode: str
    prompt: str
    current_text: str = ""
    current_path: Path | None = None
    selection: str = ""
    preview_payload: dict[str, Any] | None = None
    relationship_context: dict[str, Any] = field(default_factory=dict)


@dataclass
class ProviderResult:
    content: str
    payload: dict[str, Any] | None = None
    target_kind: str = "text"
    title: str = "Generated Artifact"
    metadata: dict[str, Any] = field(default_factory=dict)


@dataclass
class AIArtifact:
    artifact_id: str
    mode: str
    prompt: str
    provider_name: str
    target_kind: str
    title: str
    candidate_content: str
    candidate_payload: dict[str, Any] | None = None
    diff_preview: str = ""
    validation_messages: list[str] = field(default_factory=list)
    preview_summary: dict[str, Any] = field(default_factory=dict)
    source_path: Path | None = None
    apply_kind: str = "preview"


class AIProvider(Protocol):
    name: str

    def generate(self, request: AIRequest) -> ProviderResult:
        ...


class MockAIProvider:
    name = "Mock Provider"

    def generate(self, request: AIRequest) -> ProviderResult:
        mode = request.mode.strip().lower()
        prompt = request.prompt.strip() or "Untitled"
        base_name = _extract_name(prompt, request.current_path)
        if mode == "explain current selection":
            explanation = self._explain_selection(request)
            return ProviderResult(content=explanation, target_kind="markdown", title="Selection Explanation")
        if mode == "generate java class draft":
            type_name = pascal_case(base_name or "GeneratedFeature")
            package_name = _package_from_path(request.current_path)
            content = render_java_scaffold("class", type_name, package_name=package_name)
            return ProviderResult(content=content, target_kind="java", title=f"{type_name}.java")
        if mode == "generate gui draft":
            payload = self._gui_payload(base_name, prompt)
            return ProviderResult(content=json.dumps(payload, indent=2), payload=payload, target_kind="gui", title=f"{payload['name']}.gui.json")
        if mode == "generate model draft":
            payload = self._model_payload(base_name, prompt)
            return ProviderResult(content=json.dumps(payload, indent=2), payload=payload, target_kind="model", title=f"{payload['name']}.model.json")
        if mode == "convert preview to runtime definition":
            payload = dict(request.preview_payload or {})
            content = json.dumps(payload, indent=2) if payload else json.dumps({"message": "No preview payload supplied"}, indent=2)
            return ProviderResult(content=content, payload=payload or None, target_kind="json", title="runtime-definition.json")
        if mode in {"apply to current file", "generate diff preview"}:
            content = self._apply_to_current(request)
            return ProviderResult(content=content, target_kind=_kind_from_path(request.current_path), title=request.current_path.name if request.current_path else "current-buffer")
        generated = f"// Generated from prompt\n{prompt}\n"
        return ProviderResult(content=generated, target_kind=_kind_from_path(request.current_path), title="generated.txt")

    def _explain_selection(self, request: AIRequest) -> str:
        lines = ["# Explanation", "", f"Mode: {request.mode}", f"Selection: {request.selection or 'entire document'}"]
        if request.current_path is not None:
            lines.append(f"Path: {request.current_path}")
        if request.relationship_context:
            lines.append("")
            lines.append("## Relationship Context")
            for key, value in sorted(request.relationship_context.items()):
                lines.append(f"- {key}: {value}")
        lines.append("")
        lines.append(f"Prompt summary: {request.prompt.strip()}")
        return "\n".join(lines)

    def _apply_to_current(self, request: AIRequest) -> str:
        current = request.current_text or ""
        if request.current_path is not None and request.current_path.suffix == ".java":
            insert = "\n    public void generatedAction() {\n        // generated stub\n    }\n"
            if current.rstrip().endswith("}"):
                return current.rstrip()[:-1] + insert + "}\n"
            return current + insert
        if current:
            return current.rstrip() + f"\n\n# Generated change\n{request.prompt.strip()}\n"
        return request.prompt.strip() + "\n"

    def _gui_payload(self, base_name: str, prompt: str) -> dict[str, Any]:
        name = snake_case(base_name or "generated_gui")
        return {
            "schemaVersion": 2,
            "documentType": "gui-studio",
            "name": name,
            "namespace": "extremecraft",
            "screenType": "machine",
            "width": 176,
            "height": 166,
            "rootWidgets": ["root_panel", "title_label"],
            "metadata": {"generatedBy": self.name, "prompt": prompt},
            "widgets": [
                {"id": "root_panel", "type": "panel", "label": "Root Panel", "bounds": {"x": 0, "y": 0, "width": 176, "height": 166}, "anchor": {}, "properties": {"background": "textures/gui/default_panel.png"}, "binding": None, "children": [], "tags": ["root"], "visible": True, "zIndex": 0},
                {"id": "title_label", "type": "label", "label": pascal_case(name), "bounds": {"x": 8, "y": 8, "width": 120, "height": 18}, "anchor": {}, "properties": {"text": pascal_case(name)}, "binding": None, "children": [], "tags": ["generated"], "visible": True, "zIndex": 10},
            ],
        }

    def _model_payload(self, base_name: str, prompt: str) -> dict[str, Any]:
        name = snake_case(base_name or "generated_model")
        return {
            "schemaVersion": 2,
            "documentType": "cube-model-studio",
            "name": name,
            "namespace": "extremecraft",
            "modelType": "block",
            "textureSize": [64, 64],
            "metadata": {"generatedBy": self.name, "prompt": prompt},
            "bones": [{"id": "root", "pivot": [0, 0, 0], "rotation": [0, 0, 0], "cubes": ["body"], "children": [], "parent": None, "metadata": {}}],
            "cubes": [{"id": "body", "from": [0, 0, 0], "to": [16, 16, 16], "pivot": [0, 0, 0], "rotation": [0, 0, 0], "inflate": 0, "texture": "textures/block/default.png", "mirror": False, "faces": {}, "metadata": {}}],
        }


class AIWorkbenchService:
    def __init__(self, providers: list[AIProvider] | None = None) -> None:
        self.providers = providers or [MockAIProvider()]
        self.history: list[dict[str, Any]] = []

    def provider_names(self) -> list[str]:
        return [provider.name for provider in self.providers]

    def provider(self, name: str | None = None) -> AIProvider:
        if not name:
            return self.providers[0]
        for provider in self.providers:
            if provider.name == name:
                return provider
        return self.providers[0]

    def generate_artifact(self, request: AIRequest, *, provider_name: str | None = None) -> AIArtifact:
        provider = self.provider(provider_name)
        normalized_mode = request.mode.strip().lower()
        result = provider.generate(request)
        diff_preview = self._build_diff(request.current_text, result.content) if normalized_mode in {"apply to current file", "generate diff preview"} else ""
        validation_messages = self._validate_result(result)
        preview_summary = self._build_preview_summary(result)
        artifact = AIArtifact(
            artifact_id=uuid4().hex,
            mode=request.mode,
            prompt=request.prompt,
            provider_name=provider.name,
            target_kind=result.target_kind,
            title=result.title,
            candidate_content=result.content,
            candidate_payload=result.payload,
            diff_preview=diff_preview,
            validation_messages=validation_messages,
            preview_summary=preview_summary,
            source_path=request.current_path,
            apply_kind=self._apply_kind_for(request.mode, result.target_kind),
        )
        self.history.insert(0, {"mode": request.mode, "prompt": request.prompt, "provider": provider.name, "title": result.title})
        self.history = self.history[:20]
        return artifact

    def _build_diff(self, before: str, after: str) -> str:
        before_lines = before.splitlines()
        after_lines = after.splitlines()
        return "\n".join(difflib.unified_diff(before_lines, after_lines, fromfile="before", tofile="after", lineterm=""))

    def _validate_result(self, result: ProviderResult) -> list[str]:
        messages: list[str] = []
        if result.target_kind in {"gui", "model", "json"}:
            try:
                payload = result.payload or json.loads(result.content)
            except json.JSONDecodeError as exc:
                return [f"JSON decode failed: {exc}"]
            if isinstance(payload, dict):
                if "schemaVersion" in payload:
                    messages.append(f"schemaVersion={payload.get('schemaVersion')}")
                if "documentType" in payload:
                    messages.append(f"documentType={payload.get('documentType')}")
        elif result.target_kind == "java":
            if "class " not in result.content and "interface " not in result.content and "enum " not in result.content and "record " not in result.content:
                messages.append("Java draft does not contain a top-level type declaration")
            else:
                messages.append("Java draft contains a top-level type declaration")
        if not messages:
            messages.append("Artifact generated")
        return messages

    def _build_preview_summary(self, result: ProviderResult) -> dict[str, Any]:
        preview = {"title": result.title, "targetKind": result.target_kind, "bytes": len(result.content.encode('utf-8'))}
        if result.payload and isinstance(result.payload, dict):
            preview["keys"] = sorted(result.payload.keys())
            if "widgets" in result.payload:
                preview["widgetCount"] = len(result.payload.get("widgets") or [])
            if "cubes" in result.payload:
                preview["cubeCount"] = len(result.payload.get("cubes") or [])
        return preview

    def _apply_kind_for(self, mode: str, target_kind: str) -> str:
        mode = mode.strip().lower()
        if mode in {"generate gui draft", "generate model draft", "generate java class draft", "generate into new file", "convert preview to runtime definition"}:
            return "open-draft"
        if mode in {"apply to current file", "generate diff preview"}:
            return "replace-current"
        return "preview"


def _extract_name(prompt: str, current_path: Path | None) -> str:
    quoted = re.findall(r"[A-Za-z_][A-Za-z0-9_]*", prompt)
    if quoted:
        return quoted[-1]
    if current_path is not None:
        return current_path.stem
    return "generated"


def _kind_from_path(path: Path | None) -> str:
    if path is None:
        return "text"
    if path.suffix == ".java":
        return "java"
    if path.suffix == ".json":
        return "json"
    return "text"


def _package_from_path(path: Path | None) -> str:
    if path is None:
        return ""
    parts = list(path.parts)
    marker = ["src", "main", "java"]
    for index in range(len(parts) - len(marker) + 1):
        if parts[index:index + len(marker)] == marker:
            return ".".join(parts[index + len(marker):-1])
    return ""


