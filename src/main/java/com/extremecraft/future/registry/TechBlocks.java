package com.extremecraft.future.registry;

import com.extremecraft.core.ECConstants;
import com.extremecraft.machine.cable.CableBlock;
import com.extremecraft.machine.cable.CableTier;
import com.extremecraft.machine.core.MachineBlock;
import com.extremecraft.machine.core.MachineCatalog;
import com.extremecraft.machine.material.OreMaterialCatalog;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class TechBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, ECConstants.MODID);

    public static final Map<String, RegistryObject<Block>> ORE_BLOCKS = new LinkedHashMap<>();
    public static final Map<String, RegistryObject<Block>> STORAGE_BLOCKS = new LinkedHashMap<>();
    public static final Map<String, RegistryObject<Block>> MACHINE_BLOCKS = new LinkedHashMap<>();
        public static final Map<String, RegistryObject<Block>> REACTOR_PART_BLOCKS = new LinkedHashMap<>();
    public static final Map<String, RegistryObject<Block>> CONTAMINATED_TERRAIN_BLOCKS = new LinkedHashMap<>();
    public static final Map<CableTier, RegistryObject<Block>> CABLE_BLOCKS = new LinkedHashMap<>();

    static {
        OreMaterialCatalog.MATERIALS.values().forEach(material -> {
            ORE_BLOCKS.put(material.id(), BLOCKS.register(oreBlockId(material.id()), () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .strength(3.0F + material.harvestLevel(), 6.0F + material.harvestLevel())
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops())));

            STORAGE_BLOCKS.put(material.id(), BLOCKS.register(material.id() + "_block", () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(4.5F + material.harvestLevel(), 8.0F + material.harvestLevel())
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops())));
        });

        MachineCatalog.DEFINITIONS.values().forEach(definition ->
                MACHINE_BLOCKS.put(definition.id(), BLOCKS.register(definition.id(), () -> new MachineBlock(BlockBehaviour.Properties.of()
                        .mapColor(MapColor.METAL)
                        .strength(4.0F, 8.0F)
                        .sound(SoundType.METAL)
                        .requiresCorrectToolForDrops())))
        );

        REACTOR_PART_BLOCKS.put("reactor_casing", BLOCKS.register("reactor_casing", () -> new Block(BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .strength(6.0F, 14.0F)
                .sound(SoundType.NETHERITE_BLOCK)
                .requiresCorrectToolForDrops())));
        REACTOR_PART_BLOCKS.put("reactor_window", BLOCKS.register("reactor_window", () -> new Block(BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_LIGHT_GRAY)
                .strength(4.0F, 10.0F)
                .sound(SoundType.GLASS)
                .requiresCorrectToolForDrops())));
        REACTOR_PART_BLOCKS.put("reactor_fuel_rod", BLOCKS.register("reactor_fuel_rod", () -> new Block(BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_GREEN)
                .strength(5.0F, 12.0F)
                .sound(SoundType.METAL)
                .requiresCorrectToolForDrops())));
        REACTOR_PART_BLOCKS.put("reactor_control_rod", BLOCKS.register("reactor_control_rod", () -> new Block(BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_GRAY)
                .strength(5.0F, 12.0F)
                .sound(SoundType.METAL)
                .requiresCorrectToolForDrops())));
        REACTOR_PART_BLOCKS.put("reactor_access_port", BLOCKS.register("reactor_access_port", () -> new Block(BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_BLACK)
                .strength(5.5F, 12.0F)
                .sound(SoundType.METAL)
                .requiresCorrectToolForDrops())));
        REACTOR_PART_BLOCKS.put("reactor_power_tap", BLOCKS.register("reactor_power_tap", () -> new Block(BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_BLUE)
                .strength(5.5F, 12.0F)
                .sound(SoundType.METAL)
                .requiresCorrectToolForDrops())));
        REACTOR_PART_BLOCKS.put("reactor_coolant_port", BLOCKS.register("reactor_coolant_port", () -> new Block(BlockBehaviour.Properties.of()
                .mapColor(MapColor.WATER)
                .strength(5.5F, 12.0F)
                .sound(SoundType.METAL)
                .requiresCorrectToolForDrops())));
        REACTOR_PART_BLOCKS.put("reactor_waste_port", BLOCKS.register("reactor_waste_port", () -> new Block(BlockBehaviour.Properties.of()
                .mapColor(MapColor.TERRACOTTA_GREEN)
                .strength(5.5F, 12.0F)
                .sound(SoundType.METAL)
                .requiresCorrectToolForDrops())));
        REACTOR_PART_BLOCKS.put("reactor_redstone_port", BLOCKS.register("reactor_redstone_port", () -> new Block(BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_RED)
                .strength(5.5F, 12.0F)
                .sound(SoundType.METAL)
                .requiresCorrectToolForDrops())));
        REACTOR_PART_BLOCKS.put("reactor_graphite_block", BLOCKS.register("reactor_graphite_block", () -> new Block(BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_BLACK)
                .strength(4.0F, 10.0F)
                .sound(SoundType.STONE)
                .requiresCorrectToolForDrops())));

        CONTAMINATED_TERRAIN_BLOCKS.put("contaminated_dirt", BLOCKS.register("contaminated_dirt",
                () -> new Block(BlockBehaviour.Properties.copy(Blocks.DIRT).mapColor(MapColor.TERRACOTTA_BROWN))));
        CONTAMINATED_TERRAIN_BLOCKS.put("contaminated_stone", BLOCKS.register("contaminated_stone",
                () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).mapColor(MapColor.COLOR_GRAY))));
        CONTAMINATED_TERRAIN_BLOCKS.put("contaminated_wood", BLOCKS.register("contaminated_wood",
                () -> new Block(BlockBehaviour.Properties.copy(Blocks.OAK_PLANKS).mapColor(MapColor.COLOR_BROWN))));
        CONTAMINATED_TERRAIN_BLOCKS.put("contaminated_sand", BLOCKS.register("contaminated_sand",
                () -> new Block(BlockBehaviour.Properties.copy(Blocks.SAND).mapColor(MapColor.SAND))));
        CONTAMINATED_TERRAIN_BLOCKS.put("contaminated_grass", BLOCKS.register("contaminated_grass",
                () -> new Block(BlockBehaviour.Properties.copy(Blocks.GRASS_BLOCK).mapColor(MapColor.GRASS))));
        CONTAMINATED_TERRAIN_BLOCKS.put("vitrified_fallout", BLOCKS.register("vitrified_fallout",
                () -> new Block(BlockBehaviour.Properties.copy(Blocks.TINTED_GLASS).mapColor(MapColor.COLOR_GRAY).requiresCorrectToolForDrops())));

        for (CableTier tier : CableTier.values()) {
            CABLE_BLOCKS.put(tier, BLOCKS.register(tier.id(), () -> new CableBlock(tier, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(1.5F, 3.0F)
                    .sound(SoundType.METAL)
                    .noOcclusion())));
        }
    }

    public static List<RegistryObject<Block>> machineBlocks() {
        return new ArrayList<>(MACHINE_BLOCKS.values());
    }

        public static List<RegistryObject<Block>> reactorPartBlocks() {
                return new ArrayList<>(REACTOR_PART_BLOCKS.values());
        }

    public static List<RegistryObject<Block>> cableBlocks() {
        return new ArrayList<>(CABLE_BLOCKS.values());
    }

    private static String oreBlockId(String materialId) {
        String normalized = materialId == null ? "" : materialId.trim().toLowerCase();
        return normalized.endsWith("_ore") ? normalized : normalized + "_ore";
    }

    private TechBlocks() {
    }
}
