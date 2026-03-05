package com.extremecraft.progression;

import com.extremecraft.progression.stage.ProgressionStage;
import com.extremecraft.progression.stage.StageManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class ProgressionGate {
    private static final Map<String, ProgressionStage> MACHINE_REQUIREMENTS = new HashMap<>();
    private static final Map<String, ProgressionStage> RECIPE_REQUIREMENTS = new HashMap<>();

    static {
        registerMachineRequirement("pulverizer", ProgressionStage.INDUSTRIAL);
    }

    private ProgressionGate() {}

    public static void registerMachineRequirement(String machineId, ProgressionStage stage) {
        MACHINE_REQUIREMENTS.put(machineId, stage);
    }

    public static void registerRecipeRequirement(String recipeId, ProgressionStage stage) {
        RECIPE_REQUIREMENTS.put(recipeId, stage);
    }

    public static Optional<ProgressionStage> requiredMachineStage(String machineId) {
        ProgressionStage direct = MACHINE_REQUIREMENTS.get(machineId);
        if (direct != null) {
            return Optional.of(direct);
        }

        return StageDataLoader.requiredStageForUnlock("machine:" + machineId);
    }

    public static Optional<ProgressionStage> requiredRecipeStage(String recipeId) {
        ProgressionStage direct = RECIPE_REQUIREMENTS.get(recipeId);
        if (direct != null) {
            return Optional.of(direct);
        }

        return StageDataLoader.requiredStageForUnlock("recipe:" + recipeId);
    }

    public static boolean canUseMachine(Player player, String machineId) {
        return requiredMachineStage(machineId).map(stage -> StageManager.hasStage(player, stage)).orElse(true);
    }

    public static boolean canUseRecipe(Player player, ResourceLocation recipeId) {
        return canUseRecipe(player, recipeId.toString());
    }

    public static boolean canUseRecipe(Player player, String recipeId) {
        return requiredRecipeStage(recipeId).map(stage -> StageManager.hasStage(player, stage)).orElse(true);
    }
}
