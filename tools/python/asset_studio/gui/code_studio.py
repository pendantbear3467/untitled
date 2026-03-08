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
    QFileDialog,
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
    QTextEdit,
    QVBoxLayout,
    QWidget,
)

from asset_studio.code.diagnostics import CodeDiagnostic
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
        selection = QTextEdit.ExtraSelection()
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

    def __init__(self, session, workspace_root: Path | None = None) -> None:
        super().__init__()
        self.session = session if hasattr(session, "code_editor_service") else None
        self.editor_service: EditorService = session.code_editor_service if hasattr(session, "code_editor_service") else session
        self.workspace_root = workspace_root or (session.context.workspace_root if hasattr(session, "context") else Path.cwd())
        self._tabs: dict[StudioCodeEditor, _CodeTab] = {}
        self._setting_editor_text = False

        root = QVBoxLayout(self)
        root.setContentsMargins(0, 0, 0, 0)

        self.toolbar = self._build_toolbar()
        root.addLayout(self.toolbar)

        split = QSplitter(Qt.Orientation.Horizontal)
        self.tab_widget = QTabWidget()
        self.tab_widget.setDocumentMode(True)
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

        right_layout.addWidget(QLabel("Recent Files"))
        self.recent_files = QListWidget()
        self.recent_files.itemDoubleClicked.connect(self._open_recent_file)
        right_layout.addWidget(self.recent_files)

        split.addWidget(right)
        split.setSizes([980, 320])
        root.addWidget(split)

        self._update_recent_files()
        self._set_empty_state()

    def set_session(self, session) -> None:
        self.session = session if hasattr(session, "code_editor_service") else None
        self.editor_service = session.code_editor_service if hasattr(session, "code_editor_service") else session
        self._update_recent_files()
        self._sync_session_state()

    def set_workspace_root(self, workspace_root: Path) -> None:
        self.workspace_root = workspace_root

    def _build_toolbar(self) -> QHBoxLayout:
        row = QHBoxLayout()

        new_btn = QPushButton("New")
        new_btn.setToolTip("Create a new untitled document in Code Studio.")
        new_btn.setStatusTip("Create a new untitled document in Code Studio.")
        new_btn.clicked.connect(self.new_document)
        open_btn = QPushButton("Open")
        open_btn.setToolTip("Open a file from the current workspace or disk.")
        open_btn.setStatusTip("Open a file from the current workspace or disk.")
        open_btn.clicked.connect(self.open_file_dialog)
        save_btn = QPushButton("Save")
        save_btn.setToolTip("Save the active file. Untitled buffers prompt for a path.")
        save_btn.setStatusTip("Save the active file. Untitled buffers prompt for a path.")
        save_btn.clicked.connect(self.save_current)
        save_all = QPushButton("Save All")
        save_all.setToolTip("Save all open files and keep EditorService session state in sync.")
        save_all.setStatusTip("Save all open files and keep EditorService session state in sync.")
        save_all.clicked.connect(self.save_all)

        self.find_input = QLineEdit()
        self.find_input.setPlaceholderText("Find in current file")
        self.find_input.setToolTip("Search within the active file.")
        self.replace_input = QLineEdit()
        self.replace_input.setPlaceholderText("Replace with")
        self.replace_input.setToolTip("Replacement text for Replace and Replace All.")

        find_next = QPushButton("Find Next")
        find_next.setToolTip("Jump to the next match.")
        find_next.clicked.connect(lambda: self.find_next(False))
        find_prev = QPushButton("Find Prev")
        find_prev.setToolTip("Jump to the previous match.")
        find_prev.clicked.connect(lambda: self.find_next(True))
        replace_one = QPushButton("Replace")
        replace_one.setToolTip("Replace the current selection or jump to the next match.")
        replace_one.clicked.connect(self.replace_one)
        replace_all = QPushButton("Replace All")
        replace_all.setToolTip("Replace all matches in the active file.")
        replace_all.clicked.connect(self.replace_all)

        for widget in [new_btn, open_btn, save_btn, save_all, self.find_input, self.replace_input, find_next, find_prev, replace_one, replace_all]:
            row.addWidget(widget)
        return row

    def new_document(self) -> None:
        document = self.editor_service.new_document(path=None, content="")
        editor = StudioCodeEditor()
        editor.set_language(document.syntax_mode)
        self._set_editor_text(editor, document.content)
        editor.cursor_moved.connect(self._cursor_moved)
        editor.textChanged.connect(lambda editor=editor: self._editor_text_changed(editor))

        self._tabs[editor] = _CodeTab(document_id=document.document_id, path=document.path, editor=editor, dirty=document.dirty)
        index = self.tab_widget.addTab(editor, document.name)
        self.tab_widget.setCurrentIndex(index)
        self._refresh_meta(editor)
        self._refresh_outline(editor)
        self._refresh_problems(editor)
        self._sync_session_state()
        self.notifications.emit("Created new untitled document")
        self.status_message.emit("New document ready")

    def open_file_dialog(self) -> None:
        selected, _ = QFileDialog.getOpenFileName(self, "Open file", str(self.workspace_root), "All Files (*.*)")
        if selected:
            self.open_file(Path(selected))

    def open_file(self, path: Path) -> None:
        if path.is_dir() or not path.exists():
            self.notifications.emit(f"File not found: {path}")
            return

        existing_editor = self._find_editor_by_path(path)
        if existing_editor is not None:
            self.tab_widget.setCurrentWidget(existing_editor)
            return

        document = self.editor_service.open_document(path)
        editor = StudioCodeEditor()
        editor.set_language(document.syntax_mode)
        self._set_editor_text(editor, document.content)
        editor.cursor_moved.connect(self._cursor_moved)
        editor.textChanged.connect(lambda editor=editor: self._editor_text_changed(editor))

        self._tabs[editor] = _CodeTab(document_id=document.document_id, path=path, editor=editor, dirty=document.dirty)
        index = self.tab_widget.addTab(editor, path.name)
        self.tab_widget.setCurrentIndex(index)
        self._refresh_meta(editor)
        self._refresh_outline(editor)
        self._refresh_problems(editor)
        self._update_recent_files()
        self._sync_session_state()
        self.status_message.emit(f"Opened: {path}")

    def save_current(self) -> bool:
        editor = self._current_editor()
        if editor is None:
            return True
        return self._save_editor(editor)

    def save_all(self) -> int:
        count = 0
        for editor in list(self._tabs):
            if self._save_editor(editor):
                count += 1
        self._sync_session_state()
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
        self.tab_widget.setCurrentWidget(editor)
        if tab.dirty:
            result = QMessageBox.question(
                self,
                "Unsaved Changes",
                f"Save changes to {tab.display_name} before closing?",
                QMessageBox.StandardButton.Yes | QMessageBox.StandardButton.No | QMessageBox.StandardButton.Cancel,
            )
            if result == QMessageBox.StandardButton.Cancel:
                return
            if result == QMessageBox.StandardButton.Yes and not self._save_editor(editor):
                return
        self.editor_service.close_document(tab.document_id)
        self._tabs.pop(editor, None)
        self.tab_widget.removeTab(index)
        self._update_recent_files()
        self._sync_session_state()
        self._active_tab_changed(self.tab_widget.currentIndex())

    def find_next(self, backward: bool) -> None:
        editor = self._current_editor()
        tab = self._current_tab()
        if editor is None or tab is None:
            return
        query = self.find_input.text().strip()
        if not query:
            return
        results = self.editor_service.search(query, [tab.document_id])
        if not results.matches:
            self.status_message.emit(f"No matches for '{query}'")
            return
        cursor = editor.textCursor()
        current_line = cursor.blockNumber() + 1
        current_col = cursor.columnNumber() + 1
        ordered = sorted(results.matches, key=lambda match: (match.line, match.column))
        target = None
        if backward:
            for match in reversed(ordered):
                if (match.line, match.column) < (current_line, current_col):
                    target = match
                    break
            target = target or ordered[-1]
        else:
            for match in ordered:
                if (match.line, match.column) > (current_line, current_col):
                    target = match
                    break
            target = target or ordered[0]
        self._jump_to_match(target.line, target.column, len(query))
        self.status_message.emit(f"Match at line {target.line}, column {target.column}")

    def replace_one(self) -> None:
        editor = self._current_editor()
        if editor is None:
            return
        cursor = editor.textCursor()
        if cursor.hasSelection():
            cursor.insertText(self.replace_input.text())
        else:
            self.find_next(False)

    def replace_all(self) -> None:
        tab = self._current_tab()
        editor = self._current_editor()
        if tab is None or editor is None:
            return
        query = self.find_input.text().strip()
        replacement = self.replace_input.text()
        if not query:
            return
        count = self.editor_service.replace_all(tab.document_id, query, replacement)
        document = self.editor_service.documents[tab.document_id]
        self._set_editor_text(editor, document.content)
        tab.dirty = document.dirty
        self._update_tab_title(editor)
        self._refresh_outline(editor)
        self._refresh_problems(editor)
        self._sync_session_state()
        self.notifications.emit(f"Replaced {count} occurrence(s) in {tab.display_name}")

    def _editor_text_changed(self, editor: StudioCodeEditor) -> None:
        if self._setting_editor_text:
            return
        tab = self._tabs.get(editor)
        if tab is None:
            return
        self.editor_service.set_document_text(tab.document_id, editor.toPlainText())
        tab.dirty = self.editor_service.documents[tab.document_id].dirty
        self._update_tab_title(editor)
        if editor is self._current_editor():
            self._refresh_meta(editor)
            self._refresh_outline(editor)
            self._refresh_problems(editor)
        self._sync_session_state()

    def _save_editor(self, editor: StudioCodeEditor) -> bool:
        tab = self._tabs.get(editor)
        if tab is None:
            return True
        try:
            self.editor_service.set_document_text(tab.document_id, editor.toPlainText())
            if tab.path is None:
                suggested_name = f"{tab.display_name}.txt" if not tab.display_name.endswith('.txt') else tab.display_name
                selected, _ = QFileDialog.getSaveFileName(
                    self,
                    "Save file",
                    str(self.workspace_root / suggested_name),
                    "All Files (*.*)",
                )
                if not selected:
                    self.status_message.emit("Save cancelled")
                    return False
                saved_path = self.editor_service.save_document(tab.document_id, Path(selected))
            else:
                saved_path = self.editor_service.save_document(tab.document_id)
        except Exception as exc:  # noqa: BLE001
            path_label = str(tab.path) if tab.path is not None else tab.display_name
            QMessageBox.critical(self, "Save Failed", f"Could not save file:\n{path_label}\n\n{exc}")
            self.notifications.emit(f"ERROR: failed to save {tab.display_name}: {exc}")
            return False

        tab.path = saved_path
        tab.dirty = False
        document = self.editor_service.documents.get(tab.document_id)
        if document is not None:
            editor.set_language(document.syntax_mode)
        self._update_tab_title(editor)
        self._refresh_meta(editor)
        self._refresh_problems(editor)
        self._update_recent_files()
        self._sync_session_state()
        self.status_message.emit(f"Saved: {saved_path}")
        self.notifications.emit(f"Saved {saved_path.name}")
        return True

    def _refresh_meta(self, editor: StudioCodeEditor | None) -> None:
        if editor is None or editor not in self._tabs:
            self.file_meta.setText("No file open")
            return
        tab = self._tabs[editor]
        document = self.editor_service.documents.get(tab.document_id)
        text = document.content if document is not None else editor.toPlainText()
        line_count = text.count("\n") + 1
        fallback_language = LANGUAGE_BY_SUFFIX.get(tab.path.suffix.lower(), "text") if tab.path is not None else "text"
        language = document.syntax_mode if document is not None else fallback_language
        recent = len(self.editor_service.recent_files)
        self.file_meta.setText(
            f"Path: {tab.path if tab.path is not None else tab.display_name}\nLanguage: {language}\nLines: {line_count}\nDirty: {'yes' if tab.dirty else 'no'}\nRecent tracked: {recent}"
        )

    def _refresh_outline(self, editor: StudioCodeEditor | None) -> None:
        self.outline.clear()
        if editor is None or editor not in self._tabs:
            return
        tab = self._tabs[editor]
        document = self.editor_service.documents.get(tab.document_id)
        language = document.syntax_mode if document is not None else LANGUAGE_BY_SUFFIX.get(tab.path.suffix.lower(), "text") if tab.path is not None else "text"
        lines = editor.toPlainText().splitlines()

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
        elif language == "json":
            for idx, line in enumerate(lines, start=1):
                if '"' in line and ":" in line:
                    match = re.search(r'"([^"]+)"\s*:', line)
                    if match:
                        item = QListWidgetItem(f"L{idx}: {match.group(1)}")
                        item.setData(Qt.ItemDataRole.UserRole, idx)
                        self.outline.addItem(item)
        elif language == "java":
            for idx, line in enumerate(lines, start=1):
                stripped = line.strip()
                if stripped.startswith(("class ", "public class", "private ", "protected ", "public ")) and ("(" in stripped or "class" in stripped):
                    item = QListWidgetItem(f"L{idx}: {stripped}")
                    item.setData(Qt.ItemDataRole.UserRole, idx)
                    self.outline.addItem(item)
        else:
            item = QListWidgetItem("No outline provider for this file type yet")
            item.setFlags(Qt.ItemFlag.ItemIsEnabled)
            self.outline.addItem(item)

    def _refresh_problems(self, editor: StudioCodeEditor | None) -> None:
        self.problems.clear()
        if editor is None or editor not in self._tabs:
            return
        tab = self._tabs[editor]
        diagnostics = self._diagnose(tab)
        self.editor_service.problems.set_document_issues(tab.document_id, diagnostics)
        if not diagnostics:
            ok_item = QListWidgetItem("No problems detected")
            ok_item.setFlags(Qt.ItemFlag.ItemIsEnabled)
            self.problems.addItem(ok_item)
            return

        for diagnostic in diagnostics:
            item = QListWidgetItem(f"[{diagnostic.severity.upper()}] L{diagnostic.line}: {diagnostic.message}")
            item.setData(Qt.ItemDataRole.UserRole, diagnostic.line)
            self.problems.addItem(item)

    def _diagnose(self, tab: _CodeTab) -> list[CodeDiagnostic]:
        text = tab.editor.toPlainText()
        document = self.editor_service.documents.get(tab.document_id)
        language = document.syntax_mode if document is not None else LANGUAGE_BY_SUFFIX.get(tab.path.suffix.lower(), "text") if tab.path is not None else "text"
        diagnostic_path = tab.path if tab.path is not None else Path(tab.display_name)
        issues: list[CodeDiagnostic] = []

        if language == "python":
            try:
                ast.parse(text)
            except SyntaxError as exc:
                issues.append(CodeDiagnostic(severity="error", message=exc.msg, line=exc.lineno or 1, source="python", path=diagnostic_path))
        elif language == "json":
            try:
                json.loads(text)
            except json.JSONDecodeError as exc:
                issues.append(CodeDiagnostic(severity="error", message=exc.msg, line=exc.lineno, source="json", path=diagnostic_path))
        elif language == "java":
            if text.count("{") != text.count("}"):
                issues.append(CodeDiagnostic(severity="warning", message="Brace count is unbalanced", line=1, source="java", path=diagnostic_path))
        if not text.strip():
            issues.append(CodeDiagnostic(severity="warning", message="File is empty", line=1, source=language, path=diagnostic_path))
        return issues

    def _find_editor_by_path(self, path: Path) -> StudioCodeEditor | None:
        for editor, tab in self._tabs.items():
            if tab.path == path:
                return editor
        return None

    def _current_editor(self) -> StudioCodeEditor | None:
        widget = self.tab_widget.currentWidget()
        return widget if isinstance(widget, StudioCodeEditor) else None

    def _current_tab(self) -> _CodeTab | None:
        editor = self._current_editor()
        return self._tabs.get(editor) if editor is not None else None

    def _active_tab_changed(self, index: int) -> None:
        _ = index
        editor = self._current_editor()
        self._refresh_meta(editor)
        self._refresh_outline(editor)
        self._refresh_problems(editor)

    def _update_tab_title(self, editor: StudioCodeEditor) -> None:
        tab = self._tabs.get(editor)
        if tab is None:
            return
        suffix = " *" if tab.dirty else ""
        index = self.tab_widget.indexOf(editor)
        if index >= 0:
            self.tab_widget.setTabText(index, f"{tab.display_name}{suffix}")

    def _update_recent_files(self) -> None:
        self.recent_files.clear()
        for path in self.editor_service.recent_files:
            item = QListWidgetItem(str(path))
            item.setData(Qt.ItemDataRole.UserRole, str(path))
            self.recent_files.addItem(item)

    def _open_recent_file(self, item: QListWidgetItem) -> None:
        value = item.data(Qt.ItemDataRole.UserRole)
        if value:
            self.open_file(Path(str(value)))

    def _jump_to_outline(self, item: QListWidgetItem) -> None:
        self._jump_to_line(item.data(Qt.ItemDataRole.UserRole))

    def _jump_to_problem(self, item: QListWidgetItem) -> None:
        self._jump_to_line(item.data(Qt.ItemDataRole.UserRole))

    def _jump_to_line(self, line: int | None) -> None:
        editor = self._current_editor()
        if editor is None or not isinstance(line, int):
            return
        cursor = editor.textCursor()
        cursor.movePosition(QTextCursor.MoveOperation.Start)
        for _ in range(max(0, line - 1)):
            cursor.movePosition(QTextCursor.MoveOperation.Down)
        editor.setTextCursor(cursor)
        editor.setFocus()

    def _jump_to_match(self, line: int, column: int, length: int) -> None:
        editor = self._current_editor()
        if editor is None:
            return
        cursor = editor.textCursor()
        cursor.movePosition(QTextCursor.MoveOperation.Start)
        for _ in range(max(0, line - 1)):
            cursor.movePosition(QTextCursor.MoveOperation.Down)
        for _ in range(max(0, column - 1)):
            cursor.movePosition(QTextCursor.MoveOperation.Right)
        for _ in range(max(0, length)):
            cursor.movePosition(QTextCursor.MoveOperation.Right, QTextCursor.MoveMode.KeepAnchor)
        editor.setTextCursor(cursor)
        editor.setFocus()

    def _cursor_moved(self, line: int, col: int) -> None:
        self.status_message.emit(f"Ln {line}, Col {col}")

    def _set_editor_text(self, editor: StudioCodeEditor, text: str) -> None:
        self._setting_editor_text = True
        editor.setPlainText(text)
        self._setting_editor_text = False

    def _sync_session_state(self) -> None:
        self._update_recent_files()
        if self.session is not None and hasattr(self.session, "sync_code_session_state"):
            self.session.sync_code_session_state()

    def _set_empty_state(self) -> None:
        self.problems.clear()
        item = QListWidgetItem("Open a file from Project Browser or use Open to start coding")
        item.setFlags(Qt.ItemFlag.ItemIsEnabled)
        self.problems.addItem(item)
        self.outline.clear()
        self.file_meta.setText("No file open")
