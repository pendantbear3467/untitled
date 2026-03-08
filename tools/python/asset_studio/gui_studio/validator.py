from __future__ import annotations

from dataclasses import dataclass, field

from asset_studio.gui_studio.models import GuiDocument

_ALLOWED_WIDGETS = {"panel", "label", "button", "slot", "image", "progress", "text_input"}
_ALLOWED_SCREENS = {"generic", "machine", "overlay", "menu", "player"}


@dataclass(frozen=True)
class GuiValidationIssue:
    severity: str
    code: str
    widget_id: str | None
    message: str


@dataclass
class GuiValidationReport:
    issues: list[GuiValidationIssue] = field(default_factory=list)

    @property
    def errors(self) -> list[GuiValidationIssue]:
        return [issue for issue in self.issues if issue.severity == "error"]

    @property
    def warnings(self) -> list[GuiValidationIssue]:
        return [issue for issue in self.issues if issue.severity == "warning"]


class GuiDocumentValidator:
    def validate(self, document: GuiDocument) -> GuiValidationReport:
        issues: list[GuiValidationIssue] = []
        if document.screen_type not in _ALLOWED_SCREENS:
            issues.append(GuiValidationIssue("error", "screen-type", None, f"Unsupported screen type: {document.screen_type}"))
        if document.width <= 0 or document.height <= 0:
            issues.append(GuiValidationIssue("error", "screen-size", None, "Screen width and height must be positive"))

        for widget in document.widgets.values():
            if widget.widget_type not in _ALLOWED_WIDGETS:
                issues.append(GuiValidationIssue("error", "widget-type", widget.id, f"Unsupported widget type: {widget.widget_type}"))
            if widget.bounds.width <= 0 or widget.bounds.height <= 0:
                issues.append(GuiValidationIssue("error", "bounds", widget.id, "Widget width and height must be positive"))
            if widget.bounds.x < 0 or widget.bounds.y < 0:
                issues.append(GuiValidationIssue("warning", "bounds", widget.id, "Widget starts outside the top-left canvas quadrant"))
            for child_id in widget.children:
                if child_id not in document.widgets:
                    issues.append(GuiValidationIssue("error", "child-reference", widget.id, f"Missing child widget: {child_id}"))
            if widget.widget_type == "image" and not widget.properties.get("texture"):
                issues.append(GuiValidationIssue("warning", "texture", widget.id, "Image widget has no texture property"))
            if widget.widget_type == "progress" and "max" in widget.properties and float(widget.properties.get("max", 0)) <= 0:
                issues.append(GuiValidationIssue("error", "progress-range", widget.id, "Progress widget max must be greater than 0"))

        for root_id in document.root_widgets:
            if root_id not in document.widgets:
                issues.append(GuiValidationIssue("error", "root-reference", root_id, f"Root widget does not exist: {root_id}"))

        return GuiValidationReport(issues=issues)
