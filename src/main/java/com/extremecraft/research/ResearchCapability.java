package com.extremecraft.research;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import java.util.HashSet;
import java.util.Set;

public class ResearchCapability {
    private final Set<String> unlockedResearch = new HashSet<>();

    public boolean hasResearch(String id) {
        return unlockedResearch.contains(id);
    }

    public void unlockResearch(String id) {
        if (!id.isBlank()) {
            unlockedResearch.add(id);
        }
    }

    public Set<String> unlockedResearch() {
        return unlockedResearch;
    }

    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        ListTag list = new ListTag();
        unlockedResearch.forEach(id -> list.add(StringTag.valueOf(id)));
        tag.put("unlocked", list);
        return tag;
    }

    public void deserializeNBT(CompoundTag tag) {
        unlockedResearch.clear();
        if (!tag.contains("unlocked", Tag.TAG_LIST)) {
            return;
        }

        ListTag list = tag.getList("unlocked", Tag.TAG_STRING);
        for (Tag value : list) {
            unlockedResearch.add(value.getAsString());
        }
    }

    public void copyFrom(ResearchCapability other) {
        unlockedResearch.clear();
        unlockedResearch.addAll(other.unlockedResearch);
    }
}
