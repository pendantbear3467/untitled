package com.extremecraft.item.tool;

import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.Locale;

public record ECToolTier(
        int uses,
        float speed,
        float attackDamageBonus,
        int level,
        int enchantmentValue,
        Ingredient repairIngredient
) implements Tier {
    public static final ECToolTier ENDGAME = new ECToolTier(4096, 14.0F, 6.0F, 5, 24, Ingredient.of(ItemTags.create(net.minecraft.resources.ResourceLocation.tryParse("forge:ingots/draconium"))));

    public static ECToolTier forMaterial(String materialId, int harvestLevel) {
        String id = materialId.toLowerCase(Locale.ROOT);
        int uses = switch (harvestLevel) {
            case 1 -> 280;
            case 2 -> 450;
            case 3 -> 800;
            case 4 -> 1400;
            default -> 2200;
        };
        float speed = 5.0F + harvestLevel * 1.8F;
        float attack = 1.5F + harvestLevel * 0.9F;
        int enchant = 10 + harvestLevel * 2;
        Ingredient repair = Ingredient.of(ItemTags.create(net.minecraft.resources.ResourceLocation.tryParse("forge:ingots/" + id)));
        return new ECToolTier(uses, speed, attack, harvestLevel, enchant, repair);
    }

    @Override
    public int getUses() {
        return uses;
    }

    @Override
    public float getSpeed() {
        return speed;
    }

    @Override
    public float getAttackDamageBonus() {
        return attackDamageBonus;
    }

    @Override
    public int getLevel() {
        return level;
    }

    @Override
    public int getEnchantmentValue() {
        return enchantmentValue;
    }

    @Override
    public Ingredient getRepairIngredient() {
        return repairIngredient;
    }
}
