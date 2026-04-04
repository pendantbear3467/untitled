package com.extremecraft.progression;

import com.extremecraft.progression.capability.ProgressApi;
import com.extremecraft.progression.classsystem.ability.ClassAbilityService;
import com.extremecraft.quest.QuestDefinition;
import com.extremecraft.quest.QuestManager;
import com.extremecraft.quest.QuestType;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Live gameplay event hooks for progression, quest progress, and gameplay-earned skill XP.
 *
 * <p>Keep class XP out of this event surface; live class XP comes from guild quest claim flow
 * through {@link GuildQuestRewardService}.</p>
 */
public class ProgressionEvents {
    /**
     * Rehydrates synced progression state when player joins and pushes dependent mirrors.
     */
    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ProgressApi.get(player).ifPresent(data -> data.markAttributesDirty());
            ProgressionSyncService.flush(player);
            PlayerStatsService.syncProgressionMirror(player, true);
            ClassAbilityService.syncState(player);
        }
    }

    /**
     * Copies progression capability data during respawn/dimension transfer clone flows.
     */
    @SubscribeEvent
    public void onPlayerClone(PlayerEvent.Clone event) {
        if (!(event.getOriginal() instanceof ServerPlayer oldPlayer) || !(event.getEntity() instanceof ServerPlayer newPlayer)) {
            return;
        }

        ProgressApi.get(oldPlayer).ifPresent(oldData ->
                ProgressApi.get(newPlayer).ifPresent(newData -> {
                    newData.copyFrom(oldData);
                    ProgressionSyncService.flush(newPlayer);
                })
        );
    }

    /**
     * Per-tick progression flush and exploration-based progression grants.
     */
    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide) return;
        if (!(event.player instanceof ServerPlayer player)) return;

        ProgressionSyncService.flush(player);

        int regionTickInterval = 80;
        int regionTickOffset = Math.floorMod(player.getUUID().hashCode(), regionTickInterval);
        if (((player.tickCount + regionTickOffset) % regionTickInterval) == 0) {
            int rx = player.blockPosition().getX() >> 8;
            int rz = player.blockPosition().getZ() >> 8;
            // Region key is coarse-grained to prevent per-chunk progression spam.
            String regionKey = player.level().dimension().location() + "|" + rx + "|" + rz;
            if (ProgressionFacade.discoverRegion(player, regionKey)) {
                ProgressionFacade.grantPlayerXp(player, 8);
                incrementQuest(player, QuestType.EXPLORATION, 1);
                ProgressionSyncService.flush(player);
            }
        }
    }

    /**
     * Kill rewards feed player XP, combat skill XP, and matching quest counters.
     */
    @SubscribeEvent
    public void onKill(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide) return;
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) return;

        int xp = Math.max(5, (int) (event.getEntity().getMaxHealth() * 2.0D));
        ProgressionFacade.grantPlayerXp(player, xp);
        ProgressionFacade.grantCombatSkillXp(player, event.getEntity());

        incrementQuest(player, QuestType.KILL, 1);
        if (event.getEntity().getMaxHealth() >= 100.0F) {
            incrementQuest(player, QuestType.BOSS, 1);
        }
    }

    /**
     * Crafting rewards grant progression XP and skill XP based on crafted item domain.
     */
    @SubscribeEvent
    public void onCraft(PlayerEvent.ItemCraftedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        int count = Math.max(1, event.getCrafting().getCount());
        ProgressionFacade.grantPlayerXp(player, count * 2);
        incrementQuest(player, QuestType.CRAFTING, count);

        ItemStack crafted = event.getCrafting();
        String itemId = crafted.getItem().builtInRegistryHolder().key().location().getPath();
        if (itemId.contains("rune") || itemId.contains("mana") || itemId.contains("arcane")
                || itemId.contains("machine") || itemId.contains("generator") || itemId.contains("reactor") || itemId.contains("cable")) {
            // Domain-themed crafting remains quest/player-XP relevant but does not directly grant skill XP.
        }
    }

    /**
     * Pickup contributes to collection quests.
     */
    @SubscribeEvent
    public void onPickup(EntityItemPickupEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        int count = Math.max(1, event.getItem().getItem().getCount());
        incrementQuest(player, QuestType.COLLECTION, count);
    }

    /**
     * Block breaking contributes baseline XP and mining skill progression.
     */
    @SubscribeEvent
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getPlayer() instanceof ServerPlayer player) {
            int xp = event.getState().getDestroySpeed(player.level(), event.getPos()) > 5.0F ? 2 : 1;
            ProgressionFacade.grantPlayerXp(player, xp);
            incrementQuest(player, QuestType.COLLECTION, 1);
        }
    }

    /**
     * Safely increments all matching active quests by capped delta and flushes once.
     */
    private static void incrementQuest(ServerPlayer player, QuestType type, int amount) {
        if (amount <= 0) return;

        final boolean[] changed = {false};

        for (QuestDefinition quest : QuestManager.all()) {
            if (quest.type() != type) continue;
            if (ProgressionService.isQuestCompleted(player, quest.id())) continue;

            int current = ProgressionService.getQuestProgress(player, quest.id());
            int next = Math.min(quest.target(), current + amount);
            int delta = next - current;
            if (delta > 0) {
                changed[0] |= ProgressionFacade.addQuestProgress(player, quest.id(), delta);
            }
        }

        if (changed[0]) {
            ProgressionSyncService.flush(player);
        }
    }
}
