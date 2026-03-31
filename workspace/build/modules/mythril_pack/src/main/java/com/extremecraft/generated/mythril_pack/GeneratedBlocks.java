package com.extremecraft.generated.mythril_pack;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.LinkedHashMap;
import java.util.Map;

public final class GeneratedBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, GeneratedRegistries.MODID);
    public static final Map<String, RegistryObject<Block>> ALL_BLOCKS = new LinkedHashMap<>();

    static {
        // No block definitions were generated.
    }

    private GeneratedBlocks() {}

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
    }

    private static RegistryObject<Block> registerBlock(String id) {
        RegistryObject<Block> entry = BLOCKS.register(id, () -> new Block(BlockBehaviour.Properties.of().strength(3.0F, 6.0F)));
        ALL_BLOCKS.put(id, entry);
        return entry;
    }
}
