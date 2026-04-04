package com.extremecraft.progression.capability;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.Optional;

public final class PlayerProgressCapabilityApi {
    private PlayerProgressCapabilityApi() {}

    public static Optional<PlayerProgressCapability> get(Player player) {
        return player.getCapability(PlayerProgressCapabilityProvider.PLAYER_PROGRESS_CAPABILITY).resolve();
    }

    public static void sync(ServerPlayer player) {
        get(player).ifPresent(data -> PlayerProgressCapabilityEvents.sync(player, data));
    }

    public static void updateClass(ServerPlayer player, String classId) {
        get(player).ifPresent(data -> {
            data.setPlayerClass(classId);
            sync(player);
        });
    }

    public static void updateLevel(ServerPlayer player, int level) {
        get(player).ifPresent(data -> {
            data.setPlayerLevel(level);
            sync(player);
        });
    }

    public static void updateSkillPoints(ServerPlayer player, int points) {
        get(player).ifPresent(data -> {
            data.setSkillPoints(points);
            sync(player);
        });
    }

    public static void setMagicUnlocked(ServerPlayer player, boolean unlocked) {
        get(player).ifPresent(data -> {
            data.setMagicUnlocked(unlocked);
            sync(player);
        });
    }

    public static void setDualWieldUnlocked(ServerPlayer player, boolean unlocked) {
        get(player).ifPresent(data -> {
            data.setDualWieldUnlocked(unlocked);
            sync(player);
        });
    }
}
