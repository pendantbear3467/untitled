package com.extremecraft.progression;

import com.extremecraft.classsystem.ClassAccessResolver;
import com.extremecraft.classsystem.ClassPassives;
import com.extremecraft.network.ModNetwork;
import com.extremecraft.network.packet.SyncProgressPacket;
import com.extremecraft.progression.capability.ProgressApi;
import com.extremecraft.progression.classsystem.ClassIdResolver;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.network.PacketDistributor;

import java.util.UUID;

/**
 * Canonical mutation/read boundary for the live progression capability.
 *
 * <p>Progression-owned quest state, unlock grants, class unlocks, player level XP, and
 * progression skill points should converge here instead of mutating {@link PlayerProgressData}
 * directly from unrelated gameplay code.</p>
 */
final class ProgressionService {
    private static final UUID LEVEL_HEALTH_MOD = UUID.fromString("b6ab1ed2-1f7e-4965-9f53-19d8b237db0b");
    private static final UUID LEVEL_ATTACK_MOD = UUID.fromString("3a2d5b50-6b16-469f-8e0a-df018dd7707c");
    private static final UUID CLASS_HEALTH_MOD = UUID.fromString("ca6b0177-d863-4be1-a2f6-dbbd7c79e18f");
    private static final UUID CLASS_ATTACK_MOD = UUID.fromString("2f86f095-69a8-4d48-8116-770f9d4f3f70");
    private static final UUID CLASS_SPEED_MOD = UUID.fromString("f0d635c2-a0c4-40f8-a52f-c031adff12c7");
    private static final UUID CLASS_LUCK_MOD = UUID.fromString("730244df-6e1a-42c9-98f7-fbe44f0fd5cb");

    private ProgressionService() {}

    /**
     * Adds player XP via progression capability and flushes dirty flags.
     */
    static void addXp(ServerPlayer player, int amount) {
        ProgressionMutationAuthority.warnIfBypassed("addXp");
        ProgressApi.get(player).ifPresent(data -> data.addXp(amount));
        flushDirty(player);
    }

    static void setLevel(ServerPlayer player, int level) {
        ProgressionMutationAuthority.warnIfBypassed("setLevel");
        ProgressApi.get(player).ifPresent(data -> data.setLevel(level));
        flushDirty(player);
    }

    static boolean addPlayerSkillPoints(ServerPlayer player, int amount) {
        return addPlayerSkillPoints(player, amount, true);
    }

    static boolean addPlayerSkillPoints(ServerPlayer player, int amount, boolean flushImmediately) {
        ProgressionMutationAuthority.warnIfBypassed("addPlayerSkillPoints");
        if (player == null || amount <= 0) {
            return false;
        }

        boolean changed = ProgressApi.get(player).map(data -> {
            int before = data.playerSkillPoints();
            data.addPlayerSkillPoints(amount);
            return data.playerSkillPoints() != before;
        }).orElse(false);

        if (!changed) {
            return false;
        }

        PlayerStatsService.syncProgressionMirror(player, false);
        if (flushImmediately) {
            flushDirty(player);
        }
        return true;
    }

    static boolean addClassSkillPoints(ServerPlayer player, int amount) {
        return addClassSkillPoints(player, amount, true);
    }

    static boolean addClassSkillPoints(ServerPlayer player, int amount, boolean flushImmediately) {
        ProgressionMutationAuthority.warnIfBypassed("addClassSkillPoints");
        if (player == null || amount <= 0) {
            return false;
        }

        boolean changed = ProgressApi.get(player).map(data -> {
            int before = data.classSkillPoints();
            data.addClassSkillPoints(amount);
            return data.classSkillPoints() != before;
        }).orElse(false);

        if (changed && flushImmediately) {
            flushDirty(player);
        }
        return changed;
    }

    static boolean unlockClass(ServerPlayer player, String classId) {
        return unlockClass(player, classId, true);
    }

    static boolean unlockClass(ServerPlayer player, String classId, boolean flushImmediately) {
        ProgressionMutationAuthority.warnIfBypassed("unlockClass");
        if (player == null || classId == null || classId.isBlank()) {
            return false;
        }

        boolean changed = ProgressApi.get(player).map(data -> {
            int before = data.unlockedClasses().size();
            data.unlockClass(classId);
            return data.unlockedClasses().size() != before;
        }).orElse(false);

        if (changed && flushImmediately) {
            flushDirty(player);
        }
        return changed;
    }

    static boolean grantUnlock(ServerPlayer player, String unlockId) {
        return grantUnlock(player, unlockId, true);
    }

    static boolean grantUnlock(ServerPlayer player, String unlockId, boolean flushImmediately) {
        ProgressionMutationAuthority.warnIfBypassed("grantUnlock");
        if (player == null || unlockId == null || unlockId.isBlank()) {
            return false;
        }

        boolean changed = ProgressApi.get(player).map(data -> data.grantUnlock(unlockId)).orElse(false);
        if (changed && flushImmediately) {
            flushDirty(player);
        }
        return changed;
    }

    static boolean grantUnlocks(ServerPlayer player, java.util.Collection<String> unlockIds) {
        return grantUnlocks(player, unlockIds, true);
    }

    static boolean grantUnlocks(ServerPlayer player, java.util.Collection<String> unlockIds, boolean flushImmediately) {
        ProgressionMutationAuthority.warnIfBypassed("grantUnlocks");
        if (player == null || unlockIds == null || unlockIds.isEmpty()) {
            return false;
        }

        boolean changed = ProgressApi.get(player).map(data -> data.grantUnlocks(unlockIds)).orElse(false);
        if (changed && flushImmediately) {
            flushDirty(player);
        }
        return changed;
    }

    static boolean addQuestProgress(ServerPlayer player, String questId, int amount) {
        return addQuestProgress(player, questId, amount, true);
    }

    static boolean addQuestProgress(ServerPlayer player, String questId, int amount, boolean flushImmediately) {
        ProgressionMutationAuthority.warnIfBypassed("addQuestProgress");
        if (player == null || questId == null || questId.isBlank() || amount <= 0) {
            return false;
        }

        boolean changed = ProgressApi.get(player).map(data -> {
            int before = data.getQuestProgress(questId);
            data.addQuestProgress(questId, amount);
            return data.getQuestProgress(questId) != before;
        }).orElse(false);

        if (changed && flushImmediately) {
            flushDirty(player);
        }
        return changed;
    }

    static boolean markQuestCompleted(ServerPlayer player, String questId) {
        return markQuestCompleted(player, questId, true);
    }

    static boolean markQuestCompleted(ServerPlayer player, String questId, boolean flushImmediately) {
        ProgressionMutationAuthority.warnIfBypassed("markQuestCompleted");
        if (player == null || questId == null || questId.isBlank()) {
            return false;
        }

        boolean changed = ProgressApi.get(player).map(data -> {
            boolean before = data.isQuestCompleted(questId);
            data.setQuestCompleted(questId);
            return !before && data.isQuestCompleted(questId);
        }).orElse(false);

        if (changed && flushImmediately) {
            flushDirty(player);
        }
        return changed;
    }

    static boolean discoverRegion(ServerPlayer player, String regionKey) {
        return discoverRegion(player, regionKey, true);
    }

    static boolean discoverRegion(ServerPlayer player, String regionKey, boolean flushImmediately) {
        ProgressionMutationAuthority.warnIfBypassed("discoverRegion");
        if (player == null || regionKey == null || regionKey.isBlank()) {
            return false;
        }

        boolean changed = ProgressApi.get(player).map(data -> data.discoverRegion(regionKey)).orElse(false);
        if (changed && flushImmediately) {
            flushDirty(player);
        }
        return changed;
    }

    static int getQuestProgress(ServerPlayer player, String questId) {
        if (player == null || questId == null || questId.isBlank()) {
            return 0;
        }

        return ProgressApi.get(player).map(data -> data.getQuestProgress(questId)).orElse(0);
    }

    static boolean isQuestCompleted(ServerPlayer player, String questId) {
        if (player == null || questId == null || questId.isBlank()) {
            return false;
        }

        return ProgressApi.get(player).map(data -> data.isQuestCompleted(questId)).orElse(false);
    }

    static boolean switchClass(ServerPlayer player, String classId) {
        ProgressionMutationAuthority.warnIfBypassed("switchClass");
        return ProgressApi.get(player).map(data -> {
            String normalized = ClassIdResolver.normalizeCanonical(classId);
            if (normalized.isBlank() || !data.unlockedClasses().contains(normalized)) {
                return false;
            }
            data.setCurrentClass(normalized);
            flushDirty(player);
            return true;
        }).orElse(false);
    }

    /**
     * Applies deferred side effects only when capability marks relevant dirty flags.
     */
    static void flushDirty(ServerPlayer player) {
        ProgressApi.get(player).ifPresent(data -> {
            if (data.consumeAttributesDirty()) {
                applyAttributes(player);
            }

            if (data.consumeSyncDirty()) {
                sync(player);
            }
        });
    }

    /**
     * Recomputes and applies level/class attribute modifiers onto vanilla attributes.
     */
    static void applyAttributes(ServerPlayer player) {
        ProgressApi.get(player).ifPresent(data -> {
            int level = data.level();
            com.extremecraft.classsystem.PlayerClass klass = ClassAccessResolver.resolve(data.currentClass());
            if (klass == null) {
                klass = ClassAccessResolver.resolve(ClassIdResolver.DEFAULT_CLASS_ID);
            }
            java.util.Map<String, Double> passives = ClassPassives.resolve(klass);

            double levelHealth = (level - 1) * 0.4D;
            double levelAttack = (level - 1) * 0.15D;

            applyAdd(player.getAttribute(Attributes.MAX_HEALTH), LEVEL_HEALTH_MOD, "ec_level_health", levelHealth);
            applyAdd(player.getAttribute(Attributes.ATTACK_DAMAGE), LEVEL_ATTACK_MOD, "ec_level_attack", levelAttack);

            double healthPct = passives.getOrDefault("health_bonus_pct", 0.0D);
            double flatAttack = passives.getOrDefault("flat_attack_bonus", passives.getOrDefault("melee_damage", 0.0D));
            double moveSpeed = passives.getOrDefault("move_speed_bonus", passives.getOrDefault("move_speed", 0.0D));
            double luck = passives.getOrDefault("luck_bonus", passives.getOrDefault("luck", 0.0D));

            double classHealth = Math.max(0.0D, player.getAttributeValue(Attributes.MAX_HEALTH) * healthPct);
            applyAdd(player.getAttribute(Attributes.MAX_HEALTH), CLASS_HEALTH_MOD, "ec_class_health", classHealth);
            applyAdd(player.getAttribute(Attributes.ATTACK_DAMAGE), CLASS_ATTACK_MOD, "ec_class_attack", flatAttack);
            applyAdd(player.getAttribute(Attributes.MOVEMENT_SPEED), CLASS_SPEED_MOD, "ec_class_speed", moveSpeed);
            applyAdd(player.getAttribute(Attributes.LUCK), CLASS_LUCK_MOD, "ec_class_luck", luck);

            if (player.getHealth() > player.getMaxHealth()) {
                player.setHealth(player.getMaxHealth());
            }
        });
    }

    /**
     * Sends full serialized progression snapshot to the owning client.
     */
    static void sync(ServerPlayer player) {
        ProgressApi.get(player).ifPresent(data -> ModNetwork.CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new SyncProgressPacket(data.serializeNBT())
        ));
    }

    /**
     * Replaces transient additive modifier by UUID.
     */
    private static void applyAdd(AttributeInstance attr, UUID id, String name, double amount) {
        if (attr == null) return;
        AttributeModifier existing = attr.getModifier(id);
        if (existing != null) attr.removeModifier(existing);
        if (Math.abs(amount) < 0.00001D) return;
        attr.addTransientModifier(new AttributeModifier(id, name, amount, AttributeModifier.Operation.ADDITION));
    }

    static boolean consumePlayerSkillPoints(ServerPlayer player, int amount) {
        ProgressionMutationAuthority.warnIfBypassed("consumePlayerSkillPoints");
        if (player == null || amount <= 0) {
            return false;
        }

        boolean changed = ProgressApi.get(player).map(data -> data.consumePlayerSkillPoints(amount)).orElse(false);
        if (changed) {
            flushDirty(player);
        }
        return changed;
    }
}
