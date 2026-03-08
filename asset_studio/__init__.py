from __future__ import annotations

from pathlib import Path
from pkgutil import extend_path

__path__ = extend_path(__path__, __name__)

_IMPLEMENTATION_ROOT = Path(__file__).resolve().parent.parent / "tools" / "python" / "asset_studio"
if _IMPLEMENTATION_ROOT.exists():
    implementation_path = str(_IMPLEMENTATION_ROOT)
    if implementation_path not in __path__:
        __path__.append(implementation_path)

__all__ = ["main", "studio"]
