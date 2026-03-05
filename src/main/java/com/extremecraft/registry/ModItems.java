package com.extremecraft.registry;

import com.extremecraft.core.ECConstants;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, ECConstants.MODID);

    public static final RegistryObject<Item> PULVERIZER = ITEMS.register("pulverizer", () -> new BlockItem(ModBlocks.PULVERIZER.get(), new Item.Properties()));
    public static final RegistryObject<Item> IRON_DUST = ITEMS.register("iron_dust", () -> new Item(new Item.Properties()));

    private ModItems() {}
}
