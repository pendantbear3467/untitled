package com.extremecraft.progression;

import com.extremecraft.machine.core.MachineCatalog;
import com.extremecraft.progression.stage.ProgressionStage;
import com.extremecraft.progression.stage.StageManager;
import com.extremecraft.progression.unlock.UnlockRuleLoader;
import com.extremecraft.reactor.ReactorIdentity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Canonical gameplay gate for stage-locked machine and recipe access.
 *
 * <p>Checks and grants that affect the formal progression ladder should route through this
 * class so stage ownership stays explicit even while legacy systems remain online.</p>
 */
public final class ProgressionGate {
    private static final Map<String, ProgressionStage> MACHINE_REQUIREMENTS = new HashMap<>();
    private static final Map<String, ProgressionStage> RECIPE_REQUIREMENTS = new HashMap<>();

    static {
        registerMachineRequirement("pulverizer", ProgressionStage.PRIMITIVE);
        MachineCatalog.DEFINITIONS.values().forEach(definition -> registerMachineRequirement(definition.id(), definition.stage()));

        registerRecipeRequirement("extremecraft:machine_processing", ProgressionStage.PRIMITIVE);
        registerRecipeRequirement("extremecraft:hybrid_crafting", ProgressionStage.ADVANCED);
    }

    private ProgressionGate() {
    }

    public static void registerMachineRequirement(String machineId, ProgressionStage stage) {
        MACHINE_REQUIREMENTS.put(ReactorIdentity.normalizeMachineId(machineId), stage);
    }

    public static void registerRecipeRequirement(String recipeId, ProgressionStage stage) {
        RECIPE_REQUIREMENTS.put(recipeId, stage);
    }

    public static Optional<ProgressionStage> requiredMachineStage(String machineId) {
        String normalizedMachineId = ReactorIdentity.normalizeMachineId(machineId);
        ProgressionStage direct = MACHINE_REQUIREMENTS.get(normalizedMachineId);
        if (direct != null) {
            return Optional.of(direct);
        }

        return StageDataLoader.requiredStageForUnlock("machine:" + normalizedMachineId);
    }

    public static Optional<ProgressionStage> requiredRecipeStage(String recipeId) {
        ProgressionStage direct = RECIPE_REQUIREMENTS.get(recipeId);
        if (direct != null) {
            return Optional.of(direct);
        }

        return StageDataLoader.requiredStageForUnlock("recipe:" + recipeId);
    }

    public static boolean canUseMachine(Player player, String machineId) {
        String normalizedMachineId = ReactorIdentity.normalizeMachineId(machineId);
        boolean stageAllowed = requiredMachineStage(normalizedMachineId).map(stage -> StageManager.hasStage(player, stage)).orElse(true);
        if (!stageAllowed) {
            return false;
        }

        return UnlockRuleLoader.canUnlock(player, "machine:" + normalizedMachineId);
    }

    public static boolean canUseRecipe(Player player, ResourceLocation recipeId) {
        return canUseRecipe(player, recipeId.toString());
    }

    public static boolean canUseRecipe(Player player, String recipeId) {
        boolean stageAllowed = requiredRecipeStage(recipeId).map(stage -> StageManager.hasStage(player, stage)).orElse(true);
        if (!stageAllowed) {
            return false;
        }

        return UnlockRuleLoader.canUnlock(player, "recipe:" + recipeId);
    }

    public static boolean grantStage(ServerPlayer player, String stageId) {
        return ProgressionStage.byName(stageId)
                .map(stage -> grantStage(player, stage))
                .orElse(false);
    }

    public static boolean grantStage(ServerPlayer player, ProgressionStage stage) {
        if (player == null || stage == null) {
            return false;
        }
        return StageManager.upgradePlayerStage(player, stage);
    }
}
