package com.extremecraft.registry;

import com.extremecraft.core.ECConstants;
import com.extremecraft.entity.ModEntities;
import com.extremecraft.modules.item.ArcaneStaffItem;
import com.extremecraft.modules.item.ChronoPickaxeItem;
import com.extremecraft.modules.item.GravitonHammerItem;
import com.extremecraft.modules.item.ModularMiningDrillItem;
import com.extremecraft.modules.item.PioneerArmorItem;
import com.extremecraft.modules.item.QuantumMultiToolItem;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, ECConstants.MODID);

    public static final RegistryObject<Item> PULVERIZER = ITEMS.register("pulverizer", () -> new BlockItem(ModBlocks.PULVERIZER.get(), new Item.Properties()));
    public static final RegistryObject<Item> IRON_DUST = ITEMS.register("iron_dust", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> BRONZE_DUST = ITEMS.register("bronze_dust", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> STEEL_DUST = ITEMS.register("steel_dust", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> BRONZE_INGOT = ITEMS.register("bronze_ingot", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> STEEL_INGOT = ITEMS.register("steel_ingot", () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> PIONEER_CHESTPLATE = ITEMS.register("pioneer_chestplate",
            () -> new PioneerArmorItem(ArmorItem.Type.CHESTPLATE, 2, new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> MODULAR_MINING_DRILL = ITEMS.register("modular_mining_drill",
            () -> new ModularMiningDrillItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> GRAVITON_HAMMER = ITEMS.register("graviton_hammer",
            () -> new GravitonHammerItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> ARCANE_STAFF = ITEMS.register("arcane_staff",
            () -> new ArcaneStaffItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> CHRONO_PICKAXE = ITEMS.register("chrono_pickaxe",
            () -> new ChronoPickaxeItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> QUANTUM_MULTI_TOOL = ITEMS.register("quantum_multi_tool",
            () -> new QuantumMultiToolItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> TECH_CONSTRUCT_SPAWN_EGG = registerSpawnEgg("tech_construct", ModEntities.TECH_CONSTRUCT, 0x67737D, 0xE09733);
    public static final RegistryObject<Item> ARCANE_WRAITH_SPAWN_EGG = registerSpawnEgg("arcane_wraith", ModEntities.ARCANE_WRAITH, 0x41215E, 0x6EE7F5);
    public static final RegistryObject<Item> VOID_STALKER_SPAWN_EGG = registerSpawnEgg("void_stalker", ModEntities.VOID_STALKER, 0x1A1424, 0xA03CFF);
    public static final RegistryObject<Item> ANCIENT_SENTINEL_SPAWN_EGG = registerSpawnEgg("ancient_sentinel", ModEntities.ANCIENT_SENTINEL, 0x8B7A63, 0x3AC7B2);
    public static final RegistryObject<Item> ENERGY_PARASITE_SPAWN_EGG = registerSpawnEgg("energy_parasite", ModEntities.ENERGY_PARASITE, 0x3AEE9D, 0x1B7C86);
    public static final RegistryObject<Item> RUNIC_GOLEM_SPAWN_EGG = registerSpawnEgg("runic_golem", ModEntities.RUNIC_GOLEM, 0x6D6A78, 0x4CC6FF);
    public static final RegistryObject<Item> ANCIENT_CORE_GUARDIAN_SPAWN_EGG = registerSpawnEgg("ancient_core_guardian", ModEntities.ANCIENT_CORE_GUARDIAN, 0x4F423A, 0xD45C44);
    public static final RegistryObject<Item> VOID_TITAN_SPAWN_EGG = registerSpawnEgg("void_titan", ModEntities.VOID_TITAN, 0x191326, 0xC44DFF);
    public static final RegistryObject<Item> OVERCHARGED_MACHINE_GOD_SPAWN_EGG = registerSpawnEgg("overcharged_machine_god", ModEntities.OVERCHARGED_MACHINE_GOD, 0x33404A, 0xF4D03F);

        private static <T extends Mob> RegistryObject<Item> registerSpawnEgg(String id, Supplier<? extends EntityType<T>> entityType, int primaryColor, int secondaryColor) {
        return ITEMS.register(id + "_spawn_egg", () -> new ForgeSpawnEggItem(entityType, primaryColor, secondaryColor, new Item.Properties()));
    }

    private ModItems() {
    }
}
