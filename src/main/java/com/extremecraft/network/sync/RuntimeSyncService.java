package com.extremecraft.network.sync;

import com.extremecraft.ability.AbilityCooldownManager;
import com.extremecraft.machine.MachineBlockEntity;
import com.extremecraft.machine.sync.MachineStateSyncProvider;
import com.extremecraft.machine.sync.MachineSyncIndex;
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

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class RuntimeSyncService {
    private static final Map<UUID, Integer> LAST_STATS_HASH = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> LAST_ABILITIES_HASH = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> LAST_SKILLS_HASH = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> LAST_MACHINES_HASH = new ConcurrentHashMap<>();

    private RuntimeSyncService() {
    }

    public static void syncAll(ServerPlayer player) {
        syncStats(player, true);
        syncAbilities(player, true);
        syncSkillUnlocks(player, true);
        syncMachineStates(player, true);
        ManaService.sync(player);
    }

    public static void syncStats(ServerPlayer player) {
        syncStats(player, false);
    }

    public static void syncAbilities(ServerPlayer player) {
        syncAbilities(player, false);
    }

    public static void syncSkillUnlocks(ServerPlayer player) {
        syncSkillUnlocks(player, false);
    }

    public static void syncMachineStates(ServerPlayer player) {
        syncMachineStates(player, false);
    }

    public static void clearPlayer(ServerPlayer player) {
        if (player == null) {
            return;
        }

        UUID playerId = player.getUUID();
        LAST_STATS_HASH.remove(playerId);
        LAST_ABILITIES_HASH.remove(playerId);
        LAST_SKILLS_HASH.remove(playerId);
        LAST_MACHINES_HASH.remove(playerId);
    }

    private static void syncStats(ServerPlayer player, boolean force) {
        StatCalculationEngine.PlayerStatSnapshot snapshot = StatCalculationEngine.calculate(player);
        CompoundTag payload = new CompoundTag();
        for (Map.Entry<String, Double> entry : snapshot.values().entrySet()) {
            payload.putDouble(entry.getKey(), entry.getValue());
        }

        if (!force && !shouldSend(player, payload, LAST_STATS_HASH)) {
            return;
        }

        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SyncRuntimeStatsS2CPacket(payload));
    }

    private static void syncAbilities(ServerPlayer player, boolean force) {
        CompoundTag payload = AbilityCooldownManager.serializeFor(player);

        if (!force && !shouldSend(player, payload, LAST_ABILITIES_HASH)) {
            return;
        }

        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new AbilitySyncPacket(payload));
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SyncAbilityStateS2CPacket(payload));
    }

    private static void syncSkillUnlocks(ServerPlayer player, boolean force) {
        CompoundTag payload = new CompoundTag();
        ListTag skills = new ListTag();
        PlayerStatsApi.get(player).ifPresent(stats -> {
            for (String unlocked : stats.unlockedSkillNodes()) {
                skills.add(StringTag.valueOf(unlocked));
            }
        });
        payload.put("skills", skills);

        if (!force && !shouldSend(player, payload, LAST_SKILLS_HASH)) {
            return;
        }

        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SyncSkillUnlocksS2CPacket(payload));
    }

    private static void syncMachineStates(ServerPlayer player, boolean force) {
        CompoundTag payload = new CompoundTag();
        CompoundTag machines = new CompoundTag();

        int horizontalRadius = 8;
        int verticalRadius = 4;
        int maxPerSync = 48;

        List<BlockPos> nearby = MachineSyncIndex.collectNearby(player, horizontalRadius, verticalRadius, maxPerSync);
        for (BlockPos pos : nearby) {
            BlockEntity blockEntity = player.level().getBlockEntity(pos);
            if (blockEntity == null || blockEntity.isRemoved()) {
                continue;
            }

            CompoundTag machineState = extractMachineState(blockEntity);
            if (machineState.isEmpty()) {
                continue;
            }

            machines.put(Long.toString(pos.asLong()), machineState);
        }

        payload.put("machines", machines);

        if (!force && !shouldSend(player, payload, LAST_MACHINES_HASH)) {
            return;
        }

        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SyncMachineStateS2CPacket(payload));
    }

    private static boolean shouldSend(ServerPlayer player, CompoundTag payload, Map<UUID, Integer> hashCache) {
        int nextHash = payload.hashCode();
        UUID playerId = player.getUUID();
        Integer previousHash = hashCache.put(playerId, nextHash);
        return previousHash == null || previousHash != nextHash;
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

