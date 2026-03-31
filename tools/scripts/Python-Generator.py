#!/usr/bin/env python3
"""Legacy launcher kept for backward compatibility.

The maintained implementation now lives in tools/generate_assets.py.
"""

from pathlib import Path
import importlib.util
import sys


def main() -> None:
    # Legacy alias maintained for older contributor scripts that still call this filename.
    target = Path(__file__).resolve().parents[1] / "generate_assets.py"
    spec = importlib.util.spec_from_file_location("ec_generate_assets", target)
    if spec is None or spec.loader is None:
        raise RuntimeError(f"Failed to load generator module: {target}")

    module = importlib.util.module_from_spec(spec)
    sys.modules[spec.name] = module
    spec.loader.exec_module(module)
    if not hasattr(module, "main"):
        raise RuntimeError("tools/generate_assets.py does not expose main()")
    module.main()


if __name__ == "__main__":
    try:
        main()
    except Exception as exc:  # noqa: BLE001
        print(f"Asset generation failed: {exc}")
        sys.exit(1)
