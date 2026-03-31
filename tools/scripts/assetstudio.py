#!/usr/bin/env python3
from __future__ import annotations

import sys
from pathlib import Path

# Launcher scripts run as plain files, so we manually add tools/python to sys.path
# to make local package imports work without requiring an editable pip install.
TOOLS_PYTHON = Path(__file__).resolve().parents[1] / "python"
if str(TOOLS_PYTHON) not in sys.path:
    sys.path.insert(0, str(TOOLS_PYTHON))

from asset_studio.main import main


if __name__ == "__main__":
    raise SystemExit(main())
