package com.extremecraft.network.security;

import net.minecraft.server.level.ServerPlayer;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lightweight per-player packet limiter used by C2S handlers.
 * <p>
 * This runs on the server thread today, but uses concurrent collections so
 * packet guards remain safe if decoding/execution flow becomes more parallel later.
 */
public final class ServerPacketLimiter {
    private static final Map<UUID, Map<String, Counter>> COUNTERS = new ConcurrentHashMap<>();

    private ServerPacketLimiter() {
    }

    public static boolean allow(ServerPlayer player, String key, int minTickDelta, int maxPerWindow, int windowTicks) {
        if (player == null) {
            return false;
        }

        String normalizedKey = normalizeKey(key);
        if (normalizedKey.isBlank()) {
            return false;
        }

        long now = player.serverLevel().getGameTime();
        int requiredDelta = Math.max(0, minTickDelta);
        int maxRequests = Math.max(1, maxPerWindow);
        int window = Math.max(1, windowTicks);

        Map<String, Counter> playerCounters = COUNTERS.computeIfAbsent(player.getUUID(), ignored -> new ConcurrentHashMap<>());
        Counter counter = playerCounters.computeIfAbsent(normalizedKey, ignored -> new Counter());

        synchronized (counter) {
            if ((now - counter.lastAcceptedTick) < requiredDelta) {
                return false;
            }

            if ((now - counter.windowStartTick) >= window) {
                counter.windowStartTick = now;
                counter.usedInWindow = 0;
            }

            if (counter.usedInWindow >= maxRequests) {
                return false;
            }

            counter.usedInWindow++;
            counter.lastAcceptedTick = now;
            return true;
        }
    }

    public static void clearPlayer(ServerPlayer player) {
        if (player != null) {
            COUNTERS.remove(player.getUUID());
        }
    }

    public static void clearPlayer(UUID playerId) {
        if (playerId != null) {
            COUNTERS.remove(playerId);
        }
    }

    private static String normalizeKey(String key) {
        return key == null ? "" : key.trim().toLowerCase(Locale.ROOT);
    }

    private static final class Counter {
        private long lastAcceptedTick = Long.MIN_VALUE / 2L;
        private long windowStartTick = Long.MIN_VALUE / 2L;
        private int usedInWindow;
    }
}
