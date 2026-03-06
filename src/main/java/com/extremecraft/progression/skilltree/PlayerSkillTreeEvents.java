package com.extremecraft.progression.skilltree;

import com.extremecraft.core.ECConstants;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class PlayerSkillTreeEvents {
    public static final ResourceLocation ID = new ResourceLocation(ECConstants.MODID, "player_skill_tree");

    @SubscribeEvent
    public void attachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            event.addCapability(ID, new PlayerSkillDataProvider());
        }
    }

    @SubscribeEvent
    public void onPlayerClone(PlayerEvent.Clone event) {
        PlayerSkillTreeApi.get(event.getOriginal()).ifPresent(oldData ->
                PlayerSkillTreeApi.get(event.getEntity()).ifPresent(newData -> newData.copyFrom(oldData))
        );
    }

    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PlayerSkillTreeApi.get(player).ifPresent(data -> {
                data.markDirty();
                SkillTreeService.flushDirty(player);
            });
        }
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide) {
            return;
        }
        if (!(event.player instanceof ServerPlayer player)) {
            return;
        }

        // Batch sync and modifier refresh to reduce network overhead.
        int interval = 5;
        int offset = Math.floorMod(player.getUUID().hashCode(), interval);
        if (((player.tickCount + offset) % interval) == 0) {
            SkillTreeService.flushDirty(player);
        }
    }
}
