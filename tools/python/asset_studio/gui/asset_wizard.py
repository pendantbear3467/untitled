from __future__ import annotations

from dataclasses import dataclass

from PyQt6.QtCore import pyqtSignal
from PyQt6.QtWidgets import (
    QComboBox,
    QFormLayout,
    QGroupBox,
    QHBoxLayout,
    QLabel,
    QLineEdit,
    QPushButton,
    QSpinBox,
    QVBoxLayout,
    QWidget,
)


@dataclass
class ToolWizardInput:
    tool_name: str
    material: str
    durability: int
    attack_damage: int
    mining_speed: int
    tier: int
    texture_style: str


class AssetWizardPanel(QWidget):
    generate_tool_requested = pyqtSignal(object)

    def __init__(self) -> None:
        super().__init__()
        root = QVBoxLayout(self)
        root.addWidget(QLabel("Asset Wizard"))

        tool_box = QGroupBox("Tool Wizard")
        form = QFormLayout(tool_box)

        self.tool_name = QLineEdit("mythril_pickaxe")
        self.material = QLineEdit("mythril")

        self.durability = QSpinBox()
        self.durability.setRange(1, 10000)
        self.durability.setValue(1800)

        self.attack_damage = QSpinBox()
        self.attack_damage.setRange(1, 50)
        self.attack_damage.setValue(7)

        self.mining_speed = QSpinBox()
        self.mining_speed.setRange(1, 50)
        self.mining_speed.setValue(9)

        self.tier = QSpinBox()
        self.tier.setRange(1, 10)
        self.tier.setValue(4)

        self.texture_style = QComboBox()
        self.texture_style.addItems(["metallic", "crystal", "ancient", "industrial", "arcane", "void", "quantum"])

        form.addRow("tool_name", self.tool_name)
        form.addRow("material", self.material)
        form.addRow("durability", self.durability)
        form.addRow("attack_damage", self.attack_damage)
        form.addRow("mining_speed", self.mining_speed)
        form.addRow("tier", self.tier)
        form.addRow("texture_style", self.texture_style)

        actions = QHBoxLayout()
        generate_btn = QPushButton("Generate Tool Bundle")
        generate_btn.clicked.connect(self._emit_tool_request)
        actions.addWidget(generate_btn)
        actions.addStretch(1)

        root.addWidget(tool_box)
        root.addLayout(actions)
        root.addStretch(1)

    def _emit_tool_request(self) -> None:
        payload = ToolWizardInput(
            tool_name=self.tool_name.text().strip(),
            material=self.material.text().strip(),
            durability=self.durability.value(),
            attack_damage=self.attack_damage.value(),
            mining_speed=self.mining_speed.value(),
            tier=self.tier.value(),
            texture_style=self.texture_style.currentText(),
        )
        self.generate_tool_requested.emit(payload)
