package com.extremecraft.future.registry;

import com.extremecraft.core.ECConstants;
import com.extremecraft.item.armor.ECArmorMaterial;
import com.extremecraft.item.tool.ECToolTier;
import com.extremecraft.item.tool.HammerItem;
import com.extremecraft.item.tool.ModularDrillItem;
import com.extremecraft.machine.material.OreMaterialCatalog;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.SwordItem;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.LinkedHashMap;
import java.util.Map;

public final class TechItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, ECConstants.MODID);

    public static final Map<String, RegistryObject<Item>> RAW_ORES = new LinkedHashMap<>();
    public static final Map<String, RegistryObject<Item>> INGOTS = new LinkedHashMap<>();
    public static final Map<String, RegistryObject<Item>> DUSTS = new LinkedHashMap<>();
    public static final Map<String, RegistryObject<Item>> NUGGETS = new LinkedHashMap<>();

    static {
        OreMaterialCatalog.MATERIALS.values().forEach(material -> {
            RAW_ORES.put(material.id(), ITEMS.register("raw_" + material.id(), () -> new Item(new Item.Properties())));
            INGOTS.put(material.id(), ITEMS.register(material.id() + "_ingot", () -> new Item(new Item.Properties())));
            DUSTS.put(material.id(), ITEMS.register(material.id() + "_dust", () -> new Item(new Item.Properties())));
            NUGGETS.put(material.id(), ITEMS.register(material.id() + "_nugget", () -> new Item(new Item.Properties())));

            ITEMS.register(material.id() + "_ore", () -> new BlockItem(TechBlocks.ORE_BLOCKS.get(material.id()).get(), new Item.Properties()));
            ITEMS.register(material.id() + "_block", () -> new BlockItem(TechBlocks.STORAGE_BLOCKS.get(material.id()).get(), new Item.Properties()));

            if (material.hasTools()) {
                ECToolTier tier = ECToolTier.forMaterial(material.id(), material.harvestLevel());
                ITEMS.register(material.id() + "_pickaxe", () -> new PickaxeItem(tier, 1, -2.8F, new Item.Properties()));
                ITEMS.register(material.id() + "_sword", () -> new SwordItem(tier, 3, -2.4F, new Item.Properties()));
                ITEMS.register(material.id() + "_axe", () -> new AxeItem(tier, 5.0F, -3.0F, new Item.Properties()));
                ITEMS.register(material.id() + "_shovel", () -> new ShovelItem(tier, 1.5F, -3.0F, new Item.Properties()));
                ITEMS.register(material.id() + "_hammer", () -> new HammerItem(tier, 4, -3.1F, new Item.Properties()));
                ITEMS.register(material.id() + "_hoe", () -> new HoeItem(tier, -2, -1.0F, new Item.Properties()));
            }
        });

        TechBlocks.MACHINE_BLOCKS.forEach((id, block) -> ITEMS.register(id, () -> new BlockItem(block.get(), new Item.Properties())));
        TechBlocks.CABLE_BLOCKS.forEach((tier, block) -> ITEMS.register(tier.id(), () -> new BlockItem(block.get(), new Item.Properties())));

        registerArmorSet("copper", ECArmorMaterial.COPPER);
        registerArmorSet("titanium", ECArmorMaterial.TITANIUM);
        registerArmorSet("mythril", ECArmorMaterial.MYTHRIL);
        registerArmorSet("draconium", ECArmorMaterial.DRACONIUM);
        registerArmorSet("void", ECArmorMaterial.VOID);
        registerArmorSet("aether", ECArmorMaterial.AETHER);

        ITEMS.register("mana_crystal", () -> new Item(new Item.Properties()));
        ITEMS.register("arcane_dust", () -> new Item(new Item.Properties()));
        ITEMS.register("ancient_rune", () -> new Item(new Item.Properties()));
        ITEMS.register("void_essence", () -> new Item(new Item.Properties()));

        ITEMS.register("quantum_processor", () -> new Item(new Item.Properties()));
        ITEMS.register("rune_core", () -> new Item(new Item.Properties()));
        ITEMS.register("dimensional_core", () -> new Item(new Item.Properties()));

        ITEMS.register("infinity_ingot", () -> new Item(new Item.Properties().fireResistant()));
        ITEMS.register("singularity_core", () -> new Item(new Item.Properties().fireResistant()));
        ITEMS.register("celestial_engine", () -> new Item(new Item.Properties().fireResistant()));
        ITEMS.register("infinity_sword", () -> new SwordItem(ECToolTier.ENDGAME, 8, -2.2F, new Item.Properties().fireResistant()));
        ITEMS.register("quantum_pickaxe", () -> new PickaxeItem(ECToolTier.ENDGAME, 2, -2.6F, new Item.Properties().fireResistant()));
        ITEMS.register("modular_drill", () -> new ModularDrillItem(ECToolTier.ENDGAME, 2, -2.7F, new Item.Properties().fireResistant()));

        registerArmorSet("celestial", ECArmorMaterial.CELESTIAL);
    }

    private static void registerArmorSet(String id, ECArmorMaterial material) {
        ITEMS.register(id + "_helmet", () -> new ArmorItem(material, ArmorItem.Type.HELMET, new Item.Properties()));
        ITEMS.register(id + "_chestplate", () -> new ArmorItem(material, ArmorItem.Type.CHESTPLATE, new Item.Properties()));
        ITEMS.register(id + "_leggings", () -> new ArmorItem(material, ArmorItem.Type.LEGGINGS, new Item.Properties()));
        ITEMS.register(id + "_boots", () -> new ArmorItem(material, ArmorItem.Type.BOOTS, new Item.Properties()));
    }

    private TechItems() {
    }
}
