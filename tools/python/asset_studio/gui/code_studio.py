from __future__ import annotations

import ast
import json
import re
from dataclasses import dataclass
from pathlib import Path
from typing import Iterable

from PyQt6.QtCore import QRect, QSize, Qt, pyqtSignal
from PyQt6.QtGui import QColor, QFont, QPainter, QTextCharFormat, QTextCursor, QTextDocument, QTextFormat, QSyntaxHighlighter
from PyQt6.QtWidgets import (
    QCheckBox,
    QFileDialog,
    QFrame,
    QHBoxLayout,
    QLabel,
    QLineEdit,
    QListWidget,
    QListWidgetItem,
    QMessageBox,
    QPushButton,
    QPlainTextEdit,
    QSplitter,
    QStackedWidget,
    QTabWidget,
    QVBoxLayout,
    QWidget,
)

from asset_studio.code.editor_service import EditorService


LANGUAGE_BY_SUFFIX = {
    ".py": "python",
    ".json": "json",
    ".java": "java",
    ".yml": "yaml",
    ".yaml": "yaml",
    ".toml": "toml",
    ".md": "markdown",
}


@dataclass
class ProblemEntry:
    severity: str
    message: str
    line: int


class _LanguageHighlighter(QSyntaxHighlighter):
    def __init__(self, document) -> None:
        super().__init__(document)
        self.language = "text"
        self._styles = self._build_styles()

    def set_language(self, language: str) -> None:
        self.language = language
        self.rehighlight()

    def highlightBlock(self, text: str) -> None:  # noqa: N802
        self._apply_numbers(text)
        self._apply_strings(text)
        self._apply_comments(text)
        self._apply_keywords(text)

    def _build_styles(self) -> dict[str, QTextCharFormat]:
        keyword = QTextCharFormat()
        keyword.setForeground(QColor("#66d9ef"))
        keyword.setFontWeight(QFont.Weight.Bold)

        number = QTextCharFormat()
        number.setForeground(QColor("#ae81ff"))

        string = QTextCharFormat()
        string.setForeground(QColor("#a6e22e"))

        comment = QTextCharFormat()
        comment.setForeground(QColor("#7f8c98"))
        comment.setFontItalic(True)

        return {
            "keyword": keyword,
            "number": number,
            "string": string,
            "comment": comment,
        }

    def _apply_numbers(self, text: str) -> None:
        for match in re.finditer(r"\b\d+(?:\.\d+)?\b", text):
            self.setFormat(match.start(), match.end() - match.start(), self._styles["number"])

    def _apply_strings(self, text: str) -> None:
        for match in re.finditer(r"(['\"]).*?\1", text):
            self.setFormat(match.start(), match.end() - match.start(), self._styles["string"])

    def _apply_comments(self, text: str) -> None:
        if self.language in {"python", "yaml", "toml"}:
            mark = "#"
        elif self.language in {"java", "json"}:
            mark = "//"
        else:
            mark = ""
        if not mark:
            return
        idx = text.find(mark)
        if idx >= 0:
            self.setFormat(idx, len(text) - idx, self._styles["comment"])

    def _apply_keywords(self, text: str) -> None:
        keywords: dict[str, Iterable[str]] = {
            "python": ("def", "class", "import", "from", "for", "while", "if", "elif", "else", "return", "try", "except", "with", "as", "pass", "raise"),
            "json": ("true", "false", "null"),
            "java": ("class", "public", "private", "protected", "static", "void", "if", "else", "for", "while", "return", "new", "import", "package"),
            "yaml": ("true", "false", "null"),
            "toml": ("true", "false"),
            "markdown": ("#", "##", "###"),
        }
        for token in keywords.get(self.language, ()):  # pragma: no branch
            for match in re.finditer(rf"\b{re.escape(token)}\b", text):
                self.setFormat(match.start(), match.end() - match.start(), self._styles["keyword"])


class _LineNumberArea(QWidget):
    def __init__(self, editor: "StudioCodeEditor") -> None:
        super().__init__(editor)
        self.editor = editor

    def sizeHint(self) -> QSize:  # noqa: N802
        return QSize(self.editor.line_number_area_width(), 0)

    def paintEvent(self, event) -> None:  # noqa: N802
        self.editor.line_number_area_paint_event(event)


class StudioCodeEditor(QPlainTextEdit):
    cursor_moved = pyqtSignal(int, int)

    def __init__(self) -> None:
        super().__init__()
        self._line_number_area = _LineNumberArea(self)
        self._highlighter = _LanguageHighlighter(self.document())
        self.setLineWrapMode(QPlainTextEdit.LineWrapMode.NoWrap)
        self.setTabStopDistance(4 * self.fontMetrics().horizontalAdvance(" "))

        self.blockCountChanged.connect(self._update_line_number_width)
        self.updateRequest.connect(self._update_line_number_area)
        self.cursorPositionChanged.connect(self._highlight_current_line)
        self.cursorPositionChanged.connect(self._emit_cursor)

        self._update_line_number_width(0)
        self._highlight_current_line()

    def set_language(self, language: str) -> None:
        self._highlighter.set_language(language)

    def line_number_area_width(self) -> int:
        digits = len(str(max(1, self.blockCount())))
        return 12 + self.fontMetrics().horizontalAdvance("9") * digits

    def resizeEvent(self, event) -> None:  # noqa: N802
        super().resizeEvent(event)
        rect = self.contentsRect()
        self._line_number_area.setGeometry(QRect(rect.left(), rect.top(), self.line_number_area_width(), rect.height()))

    def line_number_area_paint_event(self, event) -> None:
        painter = QPainter(self._line_number_area)
        painter.fillRect(event.rect(), QColor("#1a1f2b"))

        block = self.firstVisibleBlock()
        block_number = block.blockNumber()
        top = int(self.blockBoundingGeometry(block).translated(self.contentOffset()).top())
        bottom = top + int(self.blockBoundingRect(block).height())
        while block.isValid() and top <= event.rect().bottom():
            if block.isVisible() and bottom >= event.rect().top():
                painter.setPen(QColor("#7f8c98"))
                painter.drawText(0, top, self._line_number_area.width() - 6, self.fontMetrics().height(), Qt.AlignmentFlag.AlignRight, str(block_number + 1))
            block = block.next()
            top = bottom
            bottom = top + int(self.blockBoundingRect(block).height())
            block_number += 1

    def _update_line_number_width(self, _) -> None:
        self.setViewportMargins(self.line_number_area_width(), 0, 0, 0)

    def _update_line_number_area(self, rect, dy: int) -> None:
        if dy:
            self._line_number_area.scroll(0, dy)
        else:
            self._line_number_area.update(0, rect.y(), self._line_number_area.width(), rect.height())

    def _highlight_current_line(self) -> None:
        if self.isReadOnly():
            return
        selection = QPlainTextEdit.ExtraSelection()
        selection.format.setBackground(QColor("#252c3a"))
        selection.format.setProperty(QTextFormat.Property.FullWidthSelection, True)
        selection.cursor = self.textCursor()
        selection.cursor.clearSelection()
        self.setExtraSelections([selection])

    def _emit_cursor(self) -> None:
        cursor = self.textCursor()
        self.cursor_moved.emit(cursor.blockNumber() + 1, cursor.columnNumber() + 1)


@dataclass
class _CodeTab:
    document_id: str
    path: Path | None
    editor: StudioCodeEditor
    dirty: bool = False

    @property
    def display_name(self) -> str:
        return self.path.name if self.path is not None else "untitled"


class CodeStudioPanel(QWidget):
    status_message = pyqtSignal(str)
    notifications = pyqtSignal(str)

    def __init__(self, editor_service: EditorService, workspace_root: Path) -> None:
        super().__init__()
        self.editor_service = editor_service
        self.workspace_root = workspace_root
        self._tabs: dict[StudioCodeEditor, _CodeTab] = {}

        root = QVBoxLayout(self)
        root.setContentsMargins(0, 0, 0, 0)
        root.setSpacing(8)
        root.addWidget(self._build_header())
        root.addWidget(self._build_toolbar_host())
        root.addWidget(self._build_path_bar())

        self.stack = QStackedWidget()
        self.empty_state = self._build_empty_state()
        self.workspace_surface = self._build_workspace_surface()
        self.stack.addWidget(self.empty_state)
        self.stack.addWidget(self.workspace_surface)
        root.addWidget(self.stack)

        self._refresh_recent_files()
        self._refresh_tab_details()

    def _build_header(self) -> QWidget:
        header = QFrame()
        row = QHBoxLayout(header)
        row.setContentsMargins(10, 8, 10, 8)
        title = QLabel("Code Studio")
        title.setObjectName("panelHeaderTitle")
        title.setToolTip("Full-page text editing backed by EditorService, recovery snapshots, and diagnostics.")
        helper = QLabel("Open files from Project Browser, use the search panel for in-file results, and keep problems visible while editing.")
        helper.setWordWrap(True)
        helper.setObjectName("panelHelpHint")
        row.addWidget(title)
        row.addSpacing(12)
        row.addWidget(helper, 1)
        return header

    def _build_toolbar_host(self) -> QWidget:
        host = QWidget()
        row = QHBoxLayout(host)
        row.setContentsMargins(10, 0, 10, 0)
        row.setSpacing(8)

        file_group = self._toolbar_group("Files")
        for button in [
            self._tool_button("New", self.new_document, "Create a new untitled buffer."),
            self._tool_button("Open", self.open_file_dialog, "Open a file into the tabbed editor."),
            self._tool_button("Save", self.save_current, "Save the active document."),
            self._tool_button("Save All", self.save_all, "Save every open document."),
        ]:
            file_group.layout().addWidget(button)
        row.addWidget(file_group)

        search_group = self._toolbar_group("Search / Replace")
        self.find_input = QLineEdit()
        self.find_input.setPlaceholderText("Find in current file")
        self.find_input.setToolTip("Search within the active file. Results update as you type.")
        self.find_input.textChanged.connect(self._refresh_search_results)
        self.replace_input = QLineEdit()
        self.replace_input.setPlaceholderText("Replace with")
        self.replace_input.setToolTip("Replacement text for Replace and Replace All.")
        self.case_toggle = QCheckBox("Match Case")
        self.case_toggle.setToolTip("When enabled, search and replace become case sensitive.")
        self.case_toggle.toggled.connect(self._refresh_search_results)
        for widget in [
            self.find_input,
            self.replace_input,
            self.case_toggle,
            self._tool_button("Prev", lambda: self.find_next(True), "Jump to the previous match."),
            self._tool_button("Next", lambda: self.find_next(False), "Jump to the next match."),
            self._tool_button("Replace", self.replace_one, "Replace the current selection."),
            self._tool_button("Replace All", self.replace_all, "Replace all matches in the active file."),
        ]:
            search_group.layout().addWidget(widget)
        row.addWidget(search_group, 1)

        session_group = self._toolbar_group("Session")
        self.open_count_label = QLabel("0 open files")
        self.recovery_label = QLabel("Recovery snapshots: 0")
        session_group.layout().addWidget(self.open_count_label)
        session_group.layout().addWidget(self.recovery_label)
        row.addWidget(session_group)
        return host

    def _build_path_bar(self) -> QWidget:
        host = QWidget()
        row = QHBoxLayout(host)
        row.setContentsMargins(10, 0, 10, 0)
        row.addWidget(QLabel("Path"))
        self.path_label = QLabel("No file open")
        self.path_label.setWordWrap(True)
        self.path_label.setToolTip("Absolute path to the current file, or a note when the buffer is not saved yet.")
        row.addWidget(self.path_label, 1)
        return host

    def _build_empty_state(self) -> QWidget:
        page = QWidget()
        layout = QVBoxLayout(page)
        layout.setContentsMargins(24, 24, 24, 24)
        title = QLabel("No file is open yet")
        title.setObjectName("panelHeaderTitle")
        text = QLabel(
            "Open a file from Project Browser, use Open to choose a file, or create a new untitled buffer.\n"
            "Recent files stay available here so the page is still useful when nothing is open."
        )
        text.setWordWrap(True)
        layout.addWidget(title)
        layout.addWidget(text)
        actions = QHBoxLayout()
        actions.addWidget(self._tool_button("New Document", self.new_document, "Create an untitled buffer."))
        actions.addWidget(self._tool_button("Open File", self.open_file_dialog, "Open an existing file from disk."))
        actions.addStretch(1)
        layout.addLayout(actions)
        layout.addWidget(QLabel("Recent Files"))
        self.empty_recent = QListWidget()
        self.empty_recent.setToolTip("Recent files tracked by EditorService. Double-click to reopen.")
        self.empty_recent.itemDoubleClicked.connect(self._open_recent_item)
        layout.addWidget(self.empty_recent)
        return page

    def _build_workspace_surface(self) -> QWidget:
        split = QSplitter(Qt.Orientation.Horizontal)
        self.tab_widget = QTabWidget()
        self.tab_widget.setDocumentMode(True)
        self.tab_widget.setTabsClosable(True)
        self.tab_widget.tabCloseRequested.connect(self.close_tab)
        self.tab_widget.currentChanged.connect(self._active_tab_changed)
        split.addWidget(self.tab_widget)

        side = QTabWidget()
        self.file_meta = QLabel("No file open")
        self.file_meta.setWordWrap(True)
        meta_page = QWidget()
        meta_layout = QVBoxLayout(meta_page)
        meta_layout.addWidget(self.file_meta)
        meta_layout.addStretch(1)
        side.addTab(meta_page, "Info")

        self.outline = QListWidget()
        self.outline.setToolTip("Outline generated from the active file. Double-click to jump.")
        self.outline.itemDoubleClicked.connect(self._jump_to_line_from_item)
        side.addTab(self.outline, "Outline")

        self.problems = QListWidget()
        self.problems.setToolTip("Diagnostics for the active file. Double-click to jump.")
        self.problems.itemDoubleClicked.connect(self._jump_to_line_from_item)
        side.addTab(self.problems, "Problems")

        self.search_results = QListWidget()
        self.search_results.setToolTip("Search results for the active file. Double-click to jump.")
        self.search_results.itemDoubleClicked.connect(self._jump_to_line_from_item)
        side.addTab(self.search_results, "Search")

        self.recent_files = QListWidget()
        self.recent_files.setToolTip("Recent files tracked by EditorService. Double-click to reopen.")
        self.recent_files.itemDoubleClicked.connect(self._open_recent_item)
        side.addTab(self.recent_files, "Recent")

        split.addWidget(side)
        split.setSizes([1040, 360])
        return split

    def _toolbar_group(self, title: str) -> QFrame:
        frame = QFrame()
        layout = QHBoxLayout(frame)
        layout.setContentsMargins(8, 6, 8, 6)
        layout.setSpacing(6)
        label = QLabel(f"{title}:")
        label.setObjectName("panelHelpHint")
        layout.addWidget(label)
        return frame

    def _tool_button(self, label: str, callback, help_text: str) -> QPushButton:
        button = QPushButton(label)
        button.setToolTip(help_text)
        button.setStatusTip(help_text)
        button.clicked.connect(callback)
        return button

    def set_workspace_root(self, workspace_root: Path) -> None:
        self.workspace_root = workspace_root
        self._refresh_recent_files()

    def new_document(self) -> None:
        document = self.editor_service.new_document(path=None, content="")
        editor = StudioCodeEditor()
        editor.setPlainText(document.content)
        editor.set_language(document.syntax_mode)
        editor.cursor_moved.connect(self._cursor_moved)
        editor.textChanged.connect(lambda current_editor=editor: self._mark_dirty(current_editor))
        self._tabs[editor] = _CodeTab(document_id=document.document_id, path=document.path, editor=editor, dirty=document.dirty)
        index = self.tab_widget.addTab(editor, "untitled")
        self.tab_widget.setCurrentIndex(index)
        self._refresh_tab_details()
        self.notifications.emit("Created new untitled document")
        self.status_message.emit("New document ready")

    def open_file_dialog(self) -> None:
        selected, _ = QFileDialog.getOpenFileName(self, "Open file", str(self.workspace_root))
        if not selected:
            self.notifications.emit("Open cancelled")
            return
        self.open_file(Path(selected))

    def open_file(self, path: Path) -> None:
        if path.is_dir() or not path.exists():
            self.notifications.emit(f"Cannot open {path.name}: file does not exist")
            return
        existing_index = self._find_tab_index_by_path(path)
        if existing_index is not None:
            self.tab_widget.setCurrentIndex(existing_index)
            self.status_message.emit(f"Focused: {path}")
            return

        document = self.editor_service.open_document(path)
        editor = StudioCodeEditor()
        editor.setPlainText(document.content)
        editor.set_language(document.syntax_mode)
        editor.cursor_moved.connect(self._cursor_moved)
        editor.textChanged.connect(lambda current_editor=editor: self._mark_dirty(current_editor))
        self._tabs[editor] = _CodeTab(document_id=document.document_id, path=path, editor=editor, dirty=document.dirty)
        index = self.tab_widget.addTab(editor, path.name)
        self.tab_widget.setCurrentIndex(index)
        self._refresh_recent_files()
        self._refresh_tab_details()
        self.notifications.emit(f"Opened {path.name}")
        self.status_message.emit(f"Opened: {path}")

    def save_current(self) -> bool:
        tab = self._current_tab()
        if tab is None:
            self.notifications.emit("No active document to save")
            return True
        try:
            self.editor_service.set_document_text(tab.document_id, tab.editor.toPlainText())
            target_path = tab.path
            if target_path is None:
                selected, _ = QFileDialog.getSaveFileName(self, "Save file", str(self.workspace_root / "untitled.txt"))
                if not selected:
                    self.notifications.emit("Save cancelled for untitled document")
                    return False
                target_path = Path(selected)
            saved_path = self.editor_service.save_document(tab.document_id, target_path)
        except Exception as exc:  # noqa: BLE001
            QMessageBox.critical(self, "Save Failed", f"Could not save file.\n\n{exc}")
            self.notifications.emit(f"ERROR: failed to save {tab.display_name}: {exc}")
            return False

        tab.path = saved_path
        tab.dirty = False
        tab.editor.set_language(LANGUAGE_BY_SUFFIX.get(saved_path.suffix.lower(), "text"))
        self._update_tab_title(tab.editor)
        self._refresh_recent_files()
        self._refresh_tab_details()
        self.status_message.emit(f"Saved: {saved_path}")
        self.notifications.emit(f"Saved {saved_path.name}")
        return True

    def save_all(self) -> int:
        count = 0
        for index in range(self.tab_widget.count()):
            self.tab_widget.setCurrentIndex(index)
            if self.save_current():
                count += 1
        if count:
            self.notifications.emit(f"Saved {count} open documents")
        return count

    def autosave_dirty(self) -> int:
        snapshots = self.workspace_root / ".studio" / "autosave"
        if not snapshots.exists():
            return 0
        return len(list(snapshots.glob("text-document-*.json")))

    def has_unsaved(self) -> bool:
        return any(tab.dirty for tab in self._tabs.values())

    def close_tab(self, index: int) -> None:
        editor = self.tab_widget.widget(index)
        if not isinstance(editor, StudioCodeEditor):
            return
        tab = self._tabs.get(editor)
        if tab is None:
            return
        self.tab_widget.setCurrentIndex(index)
        if tab.dirty:
            result = QMessageBox.question(
                self,
                "Unsaved Changes",
                f"Save changes to {tab.display_name} before closing?",
                QMessageBox.StandardButton.Yes | QMessageBox.StandardButton.No | QMessageBox.StandardButton.Cancel,
            )
            if result == QMessageBox.StandardButton.Cancel:
                return
            if result == QMessageBox.StandardButton.Yes and not self.save_current():
                return
        self.editor_service.close_document(tab.document_id)
        self._tabs.pop(editor, None)
        self.tab_widget.removeTab(index)
        self._refresh_tab_details()
        self.notifications.emit(f"Closed {tab.display_name}")

    def find_next(self, backward: bool) -> None:
        tab = self._current_tab()
        if tab is None:
            self.notifications.emit("Open a file before searching")
            return
        query = self.find_input.text().strip()
        if not query:
            self.notifications.emit("Enter search text first")
            return
        flags = QTextDocument.FindFlag.FindBackward if backward else QTextDocument.FindFlag(0)
        if self.case_toggle.isChecked():
            flags |= QTextDocument.FindFlag.FindCaseSensitively
        found = tab.editor.find(query, flags)
        if not found:
            cursor = tab.editor.textCursor()
            cursor.movePosition(QTextCursor.MoveOperation.End if backward else QTextCursor.MoveOperation.Start)
            tab.editor.setTextCursor(cursor)
            tab.editor.find(query, flags)

    def replace_one(self) -> None:
        tab = self._current_tab()
        if tab is None:
            self.notifications.emit("Open a file before replacing text")
            return
        if not self.find_input.text().strip():
            self.notifications.emit("Enter search text before replacing")
            return
        cursor = tab.editor.textCursor()
        if cursor.hasSelection():
            cursor.insertText(self.replace_input.text())
            self._mark_dirty(tab.editor)
        self.find_next(False)

    def replace_all(self) -> None:
        tab = self._current_tab()
        if tab is None:
            self.notifications.emit("Open a file before replacing text")
            return
        query = self.find_input.text().strip()
        if not query:
            self.notifications.emit("Enter search text before replacing")
            return
        replaced = self.editor_service.replace_all(tab.document_id, query, self.replace_input.text(), case_sensitive=self.case_toggle.isChecked())
        document = self.editor_service.documents[tab.document_id]
        tab.editor.blockSignals(True)
        tab.editor.setPlainText(document.content)
        tab.editor.blockSignals(False)
        tab.dirty = document.dirty
        self._update_tab_title(tab.editor)
        self._refresh_tab_details()
        self.notifications.emit(f"Replaced {replaced} matches in {tab.display_name}")

    def _mark_dirty(self, editor: StudioCodeEditor) -> None:
        tab = self._tabs.get(editor)
        if tab is None:
            return
        self.editor_service.set_document_text(tab.document_id, editor.toPlainText())
        tab.dirty = self.editor_service.documents[tab.document_id].dirty
        self._update_tab_title(editor)
        if editor is self.current_editor():
            self._refresh_tab_details()

    def _update_tab_title(self, editor: StudioCodeEditor) -> None:
        index = self.tab_widget.indexOf(editor)
        tab = self._tabs.get(editor)
        if index < 0 or tab is None:
            return
        suffix = " *" if tab.dirty else ""
        self.tab_widget.setTabText(index, f"{tab.display_name}{suffix}")

    def _refresh_tab_details(self) -> None:
        self.open_count_label.setText(f"{self.tab_widget.count()} open files")
        self.recovery_label.setText(f"Recovery snapshots: {self.autosave_dirty()}")
        self.stack.setCurrentWidget(self.workspace_surface if self.tab_widget.count() else self.empty_state)
        tab = self._current_tab()
        if tab is None:
            self.path_label.setText("No file open")
            self.file_meta.setText("No file open")
            self.outline.clear()
            self.problems.clear()
            self.search_results.clear()
            self._refresh_recent_files()
            return
        document = self.editor_service.documents.get(tab.document_id)
        text = document.content if document is not None else tab.editor.toPlainText()
        language = document.syntax_mode if document is not None else LANGUAGE_BY_SUFFIX.get((tab.path.suffix if tab.path else "").lower(), "text")
        self.path_label.setText(str(tab.path) if tab.path is not None else "Untitled buffer")
        self.file_meta.setText(
            f"Name: {tab.display_name}\n"
            f"Path: {tab.path if tab.path else 'Not saved yet'}\n"
            f"Language: {language}\n"
            f"Lines: {text.count(chr(10)) + 1}\n"
            f"Dirty: {'yes' if tab.dirty else 'no'}"
        )
        self._refresh_outline()
        self._refresh_problems()
        self._refresh_search_results()
        self._refresh_recent_files()

    def _refresh_outline(self) -> None:
        self.outline.clear()
        tab = self._current_tab()
        if tab is None:
            return
        language = LANGUAGE_BY_SUFFIX.get((tab.path.suffix if tab.path else "").lower(), "text")
        lines = tab.editor.toPlainText().splitlines()
        if language == "python":
            for idx, line in enumerate(lines, start=1):
                if line.lstrip().startswith(("def ", "class ")):
                    item = QListWidgetItem(f"L{idx}: {line.strip()}")
                    item.setData(Qt.ItemDataRole.UserRole, idx)
                    self.outline.addItem(item)
        elif language == "markdown":
            for idx, line in enumerate(lines, start=1):
                if line.strip().startswith("#"):
                    item = QListWidgetItem(f"L{idx}: {line.strip()}")
                    item.setData(Qt.ItemDataRole.UserRole, idx)
                    self.outline.addItem(item)
        else:
            item = QListWidgetItem("No outline provider for this file type yet")
            item.setFlags(Qt.ItemFlag.ItemIsEnabled)
            self.outline.addItem(item)

    def _refresh_problems(self) -> None:
        self.problems.clear()
        tab = self._current_tab()
        if tab is None:
            return
        entries = self._diagnose(tab)
        if not entries:
            item = QListWidgetItem("No problems detected")
            item.setFlags(Qt.ItemFlag.ItemIsEnabled)
            self.problems.addItem(item)
            return
        for entry in entries:
            item = QListWidgetItem(f"[{entry.severity.upper()}] L{entry.line}: {entry.message}")
            item.setData(Qt.ItemDataRole.UserRole, entry.line)
            self.problems.addItem(item)

    def _refresh_search_results(self) -> None:
        self.search_results.clear()
        tab = self._current_tab()
        query = self.find_input.text().strip()
        if tab is None or not query:
            item = QListWidgetItem("Type in Find to list matches in the active file")
            item.setFlags(Qt.ItemFlag.ItemIsEnabled)
            self.search_results.addItem(item)
            return
        results = self.editor_service.search(query, [tab.document_id], case_sensitive=self.case_toggle.isChecked())
        if not results.matches:
            item = QListWidgetItem("No matches in current file")
            item.setFlags(Qt.ItemFlag.ItemIsEnabled)
            self.search_results.addItem(item)
            return
        for match in results.matches:
            item = QListWidgetItem(f"L{match.line}: {match.preview}")
            item.setData(Qt.ItemDataRole.UserRole, match.line)
            self.search_results.addItem(item)

    def _refresh_recent_files(self) -> None:
        recent = self.editor_service.recent_files
        for widget in [self.recent_files, self.empty_recent]:
            widget.clear()
            if not recent:
                item = QListWidgetItem("No recent files yet")
                item.setFlags(Qt.ItemFlag.ItemIsEnabled)
                widget.addItem(item)
                continue
            for path in recent:
                item = QListWidgetItem(path.name)
                item.setToolTip(str(path))
                item.setData(Qt.ItemDataRole.UserRole, str(path))
                widget.addItem(item)

    def _diagnose(self, tab: _CodeTab) -> list[ProblemEntry]:
        text = tab.editor.toPlainText()
        language = LANGUAGE_BY_SUFFIX.get((tab.path.suffix if tab.path else "").lower(), "text")
        issues: list[ProblemEntry] = []
        if language == "python":
            try:
                ast.parse(text)
            except SyntaxError as exc:
                issues.append(ProblemEntry(severity="error", message=exc.msg, line=exc.lineno or 1))
        elif language == "json":
            try:
                json.loads(text)
            except json.JSONDecodeError as exc:
                issues.append(ProblemEntry(severity="error", message=exc.msg, line=exc.lineno))
        elif not text.strip():
            issues.append(ProblemEntry(severity="warning", message="File is empty", line=1))
        return issues

    def _find_tab_index_by_path(self, path: Path) -> int | None:
        for index in range(self.tab_widget.count()):
            editor = self.tab_widget.widget(index)
            if not isinstance(editor, StudioCodeEditor):
                continue
            tab = self._tabs.get(editor)
            if tab is not None and tab.path == path:
                return index
        return None

    def _active_tab_changed(self, index: int) -> None:
        self._refresh_tab_details()
        if index >= 0:
            self.status_message.emit(f"Active file: {self.tab_widget.tabText(index)}")

    def _open_recent_item(self, item: QListWidgetItem) -> None:
        path = item.data(Qt.ItemDataRole.UserRole)
        if path:
            self.open_file(Path(str(path)))

    def _jump_to_line_from_item(self, item: QListWidgetItem) -> None:
        line = item.data(Qt.ItemDataRole.UserRole)
        if not isinstance(line, int):
            return
        tab = self._current_tab()
        if tab is None:
            return
        cursor = tab.editor.textCursor()
        cursor.movePosition(QTextCursor.MoveOperation.Start)
        for _ in range(max(0, line - 1)):
            cursor.movePosition(QTextCursor.MoveOperation.Down)
        tab.editor.setTextCursor(cursor)
        tab.editor.setFocus()

    def _cursor_moved(self, line: int, col: int) -> None:
        self.status_message.emit(f"Ln {line}, Col {col}")

    def current_editor(self) -> StudioCodeEditor | None:
        editor = self.tab_widget.currentWidget() if hasattr(self, "tab_widget") else None
        return editor if isinstance(editor, StudioCodeEditor) else None

    def _current_tab(self) -> _CodeTab | None:
        editor = self.current_editor()
        return self._tabs.get(editor) if editor is not None else None