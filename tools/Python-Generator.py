#!/usr/bin/env python3
"""Compatibility launcher.

`generate_assets.py` is the single maintained generator implementation.
This file remains as an entry point for existing workflows.
"""

from pathlib import Path
import importlib.util
import sys


def main() -> None:
    target = Path(__file__).with_name("generate_assets.py")
    spec = importlib.util.spec_from_file_location("ec_generate_assets", target)
    if spec is None or spec.loader is None:
        raise RuntimeError(f"Failed to load generator module: {target}")

    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    if not hasattr(module, "main"):
        raise RuntimeError("generate_assets.py does not expose main()")
    module.main()


if __name__ == "__main__":
    try:
        main()
    except Exception as exc:  # noqa: BLE001
        print(f"Asset generation failed: {exc}")
        sys.exit(1)
