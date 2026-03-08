from __future__ import annotations

import json
import os
from dataclasses import asdict, dataclass, field
from pathlib import Path

from asset_studio.core.process_service import ManagedProcess, ProcessService
from asset_studio.runtime.log_model import LogStreamModel
from asset_studio.runtime.task_results import ProcessTaskResult, utc_now
from asset_studio.workspace.workspace_manager import AssetStudioContext


@dataclass
class RunConfiguration:
    name: str
    command: list[str]
    working_directory: str | None = None
    environment: dict[str, str] = field(default_factory=dict)
    description: str = ""
    kind: str = "custom"


class RunService:
    def __init__(self, context: AssetStudioContext, process_service: ProcessService, *, log_model: LogStreamModel | None = None) -> None:
        self.context = context
        self.process_service = process_service
        self.log_model = log_model or LogStreamModel()
        self.config_path = self.context.workspace_root / ".studio" / "run_configurations.json"
        self.config_path.parent.mkdir(parents=True, exist_ok=True)

    def list_configurations(self) -> list[RunConfiguration]:
        if not self.config_path.exists():
            return []
        payload = json.loads(self.config_path.read_text(encoding="utf-8"))
        return [RunConfiguration(**entry) for entry in payload.get("configurations", [])]

    def save_configuration(self, configuration: RunConfiguration) -> RunConfiguration:
        configurations = {item.name: item for item in self.list_configurations()}
        configurations[configuration.name] = configuration
        payload = {"updatedAt": utc_now(), "configurations": [asdict(item) for item in configurations.values()]}
        self.config_path.write_text(json.dumps(payload, indent=2) + "\n", encoding="utf-8")
        return configuration

    def run_named(self, name: str) -> ManagedProcess | ProcessTaskResult:
        for configuration in self.list_configurations():
            if configuration.name == name:
                return self.run_configuration(configuration)
        return ProcessTaskResult(
            task_id=f"run-{name}",
            name=f"Run {name}",
            success=False,
            started_at=utc_now(),
            finished_at=utc_now(),
            message=f"Run configuration not found: {name}",
            errors=[name],
            command=[],
        )

    def run_client(self) -> ManagedProcess | ProcessTaskResult:
        return self.run_named("client")

    def run_server(self) -> ManagedProcess | ProcessTaskResult:
        return self.run_named("server")

    def run_configuration(self, configuration: RunConfiguration) -> ManagedProcess:
        cwd = Path(configuration.working_directory) if configuration.working_directory else self.context.repo_root
        environment = dict(os.environ)
        environment.update(configuration.environment)
        return self.process_service.start(
            configuration.name,
            configuration.command,
            cwd=cwd,
            env=environment,
            log_model=self.log_model,
        )
