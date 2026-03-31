package com.extremecraft.generated.mythril_pack;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.LinkedHashMap;
import java.util.Map;

public final class GeneratedMachines {
    public static final DeferredRegister<Block> MACHINE_BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, GeneratedRegistries.MODID);
    public static final DeferredRegister<Item> MACHINE_ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, GeneratedRegistries.MODID);
    public static final Map<String, RegistryObject<Block>> ALL_MACHINES = new LinkedHashMap<>();

    static {
        // No machine definitions were generated.
    }

    private GeneratedMachines() {}

    public static void register(IEventBus modBus) {
        MACHINE_BLOCKS.register(modBus);
        MACHINE_ITEMS.register(modBus);
    }

    private static RegistryObject<Block> registerMachine(String id) {
        RegistryObject<Block> machine = MACHINE_BLOCKS.register(id, () -> new Block(BlockBehaviour.Properties.of().strength(4.0F, 8.0F)));
        MACHINE_ITEMS.register(id, () -> new BlockItem(machine.get(), new Item.Properties()));
        ALL_MACHINES.put(id, machine);
        return machine;
    }
}
