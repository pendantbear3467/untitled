package com.extremecraft.skills;

import com.extremecraft.core.ECConstants;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class SkillsCapabilityEvents {
    public static final ResourceLocation ID = new ResourceLocation(ECConstants.MODID, "player_skills");

    @SubscribeEvent
    public void attachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            event.addCapability(ID, new PlayerSkillsProvider());
        }
    }

    @SubscribeEvent
    public void onPlayerClone(PlayerEvent.Clone event) {
        SkillsApi.get(event.getOriginal()).ifPresent(oldData ->
                SkillsApi.get(event.getEntity()).ifPresent(newData -> newData.copyFrom(oldData))
        );
    }
}
