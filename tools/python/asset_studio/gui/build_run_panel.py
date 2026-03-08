from __future__ import annotations

from PyQt6.QtCore import QTimer
from PyQt6.QtWidgets import QGridLayout, QHBoxLayout, QLabel, QPushButton, QPlainTextEdit, QVBoxLayout, QWidget


class BuildRunPanel(QWidget):
    def __init__(self, session, callbacks: dict[str, object]) -> None:
        super().__init__()
        self.session = session
        self.callbacks = callbacks

        root = QVBoxLayout(self)
        root.addWidget(QLabel("Build, validate, export, and run workspace tasks through studio services."))

        actions = QGridLayout()
        buttons = [
            ("Validate", "validate_assets"),
            ("Build Assets", "compile_assets"),
            ("Compile Expansion", "compile_expansion"),
            ("Run Client", "run_client"),
            ("Run Server", "run_server"),
            ("Build Release", "release_build"),
            ("Build Modpack", "modpack_build"),
            ("Export Datapack", "export_datapack"),
            ("Export Resourcepack", "export_resourcepack"),
            ("Latest Log", "latest_log"),
            ("Clear Logs", "clear_logs"),
        ]
        for index, (title, key) in enumerate(buttons):
            button = QPushButton(title)
            callback = self.callbacks.get(key)
            if callable(callback):
                button.clicked.connect(callback)
            actions.addWidget(button, index // 3, index % 3)
        root.addLayout(actions)

        info_row = QHBoxLayout()
        self.run_configs = QLabel("Run configs: none")
        self.log_path = QLabel("Latest log: unavailable")
        info_row.addWidget(self.run_configs)
        info_row.addWidget(self.log_path)
        root.addLayout(info_row)

        self.task_status = QPlainTextEdit()
        self.task_status.setReadOnly(True)
        root.addWidget(self.task_status)

        self._timer = QTimer(self)
        self._timer.setInterval(800)
        self._timer.timeout.connect(self.refresh)
        self._timer.start()
        self.refresh()

    def refresh(self) -> None:
        configurations = self.session.run_service.list_configurations()
        self.run_configs.setText("Run configs: " + (", ".join(config.name for config in configurations) if configurations else "none"))
        latest = self.session.latest_log_path()
        self.log_path.setText(f"Latest log: {latest}" if latest else "Latest log: unavailable")

        lines: list[str] = []
        for entry in self.session.log_model.tail(20):
            lines.append(f"[{entry.level}] {entry.source}: {entry.message}")
        notifications = self.session.notification_service.recent(10)
        if notifications:
            lines.append("")
            lines.append("Notifications")
            for notification in notifications:
                lines.append(f"[{notification.severity}] {notification.source}: {notification.message}")
        self.task_status.setPlainText("\n".join(lines) if lines else "No task output yet.")
