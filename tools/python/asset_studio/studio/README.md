# Studio Source Index

This folder is the canonical place to find the desktop Studio source.

Primary entry points:
- `app_window.py`: main desktop shell window
- `code_studio.py`: Code Studio page and editor widget
- `studio_panels.py`: GUI Studio, Model Studio, and Build/Run panels
- `studio_session.py`: authoritative Studio session/services bootstrap

These files currently re-export the existing implementation from the legacy package layout so the rest of the project keeps working while giving you one place to start looking.

Legacy implementation locations:
- `../gui/app_window.py`
- `../gui/code_studio.py`
- `../gui/studio_panels.py`
- `../core/studio_session.py`
