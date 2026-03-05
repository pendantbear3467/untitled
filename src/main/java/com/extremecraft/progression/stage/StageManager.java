package com.extremecraft.progression.stage;

import net.minecraft.world.entity.player.Player;

public final class StageManager {
    private StageManager() {}

    public static ProgressionStage getPlayerStage(Player player) {
        return StageApi.get(player).map(PlayerStageData::stage).orElse(ProgressionStage.PRIMITIVE);
    }

    public static void setPlayerStage(Player player, ProgressionStage stage) {
        StageApi.get(player).ifPresent(data -> data.setStage(stage));
    }

    public static boolean hasStage(Player player, ProgressionStage required) {
        return getPlayerStage(player).includes(required);
    }

    public static boolean upgradePlayerStage(Player player, ProgressionStage target) {
        if (target == null) {
            return false;
        }

        ProgressionStage current = getPlayerStage(player);
        if (current.includes(target)) {
            return false;
        }

        setPlayerStage(player, target);
        return true;
    }
}
