package com.extremecraft.radiation;

import com.extremecraft.config.ECFoundationConfig;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;

public final class RadiationService {
    private static final String TAG = "ec_radiation";

    private RadiationService() {
    }

    public static void tickPlayer(ServerPlayer player) {
        if (player == null || player.level().isClientSide || !ECFoundationConfig.isRadiationEnabled()) {
            return;
        }
        if ((player.tickCount % Math.max(5, ECFoundationConfig.radiationSampleIntervalTicks())) != 0) {
            return;
        }

        CompoundTag tag = player.getPersistentData().getCompound(TAG);
        double ambient = RadiationSourceService.sampleAmbient(player);
        double contamination = ChunkContaminationService.getContamination(player.serverLevel(), player.chunkPosition());
        double protection = RadiationProtectionService.protectionFactor(player);
        double effective = Math.max(0.0D, (ambient + (contamination / 100.0D)) * (1.0D - protection));

        double dose = tag.getDouble("dose");
        if (effective > 0.0D) {
            dose += effective;
            if (effective >= 1.0D) {
                ChunkContaminationService.addContamination(player.serverLevel(), player.chunkPosition(), effective * 0.15D);
            }
        } else {
            dose = Math.max(0.0D, dose - ChunkContaminationService.profile().doseDecayPerPulse());
        }

        tag.putDouble("ambient", ambient);
        tag.putDouble("contamination", contamination);
        tag.putDouble("dose", dose);
        player.getPersistentData().put(TAG, tag);
        applyDebuffs(player, dose, contamination);
    }

    public static void tickLevel(ServerLevel level) {
        if (level == null || level.isClientSide || !ECFoundationConfig.isRadiationEnabled()) {
            return;
        }
        ChunkContaminationService.tickLevel(level);
    }

    public static void releaseContamination(ServerLevel level, net.minecraft.core.BlockPos center, double amount, int radius) {
        if (level == null || amount <= 0.0D) {
            return;
        }
        ChunkContaminationService.releaseArea(level, center, amount, radius);
        ContaminationTerrainService.seedRelease(level, center, amount, radius);
    }

    public static void releaseMeltdown(ServerLevel level, net.minecraft.core.BlockPos center, double amount, int radius) {
        releaseContamination(level, center, amount, radius);
    }

    public static double ambientExposure(Player player) {
        return playerState(player).getDouble("ambient");
    }

    public static double accumulatedDose(Player player) {
        return playerState(player).getDouble("dose");
    }

    public static double contaminationPressure(Player player) {
        return playerState(player).getDouble("contamination");
    }

    public static CompoundTag playerState(Player player) {
        if (player == null) {
            return new CompoundTag();
        }
        return player.getPersistentData().getCompound(TAG).copy();
    }

    private static void applyDebuffs(ServerPlayer player, double dose, double contamination) {
        double threshold = ChunkContaminationService.profile().debuffThreshold();
        if (dose < threshold && contamination < threshold) {
            return;
        }

        player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, dose >= threshold * 2.0D ? 1 : 0, true, false));
        player.addEffect(new MobEffectInstance(MobEffects.POISON, 80, contamination >= threshold * 2.0D ? 1 : 0, true, false));
        if (dose >= threshold * 3.0D) {
            player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100, 0, true, false));
        }
    }
}
