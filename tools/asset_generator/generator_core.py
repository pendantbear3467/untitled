from __future__ import annotations

import logging
import time
from concurrent.futures import ThreadPoolExecutor
from dataclasses import dataclass
from pathlib import Path

from PIL import Image

from .gui_generator import GuiGenerator
from .material_catalog import MaterialCatalog, MaterialDefinition
from .model_generator import ModelGenerator, ModelTargets
from .texture_generator import GeneratedTexture, TextureGenerator

MODID = "extremecraft"


@dataclass
class PipelineOptions:
    generate_materials: bool = False
    generate_ores: bool = False
    generate_items: bool = False
    generate_machines: bool = False
    generate_gui: bool = False
    generate_all: bool = False
    export_blockbench: bool = False
    export_minecraft: bool = False
    preview: bool = False
    dry_run: bool = False
    watch: bool = False
    force: bool = False
    seed: int = 1337
    workers: int = 0


@dataclass
class PipelineResult:
    generated_files: int
    materials_processed: int


class AssetPipeline:
    def __init__(self, repo_root: Path, materials_path: Path, logger: logging.Logger | None = None) -> None:
        self.repo_root = repo_root
        self.material_catalog = MaterialCatalog(materials_path)
        self.logger = logger or logging.getLogger("extremecraft.asset_generator")

        self.assets_root = repo_root / "src" / "main" / "resources" / "assets" / MODID
        self.block_texture_dir = self.assets_root / "textures" / "block"
        self.item_texture_dir = self.assets_root / "textures" / "item"
        self.gui_texture_dir = self.assets_root / "textures" / "gui"
        self.block_model_dir = self.assets_root / "models" / "block"
        self.item_model_dir = self.assets_root / "models" / "item"
        self.blockstates_dir = self.assets_root / "blockstates"

        self.generated_root = repo_root / "tools" / "generated"
        self.blockbench_dir = self.generated_root / "blockbench"
        self.preview_dir = self.generated_root / "previews"

    def run(self, options: PipelineOptions) -> PipelineResult:
        if options.watch:
            return self._watch(options)
        return self._run_once(options)

    def _watch(self, options: PipelineOptions) -> PipelineResult:
        self.logger.info("Watch mode active. Monitoring materials catalog for changes...")
        last_mtime = 0.0
        total_generated = 0
        materials_processed = 0

        while True:
            self.material_catalog.ensure_exists()
            current_mtime = self.material_catalog.path.stat().st_mtime
            if current_mtime > last_mtime:
                last_mtime = current_mtime
                self.logger.info("Detected material catalog change. Regenerating assets...")
                run_result = self._run_once(options)
                total_generated += run_result.generated_files
                materials_processed = run_result.materials_processed
            time.sleep(1.0)

        # Unreachable, kept for type clarity.
        return PipelineResult(generated_files=total_generated, materials_processed=materials_processed)

    def _save_texture(self, path: Path, texture: GeneratedTexture, dry_run: bool) -> int:
        changed = 0
        if dry_run:
            changed += int(not path.exists())
            if texture.emissive is not None:
                emissive_path = path.with_name(path.stem + "_e.png")
                changed += int(not emissive_path.exists())
            return changed

        path.parent.mkdir(parents=True, exist_ok=True)
        if not path.exists():
            texture.image.save(path, format="PNG")
            changed += 1

        if texture.emissive is not None:
            emissive_path = path.with_name(path.stem + "_e.png")
            existed = emissive_path.exists()
            texture.emissive.save(emissive_path, format="PNG")
            changed += int(not existed)

        return changed

    def _ensure_dirs(self) -> None:
        for directory in (
            self.block_texture_dir,
            self.item_texture_dir,
            self.gui_texture_dir,
            self.block_model_dir,
            self.item_model_dir,
            self.blockstates_dir,
            self.blockbench_dir,
            self.preview_dir,
        ):
            directory.mkdir(parents=True, exist_ok=True)

    def _materials_scope_active(self, options: PipelineOptions) -> bool:
        return options.generate_all or options.generate_materials

    def _ores_scope_active(self, options: PipelineOptions) -> bool:
        return options.generate_all or options.generate_ores or self._materials_scope_active(options)

    def _items_scope_active(self, options: PipelineOptions) -> bool:
        return options.generate_all or options.generate_items or self._materials_scope_active(options)

    def _machines_scope_active(self, options: PipelineOptions) -> bool:
        return options.generate_all or options.generate_machines or self._materials_scope_active(options)

    def _minecraft_export_active(self, options: PipelineOptions) -> bool:
        return options.export_minecraft or self._ores_scope_active(options) or self._items_scope_active(options) or self._machines_scope_active(options)

    def _run_once(self, options: PipelineOptions) -> PipelineResult:
        self._ensure_dirs()
        texture_gen = TextureGenerator(seed=options.seed)
        model_gen = ModelGenerator(
            ModelTargets(
                block_models=self.block_model_dir,
                item_models=self.item_model_dir,
                blockstates=self.blockstates_dir,
                blockbench=self.blockbench_dir,
            ),
            dry_run=options.dry_run,
        )

        materials = self.material_catalog.load()
        workers = options.workers if options.workers > 0 else min(32, max(4, len(materials) // 8 or 4))

        def process_material(material: MaterialDefinition) -> int:
            changed = 0
            ore_id = f"{material.name}_ore"
            block_id = f"{material.name}_block"
            casing_id = f"{material.name}_machine_casing"

            if self._ores_scope_active(options):
                changed += self._save_texture(
                    self.block_texture_dir / f"{ore_id}.png",
                    texture_gen.generate_ore(material),
                    options.dry_run,
                )
                changed += self._save_texture(
                    self.block_texture_dir / f"{block_id}.png",
                    texture_gen.generate_metal_block(material),
                    options.dry_run,
                )
                if self._minecraft_export_active(options):
                    changed += model_gen.generate_block_and_item_models(ore_id)
                    changed += model_gen.generate_block_and_item_models(block_id)

                # Optional biome variants and LOD textures for richer visual variety.
                changed += self._save_texture(
                    self.block_texture_dir / f"{ore_id}_cold.png",
                    texture_gen.generate_ore(MaterialDefinition(material.name, "#8fb8ff", material.tier, material.glow)),
                    options.dry_run,
                )
                changed += self._save_texture(
                    self.block_texture_dir / f"{ore_id}_hot.png",
                    texture_gen.generate_ore(MaterialDefinition(material.name, "#ff9e5a", material.tier, material.glow)),
                    options.dry_run,
                )

            if self._items_scope_active(options):
                item_ids = [
                    f"{material.name}_ingot",
                    f"{material.name}_nugget",
                    f"raw_{material.name}",
                    f"{material.name}_dust",
                ]
                item_textures = [
                    texture_gen.generate_ingot(material),
                    texture_gen.generate_nugget(material),
                    texture_gen.generate_nugget(material),
                    texture_gen.generate_nugget(material),
                ]

                tool_ids = [
                    f"{material.name}_pickaxe",
                    f"{material.name}_sword",
                    f"{material.name}_axe",
                    f"{material.name}_shovel",
                    f"{material.name}_hoe",
                    f"{material.name}_hammer",
                    f"{material.name}_drill",
                ]

                for item_id, texture in zip(item_ids, item_textures):
                    changed += self._save_texture(self.item_texture_dir / f"{item_id}.png", texture, options.dry_run)
                    if self._minecraft_export_active(options):
                        changed += model_gen.generate_item_model(item_id)

                for tool_id in tool_ids:
                    changed += self._save_texture(
                        self.item_texture_dir / f"{tool_id}.png",
                        texture_gen.generate_tool(material, tool_id),
                        options.dry_run,
                    )
                    changed += self._save_texture(
                        self.item_texture_dir / f"{tool_id}_damage_overlay.png",
                        texture_gen.generate_damage_overlay(tool_id),
                        options.dry_run,
                    )
                    if self._minecraft_export_active(options):
                        changed += model_gen.generate_item_model(tool_id)

                armor_slots = ["helmet", "chestplate", "leggings", "boots"]
                for slot in armor_slots:
                    armor_id = f"{material.name}_{slot}"
                    changed += self._save_texture(
                        self.item_texture_dir / f"{armor_id}.png",
                        texture_gen.generate_armor_icon(material, slot),
                        options.dry_run,
                    )
                    if self._minecraft_export_active(options):
                        changed += model_gen.generate_item_model(armor_id)

            if self._machines_scope_active(options):
                changed += self._save_texture(
                    self.block_texture_dir / f"{casing_id}.png",
                    texture_gen.generate_machine_casing(material),
                    options.dry_run,
                )
                if self._minecraft_export_active(options):
                    changed += model_gen.generate_block_and_item_models(casing_id)

            if options.export_blockbench:
                changed += model_gen.export_blockbench_templates(material.name, include_drill=True)

            return changed

        generated = 0
        with ThreadPoolExecutor(max_workers=workers) as executor:
            for changed in executor.map(process_material, materials):
                generated += changed

        if options.generate_gui or options.generate_all:
            gui_result = GuiGenerator(self.gui_texture_dir, dry_run=options.dry_run, force=options.force).generate()
            generated += gui_result.generated

        if options.preview:
            generated += self._generate_preview(materials, texture_gen, options.dry_run)

        self.logger.info("Processed %d materials", len(materials))
        self.logger.info("Generated/updated %d files", generated)
        return PipelineResult(generated_files=generated, materials_processed=len(materials))

    def _generate_preview(self, materials: list[MaterialDefinition], texture_gen: TextureGenerator, dry_run: bool) -> int:
        if not materials:
            return 0

        tile = 40
        cols = 8
        rows = (len(materials) + cols - 1) // cols
        sheet = Image.new("RGBA", (cols * tile, rows * tile), (18, 20, 24, 255))

        for index, material in enumerate(materials):
            ore = texture_gen.generate_ore(material, size=32).image
            x = (index % cols) * tile + 4
            y = (index // cols) * tile + 4
            sheet.paste(ore.resize((32, 32), Image.NEAREST), (x, y))

        path = self.preview_dir / "materials_preview.png"
        if dry_run:
            return int(not path.exists())
        path.parent.mkdir(parents=True, exist_ok=True)
        sheet.save(path, format="PNG")
        return 1
