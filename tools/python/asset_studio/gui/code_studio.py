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
    QHBoxLayout,
    QLabel,
    QLineEdit,
    QListWidget,
    QListWidgetItem,
    QMessageBox,
    QPushButton,
    QPlainTextEdit,
    QSplitter,
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
        for token in keywords.get(self.language, ()):  # pragma: no branch - tiny loop
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
    path: Path
    editor: StudioCodeEditor
    dirty: bool = False


class CodeStudioPanel(QWidget):
    status_message = pyqtSignal(str)
    notifications = pyqtSignal(str)

    def __init__(self, editor_service: EditorService, workspace_root: Path) -> None:
        super().__init__()
        self.editor_service = editor_service
        self.workspace_root = workspace_root
        self._tabs: dict[int, _CodeTab] = {}

        root = QVBoxLayout(self)
        root.setContentsMargins(0, 0, 0, 0)

        self.toolbar = self._build_toolbar()
        root.addLayout(self.toolbar)

        split = QSplitter(Qt.Orientation.Horizontal)
        self.tab_widget = QTabWidget()
        self.tab_widget.setTabsClosable(True)
        self.tab_widget.tabCloseRequested.connect(self.close_tab)
        self.tab_widget.currentChanged.connect(self._active_tab_changed)
        split.addWidget(self.tab_widget)

        right = QWidget()
        right_layout = QVBoxLayout(right)
        right_layout.addWidget(QLabel("File Metadata"))
        self.file_meta = QLabel("No file open")
        self.file_meta.setWordWrap(True)
        right_layout.addWidget(self.file_meta)

        right_layout.addWidget(QLabel("Outline"))
        self.outline = QListWidget()
        self.outline.itemDoubleClicked.connect(self._jump_to_outline)
        right_layout.addWidget(self.outline)

        right_layout.addWidget(QLabel("Problems"))
        self.problems = QListWidget()
        self.problems.itemDoubleClicked.connect(self._jump_to_problem)
        right_layout.addWidget(self.problems)

        split.addWidget(right)
        split.setSizes([980, 300])
        root.addWidget(split)

        self._set_empty_state()

    def _build_toolbar(self) -> QHBoxLayout:
        row = QHBoxLayout()

        self.find_input = QLineEdit()
        self.find_input.setPlaceholderText("Find in current file")
        self.find_input.setToolTip("Search within the active file")
        self.replace_input = QLineEdit()
        self.replace_input.setPlaceholderText("Replace with")
        self.replace_input.setToolTip("Replacement text for find/replace actions")

        find_next = QPushButton("Find Next")
        find_next.setToolTip("Jump to next match")
        find_next.clicked.connect(lambda: self.find_next(False))
        find_prev = QPushButton("Find Prev")
        find_prev.setToolTip("Jump to previous match")
        find_prev.clicked.connect(lambda: self.find_next(True))
        replace_one = QPushButton("Replace")
        replace_one.setToolTip("Replace currently selected match")
        replace_one.clicked.connect(self.replace_one)
        replace_all = QPushButton("Replace All")
        replace_all.setToolTip("Replace all matches in this file")
        replace_all.clicked.connect(self.replace_all)
        save_btn = QPushButton("Save")
        save_btn.setToolTip("Save active file to disk")
        save_btn.clicked.connect(self.save_current)

        for widget in [
            self.find_input,
            self.replace_input,
            find_next,
            find_prev,
            replace_one,
            replace_all,
            save_btn,
        ]:
            row.addWidget(widget)

        return row

    def open_file(self, path: Path) -> None:
        if path.is_dir() or not path.exists():
            return

        existing_index = self._find_tab_by_path(path)
        if existing_index is not None:
            self.tab_widget.setCurrentIndex(existing_index)
            return

        document = self.editor_service.open_document(path)

        editor = StudioCodeEditor()
        editor.setPlainText(document.content)
        editor.set_language(document.syntax_mode)
        editor.cursor_moved.connect(self._cursor_moved)

        index = self.tab_widget.addTab(editor, path.name)
        self._tabs[index] = _CodeTab(document_id=document.document_id, path=path, editor=editor, dirty=document.dirty)
        editor.textChanged.connect(lambda idx=index: self._mark_dirty(idx))
        self.tab_widget.setCurrentIndex(index)
        self._refresh_meta(index)
        self._refresh_outline(index)
        self._refresh_problems(index)
        self.status_message.emit(f"Opened: {path}")

    def save_current(self) -> bool:
        index = self.tab_widget.currentIndex()
        tab = self._tabs.get(index)
        if tab is None:
            return True

        try:
            self.editor_service.set_document_text(tab.document_id, tab.editor.toPlainText())
            saved_path = self.editor_service.save_document(tab.document_id)
        except Exception as exc:  # noqa: BLE001
            QMessageBox.critical(self, "Save Failed", f"Could not save file:\n{tab.path}\n\n{exc}")
            self.notifications.emit(f"ERROR: failed to save {tab.path.name}: {exc}")
            return False

        tab.path = saved_path
        tab.dirty = False
        self._update_tab_title(index)
        self._refresh_problems(index)
        self.status_message.emit(f"Saved: {saved_path}")
        self.notifications.emit(f"Saved {tab.path.name}")
        return True

    def autosave_dirty(self) -> int:
        snapshots = self.workspace_root / ".studio" / "autosave"
        if not snapshots.exists():
            return 0
        return len(list(snapshots.glob("text-document-*.json")))

    def has_unsaved(self) -> bool:
        return any(tab.dirty for tab in self._tabs.values())

    def close_tab(self, index: int) -> None:
        tab = self._tabs.get(index)
        if tab is None:
            return
        self.tab_widget.setCurrentIndex(index)
        if tab.dirty:
            result = QMessageBox.question(
                self,
                "Unsaved Changes",
                f"Save changes to {tab.path.name} before closing?",
                QMessageBox.StandardButton.Yes | QMessageBox.StandardButton.No | QMessageBox.StandardButton.Cancel,
            )
            if result == QMessageBox.StandardButton.Cancel:
                return
            if result == QMessageBox.StandardButton.Yes:
                if not self.save_current():
                    return
        self.tab_widget.removeTab(index)
        self.editor_service.close_document(tab.document_id)
        self._reindex_tabs()
        self._active_tab_changed(self.tab_widget.currentIndex())

    def save_all(self) -> int:
        count = 0
        for index in list(self._tabs.keys()):
            self.tab_widget.setCurrentIndex(index)
            if self.save_current():
                count += 1
        return count

    def find_next(self, backward: bool) -> None:
        tab = self._tabs.get(self.tab_widget.currentIndex())
        if tab is None:
            return
        query = self.find_input.text()
        if not query:
            return
        flags = QTextDocument.FindFlag.FindBackward if backward else QTextDocument.FindFlag(0)
        found = tab.editor.find(query, flags)
        if not found:
            cursor = tab.editor.textCursor()
            cursor.movePosition(QTextCursor.MoveOperation.End if backward else QTextCursor.MoveOperation.Start)
            tab.editor.setTextCursor(cursor)
            tab.editor.find(query, flags)

    def replace_one(self) -> None:
        tab = self._tabs.get(self.tab_widget.currentIndex())
        if tab is None:
            return
        cursor = tab.editor.textCursor()
        if cursor.hasSelection():
            cursor.insertText(self.replace_input.text())
        self.find_next(False)

    def replace_all(self) -> None:
        tab = self._tabs.get(self.tab_widget.currentIndex())
        if tab is None:
            return
        source = tab.editor.toPlainText()
        query = self.find_input.text()
        if not query:
            return
        tab.editor.setPlainText(source.replace(query, self.replace_input.text()))

    def _mark_dirty(self, index: int) -> None:
        tab = self._tabs.get(index)
        if tab is None:
            return
        self.editor_service.set_document_text(tab.document_id, tab.editor.toPlainText())
        tab.dirty = self.editor_service.documents[tab.document_id].dirty
        self._update_tab_title(index)
        if self.tab_widget.currentIndex() == index:
            self._refresh_outline(index)

    def _update_tab_title(self, index: int) -> None:
        tab = self._tabs.get(index)
        if tab is None:
            return
        suffix = " *" if tab.dirty else ""
        self.tab_widget.setTabText(index, f"{tab.path.name}{suffix}")

    def _refresh_meta(self, index: int) -> None:
        tab = self._tabs.get(index)
        if tab is None:
            self.file_meta.setText("No file open")
            return
        document = self.editor_service.documents.get(tab.document_id)
        text = document.content if document is not None else tab.editor.toPlainText()
        line_count = text.count("\n") + 1
        language = document.syntax_mode if document is not None else LANGUAGE_BY_SUFFIX.get(tab.path.suffix.lower(), "text")
        self.file_meta.setText(f"Path: {tab.path}\nLanguage: {language}\nLines: {line_count}")

    def _refresh_outline(self, index: int) -> None:
        self.outline.clear()
        tab = self._tabs.get(index)
        if tab is None:
            return
        language = LANGUAGE_BY_SUFFIX.get(tab.path.suffix.lower(), "text")
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

    def _refresh_problems(self, index: int) -> None:
        self.problems.clear()
        tab = self._tabs.get(index)
        if tab is None:
            return

        entries = self._diagnose(tab)
        if not entries:
            ok_item = QListWidgetItem("No problems detected")
            ok_item.setFlags(Qt.ItemFlag.ItemIsEnabled)
            self.problems.addItem(ok_item)
            return

        for entry in entries:
            item = QListWidgetItem(f"[{entry.severity.upper()}] L{entry.line}: {entry.message}")
            item.setData(Qt.ItemDataRole.UserRole, entry.line)
            self.problems.addItem(item)

    def _diagnose(self, tab: _CodeTab) -> list[ProblemEntry]:
        text = tab.editor.toPlainText()
        language = LANGUAGE_BY_SUFFIX.get(tab.path.suffix.lower(), "text")
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
        elif language in {"yaml", "toml", "java", "markdown", "text"}:
            if not text.strip():
                issues.append(ProblemEntry(severity="warning", message="File is empty", line=1))
        return issues

    def _find_tab_by_path(self, path: Path) -> int | None:
        for index, tab in self._tabs.items():
            if tab.path == path:
                return index
        return None

    def _reindex_tabs(self) -> None:
        updated: dict[int, _CodeTab] = {}
        for index in range(self.tab_widget.count()):
            widget = self.tab_widget.widget(index)
            if widget is None:
                continue
            for old_idx, tab in self._tabs.items():
                if tab.editor is widget:
                    updated[index] = tab
                    break
        self._tabs = updated

    def _active_tab_changed(self, index: int) -> None:
        self._refresh_meta(index)
        self._refresh_outline(index)
        self._refresh_problems(index)

    def _jump_to_outline(self, item: QListWidgetItem) -> None:
        self._jump_to_line(item.data(Qt.ItemDataRole.UserRole))

    def _jump_to_problem(self, item: QListWidgetItem) -> None:
        self._jump_to_line(item.data(Qt.ItemDataRole.UserRole))

    def _jump_to_line(self, line: int | None) -> None:
        if not isinstance(line, int):
            return
        tab = self._tabs.get(self.tab_widget.currentIndex())
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

    def _set_empty_state(self) -> None:
        self.problems.clear()
        item = QListWidgetItem("Open a file from Project Browser to start coding")
        item.setFlags(Qt.ItemFlag.ItemIsEnabled)
        self.problems.addItem(item)
        self.outline.clear()
        self.file_meta.setText("No file open")
