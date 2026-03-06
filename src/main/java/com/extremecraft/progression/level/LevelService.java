package com.extremecraft.progression.level;

import com.extremecraft.network.ModNetwork;
import com.extremecraft.network.packet.SyncPlayerLevelS2CPacket;
import com.extremecraft.progression.PlayerStatsService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;

public final class LevelService {
    private static final String[] DEFAULT_ABILITY_SLOT_IDS = {
            "firebolt",
            "blink",
            "arcane_shield",
            "meteor"
    };

    private LevelService() {
    }

    public static int grantXp(ServerPlayer player, int amount) {
        if (player == null || amount <= 0) {
            return 0;
        }

        int[] levelUps = new int[]{0};
        PlayerLevelApi.get(player).ifPresent(levelData -> {
            levelUps[0] = levelData.grantXp(amount);
            sync(player, levelData);
        });

        // Keep existing progression stats in sync with new XP grants.
        PlayerStatsService.addExperience(player, amount);
        return levelUps[0];
    }

    public static void setLevel(ServerPlayer player, int level) {
        if (player == null) {
            return;
        }

        PlayerLevelApi.get(player).ifPresent(levelData -> {
            levelData.setLevel(level);
            sync(player, levelData);
        });
    }

    public static void sync(ServerPlayer player) {
        if (player == null) {
            return;
        }
        PlayerLevelApi.get(player).ifPresent(levelData -> sync(player, levelData));
    }

    public static void sync(ServerPlayer player, PlayerLevelCapability levelData) {
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SyncPlayerLevelS2CPacket(levelData.serializeNBT()));
    }

    public static int xpRequiredForLevel(int level) {
        return PlayerLevelCapability.xpRequired(level);
    }

    public static void grantAbility(ServerPlayer player, String abilityId) {
        // Ability unlock persistence is not yet backed by PlayerLevelCapability.
        // Keep this as a no-op compatibility hook for dev commands.
    }

    public static String abilityInSlot(ServerPlayer player, int slotIndex) {
        return defaultAbilityForSlot(slotIndex);
    }

    public static int skillPoints(ServerPlayer player) {
        return PlayerLevelApi.get(player).map(PlayerLevelCapability::skillPoints).orElse(0);
    }

    public static String defaultAbilityForSlot(int slotIndex) {
        if (slotIndex < 0 || slotIndex >= DEFAULT_ABILITY_SLOT_IDS.length) {
            return "";
        }
        return DEFAULT_ABILITY_SLOT_IDS[slotIndex];
    }
}
