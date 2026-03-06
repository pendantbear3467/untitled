package com.extremecraft.server.task;

import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.NavigableMap;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * Small deferred work queue for server-thread tasks that can be spread across ticks.
 */
public final class ServerDeferredWorkQueue {
    private static final NavigableMap<Long, Queue<Runnable>> TASKS = new ConcurrentSkipListMap<>();

    private ServerDeferredWorkQueue() {
    }

    public static void schedule(ServerPlayer player, int delayTicks, Runnable task) {
        if (player == null || task == null) {
            return;
        }

        long runAt = player.serverLevel().getGameTime() + Math.max(0L, delayTicks);
        TASKS.computeIfAbsent(runAt, ignored -> new ConcurrentLinkedQueue<>()).add(task);
    }

    public static void tick(long gameTime, int maxTasks) {
        int budget = Math.max(1, maxTasks);

        while (budget > 0) {
            Map.Entry<Long, Queue<Runnable>> next = TASKS.firstEntry();
            if (next == null || next.getKey() > gameTime) {
                break;
            }

            Queue<Runnable> queue = next.getValue();
            Runnable task = queue.poll();
            if (task != null) {
                try {
                    task.run();
                } catch (Exception ignored) {
                    // Individual task failures are isolated to avoid starving the queue.
                }
                budget--;
            }

            if (queue.isEmpty()) {
                TASKS.remove(next.getKey(), queue);
            }
        }
    }

    public static void clear() {
        TASKS.clear();
    }
}
