package com.extremecraft.combat.dualwield;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;

/**
 * Holds three dual-wield loadout slots and tracks the currently active index.
 */
public class PlayerDualWieldData {
    private final DualWieldLoadout[] loadouts = {
            new DualWieldLoadout(),
            new DualWieldLoadout(),
            new DualWieldLoadout()
    };
    private int activeLoadoutIndex = 0;
    private boolean initialized;
    private boolean dirty = true;

    public int activeLoadoutIndex() {
        return activeLoadoutIndex;
    }

    public DualWieldLoadout getLoadout(int index) {
        return loadouts[Math.max(0, Math.min(loadouts.length - 1, index))];
    }

    public int loadoutCount() {
        return loadouts.length;
    }

    public void ensureInitialized(Player player) {
        if (initialized || player == null) {
            return;
        }

        for (DualWieldLoadout loadout : loadouts) {
            loadout.set(player.getMainHandItem(), player.getOffhandItem());
        }
        initialized = true;
        dirty = true;
    }

    public void saveActiveHands(Player player) {
        getLoadout(activeLoadoutIndex).set(player.getMainHandItem(), player.getOffhandItem());
        dirty = true;
    }

    public void cycleLoadout(Player player) {
        saveActiveHands(player);
        activeLoadoutIndex = (activeLoadoutIndex + 1) % loadouts.length;
        applyActiveLoadout(player);
        dirty = true;
    }

    public void selectLoadout(Player player, int index) {
        int clamped = Math.max(0, Math.min(loadouts.length - 1, index));
        saveActiveHands(player);
        activeLoadoutIndex = clamped;
        applyActiveLoadout(player);
        dirty = true;
    }

    public void saveCurrentHandsToLoadout(Player player, int index) {
        int clamped = Math.max(0, Math.min(loadouts.length - 1, index));
        getLoadout(clamped).set(player.getMainHandItem(), player.getOffhandItem());
        dirty = true;
    }

    public void applyActiveLoadout(Player player) {
        DualWieldLoadout loadout = getLoadout(activeLoadoutIndex);
        player.setItemInHand(InteractionHand.MAIN_HAND, loadout.mainHandItem().copy());
        player.setItemInHand(InteractionHand.OFF_HAND, loadout.offHandItem().copy());
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
        tag.putInt("active", activeLoadoutIndex);
        tag.putBoolean("initialized", initialized);
        for (int i = 0; i < loadouts.length; i++) {
            tag.put("loadout_" + i, loadouts[i].serializeNBT());
        }
        return tag;
    }

    public void deserializeNBT(CompoundTag tag) {
        activeLoadoutIndex = Math.max(0, Math.min(loadouts.length - 1, tag.getInt("active")));
        initialized = tag.getBoolean("initialized");
        for (int i = 0; i < loadouts.length; i++) {
            if (tag.contains("loadout_" + i)) {
                loadouts[i].deserializeNBT(tag.getCompound("loadout_" + i));
            }
        }
        dirty = false;
    }

    public void copyFrom(PlayerDualWieldData other) {
        this.activeLoadoutIndex = other.activeLoadoutIndex;
        this.initialized = other.initialized;
        for (int i = 0; i < loadouts.length; i++) {
            this.loadouts[i].set(other.loadouts[i].mainHandItem(), other.loadouts[i].offHandItem());
        }
        this.dirty = true;
    }
}
