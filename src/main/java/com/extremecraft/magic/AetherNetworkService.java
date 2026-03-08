package com.extremecraft.magic;

import com.extremecraft.config.ECFoundationConfig;
import com.extremecraft.machine.core.MachineDefinition;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;

public final class AetherNetworkService {
    private static final String DATA_NAME = "extremecraft_aether_network";

    private AetherNetworkService() {
    }

    public static boolean canConsume(ServerLevel level, net.minecraft.core.BlockPos pos, int amount) {
        if (level == null || amount <= 0) {
            return true;
        }
        AetherState state = state(level);
        long key = new ChunkPos(pos).toLong();
        RechargeEntry entry = state.entry(key, level.getGameTime());
        return entry.reserve >= amount;
    }

    public static boolean tryConsume(Level level, net.minecraft.core.BlockPos pos, int amount) {
        if (!(level instanceof ServerLevel serverLevel) || amount <= 0) {
            return amount <= 0;
        }

        AetherState state = state(serverLevel);
        long key = new ChunkPos(pos).toLong();
        RechargeEntry entry = state.entry(key, serverLevel.getGameTime());
        if (entry.reserve < amount) {
            return false;
        }

        entry.reserve -= amount;
        state.setDirty();
        return true;
    }

    public static void refund(ServerLevel level, net.minecraft.core.BlockPos pos, int amount) {
        if (level == null || amount <= 0) {
            return;
        }

        AetherState state = state(level);
        long key = new ChunkPos(pos).toLong();
        RechargeEntry entry = state.entry(key, level.getGameTime());
        entry.reserve = Math.min(ECFoundationConfig.aetherChunkMaxReserve(), entry.reserve + amount);
        state.setDirty();
    }

    public static int getReserve(ServerLevel level, net.minecraft.core.BlockPos pos) {
        if (level == null) {
            return 0;
        }
        return state(level).entry(new ChunkPos(pos).toLong(), level.getGameTime()).reserve;
    }

    public static int machineAetherCost(MachineDefinition definition) {
        if (definition == null) {
            return 0;
        }

        return switch (definition.category()) {
            case MAGIC -> Math.max(4, definition.energyPerTick() / 30);
            case ENDGAME -> Math.max(8, definition.energyPerTick() / 40);
            default -> 0;
        };
    }

    public static int spellAetherCost(Spell spell) {
        if (spell == null) {
            return 0;
        }
        if (spell.aetherCost() > 0) {
            return spell.aetherCost();
        }
        if (spell.form() == Spell.SpellForm.RITUAL || spell.form() == Spell.SpellForm.SIGIL || spell.catastrophic()) {
            return Math.max(4, spell.manaCost() / 4);
        }
        return 0;
    }

    private static AetherState state(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(AetherState::load, AetherState::new, DATA_NAME);
    }

    private static final class AetherState extends SavedData {
        private final Map<Long, RechargeEntry> reserves = new HashMap<>();

        private static AetherState load(CompoundTag tag) {
            AetherState state = new AetherState();
            ListTag list = tag.getList("chunks", Tag.TAG_COMPOUND);
            for (Tag element : list) {
                if (!(element instanceof CompoundTag entryTag)) {
                    continue;
                }
                long key = entryTag.getLong("key");
                state.reserves.put(key, new RechargeEntry(entryTag.getInt("reserve"), entryTag.getLong("last_tick")));
            }
            return state;
        }

        private RechargeEntry entry(long chunkKey, long gameTime) {
            RechargeEntry entry = reserves.computeIfAbsent(chunkKey, ignored -> new RechargeEntry(ECFoundationConfig.aetherChunkBaseReserve(), gameTime));
            recharge(entry, gameTime);
            return entry;
        }

        private void recharge(RechargeEntry entry, long gameTime) {
            long delta = Math.max(0L, gameTime - entry.lastTick);
            int interval = Math.max(1, ECFoundationConfig.aetherRechargeIntervalTicks());
            if (delta < interval) {
                return;
            }

            long pulses = delta / interval;
            long restored = pulses * (long) ECFoundationConfig.aetherRechargePerPulse();
            entry.reserve = (int) Math.min(ECFoundationConfig.aetherChunkMaxReserve(), entry.reserve + restored);
            entry.lastTick = gameTime;
            setDirty();
        }

        @Override
        public CompoundTag save(CompoundTag tag) {
            ListTag list = new ListTag();
            for (Map.Entry<Long, RechargeEntry> entry : reserves.entrySet()) {
                CompoundTag entryTag = new CompoundTag();
                entryTag.putLong("key", entry.getKey());
                entryTag.putInt("reserve", entry.getValue().reserve);
                entryTag.putLong("last_tick", entry.getValue().lastTick);
                list.add(entryTag);
            }
            tag.put("chunks", list);
            return tag;
        }
    }

    private static final class RechargeEntry {
        private int reserve;
        private long lastTick;

        private RechargeEntry(int reserve, long lastTick) {
            this.reserve = reserve;
            this.lastTick = lastTick;
        }
    }
}

