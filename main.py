#!/usr/bin/env python3
from __future__ import annotations

import sys
from pathlib import Path

TOOLS_PYTHON = Path(__file__).resolve().parent / "tools" / "python"
if str(TOOLS_PYTHON) not in sys.path:
    sys.path.insert(0, str(TOOLS_PYTHON))

from asset_studio.main import main


if __name__ == "__main__":
    raise SystemExit(main())
