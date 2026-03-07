from __future__ import annotations

import re
from dataclasses import dataclass
from pathlib import Path

from extremecraft_sdk.definitions.definition_types import AddonSpec


@dataclass(frozen=True)
class RegistrySets:
    items: tuple[str, ...]
    tools: tuple[str, ...]
    armor: tuple[str, ...]
    blocks: tuple[str, ...]
    machines: tuple[str, ...]
    recipes: tuple[str, ...]
    worldgen: tuple[str, ...]


class CodeGenerator:
    """Generates Forge-oriented Java registry sources for compiled addon modules."""

    def __init__(self) -> None:
        self.generated_sources: list[Path] = []

    def generate_registry_code(self, addon: AddonSpec, output_root: Path) -> Path:
        package_name = self._package_name(addon.namespace)
        java_dir = output_root / "src" / "main" / "java" / Path(package_name.replace(".", "/"))
        java_dir.mkdir(parents=True, exist_ok=True)

        sets = self._collect_sets(addon)
        files: dict[str, str] = {
            "GeneratedItems.java": self._generated_items(package_name, sets),
            "GeneratedBlocks.java": self._generated_blocks(package_name, sets),
            "GeneratedMachines.java": self._generated_machines(package_name, sets),
            "GeneratedRecipes.java": self._generated_recipes(package_name, sets),
            "GeneratedWorldgen.java": self._generated_worldgen(package_name, sets),
            "GeneratedRegistries.java": self._generated_registries(package_name),
        }

        self.generated_sources = []
        for filename, source in files.items():
            path = java_dir / filename
            path.write_text(source, encoding="utf-8")
            self.generated_sources.append(path)

        return java_dir / "GeneratedRegistries.java"

    def _generated_registries(self, package_name: str) -> str:
        return (
            f"package {package_name};\n\n"
            "import net.minecraftforge.eventbus.api.IEventBus;\n\n"
            "public final class GeneratedRegistries {\n"
            "    public static final String MODID = \"" + package_name.split(".")[-1] + "\";\n\n"
            "    private GeneratedRegistries() {}\n\n"
            "    public static void register(IEventBus modBus) {\n"
            "        GeneratedItems.register(modBus);\n"
            "        GeneratedBlocks.register(modBus);\n"
            "        GeneratedMachines.register(modBus);\n"
            "        GeneratedRecipes.touch();\n"
            "        GeneratedWorldgen.touch();\n"
            "    }\n"
            "}\n"
        )

    def _generated_items(self, package_name: str, sets: RegistrySets) -> str:
        item_lines = "\n".join(f"        registerItem(\"{item_id}\");" for item_id in sets.items)
        tool_lines = "\n".join(f"        registerTool(\"{tool_id}\");" for tool_id in sets.tools)
        armor_lines = "\n".join(f"        registerArmor(\"{armor_id}\");" for armor_id in sets.armor)

        body = "\n".join(line for line in [item_lines, tool_lines, armor_lines] if line)
        if not body:
            body = "        // No item-like definitions were generated."

        return (
            f"package {package_name};\n\n"
            "import net.minecraft.world.item.Item;\n"
            "import net.minecraftforge.eventbus.api.IEventBus;\n"
            "import net.minecraftforge.registries.DeferredRegister;\n"
            "import net.minecraftforge.registries.ForgeRegistries;\n"
            "import net.minecraftforge.registries.RegistryObject;\n\n"
            "import java.util.LinkedHashMap;\n"
            "import java.util.Map;\n\n"
            "public final class GeneratedItems {\n"
            "    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, GeneratedRegistries.MODID);\n"
            "    public static final Map<String, RegistryObject<Item>> ALL_ITEMS = new LinkedHashMap<>();\n"
            "    public static final Map<String, RegistryObject<Item>> TOOLS = new LinkedHashMap<>();\n"
            "    public static final Map<String, RegistryObject<Item>> ARMOR = new LinkedHashMap<>();\n\n"
            "    static {\n"
            f"{body}\n"
            "    }\n\n"
            "    private GeneratedItems() {}\n\n"
            "    public static void register(IEventBus modBus) {\n"
            "        ITEMS.register(modBus);\n"
            "    }\n\n"
            "    private static RegistryObject<Item> registerItem(String id) {\n"
            "        RegistryObject<Item> entry = ITEMS.register(id, () -> new Item(new Item.Properties()));\n"
            "        ALL_ITEMS.put(id, entry);\n"
            "        return entry;\n"
            "    }\n\n"
            "    private static RegistryObject<Item> registerTool(String id) {\n"
            "        RegistryObject<Item> entry = registerItem(id);\n"
            "        TOOLS.put(id, entry);\n"
            "        return entry;\n"
            "    }\n\n"
            "    private static RegistryObject<Item> registerArmor(String id) {\n"
            "        RegistryObject<Item> entry = registerItem(id);\n"
            "        ARMOR.put(id, entry);\n"
            "        return entry;\n"
            "    }\n"
            "}\n"
        )

    def _generated_blocks(self, package_name: str, sets: RegistrySets) -> str:
        block_lines = "\n".join(f"        registerBlock(\"{block_id}\");" for block_id in sets.blocks)
        if not block_lines:
            block_lines = "        // No block definitions were generated."

        return (
            f"package {package_name};\n\n"
            "import net.minecraft.world.level.block.Block;\n"
            "import net.minecraft.world.level.block.state.BlockBehaviour;\n"
            "import net.minecraftforge.eventbus.api.IEventBus;\n"
            "import net.minecraftforge.registries.DeferredRegister;\n"
            "import net.minecraftforge.registries.ForgeRegistries;\n"
            "import net.minecraftforge.registries.RegistryObject;\n\n"
            "import java.util.LinkedHashMap;\n"
            "import java.util.Map;\n\n"
            "public final class GeneratedBlocks {\n"
            "    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, GeneratedRegistries.MODID);\n"
            "    public static final Map<String, RegistryObject<Block>> ALL_BLOCKS = new LinkedHashMap<>();\n\n"
            "    static {\n"
            f"{block_lines}\n"
            "    }\n\n"
            "    private GeneratedBlocks() {}\n\n"
            "    public static void register(IEventBus modBus) {\n"
            "        BLOCKS.register(modBus);\n"
            "    }\n\n"
            "    private static RegistryObject<Block> registerBlock(String id) {\n"
            "        RegistryObject<Block> entry = BLOCKS.register(id, () -> new Block(BlockBehaviour.Properties.of().strength(3.0F, 6.0F)));\n"
            "        ALL_BLOCKS.put(id, entry);\n"
            "        return entry;\n"
            "    }\n"
            "}\n"
        )

    def _generated_machines(self, package_name: str, sets: RegistrySets) -> str:
        machine_lines = "\n".join(f"        registerMachine(\"{machine_id}\");" for machine_id in sets.machines)
        if not machine_lines:
            machine_lines = "        // No machine definitions were generated."

        return (
            f"package {package_name};\n\n"
            "import net.minecraft.world.item.BlockItem;\n"
            "import net.minecraft.world.item.Item;\n"
            "import net.minecraft.world.level.block.Block;\n"
            "import net.minecraft.world.level.block.state.BlockBehaviour;\n"
            "import net.minecraftforge.eventbus.api.IEventBus;\n"
            "import net.minecraftforge.registries.DeferredRegister;\n"
            "import net.minecraftforge.registries.ForgeRegistries;\n"
            "import net.minecraftforge.registries.RegistryObject;\n\n"
            "import java.util.LinkedHashMap;\n"
            "import java.util.Map;\n\n"
            "public final class GeneratedMachines {\n"
            "    public static final DeferredRegister<Block> MACHINE_BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, GeneratedRegistries.MODID);\n"
            "    public static final DeferredRegister<Item> MACHINE_ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, GeneratedRegistries.MODID);\n"
            "    public static final Map<String, RegistryObject<Block>> ALL_MACHINES = new LinkedHashMap<>();\n\n"
            "    static {\n"
            f"{machine_lines}\n"
            "    }\n\n"
            "    private GeneratedMachines() {}\n\n"
            "    public static void register(IEventBus modBus) {\n"
            "        MACHINE_BLOCKS.register(modBus);\n"
            "        MACHINE_ITEMS.register(modBus);\n"
            "    }\n\n"
            "    private static RegistryObject<Block> registerMachine(String id) {\n"
            "        RegistryObject<Block> machine = MACHINE_BLOCKS.register(id, () -> new Block(BlockBehaviour.Properties.of().strength(4.0F, 8.0F)));\n"
            "        MACHINE_ITEMS.register(id, () -> new BlockItem(machine.get(), new Item.Properties()));\n"
            "        ALL_MACHINES.put(id, machine);\n"
            "        return machine;\n"
            "    }\n"
            "}\n"
        )

    def _generated_recipes(self, package_name: str, sets: RegistrySets) -> str:
        recipe_values = ",\n".join(f"            \"{recipe_id}\"" for recipe_id in sets.recipes)
        if recipe_values:
            expression = "Set.of(\n" + recipe_values + "\n    )"
        else:
            expression = "Set.of()"

        return (
            f"package {package_name};\n\n"
            "import java.util.Set;\n\n"
            "public final class GeneratedRecipes {\n"
            "    public static final Set<String> RECIPE_IDS = " + expression + ";\n\n"
            "    private GeneratedRecipes() {}\n\n"
            "    public static void touch() {\n"
            "        // Marker hook used by generated registries bootstrap.\n"
            "    }\n"
            "}\n"
        )

    def _generated_worldgen(self, package_name: str, sets: RegistrySets) -> str:
        worldgen_values = ",\n".join(
            f"            new ResourceLocation(GeneratedRegistries.MODID, \"{worldgen_id}\")" for worldgen_id in sets.worldgen
        )
        if worldgen_values:
            expression = "Set.of(\n" + worldgen_values + "\n    )"
        else:
            expression = "Set.of()"

        return (
            f"package {package_name};\n\n"
            "import net.minecraft.resources.ResourceLocation;\n\n"
            "import java.util.Set;\n\n"
            "public final class GeneratedWorldgen {\n"
            "    public static final Set<ResourceLocation> FEATURE_IDS = " + expression + ";\n\n"
            "    private GeneratedWorldgen() {}\n\n"
            "    public static void touch() {\n"
            "        // Marker hook used by generated registries bootstrap.\n"
            "    }\n"
            "}\n"
        )

    def _collect_sets(self, addon: AddonSpec) -> RegistrySets:
        items: set[str] = set()
        tools: set[str] = set()
        armor: set[str] = set()
        blocks: set[str] = set()
        machines: set[str] = set()
        recipes: set[str] = set()
        worldgen: set[str] = set()

        for definition in addon.definitions:
            definition_type = definition.type
            definition_id = definition.id

            if definition_type == "item":
                items.add(definition_id)
            elif definition_type in {"tool", "weapon"}:
                tools.add(definition_id)
            elif definition_type == "armor":
                armor.add(definition_id)
            elif definition_type == "block":
                blocks.add(definition_id)
            elif definition_type == "machine":
                machines.add(definition_id)
            elif definition_type == "recipe":
                recipes.add(definition_id)
            elif definition_type == "worldgen":
                worldgen.add(definition_id)
            elif definition_type == "material":
                material_id = definition_id
                items.update({f"{material_id}_ingot", f"{material_id}_nugget"})
                blocks.update({f"{material_id}_ore", f"{material_id}_block"})
                worldgen.add(f"{material_id}_ore")

                if bool(definition.payload.get("generate_tools", True)):
                    tools.update({
                        f"{material_id}_pickaxe",
                        f"{material_id}_axe",
                        f"{material_id}_shovel",
                        f"{material_id}_sword",
                        f"{material_id}_hoe",
                    })

                if bool(definition.payload.get("generate_armor", True)):
                    armor.update({
                        f"{material_id}_helmet",
                        f"{material_id}_chestplate",
                        f"{material_id}_leggings",
                        f"{material_id}_boots",
                    })

        # Keep deterministic ordering and avoid duplicate item registration across categories.
        items_only = tuple(sorted(items - tools - armor))
        return RegistrySets(
            items=items_only,
            tools=tuple(sorted(tools)),
            armor=tuple(sorted(armor)),
            blocks=tuple(sorted(blocks)),
            machines=tuple(sorted(machines)),
            recipes=tuple(sorted(recipes)),
            worldgen=tuple(sorted(worldgen)),
        )

    def _package_name(self, namespace: str) -> str:
        cleaned = re.sub(r"[^a-zA-Z0-9_]", "_", namespace.strip().lower())
        cleaned = cleaned or "addon"
        return f"com.extremecraft.generated.{cleaned}"
