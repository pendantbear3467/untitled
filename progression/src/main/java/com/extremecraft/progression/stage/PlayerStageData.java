package com.extremecraft.progression.stage;

import net.minecraft.nbt.CompoundTag;

public class PlayerStageData {
    private ProgressionStage stage = ProgressionStage.PRIMITIVE;

    public ProgressionStage stage() {
        return stage;
    }

    public void setStage(ProgressionStage stage) {
        this.stage = stage == null ? ProgressionStage.PRIMITIVE : stage;
    }

    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putString("stage", stage.name());
        return tag;
    }

    public void deserializeNBT(CompoundTag tag) {
        stage = ProgressionStage.byName(tag.getString("stage")).orElse(ProgressionStage.PRIMITIVE);
    }

    public void copyFrom(PlayerStageData other) {
        this.stage = other.stage;
    }
}
