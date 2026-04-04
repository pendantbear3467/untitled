package com.extremecraft.progression.skilltree.effect;

import com.extremecraft.progression.capability.PlayerStatsCapability;
import com.extremecraft.progression.skilltree.SkillNode;

/**
 * Extension point for applying advanced node effects beyond stat modifiers.
 */
public interface ISkillEffectResolver {
    void apply(PlayerStatsCapability stats, SkillNode node);
}
