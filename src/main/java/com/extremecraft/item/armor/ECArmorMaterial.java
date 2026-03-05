package com.extremecraft.item.armor;

import com.extremecraft.core.ECConstants;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.EnumMap;

public enum ECArmorMaterial implements ArmorMaterial {
    COPPER("copper", 14, map(2, 5, 4, 2), 12, 0.0F, 0.0F),
    TITANIUM("titanium", 24, map(3, 7, 6, 3), 16, 1.0F, 0.0F),
    MYTHRIL("mythril", 28, map(3, 8, 6, 3), 20, 2.0F, 0.05F),
    DRACONIUM("draconium", 35, map(4, 9, 7, 4), 24, 3.5F, 0.1F),
    VOID("void", 38, map(4, 10, 8, 4), 26, 4.0F, 0.12F),
    AETHER("aether", 34, map(3, 8, 7, 3), 24, 3.0F, 0.10F),
    CELESTIAL("celestial", 42, map(5, 11, 9, 5), 30, 5.0F, 0.15F);

    private final String id;
    private final int durabilityMultiplier;
    private final EnumMap<ArmorItem.Type, Integer> defenses;
    private final int enchantmentValue;
    private final float toughness;
    private final float knockbackResistance;

    ECArmorMaterial(String id, int durabilityMultiplier, EnumMap<ArmorItem.Type, Integer> defenses, int enchantmentValue, float toughness, float knockbackResistance) {
        this.id = id;
        this.durabilityMultiplier = durabilityMultiplier;
        this.defenses = defenses;
        this.enchantmentValue = enchantmentValue;
        this.toughness = toughness;
        this.knockbackResistance = knockbackResistance;
    }

    @Override
    public int getDurabilityForType(ArmorItem.Type type) {
        int base = switch (type) {
            case HELMET -> 11;
            case CHESTPLATE -> 16;
            case LEGGINGS -> 15;
            case BOOTS -> 13;
        };
        return base * durabilityMultiplier;
    }

    @Override
    public int getDefenseForType(ArmorItem.Type type) {
        return defenses.getOrDefault(type, 0);
    }

    @Override
    public int getEnchantmentValue() {
        return enchantmentValue;
    }

    @Override
    public SoundEvent getEquipSound() {
        return SoundEvents.ARMOR_EQUIP_NETHERITE;
    }

    @Override
    public Ingredient getRepairIngredient() {
        return Ingredient.of(ItemTags.create(new ResourceLocation("forge", "ingots/" + id)));
    }

    @Override
    public String getName() {
        return ECConstants.MODID + ":" + id;
    }

    @Override
    public float getToughness() {
        return toughness;
    }

    @Override
    public float getKnockbackResistance() {
        return knockbackResistance;
    }

    private static EnumMap<ArmorItem.Type, Integer> map(int boots, int chest, int legs, int helmet) {
        EnumMap<ArmorItem.Type, Integer> values = new EnumMap<>(ArmorItem.Type.class);
        values.put(ArmorItem.Type.BOOTS, boots);
        values.put(ArmorItem.Type.CHESTPLATE, chest);
        values.put(ArmorItem.Type.LEGGINGS, legs);
        values.put(ArmorItem.Type.HELMET, helmet);
        return values;
    }
}
