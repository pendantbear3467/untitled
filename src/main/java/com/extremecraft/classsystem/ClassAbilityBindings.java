package com.extremecraft.classsystem;

import com.extremecraft.progression.capability.ProgressApi;
import net.minecraft.server.level.ServerPlayer;

public final class ClassAbilityBindings {
    private ClassAbilityBindings() {
    }

    public static boolean canUseAbility(ServerPlayer player, String abilityId, String requiredClass) {
        PlayerClass playerClass = current(player);
        if (playerClass == null) {
            return true;
        }

        if (requiredClass != null && !requiredClass.isBlank() && !playerClass.id().equalsIgnoreCase(requiredClass)) {
            return false;
        }

        return playerClass.abilityAccess().isEmpty() || playerClass.abilityAccess().contains(abilityId);
    }

    public static boolean canUseSpell(ServerPlayer player, String spellId) {
        PlayerClass playerClass = current(player);
        if (playerClass == null) {
            return true;
        }

        return playerClass.spellAccess().isEmpty() || playerClass.spellAccess().contains(spellId);
    }

    public static PlayerClass current(ServerPlayer player) {
        String classId = ProgressApi.get(player).map(data -> data.currentClass()).orElse("warrior");
        return ClassRegistry.get(classId);
    }
}
