package com.extremecraft.registry;

import com.extremecraft.ExtremeCraftMod;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ECItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, ExtremeCraftMod.MODID);

    public static final RegistryObject<Item> PULVERIZER = register("pulverizer");
    public static final RegistryObject<Item> AUTO_FARMER = register("auto_farmer");
    public static final RegistryObject<Item> WINDMILL_TURBINE = register("windmill_turbine");

    public static final RegistryObject<Item> GENERATOR_TIER_1 = register("generator_tier_1");
    public static final RegistryObject<Item> GENERATOR_TIER_2 = register("generator_tier_2");
    public static final RegistryObject<Item> GENERATOR_TIER_3 = register("generator_tier_3");
    public static final RegistryObject<Item> GENERATOR_TIER_4 = register("generator_tier_4");
    public static final RegistryObject<Item> GENERATOR_TIER_5 = register("generator_tier_5");
    public static final RegistryObject<Item> GENERATOR_TIER_6 = register("generator_tier_6");
    public static final RegistryObject<Item> GENERATOR_TIER_7 = register("generator_tier_7");
    public static final RegistryObject<Item> GENERATOR_TIER_8 = register("generator_tier_8");
    public static final RegistryObject<Item> GENERATOR_TIER_9 = register("generator_tier_9");
    public static final RegistryObject<Item> GENERATOR_TIER_10 = register("generator_tier_10");
    public static final RegistryObject<Item> GENERATOR_TIER_11 = register("generator_tier_11");
    public static final RegistryObject<Item> GENERATOR_TIER_12 = register("generator_tier_12");

    public static final RegistryObject<Item> REACTOR_CORE = register("reactor_core");
    public static final RegistryObject<Item> COOLANT_INJECTOR = register("coolant_injector");
    public static final RegistryObject<Item> FUEL_FABRICATOR = register("fuel_fabricator");

    private static RegistryObject<Item> register(String id) {
        return ITEMS.register(id, () -> new Item(new Item.Properties()));
    }

    private ECItems() {}
}
