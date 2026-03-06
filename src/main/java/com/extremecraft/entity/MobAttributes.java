package com.extremecraft.entity;

import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;

public final class MobAttributes {
    public static AttributeSupplier.Builder basic(float health, float damage, float speed, float armor) {
        return net.minecraft.world.entity.monster.Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, health)
                .add(Attributes.ATTACK_DAMAGE, damage)
                .add(Attributes.MOVEMENT_SPEED, speed)
                .add(Attributes.ARMOR, armor)
                .add(Attributes.FOLLOW_RANGE, 32.0D);
    }

    public static AttributeSupplier.Builder boss(float health, float damage, float speed, float armor, float toughness) {
        return basic(health, damage, speed, armor)
                .add(Attributes.ARMOR_TOUGHNESS, toughness)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.5D)
                .add(Attributes.ATTACK_KNOCKBACK, 1.0D);
    }

    private MobAttributes() {
    }
}
