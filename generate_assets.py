#!/usr/bin/env python3
"""Launcher for the maintained asset generator in tools/generate_assets.py."""

from __future__ import annotations

import importlib.util
import sys
from pathlib import Path


def main() -> None:
    target = Path(__file__).resolve().parent / "tools" / "generate_assets.py"
    spec = importlib.util.spec_from_file_location("ec_generate_assets", target)
    if spec is None or spec.loader is None:
        raise RuntimeError(f"Failed to load generator module: {target}")

    module = importlib.util.module_from_spec(spec)
    sys.modules[spec.name] = module
    spec.loader.exec_module(module)

    if not hasattr(module, "main"):
        raise RuntimeError("tools/generate_assets.py does not expose main()")

    module.main(sys.argv[1:])


if __name__ == "__main__":
    try:
        main()
    except Exception as exc:  # noqa: BLE001
        print(f"Asset generation failed: {exc}")
        sys.exit(1)
