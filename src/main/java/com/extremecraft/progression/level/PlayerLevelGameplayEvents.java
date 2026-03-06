package com.extremecraft.progression.level;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class PlayerLevelGameplayEvents {
    @SubscribeEvent
    public void onMobKill(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) {
            return;
        }

        LivingEntity target = event.getEntity();
        int xp = xpFor(target.getType());
        if (xp > 0) {
            LevelService.grantXp(player, xp);
        }
    }

    private int xpFor(EntityType<?> type) {
        if (type == EntityType.ZOMBIE) {
            return 5;
        }
        if (type == EntityType.SKELETON) {
            return 6;
        }
        if (type == EntityType.CREEPER) {
            return 8;
        }
        return 0;
    }
}
