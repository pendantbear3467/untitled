package com.extremecraft.progression;

import com.extremecraft.progression.capability.ProgressApi;
import com.extremecraft.quest.QuestDefinition;
import com.extremecraft.quest.QuestManager;
import com.extremecraft.quest.QuestType;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class ProgressionEvents {
    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ProgressApi.get(player).ifPresent(data -> data.markAttributesDirty());
            ProgressionService.flushDirty(player);
        }
    }

    @SubscribeEvent
    public void onPlayerClone(PlayerEvent.Clone event) {
        if (!(event.getOriginal() instanceof ServerPlayer oldPlayer) || !(event.getEntity() instanceof ServerPlayer newPlayer)) {
            return;
        }

        ProgressApi.get(oldPlayer).ifPresent(oldData ->
                ProgressApi.get(newPlayer).ifPresent(newData -> {
                    newData.copyFrom(oldData);
                    ProgressionService.flushDirty(newPlayer);
                })
        );
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide) return;
        if (!(event.player instanceof ServerPlayer player)) return;

        ProgressionService.flushDirty(player);

        // Exploration XP source: discover new 256x256 regions.
        if (player.tickCount % 80 == 0) {
            int rx = player.blockPosition().getX() >> 8;
            int rz = player.blockPosition().getZ() >> 8;
            String regionKey = player.level().dimension().location() + "|" + rx + "|" + rz;
            ProgressApi.get(player).ifPresent(data -> {
                if (data.discoverRegion(regionKey)) {
                    data.addXp(8);
                    incrementQuest(player, QuestType.EXPLORATION, 1);
                    ProgressionService.flushDirty(player);
                }
            });
        }
    }

    @SubscribeEvent
    public void onKill(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide) return;
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) return;

        int xp = Math.max(5, (int) (event.getEntity().getMaxHealth() * 2.0D));
        ProgressionService.addXp(player, xp);

        incrementQuest(player, QuestType.KILL, 1);
        if (event.getEntity().getMaxHealth() >= 100.0F) {
            incrementQuest(player, QuestType.BOSS, 1);
        }
    }

    @SubscribeEvent
    public void onCraft(PlayerEvent.ItemCraftedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        int count = Math.max(1, event.getCrafting().getCount());
        ProgressionService.addXp(player, count * 2);
        incrementQuest(player, QuestType.CRAFTING, count);
    }

    @SubscribeEvent
    public void onPickup(EntityItemPickupEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        int count = Math.max(1, event.getItem().getItem().getCount());
        incrementQuest(player, QuestType.COLLECTION, count);
    }

    @SubscribeEvent
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getPlayer() instanceof ServerPlayer player) {
            incrementQuest(player, QuestType.COLLECTION, 1);
        }
    }

    private static void incrementQuest(ServerPlayer player, QuestType type, int amount) {
        if (amount <= 0) return;

        final boolean[] changed = {false};

        for (QuestDefinition quest : QuestManager.all()) {
            if (quest.type() != type) continue;

            ProgressApi.get(player).ifPresent(data -> {
                if (data.isQuestCompleted(quest.id())) return;
                int current = data.getQuestProgress(quest.id());
                int next = Math.min(quest.target(), current + amount);
                int delta = next - current;
                if (delta > 0) {
                    data.addQuestProgress(quest.id(), delta);
                    changed[0] = true;
                }
            });
        }

        if (changed[0]) {
            ProgressionService.flushDirty(player);
        }
    }
}
