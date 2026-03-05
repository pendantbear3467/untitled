package com.extremecraft.game;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.Locale;

public enum PlayerClass {
    WARRIOR("warrior", 0.30, 4.0, 0.00, 0.00),
    SCIENTIST("scientist", 0.10, 1.0, 0.02, 1.00),
    MEDICAL("medical", 0.35, 1.0, 0.00, 0.25),
    DOCTOR("doctor", 0.45, 1.5, 0.00, 0.20),
    TRADER("trader", 0.05, 0.5, 0.01, 2.00),
    FIGHTER("fighter", 0.20, 3.0, 0.03, 0.00),
    MINER("miner", 0.15, 1.5, 0.00, 0.50),
    EXPLORER("explorer", 0.10, 0.5, 0.06, 0.50);

    public final String id;
    public final double healthPctBonus;
    public final double flatAttackBonus;
    public final double speedBonus;
    public final double luckBonus;

    PlayerClass(String id, double healthPctBonus, double flatAttackBonus, double speedBonus, double luckBonus) {
        this.id = id;
        this.healthPctBonus = healthPctBonus;
        this.flatAttackBonus = flatAttackBonus;
        this.speedBonus = speedBonus;
        this.luckBonus = luckBonus;
    }

    public static PlayerClass fromId(String id) {
        if (id == null || id.isBlank()) return WARRIOR;
        String normalized = id.toLowerCase(Locale.ROOT).trim();
        for (PlayerClass c : values()) {
            if (c.id.equals(normalized)) return c;
        }
        return WARRIOR;
    }

    public Component asDisplay() {
        return Component.literal(id).withStyle(ChatFormatting.GOLD);
    }
}
