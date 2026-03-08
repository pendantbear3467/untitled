from __future__ import annotations

import ast
import json
import re
from collections import defaultdict
from dataclasses import dataclass
from pathlib import Path
from typing import Iterable

from PyQt6.QtCore import QRect, QSize, Qt, pyqtSignal
from PyQt6.QtGui import QColor, QFont, QPainter, QTextCharFormat, QTextCursor, QTextFormat, QSyntaxHighlighter
from PyQt6.QtWidgets import (
    QFileDialog,
    QHBoxLayout,
    QInputDialog,
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
    QTreeWidget,
    QTreeWidgetItem,
    QVBoxLayout,
    QWidget,
)

from asset_studio.code.diagnostics import CodeDiagnostic
from asset_studio.code.document import TextDocument
from asset_studio.code.editor_service import EditorService
from asset_studio.code.java_support import JavaAnalysis, JavaSymbol, analyze_java_source, render_java_scaffold, suggest_java_target_path


LANGUAGE_BY_SUFFIX = {
    ".py": "python",
    ".json": "json",
    ".java": "java",
    ".yml": "yaml",
    ".yaml": "yaml",
    ".toml": "toml",
    ".md": "markdown",
}

SYMBOL_GROUP_TITLES = {
    "package": "Package",
    "import": "Imports",
    "class": "Classes",
    "interface": "Interfaces",
    "enum": "Enums",
    "record": "Records",
    "field": "Fields",
    "method": "Methods",
}

SEVERITY_ORDER = {"error": 0, "warning": 1, "info": 2}


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

        return {"keyword": keyword, "number": number, "string": string, "comment": comment}

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
            "java": ("class", "interface", "enum", "record", "public", "private", "protected", "static", "void", "if", "else", "for", "while", "return", "new", "import", "package"),
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
    current_file_changed = pyqtSignal(object)
    open_link_requested = pyqtSignal(object)

    def __init__(self, session, workspace_root: Path | None = None) -> None:
        super().__init__()
        self.session = session if hasattr(session, "code_editor_service") else None
        self.editor_service: EditorService = session.code_editor_service if hasattr(session, "code_editor_service") else session
        self.workspace_root = workspace_root or (session.context.workspace_root if hasattr(session, "context") else Path.cwd())
        self.relationship_service = getattr(session, "relationship_service", None) if hasattr(session, "relationship_service") else None
        self._tabs: dict[StudioCodeEditor, _CodeTab] = {}
        self._setting_editor_text = False
        self._outline_cache: dict[StudioCodeEditor, list[JavaSymbol]] = {}
        self._symbol_nav_index = -1

        root = QVBoxLayout(self)
        root.setContentsMargins(0, 0, 0, 0)
        root.addLayout(self._build_primary_toolbar())
        root.addLayout(self._build_search_toolbar())

        split = QSplitter(Qt.Orientation.Horizontal)
        self.tab_widget = QTabWidget()
        self.tab_widget.setDocumentMode(True)
        self.tab_widget.setTabsClosable(True)
        self.tab_widget.tabCloseRequested.connect(self.close_tab)
        self.tab_widget.currentChanged.connect(self._active_tab_changed)
        split.addWidget(self.tab_widget)

        right = QWidget()
        right_layout = QVBoxLayout(right)
        right_layout.setContentsMargins(0, 0, 0, 0)
        right_layout.addWidget(QLabel("File Metadata"))
        self.file_meta = QLabel("No file open")
        self.file_meta.setWordWrap(True)
        right_layout.addWidget(self.file_meta)

        self.symbol_filter = QLineEdit()
        self.symbol_filter.setPlaceholderText("Go to symbol")
        self.symbol_filter.returnPressed.connect(self.go_to_symbol)
        right_layout.addWidget(self.symbol_filter)

        right_layout.addWidget(QLabel("Linked Assets / Targets"))
        self.relationships = QTreeWidget()
        self.relationships.setHeaderLabels(["Relation", "Target"])
        self.relationships.itemDoubleClicked.connect(self._open_relation_item)
        right_layout.addWidget(self.relationships)

        right_layout.addWidget(QLabel("Outline"))
        self.outline = QTreeWidget()
        self.outline.setHeaderLabels(["Symbol", "Line"])
        self.outline.itemDoubleClicked.connect(self._jump_to_outline)
        right_layout.addWidget(self.outline)

        right_layout.addWidget(QLabel("Problems"))
        self.problems = QTreeWidget()
        self.problems.setHeaderLabels(["Problem", "Location"])
        self.problems.itemDoubleClicked.connect(self._jump_to_problem)
        right_layout.addWidget(self.problems)

        right_layout.addWidget(QLabel("Recent Files"))
        self.recent_files = QListWidget()
        self.recent_files.itemDoubleClicked.connect(self._open_recent_file)
        right_layout.addWidget(self.recent_files)

        split.addWidget(right)
        split.setSizes([980, 420])
        root.addWidget(split)

        self._update_recent_files()
        self._restore_tabs_from_service()
        if not self._tabs:
            self._set_empty_state()

    def set_session(self, session) -> None:
        self.session = session if hasattr(session, "code_editor_service") else None
        self.editor_service = session.code_editor_service if hasattr(session, "code_editor_service") else session
        self.relationship_service = getattr(session, "relationship_service", None) if hasattr(session, "relationship_service") else None
        self._clear_tabs()
        self._update_recent_files()
        self._restore_tabs_from_service()
        if not self._tabs:
            self._set_empty_state()
        self._sync_session_state()
        self._notify_current_file()

    def set_workspace_root(self, workspace_root: Path) -> None:
        self.workspace_root = workspace_root

    def _build_primary_toolbar(self) -> QHBoxLayout:
        row = QHBoxLayout()
        controls = [
            ("New", self.new_document, "Create a new untitled document."),
            ("Open", self.open_file_dialog, "Open a file from the current workspace or disk."),
            ("Save", self.save_current, "Save the active file."),
            ("Save All", self.save_all, "Save all open files."),
            ("New Java File", self.new_java_file, "Create a Java file scaffold and target path."),
            ("New Class", lambda: self.new_java_scaffold("class"), "Create a Java class scaffold in src/main/java."),
            ("New Interface", lambda: self.new_java_scaffold("interface"), "Create a Java interface scaffold."),
            ("New Enum", lambda: self.new_java_scaffold("enum"), "Create a Java enum scaffold."),
            ("New Record", lambda: self.new_java_scaffold("record"), "Create a Java record scaffold."),
            ("Go To Symbol", self.go_to_symbol, "Jump to a symbol in the active file."),
            ("Prev Symbol", self.go_to_previous_symbol, "Jump to the previous symbol in the active outline."),
            ("Next Symbol", self.go_to_next_symbol, "Jump to the next symbol in the active outline."),
            ("Open Source", lambda: self.open_linked_target("source_document"), "Open linked source document for current file."),
            ("Open Runtime", lambda: self.open_linked_target("runtime_export"), "Open linked runtime export for current file."),
            ("Open Java", lambda: self.open_linked_target("java_target"), "Open linked Java target for current file."),
            ("Open Asset", self.open_linked_asset, "Open linked texture/model asset for current file."),
        ]
        for label, callback, help_text in controls:
            button = QPushButton(label)
            button.setToolTip(help_text)
            button.clicked.connect(callback)
            row.addWidget(button)
        return row

    def new_java_file(self) -> None:
        kinds = ["class", "interface", "enum", "record"]
        kind, accepted = QInputDialog.getItem(self, "New Java File", "Type kind:", kinds, 0, False)
        if not accepted:
            return
        self.new_java_scaffold(str(kind))

    def _build_search_toolbar(self) -> QHBoxLayout:
        row = QHBoxLayout()
        self.find_input = QLineEdit()
        self.find_input.setPlaceholderText("Find in current file")
        self.replace_input = QLineEdit()
        self.replace_input.setPlaceholderText("Replace with")
        find_next = QPushButton("Find Next")
        find_next.clicked.connect(lambda: self.find_next(False))
        find_prev = QPushButton("Find Prev")
        find_prev.clicked.connect(lambda: self.find_next(True))
        replace_one = QPushButton("Replace")
        replace_one.clicked.connect(self.replace_one)
        replace_all = QPushButton("Replace All")
        replace_all.clicked.connect(self.replace_all)
        for widget in [self.find_input, self.replace_input, find_next, find_prev, replace_one, replace_all]:
            row.addWidget(widget)
        return row

    def new_document(self) -> None:
        document = self.editor_service.new_document(path=None, content="")
        editor = self._open_document_tab(document, document.name)
        self._refresh_sidebar(editor)
        self._sync_session_state()
        self._notify_current_file()
        self.notifications.emit("Created new untitled document")
        self.status_message.emit("New document ready")

    def new_java_scaffold(self, kind: str) -> None:
        qualified_name, accepted = QInputDialog.getText(self, f"New Java {kind.title()}", "Qualified type name (package.TypeName or TypeName):")
        if not accepted or not qualified_name.strip():
            return
        raw_name = qualified_name.strip()
        package_name = ""
        type_name = raw_name
        if "." in raw_name:
            package_name, type_name = raw_name.rsplit(".", 1)
        path_hint = suggest_java_target_path(
            self.workspace_root,
            package_name,
            type_name,
            repo_root=self.session.context.repo_root if self.session is not None else self.workspace_root,
        )
        scaffold = render_java_scaffold(kind, type_name, package_name=package_name)
        document = self.editor_service.new_document(path=path_hint, content="")
        document.syntax_mode = "java"
        document.set_content(scaffold)
        editor = self._open_document_tab(document, path_hint.name)
        self._set_editor_text(editor, scaffold)
        tab = self._tabs[editor]
        tab.dirty = True
        self._update_tab_title(editor)
        self._refresh_sidebar(editor)
        self._sync_session_state()
        self._notify_current_file()
        self.notifications.emit(f"Prepared Java {kind} scaffold: {type_name}")
        self.status_message.emit(f"Java scaffold ready: {type_name}")

    def open_generated_content(self, content: str, *, language: str = "text", path: Path | None = None, title: str | None = None) -> None:
        document = self.editor_service.new_document(path=path, content="")
        document.syntax_mode = language
        document.set_content(content)
        editor = self._open_document_tab(document, title or document.name)
        self._set_editor_text(editor, content)
        tab = self._tabs[editor]
        tab.dirty = True
        self._update_tab_title(editor)
        self._refresh_sidebar(editor)
        self._sync_session_state()
        self._notify_current_file()

    def apply_text_to_current(self, content: str) -> bool:
        editor = self._current_editor()
        if editor is None:
            return False
        tab = self._current_tab()
        if tab is None:
            return False
        self.editor_service.set_document_text(tab.document_id, content)
        self._set_editor_text(editor, content)
        tab.dirty = self.editor_service.documents[tab.document_id].dirty
        self._update_tab_title(editor)
        self._refresh_sidebar(editor)
        self._sync_session_state()
        return True

    def open_file_dialog(self) -> None:
        selected, _ = QFileDialog.getOpenFileName(self, "Open file", str(self.workspace_root), "All Files (*.*)")
        if selected:
            self.open_file(Path(selected))

    def open_file(self, path: Path) -> None:
        normalized = path.resolve(strict=False)
        if normalized.is_dir() or not normalized.exists():
            self.notifications.emit(f"File not found: {path}")
            return
        existing_editor = self._find_editor_by_path(normalized)
        if existing_editor is not None:
            self.tab_widget.setCurrentWidget(existing_editor)
            self._notify_current_file()
            return
        try:
            document = self.editor_service.open_document(normalized)
        except Exception as exc:  # noqa: BLE001
            self.notifications.emit(f"Failed to open {normalized}: {exc}")
            self.status_message.emit(f"Open failed: {normalized.name}")
            return
        editor = self._open_document_tab(document, normalized.name)
        self._refresh_sidebar(editor)
        self._update_recent_files()
        self._sync_session_state()
        self._notify_current_file()
        self.status_message.emit(f"Opened: {normalized}")

    def _open_document_tab(self, document: TextDocument, title: str | None = None, *, make_current: bool = True) -> StudioCodeEditor:
        if document.path is not None:
            existing_editor = self._find_editor_by_path(document.path)
            if existing_editor is not None:
                if make_current:
                    self.tab_widget.setCurrentWidget(existing_editor)
                    self._notify_current_file()
                return existing_editor
        editor = StudioCodeEditor()
        editor.set_language(document.syntax_mode)
        self._set_editor_text(editor, document.content)
        editor.cursor_moved.connect(self._cursor_moved)
        editor.textChanged.connect(lambda editor=editor: self._editor_text_changed(editor))
        self._tabs[editor] = _CodeTab(document_id=document.document_id, path=document.path, editor=editor, dirty=document.dirty)
        index = self.tab_widget.addTab(editor, title or document.name)
        if make_current:
            self.tab_widget.setCurrentIndex(index)
            self._notify_current_file()
        return editor

    def _restore_tabs_from_service(self) -> None:
        for document in self.editor_service.documents.values():
            self._open_document_tab(document, document.name, make_current=False)
        if self.tab_widget.count() > 0:
            self.tab_widget.setCurrentIndex(0)
            self._active_tab_changed(0)
        else:
            self.current_file_changed.emit(None)

    def _clear_tabs(self) -> None:
        self.tab_widget.clear()
        self._tabs.clear()
        self._outline_cache.clear()
        self.current_file_changed.emit(None)

    def _notify_current_file(self) -> None:
        tab = self._current_tab()
        self.current_file_changed.emit(tab.path if tab is not None else None)

    def current_file(self) -> Path | None:
        tab = self._current_tab()
        return tab.path if tab is not None else None

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
        self._outline_cache.pop(editor, None)
        self.tab_widget.removeTab(index)
        self._update_recent_files()
        self._sync_session_state()
        if self.tab_widget.count() == 0:
            self._set_empty_state()
            self.current_file_changed.emit(None)
        else:
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
        self._refresh_sidebar(editor)
        self._sync_session_state()
        self.notifications.emit(f"Replaced {count} occurrence(s) in {tab.display_name}")

    def go_to_symbol(self) -> None:
        editor = self._current_editor()
        if editor is None:
            return
        query = self.symbol_filter.text().strip().lower()
        symbols = self._outline_symbols(editor)
        if not symbols:
            self.status_message.emit("No outline symbols available for the active file")
            return
        candidate = None
        if not query:
            candidate = symbols[0]
        else:
            for symbol in symbols:
                haystack = f"{symbol.symbol_type} {symbol.name} {symbol.display_name}".lower()
                if query in haystack:
                    candidate = symbol
                    break
        if candidate is None:
            self.status_message.emit(f"No symbol match for '{query}'")
            return
        self._symbol_nav_index = max(0, symbols.index(candidate))
        self._jump_to_position(candidate.line, candidate.column, max(1, len(candidate.name)))
        self.status_message.emit(f"Symbol: {candidate.display_name}")

    def go_to_next_symbol(self) -> None:
        self._step_symbol_navigation(1)

    def go_to_previous_symbol(self) -> None:
        self._step_symbol_navigation(-1)

    def open_linked_target(self, relation: str) -> None:
        tab = self._current_tab()
        if tab is None or tab.path is None:
            self.status_message.emit("No active file for linked navigation")
            return
        record = self._relationship_record(tab.path)
        if record is None:
            self.status_message.emit("No relationship data available")
            return
        target = record.first_target(relation)
        if target is None:
            self.status_message.emit(f"No linked {relation.replace('_', ' ')}")
            return
        self.open_link_requested.emit(target.path)
        self.status_message.emit(f"Opened linked {relation.replace('_', ' ')}: {target.path.name}")

    def open_linked_asset(self) -> None:
        tab = self._current_tab()
        if tab is None or tab.path is None:
            self.status_message.emit("No active file for linked navigation")
            return
        record = self._relationship_record(tab.path)
        if record is None:
            self.status_message.emit("No relationship data available")
            return
        asset_kinds = {"texture_asset", "item_model", "block_model", "model_runtime", "json"}
        for target in record.targets:
            if target.kind in asset_kinds:
                self.open_link_requested.emit(target.path)
                self.status_message.emit(f"Opened linked asset: {target.path.name}")
                return
        self.status_message.emit("No linked asset target")

    def _editor_text_changed(self, editor: StudioCodeEditor) -> None:
        if self._setting_editor_text:
            return
        tab = self._tabs.get(editor)
        if tab is None:
            return
        self.editor_service.set_document_text(tab.document_id, editor.toPlainText())
        tab.dirty = self.editor_service.documents[tab.document_id].dirty
        self._outline_cache.pop(editor, None)
        self._update_tab_title(editor)
        if editor is self._current_editor():
            self._refresh_sidebar(editor)
        self._sync_session_state()

    def _save_editor(self, editor: StudioCodeEditor) -> bool:
        tab = self._tabs.get(editor)
        if tab is None:
            return True
        try:
            self.editor_service.set_document_text(tab.document_id, editor.toPlainText())
            if tab.path is None:
                suggested_name = f"{tab.display_name}.txt" if not tab.display_name.endswith(".txt") else tab.display_name
                selected, _ = QFileDialog.getSaveFileName(self, "Save file", str(self.workspace_root / suggested_name), "All Files (*.*)")
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
        tab.path = saved_path.resolve(strict=False)
        tab.dirty = False
        document = self.editor_service.documents.get(tab.document_id)
        if document is not None:
            editor.set_language(document.syntax_mode)
        self._update_tab_title(editor)
        self._refresh_sidebar(editor)
        self._update_recent_files()
        self._sync_session_state()
        self.status_message.emit(f"Saved: {saved_path}")
        self.notifications.emit(f"Saved {saved_path.name}")
        self._notify_current_file()
        return True

    def _refresh_sidebar(self, editor: StudioCodeEditor | None) -> None:
        self._refresh_meta(editor)
        self._refresh_relations(editor)
        self._refresh_outline(editor)
        self._refresh_problems(editor)

    def _refresh_meta(self, editor: StudioCodeEditor | None) -> None:
        if editor is None or editor not in self._tabs:
            self.file_meta.setText("No file open")
            return
        tab = self._tabs[editor]
        document = self.editor_service.documents.get(tab.document_id)
        text = document.content if document is not None else editor.toPlainText()
        line_count = text.count("\n") + 1
        language = self._document_language(tab, document)
        recent = len(self.editor_service.recent_files)
        details = [
            f"Path: {tab.path if tab.path is not None else tab.display_name}",
            f"Language: {language}",
            f"Lines: {line_count}",
            f"Dirty: {'yes' if tab.dirty else 'no'}",
            f"Recent tracked: {recent}",
        ]
        if language == "java":
            analysis = self._analyze_java(tab, text)
            if analysis.package_name:
                details.append(f"Package: {analysis.package_name}")
            if analysis.imports:
                details.append(f"Imports: {len(analysis.imports)}")
            if analysis.resource_ids:
                details.append(f"Resource IDs: {', '.join(analysis.resource_ids[:3])}")
        record = self._relationship_record(tab.path)
        if record is not None:
            if record.resource_id:
                details.append(f"Relationship resource: {record.resource_id}")
            if record.targets:
                details.append(f"Linked targets: {len(record.targets)}")
            linked_resource_ids = record.metadata.get("linkedJavaResourceIds")
            if isinstance(linked_resource_ids, list) and linked_resource_ids:
                details.append(f"Linked Java resources: {', '.join(str(item) for item in linked_resource_ids[:3])}")
        self.file_meta.setText("\n".join(details))

    def _refresh_relations(self, editor: StudioCodeEditor | None) -> None:
        self.relationships.clear()
        if editor is None or editor not in self._tabs:
            return
        tab = self._tabs[editor]
        record = self._relationship_record(tab.path)
        if record is None:
            item = QTreeWidgetItem(["No relationship data", ""])
            item.setFlags(Qt.ItemFlag.ItemIsEnabled)
            self.relationships.addTopLevelItem(item)
            return
        grouped: dict[str, list[object]] = defaultdict(list)
        for target in record.targets:
            grouped[target.relation].append(target)
        for relation in sorted(grouped):
            group_item = QTreeWidgetItem([relation.replace("_", " ").title(), ""])
            group_item.setFlags(Qt.ItemFlag.ItemIsEnabled)
            self.relationships.addTopLevelItem(group_item)
            for target in grouped[relation]:
                child = QTreeWidgetItem([target.kind, target.path.name])
                child.setData(0, Qt.ItemDataRole.UserRole, str(target.path))
                tooltip = [str(target.path)]
                if target.resource_id:
                    tooltip.append(f"resourceId: {target.resource_id}")
                tooltip.append(f"confidence: {target.confidence}")
                child.setToolTip(0, "\n".join(tooltip))
                child.setToolTip(1, "\n".join(tooltip))
                group_item.addChild(child)
        if record.warnings:
            warning_group = QTreeWidgetItem(["Warnings", ""])
            warning_group.setFlags(Qt.ItemFlag.ItemIsEnabled)
            self.relationships.addTopLevelItem(warning_group)
            for message in record.warnings[:8]:
                warning_item = QTreeWidgetItem([message, ""])
                warning_item.setFlags(Qt.ItemFlag.ItemIsEnabled)
                warning_group.addChild(warning_item)
        self.relationships.expandAll()

    def _refresh_outline(self, editor: StudioCodeEditor | None) -> None:
        self.outline.clear()
        if editor is None or editor not in self._tabs:
            return
        tab = self._tabs[editor]
        document = self.editor_service.documents.get(tab.document_id)
        language = self._document_language(tab, document)
        text = editor.toPlainText()
        if language == "java":
            analysis = self._analyze_java(tab, text)
            self._outline_cache[editor] = list(analysis.symbols)
            if analysis.package_name:
                package_group = QTreeWidgetItem([SYMBOL_GROUP_TITLES["package"], "1"])
                package_group.setFlags(Qt.ItemFlag.ItemIsEnabled)
                self.outline.addTopLevelItem(package_group)
                package_line = self._line_for_regex(text, rf"^\s*package\s+{re.escape(analysis.package_name)}\s*;")
                package_child = QTreeWidgetItem([analysis.package_name, str(package_line)])
                package_child.setData(0, Qt.ItemDataRole.UserRole, (package_line, 1, max(1, len(analysis.package_name))))
                package_group.addChild(package_child)

            if analysis.imports:
                import_group = QTreeWidgetItem([SYMBOL_GROUP_TITLES["import"], str(len(analysis.imports))])
                import_group.setFlags(Qt.ItemFlag.ItemIsEnabled)
                self.outline.addTopLevelItem(import_group)
                for import_name in analysis.imports:
                    import_line = self._line_for_regex(text, rf"^\s*import\s+(?:static\s+)?{re.escape(import_name)}\s*;")
                    import_child = QTreeWidgetItem([import_name, str(import_line)])
                    import_child.setData(0, Qt.ItemDataRole.UserRole, (import_line, 1, max(1, len(import_name))))
                    import_group.addChild(import_child)

            for symbol_type in ["class", "interface", "enum", "record", "field", "method"]:
                symbols = [symbol for symbol in analysis.symbols if symbol.symbol_type == symbol_type]
                if not symbols:
                    continue
                group = QTreeWidgetItem([SYMBOL_GROUP_TITLES[symbol_type], str(len(symbols))])
                group.setFlags(Qt.ItemFlag.ItemIsEnabled)
                self.outline.addTopLevelItem(group)
                for symbol in symbols:
                    child = QTreeWidgetItem([symbol.display_name, str(symbol.line)])
                    child.setData(0, Qt.ItemDataRole.UserRole, (symbol.line, symbol.column, len(symbol.name)))
                    group.addChild(child)
            self.outline.expandAll()
            return

        self._outline_cache[editor] = []
        lines = text.splitlines()
        entries: list[tuple[str, int]] = []
        if language == "python":
            for idx, line in enumerate(lines, start=1):
                if line.lstrip().startswith(("def ", "class ")):
                    entries.append((line.strip(), idx))
        elif language == "markdown":
            for idx, line in enumerate(lines, start=1):
                if line.strip().startswith("#"):
                    entries.append((line.strip(), idx))
        elif language == "json":
            for idx, line in enumerate(lines, start=1):
                match = re.search(r'"([^"]+)"\s*:', line)
                if match:
                    entries.append((match.group(1), idx))
        if not entries:
            item = QTreeWidgetItem(["No outline symbols", ""])
            item.setFlags(Qt.ItemFlag.ItemIsEnabled)
            self.outline.addTopLevelItem(item)
            return
        group = QTreeWidgetItem(["Outline", str(len(entries))])
        group.setFlags(Qt.ItemFlag.ItemIsEnabled)
        self.outline.addTopLevelItem(group)
        for label, line in entries:
            child = QTreeWidgetItem([label, str(line)])
            child.setData(0, Qt.ItemDataRole.UserRole, (line, 1, max(1, len(label))))
            group.addChild(child)
        self.outline.expandAll()

    def _refresh_problems(self, editor: StudioCodeEditor | None) -> None:
        self.problems.clear()
        if editor is None or editor not in self._tabs:
            return
        tab = self._tabs[editor]
        diagnostics = self._diagnose(tab)
        self.editor_service.problems.set_document_issues(tab.document_id, diagnostics)
        if not diagnostics:
            ok_item = QTreeWidgetItem(["No problems detected", ""])
            ok_item.setFlags(Qt.ItemFlag.ItemIsEnabled)
            self.problems.addTopLevelItem(ok_item)
            return
        file_label = str(tab.path) if tab.path is not None else tab.display_name
        file_item = QTreeWidgetItem([file_label, str(len(diagnostics))])
        file_item.setFlags(Qt.ItemFlag.ItemIsEnabled)
        self.problems.addTopLevelItem(file_item)
        by_severity: dict[str, list[CodeDiagnostic]] = defaultdict(list)
        for diagnostic in diagnostics:
            by_severity[diagnostic.severity].append(diagnostic)
        for severity in sorted(by_severity, key=lambda value: SEVERITY_ORDER.get(value, 99)):
            group = QTreeWidgetItem([severity.upper(), str(len(by_severity[severity]))])
            group.setFlags(Qt.ItemFlag.ItemIsEnabled)
            file_item.addChild(group)
            for diagnostic in by_severity[severity]:
                location = f"L{diagnostic.line}:C{diagnostic.column}"
                child = QTreeWidgetItem([diagnostic.message, location])
                child.setData(0, Qt.ItemDataRole.UserRole, (diagnostic.line, diagnostic.column, 1))
                group.addChild(child)
        self.problems.expandAll()

    def _diagnose(self, tab: _CodeTab) -> list[CodeDiagnostic]:
        text = tab.editor.toPlainText()
        document = self.editor_service.documents.get(tab.document_id)
        language = self._document_language(tab, document)
        diagnostic_path = tab.path if tab.path is not None else Path(tab.display_name)
        issues: list[CodeDiagnostic] = []
        if language == "python":
            try:
                ast.parse(text)
            except SyntaxError as exc:
                issues.append(CodeDiagnostic(severity="error", message=exc.msg, line=exc.lineno or 1, column=exc.offset or 1, source="python", path=diagnostic_path))
        elif language == "json":
            try:
                json.loads(text)
            except json.JSONDecodeError as exc:
                issues.append(CodeDiagnostic(severity="error", message=exc.msg, line=exc.lineno, column=exc.colno, source="json", path=diagnostic_path))
        elif language == "java":
            analysis = self._analyze_java(tab, text)
            issues.extend(analysis.diagnostics)
            if tab.path is not None:
                record = self._relationship_record(tab.path)
                if record is not None:
                    for warning in record.warnings[:6]:
                        issues.append(CodeDiagnostic(severity="info", message=warning, line=1, column=1, source="relationship", path=diagnostic_path))
        if not text.strip():
            issues.append(CodeDiagnostic(severity="warning", message="File is empty", line=1, column=1, source=language, path=diagnostic_path))
        return issues

    def _outline_symbols(self, editor: StudioCodeEditor) -> list[JavaSymbol]:
        cached = self._outline_cache.get(editor)
        if cached is not None:
            return cached
        tab = self._tabs.get(editor)
        if tab is None:
            return []
        analysis = self._analyze_java(tab, editor.toPlainText())
        self._outline_cache[editor] = list(analysis.symbols)
        return self._outline_cache[editor]

    def _analyze_java(self, tab: _CodeTab, text: str) -> JavaAnalysis:
        path = tab.path if tab.path is not None else self.workspace_root / f"{tab.display_name}.java"
        return analyze_java_source(text, path=path)

    def _document_language(self, tab: _CodeTab, document: TextDocument | None) -> str:
        fallback_language = LANGUAGE_BY_SUFFIX.get(tab.path.suffix.lower(), "text") if tab.path is not None else "text"
        return document.syntax_mode if document is not None else fallback_language

    def _relationship_record(self, path: Path | None):
        if path is None or self.relationship_service is None:
            return None
        return self.relationship_service.resolve_path(path)

    def _find_editor_by_path(self, path: Path) -> StudioCodeEditor | None:
        normalized = path.resolve(strict=False)
        for editor, tab in self._tabs.items():
            if tab.path == normalized:
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
        self._symbol_nav_index = -1
        editor = self._current_editor()
        self._refresh_sidebar(editor)
        self._notify_current_file()

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

    def _jump_to_outline(self, item: QTreeWidgetItem) -> None:
        payload = item.data(0, Qt.ItemDataRole.UserRole)
        if isinstance(payload, tuple) and len(payload) == 3:
            self._jump_to_position(payload[0], payload[1], payload[2])

    def _jump_to_problem(self, item: QTreeWidgetItem) -> None:
        payload = item.data(0, Qt.ItemDataRole.UserRole)
        if isinstance(payload, tuple) and len(payload) == 3:
            self._jump_to_position(payload[0], payload[1], payload[2])

    def _open_relation_item(self, item: QTreeWidgetItem) -> None:
        value = item.data(0, Qt.ItemDataRole.UserRole)
        if value:
            self.open_link_requested.emit(Path(str(value)))

    def _jump_to_position(self, line: int, column: int, length: int) -> None:
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

    def _jump_to_match(self, line: int, column: int, length: int) -> None:
        self._jump_to_position(line, column, length)

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
        self.outline.clear()
        self.relationships.clear()
        item = QTreeWidgetItem(["Open a file from Project Browser or use Open to start coding", ""])
        item.setFlags(Qt.ItemFlag.ItemIsEnabled)
        self.problems.addTopLevelItem(item)
        self.file_meta.setText("No file open")

    def _step_symbol_navigation(self, step: int) -> None:
        editor = self._current_editor()
        if editor is None:
            return
        symbols = self._outline_symbols(editor)
        if not symbols:
            self.status_message.emit("No symbols available for navigation")
            return
        if self._symbol_nav_index < 0:
            self._symbol_nav_index = 0 if step >= 0 else len(symbols) - 1
        else:
            self._symbol_nav_index = (self._symbol_nav_index + step) % len(symbols)
        symbol = symbols[self._symbol_nav_index]
        self._jump_to_position(symbol.line, symbol.column, max(1, len(symbol.name)))
        self.status_message.emit(f"Symbol {self._symbol_nav_index + 1}/{len(symbols)}: {symbol.display_name}")

    def _line_for_regex(self, text: str, pattern: str) -> int:
        regex = re.compile(pattern)
        for index, line in enumerate(text.splitlines(), start=1):
            if regex.match(line):
                return index
        return 1
