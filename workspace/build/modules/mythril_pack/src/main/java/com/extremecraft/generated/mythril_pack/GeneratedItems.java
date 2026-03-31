package com.extremecraft.generated.mythril_pack;

import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.LinkedHashMap;
import java.util.Map;

public final class GeneratedItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, GeneratedRegistries.MODID);
    public static final Map<String, RegistryObject<Item>> ALL_ITEMS = new LinkedHashMap<>();
    public static final Map<String, RegistryObject<Item>> TOOLS = new LinkedHashMap<>();
    public static final Map<String, RegistryObject<Item>> ARMOR = new LinkedHashMap<>();

    static {
        // No item-like definitions were generated.
    }

    private GeneratedItems() {}

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
    }

    private static RegistryObject<Item> registerItem(String id) {
        RegistryObject<Item> entry = ITEMS.register(id, () -> new Item(new Item.Properties()));
        ALL_ITEMS.put(id, entry);
        return entry;
    }

    private static RegistryObject<Item> registerTool(String id) {
        RegistryObject<Item> entry = registerItem(id);
        TOOLS.put(id, entry);
        return entry;
    }

    private static RegistryObject<Item> registerArmor(String id) {
        RegistryObject<Item> entry = registerItem(id);
        ARMOR.put(id, entry);
        return entry;
    }
}
