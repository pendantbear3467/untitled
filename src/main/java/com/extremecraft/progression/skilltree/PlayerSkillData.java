package com.extremecraft.progression.skilltree;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

/**
 * Stores unlocked skill node keys in the form treeId:nodeId.
 */
public class PlayerSkillData {
    private final Set<String> unlockedNodes = new HashSet<>();
    private boolean dirty = true;

    public Set<String> unlockedNodes() {
        return unlockedNodes;
    }

    public boolean isUnlocked(String treeId, String nodeId) {
        return unlockedNodes.contains(key(treeId, nodeId));
    }

    public boolean unlock(String treeId, String nodeId) {
        boolean changed = unlockedNodes.add(key(treeId, nodeId));
        if (changed) {
            dirty = true;
        }
        return changed;
    }

    public void markDirty() {
        dirty = true;
    }

    public boolean consumeDirty() {
        boolean wasDirty = dirty;
        dirty = false;
        return wasDirty;
    }

    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        ListTag nodes = new ListTag();
        for (String node : unlockedNodes) {
            nodes.add(StringTag.valueOf(node));
        }
        tag.put("unlocked_nodes", nodes);
        return tag;
    }

    public void deserializeNBT(CompoundTag tag) {
        unlockedNodes.clear();
        ListTag list = tag.getList("unlocked_nodes", Tag.TAG_STRING);
        for (Tag entry : list) {
            unlockedNodes.add(entry.getAsString());
        }
        dirty = false;
    }

    public void copyFrom(PlayerSkillData other) {
        unlockedNodes.clear();
        unlockedNodes.addAll(other.unlockedNodes);
        dirty = true;
    }

    public static String key(String treeId, String nodeId) {
        return treeId + ":" + nodeId;
    }
}
