package com.extremecraft.entity.system;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashSet;
import java.util.Set;

public final class BossArenaState extends SavedData {
    private static final String DATA_NAME = "extremecraft_boss_arena_state";
    private final Set<String> triggeredArenas = new HashSet<>();

    public static BossArenaState get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(BossArenaState::load, BossArenaState::new, DATA_NAME);
    }

    public static BossArenaState load(CompoundTag tag) {
        BossArenaState state = new BossArenaState();
        ListTag list = tag.getList("triggered", Tag.TAG_STRING);
        for (Tag entry : list) {
            state.triggeredArenas.add(entry.getAsString());
        }
        return state;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (String key : triggeredArenas) {
            list.add(StringTag.valueOf(key));
        }
        tag.put("triggered", list);
        return tag;
    }

    public boolean hasTriggered(String key) {
        return triggeredArenas.contains(key);
    }

    public void markTriggered(String key) {
        if (triggeredArenas.add(key)) {
            setDirty();
        }
    }
}
