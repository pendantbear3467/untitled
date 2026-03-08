package com.extremecraft.net;

import com.extremecraft.combat.dualwield.service.OffhandActionExecutor;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

public final class OffhandExecutor {

    /** Compatibility facade for legacy call sites. Canonical offhand combat lives in OffhandActionExecutor. */
    public static void attackEntityWithOffhand(ServerPlayer sp, Entity target) {
        OffhandActionExecutor.attackEntityWithLegacyCharge(sp, target);
    }
}
