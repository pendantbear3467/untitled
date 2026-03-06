package com.extremecraft.progression;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class PlayerProgressData {
    private int level = 1;
    private int xp = 0;
    private int playerSkillPoints = 0;
    private int classSkillPoints = 0;
    private String currentClass = PlayerClass.WARRIOR.id();

    private final Set<String> unlockedClasses = new HashSet<>();
    private final Set<String> completedQuests = new HashSet<>();
    private final Map<String, Integer> questProgress = new HashMap<>();
    private final Set<String> discoveredRegions = new HashSet<>();

    private boolean syncDirty = true;
    private boolean attributesDirty = true;

    public PlayerProgressData() {
        unlockedClasses.add(PlayerClass.WARRIOR.id());
    }

    public int level() { return level; }
    public int xp() { return xp; }
    public int playerSkillPoints() { return playerSkillPoints; }
    public int classSkillPoints() { return classSkillPoints; }
    public String currentClass() { return currentClass; }

    public Set<String> unlockedClasses() { return unlockedClasses; }
    public Set<String> completedQuests() { return completedQuests; }
    public Map<String, Integer> questProgress() { return questProgress; }
    public Set<String> discoveredRegions() { return discoveredRegions; }

    public void setCurrentClass(String classId) {
        if (classId == null || classId.isBlank() || classId.equalsIgnoreCase(this.currentClass)) {
            return;
        }

        this.currentClass = classId;
        markAttributesDirty();
        markSyncDirty();
    }

    public void unlockClass(String classId) {
        if (classId == null || classId.isBlank()) {
            return;
        }

        if (unlockedClasses.add(classId)) {
            markSyncDirty();
        }
    }

    public void addXp(int amount) {
        if (amount <= 0) return;
        xp += amount;

        while (xp >= xpToNextLevel(level)) {
            xp -= xpToNextLevel(level);
            level++;
            playerSkillPoints += 1;
            if (level % 3 == 0) {
                classSkillPoints += 1;
            }

            markAttributesDirty();
        }

        markSyncDirty();
    }

    public void addPlayerSkillPoints(int amount) {
        if (amount > 0) {
            playerSkillPoints += amount;
            markSyncDirty();
        }
    }

    public boolean consumePlayerSkillPoints(int amount) {
        if (amount <= 0 || playerSkillPoints < amount) {
            return false;
        }

        playerSkillPoints -= amount;
        markSyncDirty();
        return true;
    }

    public void addClassSkillPoints(int amount) {
        if (amount > 0) {
            classSkillPoints += amount;
            markSyncDirty();
        }
    }

    public void addQuestProgress(String questId, int amount) {
        if (amount <= 0) return;
        questProgress.merge(questId, amount, Integer::sum);
        markSyncDirty();
    }

    public boolean isQuestCompleted(String questId) {
        return completedQuests.contains(questId);
    }

    public void setQuestCompleted(String questId) {
        if (completedQuests.add(questId)) {
            markSyncDirty();
        }
    }

    public int getQuestProgress(String questId) {
        return questProgress.getOrDefault(questId, 0);
    }

    public boolean discoverRegion(String regionKey) {
        if (regionKey == null || regionKey.isBlank()) {
            return false;
        }

        if (discoveredRegions.add(regionKey)) {
            markSyncDirty();
            return true;
        }

        return false;
    }

    public void markSyncDirty() {
        syncDirty = true;
    }

    public void markAttributesDirty() {
        attributesDirty = true;
        syncDirty = true;
    }

    public boolean consumeSyncDirty() {
        boolean dirty = syncDirty;
        syncDirty = false;
        return dirty;
    }

    public boolean consumeAttributesDirty() {
        boolean dirty = attributesDirty;
        attributesDirty = false;
        return dirty;
    }

    public static int xpToNextLevel(int level) {
        int safeLevel = Math.max(1, level);
        return safeLevel * safeLevel * 10;
    }

    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("level", level);
        tag.putInt("xp", xp);
        tag.putInt("player_skill_points", playerSkillPoints);
        tag.putInt("class_skill_points", classSkillPoints);
        tag.putString("current_class", currentClass);

        ListTag unlocked = new ListTag();
        unlockedClasses.forEach(c -> unlocked.add(StringTag.valueOf(c)));
        tag.put("unlocked_classes", unlocked);

        ListTag completed = new ListTag();
        completedQuests.forEach(q -> completed.add(StringTag.valueOf(q)));
        tag.put("completed_quests", completed);

        CompoundTag progressTag = new CompoundTag();
        questProgress.forEach(progressTag::putInt);
        tag.put("quest_progress", progressTag);

        ListTag regions = new ListTag();
        discoveredRegions.forEach(r -> regions.add(StringTag.valueOf(r)));
        tag.put("discovered_regions", regions);

        return tag;
    }

    public void deserializeNBT(CompoundTag tag) {
        level = Math.max(1, tag.getInt("level"));
        xp = Math.max(0, tag.getInt("xp"));
        playerSkillPoints = Math.max(0, tag.getInt("player_skill_points"));
        classSkillPoints = Math.max(0, tag.getInt("class_skill_points"));
        currentClass = tag.contains("current_class") ? tag.getString("current_class") : PlayerClass.WARRIOR.id();

        unlockedClasses.clear();
        if (tag.contains("unlocked_classes", Tag.TAG_LIST)) {
            ListTag unlocked = tag.getList("unlocked_classes", Tag.TAG_STRING);
            for (Tag t : unlocked) unlockedClasses.add(t.getAsString());
        }
        if (unlockedClasses.isEmpty()) unlockedClasses.add(PlayerClass.WARRIOR.id());

        completedQuests.clear();
        if (tag.contains("completed_quests", Tag.TAG_LIST)) {
            ListTag completed = tag.getList("completed_quests", Tag.TAG_STRING);
            for (Tag t : completed) completedQuests.add(t.getAsString());
        }

        questProgress.clear();
        if (tag.contains("quest_progress", Tag.TAG_COMPOUND)) {
            CompoundTag progressTag = tag.getCompound("quest_progress");
            for (String key : progressTag.getAllKeys()) {
                questProgress.put(key, Math.max(0, progressTag.getInt(key)));
            }
        }

        discoveredRegions.clear();
        if (tag.contains("discovered_regions", Tag.TAG_LIST)) {
            ListTag regions = tag.getList("discovered_regions", Tag.TAG_STRING);
            for (Tag t : regions) discoveredRegions.add(t.getAsString());
        }

        syncDirty = false;
        attributesDirty = false;
    }

    public void copyFrom(PlayerProgressData other) {
        this.level = other.level;
        this.xp = other.xp;
        this.playerSkillPoints = other.playerSkillPoints;
        this.classSkillPoints = other.classSkillPoints;
        this.currentClass = other.currentClass;

        this.unlockedClasses.clear();
        this.unlockedClasses.addAll(other.unlockedClasses);

        this.completedQuests.clear();
        this.completedQuests.addAll(other.completedQuests);

        this.questProgress.clear();
        this.questProgress.putAll(other.questProgress);

        this.discoveredRegions.clear();
        this.discoveredRegions.addAll(other.discoveredRegions);

        markAttributesDirty();
        markSyncDirty();
    }
}


