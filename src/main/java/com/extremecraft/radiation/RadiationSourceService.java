package com.extremecraft.radiation;

import com.extremecraft.config.ECFoundationConfig;
import com.extremecraft.platform.data.definition.RadiationSourceDefinition;
import com.extremecraft.platform.data.registry.RadiationSourceDataRegistry;
import com.extremecraft.reactor.ReactorControlService;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class RadiationSourceService {
    private RadiationSourceService() {
    }

    public static double sampleAmbient(ServerPlayer player) {
        if (player == null || !(player.level() instanceof ServerLevel level)) {
            return 0.0D;
        }

        return sampleBlocks(level, player.blockPosition())
                + sampleInventory(player)
                + ReactorControlService.sampleAmbientRadiation(level, player.blockPosition(), 12);
    }

    private static double sampleBlocks(ServerLevel level, BlockPos center) {
        List<RadiationSourceDefinition> sources = activeSources();
        int maxBlocks = Math.max(8, ECFoundationConfig.radiationMaxSampleBlocks());
        int sampleRadius = Math.min(6, Math.max(2, (int) Math.sqrt(maxBlocks / 4.0D)));
        double total = 0.0D;
        int scanned = 0;
        int radiusSq = sampleRadius * sampleRadius;

        for (BlockPos target : BlockPos.betweenClosed(center.offset(-sampleRadius, -sampleRadius, -sampleRadius), center.offset(sampleRadius, sampleRadius, sampleRadius))) {
            if (scanned++ >= maxBlocks) {
                break;
            }
            if (center.distSqr(target) > radiusSq) {
                continue;
            }

            String blockId = BuiltInRegistries.BLOCK.getKey(level.getBlockState(target).getBlock()).toString();
            for (RadiationSourceDefinition source : sources) {
                if (!"block".equals(source.targetType()) || !source.target().equals(blockId)) {
                    continue;
                }
                double distance = Math.max(1.0D, Math.sqrt(center.distSqr(target)));
                total += source.intensity() / distance;
            }
        }

        return total;
    }

    private static double sampleInventory(ServerPlayer player) {
        double total = 0.0D;
        for (ItemStack stack : player.getInventory().items) {
            total += sampleStack(stack);
        }
        for (ItemStack stack : player.getInventory().offhand) {
            total += sampleStack(stack);
        }
        for (ItemStack stack : player.getArmorSlots()) {
            total += sampleStack(stack);
        }
        return total;
    }

    private static double sampleStack(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return 0.0D;
        }
        String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        if (itemId.contains("uranium")) {
            return 0.12D * stack.getCount();
        }
        if (itemId.contains("dirty_bomb") || itemId.contains("tactical_nuke") || itemId.contains("reactor_core")) {
            return 1.5D;
        }
        for (RadiationSourceDefinition source : activeSources()) {
            if ("item".equals(source.targetType()) && source.target().equals(itemId)) {
                return source.intensity() * stack.getCount();
            }
        }
        return 0.0D;
    }

    private static List<RadiationSourceDefinition> activeSources() {
        List<RadiationSourceDefinition> loaded = new ArrayList<>(RadiationSourceDataRegistry.registry().all());
        if (!loaded.isEmpty()) {
            return loaded;
        }
        return List.of(
                new RadiationSourceDefinition("uranium_ore", "block", "extremecraft:uranium_ore", 0.8D, 5, 0.3D, 0.0D),
                new RadiationSourceDefinition("uranium_block", "block", "extremecraft:uranium_block", 1.2D, 5, 0.45D, 0.0D),
                new RadiationSourceDefinition("dirty_bomb_cloud", "event", "extremecraft:dirty_bomb_cloud", 6.5D, 10, 8.0D, 0.8D)
        );
    }
}
