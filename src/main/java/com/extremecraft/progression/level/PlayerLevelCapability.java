package com.extremecraft.progression.level;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class PlayerLevelCapability {
    private int level = 1;
    private int xp = 0;
    private int skillPoints = 0;

    private final Set<String> unlockedAbilities = new LinkedHashSet<>();
    private final String[] abilitySlots = new String[]{"firebolt", "blink", "arcane_shield", "meteor"};

    public int level() {
        return level;
    }

    public int xp() {
        return xp;
    }

    public int skillPoints() {
        return skillPoints;
    }

    public Set<String> unlockedAbilities() {
        return Set.copyOf(unlockedAbilities);
    }

    public List<String> abilitySlots() {
        return List.of(abilitySlots);
    }

    public String abilityInSlot(int slotIndex) {
        if (slotIndex < 0 || slotIndex >= abilitySlots.length) {
            return "";
        }
        return normalize(abilitySlots[slotIndex]);
    }

    public void setProgression(int level, int xp, int skillPoints) {
        this.level = Math.max(1, level);
        this.xp = Math.max(0, xp);
        this.skillPoints = Math.max(0, skillPoints);
    }

    public boolean grantAbility(String abilityId) {
        String normalized = normalize(abilityId);
        if (normalized.isBlank()) {
            return false;
        }

        boolean changed = unlockedAbilities.add(normalized);
        for (int i = 0; i < abilitySlots.length; i++) {
            if (normalize(abilitySlots[i]).isBlank()) {
                abilitySlots[i] = normalized;
                return true;
            }
            if (normalize(abilitySlots[i]).equals(normalized)) {
                return changed;
            }
        }

        return changed;
    }

    public boolean setAbilitySlot(int slotIndex, String abilityId) {
        if (slotIndex < 0 || slotIndex >= abilitySlots.length) {
            return false;
        }

        String normalized = normalize(abilityId);
        if (normalized.isBlank()) {
            return false;
        }

        unlockedAbilities.add(normalized);
        if (normalized.equals(normalize(abilitySlots[slotIndex]))) {
            return false;
        }

        abilitySlots[slotIndex] = normalized;
        return true;
    }

    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("level", level);
        tag.putInt("xp", xp);
        tag.putInt("skill_points", skillPoints);

        ListTag abilities = new ListTag();
        for (String ability : unlockedAbilities) {
            abilities.add(StringTag.valueOf(ability));
        }
        tag.put("unlocked_abilities", abilities);

        CompoundTag slotsTag = new CompoundTag();
        for (int i = 0; i < abilitySlots.length; i++) {
            slotsTag.putString("slot_" + (i + 1), normalize(abilitySlots[i]));
        }
        tag.put("ability_slots", slotsTag);
        return tag;
    }

    public void deserializeNBT(CompoundTag tag) {
        level = Math.max(1, tag.getInt("level"));
        xp = Math.max(0, tag.getInt("xp"));
        skillPoints = Math.max(0, tag.getInt("skill_points"));

        unlockedAbilities.clear();
        ListTag abilities = tag.getList("unlocked_abilities", Tag.TAG_STRING);
        for (Tag entry : abilities) {
            String ability = normalize(entry.getAsString());
            if (!ability.isBlank()) {
                unlockedAbilities.add(ability);
            }
        }

        CompoundTag slotsTag = tag.getCompound("ability_slots");
        for (int i = 0; i < abilitySlots.length; i++) {
            abilitySlots[i] = normalize(slotsTag.getString("slot_" + (i + 1)));
        }

        for (String slot : abilitySlots) {
            if (!slot.isBlank()) {
                unlockedAbilities.add(slot);
            }
        }
    }

    public void copyFrom(PlayerLevelCapability other) {
        this.level = other.level;
        this.xp = other.xp;
        this.skillPoints = other.skillPoints;

        this.unlockedAbilities.clear();
        this.unlockedAbilities.addAll(other.unlockedAbilities);

        Arrays.fill(this.abilitySlots, "");
        for (int i = 0; i < this.abilitySlots.length; i++) {
            this.abilitySlots[i] = other.abilitySlots[i];
        }
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }
}
