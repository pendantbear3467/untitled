package com.extremecraft.progression;

import java.util.Arrays;
import java.util.Optional;

public enum PlayerClass {
    WARRIOR("warrior", 4.0D, 0.20D, 0.00D, 0.00D),
    SCIENTIST("scientist", 1.0D, 0.05D, 0.02D, 0.10D),
    MEDIC("medic", 1.0D, 0.10D, 0.02D, 0.20D),
    TRADER("trader", 0.5D, 0.05D, 0.01D, 1.50D),
    FIGHTER("fighter", 3.0D, 0.15D, 0.03D, 0.00D),
    MINER("miner", 1.5D, 0.10D, 0.00D, 0.20D),
    EXPLORER("explorer", 0.5D, 0.05D, 0.06D, 0.40D);

    private final String id;
    private final double flatAttackBonus;
    private final double healthBonusPct;
    private final double moveSpeedBonus;
    private final double luckBonus;

    PlayerClass(String id, double flatAttackBonus, double healthBonusPct, double moveSpeedBonus, double luckBonus) {
        this.id = id;
        this.flatAttackBonus = flatAttackBonus;
        this.healthBonusPct = healthBonusPct;
        this.moveSpeedBonus = moveSpeedBonus;
        this.luckBonus = luckBonus;
    }

    public String id() { return id; }
    public double flatAttackBonus() { return flatAttackBonus; }
    public double healthBonusPct() { return healthBonusPct; }
    public double moveSpeedBonus() { return moveSpeedBonus; }
    public double luckBonus() { return luckBonus; }

    public static Optional<PlayerClass> byId(String id) {
        return Arrays.stream(values()).filter(c -> c.id.equalsIgnoreCase(id)).findFirst();
    }
}
