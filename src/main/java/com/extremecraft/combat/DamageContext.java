package com.extremecraft.combat;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.EnumMap;
import java.util.Map;

public final class DamageContext {
    private final LivingEntity attacker;
    private final LivingEntity target;
    private float damageAmount;
    private DamageType damageType;
    private String abilitySource;
    private ItemStack weaponSource;
    private float criticalChance;
    private float criticalMultiplier;
    private float armorValue;
    private final EnumMap<DamageType, Float> resistances;
    private final DamageModifiers modifiers;

    private DamageContext(Builder builder) {
        this.attacker = builder.attacker;
        this.target = builder.target;
        this.damageAmount = Math.max(0.0F, builder.damageAmount);
        this.damageType = builder.damageType == null ? DamageType.PHYSICAL : builder.damageType;
        this.abilitySource = builder.abilitySource == null ? "" : builder.abilitySource;
        this.weaponSource = builder.weaponSource == null ? ItemStack.EMPTY : builder.weaponSource;
        this.criticalChance = Math.max(0.0F, builder.criticalChance);
        this.criticalMultiplier = Math.max(1.0F, builder.criticalMultiplier);
        this.armorValue = Math.max(0.0F, builder.armorValue);
        this.resistances = new EnumMap<>(DamageType.class);
        this.resistances.putAll(builder.resistances);
        this.modifiers = builder.modifiers == null ? new DamageModifiers() : builder.modifiers;
    }

    public LivingEntity attacker() {
        return attacker;
    }

    public LivingEntity target() {
        return target;
    }

    public float damageAmount() {
        return damageAmount;
    }

    public void setDamageAmount(float damageAmount) {
        this.damageAmount = Math.max(0.0F, damageAmount);
    }

    public DamageType damageType() {
        return damageType;
    }

    public void setDamageType(DamageType damageType) {
        this.damageType = damageType == null ? DamageType.PHYSICAL : damageType;
    }

    public String abilitySource() {
        return abilitySource;
    }

    public void setAbilitySource(String abilitySource) {
        this.abilitySource = abilitySource == null ? "" : abilitySource;
    }

    public ItemStack weaponSource() {
        return weaponSource;
    }

    public void setWeaponSource(ItemStack weaponSource) {
        this.weaponSource = weaponSource == null ? ItemStack.EMPTY : weaponSource;
    }

    public float criticalChance() {
        return criticalChance;
    }

    public void setCriticalChance(float criticalChance) {
        this.criticalChance = Math.max(0.0F, criticalChance);
    }

    public float criticalMultiplier() {
        return criticalMultiplier;
    }

    public void setCriticalMultiplier(float criticalMultiplier) {
        this.criticalMultiplier = Math.max(1.0F, criticalMultiplier);
    }

    public float armorValue() {
        return armorValue;
    }

    public void setArmorValue(float armorValue) {
        this.armorValue = Math.max(0.0F, armorValue);
    }

    public float resistance(DamageType type) {
        return clampResistance(resistances.getOrDefault(type, 0.0F));
    }

    public Map<DamageType, Float> resistances() {
        return resistances;
    }

    public void setResistance(DamageType type, float value) {
        if (type == null) {
            return;
        }
        resistances.put(type, clampResistance(value));
    }

    public DamageModifiers modifiers() {
        return modifiers;
    }

    private static float clampResistance(float value) {
        return Math.max(0.0F, Math.min(1.0F, value));
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private LivingEntity attacker;
        private LivingEntity target;
        private float damageAmount;
        private DamageType damageType = DamageType.PHYSICAL;
        private String abilitySource = "";
        private ItemStack weaponSource = ItemStack.EMPTY;
        private float criticalChance = 0.0F;
        private float criticalMultiplier = 1.5F;
        private float armorValue = 0.0F;
        private final EnumMap<DamageType, Float> resistances = new EnumMap<>(DamageType.class);
        private DamageModifiers modifiers = new DamageModifiers();

        private Builder() {
        }

        public Builder attacker(LivingEntity attacker) {
            this.attacker = attacker;
            return this;
        }

        public Builder target(LivingEntity target) {
            this.target = target;
            return this;
        }

        public Builder damageAmount(float damageAmount) {
            this.damageAmount = damageAmount;
            return this;
        }

        public Builder damageType(DamageType damageType) {
            this.damageType = damageType;
            return this;
        }

        public Builder abilitySource(String abilitySource) {
            this.abilitySource = abilitySource;
            return this;
        }

        public Builder weaponSource(ItemStack weaponSource) {
            this.weaponSource = weaponSource;
            return this;
        }

        public Builder criticalChance(float criticalChance) {
            this.criticalChance = criticalChance;
            return this;
        }

        public Builder criticalMultiplier(float criticalMultiplier) {
            this.criticalMultiplier = criticalMultiplier;
            return this;
        }

        public Builder armorValue(float armorValue) {
            this.armorValue = armorValue;
            return this;
        }

        public Builder resistance(DamageType type, float value) {
            if (type != null) {
                this.resistances.put(type, clampResistance(value));
            }
            return this;
        }

        public Builder modifiers(DamageModifiers modifiers) {
            this.modifiers = modifiers;
            return this;
        }

        public DamageContext build() {
            return new DamageContext(this);
        }
    }
}
