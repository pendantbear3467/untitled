package com.extremecraft.progression;

import com.extremecraft.skills.PlayerSkillsCapability;
import com.extremecraft.skills.SkillRegistry;
import com.extremecraft.skills.SkillsApi;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Canonical live write path for skill XP.
 *
 * <p>Gameplay systems may award skill XP through this service; unrelated mirrors and client sync
 * code should not mutate skill progression directly.</p>
 */
public final class SkillProgressionService {
    private static final Logger LOGGER = LogManager.getLogger();

    public enum Source {
        COMBAT,
        MINING,
        ENGINEERING,
        ARCANE,
        EXPLORATION,
        DEBUG_COMMAND
    }

    private SkillProgressionService() {
    }

    public static int grantSkillXp(ServerPlayer player, String skillId, int amount, Source source) {
        if (player == null || skillId == null || skillId.isBlank() || amount <= 0 || source == null) {
            return 0;
        }

        // Live policy: skill XP is combat-authoritative, with debug-only administrative overrides.
        if (source != Source.COMBAT && source != Source.DEBUG_COMMAND) {
            LOGGER.warn("[ProgressionGuard] Rejected non-combat skill XP write: source={} skill={} amount={}", source, skillId, amount);
            return 0;
        }

        if (SkillRegistry.byId(skillId) == null) {
            return 0;
        }

        return SkillsApi.get(player)
                .map(skills -> skills.addSkillXp(skillId, amount))
                .orElse(0);
    }

    public static int grantCombatKillXp(ServerPlayer player, LivingEntity target) {
        if (player == null || target == null) {
            return 0;
        }

        int xp = Math.max(8, Math.min(160, (int) Math.ceil(target.getMaxHealth() * 0.75D)));
        return grantSkillXp(player, "combat", xp, Source.COMBAT);
    }

    public static int xpUntilNextLevel(ServerPlayer player, String skillId) {
        if (player == null || skillId == null || skillId.isBlank()) {
            return 0;
        }

        return SkillsApi.get(player).map(skills -> {
            int currentLevel = skills.getSkillLevel(skillId);
            return Math.max(0, PlayerSkillsCapability.xpForNextLevel(currentLevel) - skills.getSkillXp(skillId));
        }).orElse(0);
    }
}
