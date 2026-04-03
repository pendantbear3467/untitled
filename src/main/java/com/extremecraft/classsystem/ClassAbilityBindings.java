package com.extremecraft.classsystem;

import com.extremecraft.progression.capability.ProgressApi;
import net.minecraft.server.level.ServerPlayer;

import java.util.Locale;

/**
 * Canonical runtime gate for class-based ability/spell access checks.
 * Reads player class state from progression capabilities and resolves metadata via {@link ClassAccessResolver}.
 */
public final class ClassAbilityBindings {
    private ClassAbilityBindings() {
    }

    /**
     * Enforces required class id and optional ability access allow-list.
     */
    public static boolean canUseAbility(ServerPlayer player, String abilityId, String requiredClass) {
        PlayerClass playerClass = current(player);
        if (playerClass == null) {
            return true;
        }

        String normalizedAbilityId = abilityId == null ? "" : abilityId.trim().toLowerCase(Locale.ROOT);
        if (requiredClass != null && !requiredClass.isBlank() && !playerClass.id().equalsIgnoreCase(requiredClass)) {
            return false;
        }

        return playerClass.abilityAccess().isEmpty() || playerClass.abilityAccess().contains(normalizedAbilityId);
    }

    /**
     * Enforces spell access allow-list for current class.
     */
    public static boolean canUseSpell(ServerPlayer player, String spellId) {
        PlayerClass playerClass = current(player);
        if (playerClass == null) {
            return true;
        }

        String normalizedSpellId = spellId == null ? "" : spellId.trim().toLowerCase(Locale.ROOT);
        return playerClass.spellAccess().isEmpty() || playerClass.spellAccess().contains(normalizedSpellId);
    }

    /**
     * Reads active class id from progression capability and resolves canonical metadata.
     */
    public static PlayerClass current(ServerPlayer player) {
        String classId = ProgressApi.get(player).map(data -> data.currentClass()).orElse("warrior");
        return ClassAccessResolver.resolve(classId);
    }
}

