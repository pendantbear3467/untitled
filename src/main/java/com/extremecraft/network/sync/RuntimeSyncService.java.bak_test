package com.extremecraft.network.sync;

import com.extremecraft.ability.AbilityCooldownManager;
import com.extremecraft.machine.MachineBlockEntity;
import com.extremecraft.machine.sync.MachineStateSyncProvider;
import com.extremecraft.magic.mana.ManaService;
import com.extremecraft.network.ModNetwork;
import com.extremecraft.network.packet.AbilitySyncPacket;
import com.extremecraft.progression.StatCalculationEngine;
import com.extremecraft.progression.capability.PlayerStatsApi;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.PacketDistributor;

import java.util.Map;

public final class RuntimeSyncService {
    private RuntimeSyncService() {
    }

    public static void syncAll(ServerPlayer player) {
        syncStats(player);
        syncAbilities(player);
        syncSkillUnlocks(player);
        syncMachineStates(player);
        ManaService.sync(player);
    }

    public static void syncStats(ServerPlayer player) {
        StatCalculationEngine.PlayerStatSnapshot snapshot = StatCalculationEngine.calculate(player);
        CompoundTag payload = new CompoundTag();
        for (Map.Entry<String, Double> entry : snapshot.values().entrySet()) {
            payload.putDouble(entry.getKey(), entry.getValue());
        }

        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SyncRuntimeStatsS2CPacket(payload));
    }

    public static void syncAbilities(ServerPlayer player) {
        CompoundTag payload = AbilityCooldownManager.serializeFor(player);
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new AbilitySyncPacket(payload));
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SyncAbilityStateS2CPacket(payload));
    }

    public static void syncSkillUnlocks(ServerPlayer player) {
        CompoundTag payload = new CompoundTag();
        ListTag skills = new ListTag();
        PlayerStatsApi.get(player).ifPresent(stats -> {
            for (String unlocked : stats.unlockedSkillNodes()) {
                skills.add(StringTag.valueOf(unlocked));
            }
        });
        payload.put("skills", skills);
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SyncSkillUnlocksS2CPacket(payload));
    }

    public static void syncMachineStates(ServerPlayer player) {
        CompoundTag payload = new CompoundTag();
        CompoundTag machines = new CompoundTag();

        int horizontalRadius = 8;
        int verticalRadius = 4;
        int maxPerSync = 48;
        int included = 0;

        BlockPos center = player.blockPosition();
        BlockPos min = center.offset(-horizontalRadius, -verticalRadius, -horizontalRadius);
        BlockPos max = center.offset(horizontalRadius, verticalRadius, horizontalRadius);

        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            if (included >= maxPerSync) {
                break;
            }

            if (!player.level().isLoaded(pos)) {
                continue;
            }

            BlockEntity blockEntity = player.level().getBlockEntity(pos);
            if (blockEntity == null || blockEntity.isRemoved()) {
                continue;
            }

            CompoundTag machineState = extractMachineState(blockEntity);
            if (machineState.isEmpty()) {
                continue;
            }

            machines.put(Long.toString(pos.asLong()), machineState);
            included++;
        }

        payload.put("machines", machines);
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SyncMachineStateS2CPacket(payload));
    }

    private static CompoundTag extractMachineState(BlockEntity blockEntity) {
        if (blockEntity instanceof MachineStateSyncProvider provider) {
            return provider.machineSyncTag();
        }

        if (blockEntity instanceof MachineBlockEntity runtimeMachine) {
            return runtimeMachine.syncTag();
        }
        return new CompoundTag();
    }
}


