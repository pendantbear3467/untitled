#!/usr/bin/env python3
"""ExtremeCraft modular procedural asset generator CLI."""

from __future__ import annotations

import argparse
import logging
import subprocess
import sys
from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[1]
MATERIALS_PATH = REPO_ROOT / "tools" / "materials.json"


def launch_skill_tree_editor() -> None:
    editor_path = Path(__file__).resolve().parent / "extremecraft_skill_tree_editor.py"
    if not editor_path.exists():
        raise RuntimeError(f"Skill tree editor not found: {editor_path}")
    subprocess.run([sys.executable, str(editor_path)], check=True)


def parse_args(argv: list[str] | None = None) -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="ExtremeCraft procedural asset generation toolkit")

    parser.add_argument("--materials", action="store_true", help="Generate all material-driven assets")
    parser.add_argument("--ores", action="store_true", help="Generate ore and block textures/models")
    parser.add_argument("--items", action="store_true", help="Generate item, tool, and armor assets")
    parser.add_argument("--machines", action="store_true", help="Generate machine casing textures/models")
    parser.add_argument("--gui", action="store_true", help="Generate GUI assets")
    parser.add_argument("--all", action="store_true", help="Generate all supported assets")

    parser.add_argument("--export-blockbench", action="store_true", help="Export Blockbench .bbmodel templates")
    parser.add_argument("--export-minecraft", action="store_true", help="Force Minecraft JSON generation")
    parser.add_argument("--preview", action="store_true", help="Generate preview atlas images")
    parser.add_argument("--seed", type=int, default=1337, help="Seed for deterministic procedural generation")
    parser.add_argument("--workers", type=int, default=0, help="Parallel worker count (0 = auto)")
    parser.add_argument("--watch", action="store_true", help="Watch materials catalog and regenerate on change")
    parser.add_argument("--dry-run", action="store_true", help="Compute outputs without writing files")
    parser.add_argument("--force", action="store_true", help="Overwrite GUI assets where applicable")
    parser.add_argument("--editor", action="store_true", help="Launch the ExtremeCraft Skill Tree Editor")

    # Backward compatibility alias for old mode.
    parser.add_argument("--core", action="store_true", help="Alias for --materials")

    return parser.parse_args(argv)


def configure_logging() -> logging.Logger:
    logging.basicConfig(level=logging.INFO, format="[%(levelname)s] %(message)s")
    return logging.getLogger("extremecraft.asset_generator")


def main(argv: list[str] | None = None) -> None:
    args = parse_args(argv)
    logger = configure_logging()

    if args.editor:
        launch_skill_tree_editor()

    explicit_mode = args.materials or args.ores or args.items or args.machines or args.gui or args.all or args.core
    if not explicit_mode:
        args.materials = True

    try:
        from asset_generator.generator_core import AssetPipeline, PipelineOptions
    except ModuleNotFoundError as exc:
        raise RuntimeError(
            "Missing Python dependency for asset generator. "
            "Install required packages with: pip install pillow numpy"
        ) from exc

    options = PipelineOptions(
        generate_materials=args.materials or args.core,
        generate_ores=args.ores,
        generate_items=args.items,
        generate_machines=args.machines,
        generate_gui=args.gui,
        generate_all=args.all,
        export_blockbench=args.export_blockbench,
        export_minecraft=args.export_minecraft,
        preview=args.preview,
        dry_run=args.dry_run,
        watch=args.watch,
        force=args.force,
        seed=args.seed,
        workers=args.workers,
    )

    pipeline = AssetPipeline(repo_root=REPO_ROOT, materials_path=MATERIALS_PATH, logger=logger)
    result = pipeline.run(options)
    logger.info("Asset generation complete: %d files, %d materials", result.generated_files, result.materials_processed)


if __name__ == "__main__":
    main()