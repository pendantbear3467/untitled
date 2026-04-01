package com.extremecraft.progression.stage;

import com.extremecraft.network.sync.RuntimeSyncService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * Canonical runtime owner for player stage capability reads/writes.
 *
 * <p>Server-side stage grants should flow through this manager or {@code ProgressionGate}. Client
 * mirrors are sync-only and update from here; they must not become mutation authorities.</p>
 */
public final class StageManager {
    private StageManager() {}

    public static ProgressionStage getPlayerStage(Player player) {
        return StageApi.get(player).map(PlayerStageData::stage).orElse(ProgressionStage.PRIMITIVE);
    }

    public static void setPlayerStage(Player player, ProgressionStage stage) {
        StageApi.get(player).ifPresent(data -> data.setStage(stage));
        if (player instanceof ServerPlayer serverPlayer) {
            RuntimeSyncService.syncStageState(serverPlayer);
        }
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
