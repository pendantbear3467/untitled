package com.extremecraft.machine.sync;

import net.minecraft.nbt.CompoundTag;

/**
 * Implemented by machine block entities that expose compact runtime state for client sync.
 */
public interface MachineStateSyncProvider {
    CompoundTag machineSyncTag();
}
