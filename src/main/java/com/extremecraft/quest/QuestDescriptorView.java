package com.extremecraft.quest;

import com.extremecraft.ecosystem.core.progression.ProgressionQuestDescriptor;

public final class QuestDescriptorView {
    private QuestDescriptorView() {
    }

    public static ProgressionQuestDescriptor from(QuestDefinition definition) {
        return new ProgressionQuestDescriptor(
                definition.id(),
                definition.title(),
                String.valueOf(definition.type()),
                definition.target(),
                definition.rewardXp(),
                definition.rewardPlayerSkillPoints(),
                definition.rewardClassSkillPoints(),
                definition.rewardUnlockClass(),
                definition.rewardUnlockStage()
        );
    }
}
