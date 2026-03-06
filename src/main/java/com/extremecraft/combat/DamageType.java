package com.extremecraft.combat;

import net.minecraft.world.damagesource.DamageSource;

public enum DamageType {
    PHYSICAL("physical_resistance"),
    MAGIC("magic_resistance"),
    FIRE("fire_resistance"),
    ICE("ice_resistance"),
    LIGHTNING("lightning_resistance"),
    POISON("poison_resistance"),
    VOID("void_resistance"),
    HOLY("holy_resistance"),
    BLEED("bleed_resistance");

    private final String resistanceKey;

    DamageType(String resistanceKey) {
        this.resistanceKey = resistanceKey;
    }

    public String resistanceKey() {
        return resistanceKey;
    }

    public static DamageType infer(DamageSource source) {
        if (source == null) {
            return PHYSICAL;
        }
        if (source.is(net.minecraft.tags.DamageTypeTags.IS_FIRE)) {
            return FIRE;
        }
        if (source.is(net.minecraft.tags.DamageTypeTags.WITCH_RESISTANT_TO)) {
            return MAGIC;
        }
        if (source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_ARMOR)) {
            return VOID;
        }
        return PHYSICAL;
    }
}
