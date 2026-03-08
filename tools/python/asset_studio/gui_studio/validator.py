from __future__ import annotations

from dataclasses import dataclass, field

from asset_studio.gui_studio.models import GuiDocument

_ALLOWED_WIDGETS = {
    "panel",
    "label",
    "button",
    "image",
    "progress",
    "inventory_slot",
    "machine_slot",
    "player_inventory_grid",
    "hotbar",
    "armor_slots",
    "offhand_slot",
}
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

        seen_slot_ids: set[str] = set()
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
            if widget.widget_type == "progress" and float(widget.properties.get("max", 0) or 0) <= 0:
                issues.append(GuiValidationIssue("error", "progress-range", widget.id, "Progress widget max must be greater than 0"))

            if widget.widget_type in {"inventory_slot", "machine_slot", "player_inventory_grid", "hotbar", "armor_slots", "offhand_slot"}:
                if widget.binding is None or not widget.binding.kind:
                    issues.append(GuiValidationIssue("error", "binding", widget.id, "Inventory widget is missing binding metadata"))
                elif widget.binding.slot_id:
                    if widget.binding.slot_id in seen_slot_ids:
                        issues.append(GuiValidationIssue("error", "slot-id", widget.id, f"Duplicate slot binding id: {widget.binding.slot_id}"))
                    seen_slot_ids.add(widget.binding.slot_id)

            if widget.widget_type == "player_inventory_grid":
                rows = int(widget.properties.get("rows", widget.binding.rows if widget.binding else 0) or 0)
                columns = int(widget.properties.get("columns", widget.binding.columns if widget.binding else 0) or 0)
                if rows <= 0 or columns <= 0:
                    issues.append(GuiValidationIssue("error", "inventory-grid", widget.id, "Player inventory grid requires positive rows and columns"))
            if widget.widget_type == "hotbar" and int(widget.properties.get("columns", 9) or 0) != 9:
                issues.append(GuiValidationIssue("warning", "hotbar-columns", widget.id, "Hotbar should use exactly 9 columns"))
            if widget.widget_type == "armor_slots" and int(widget.properties.get("count", 4) or 0) != 4:
                issues.append(GuiValidationIssue("warning", "armor-count", widget.id, "Armor slots should expose 4 equipment bindings"))

        for root_id in document.root_widgets:
            if root_id not in document.widgets:
                issues.append(GuiValidationIssue("error", "root-reference", root_id, f"Root widget does not exist: {root_id}"))

        return GuiValidationReport(issues=issues)
