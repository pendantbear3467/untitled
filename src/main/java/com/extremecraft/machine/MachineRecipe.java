package com.extremecraft.machine;

import java.util.Map;

public record MachineRecipe(
        String id,
        String machineId,
        Map<String, Integer> input,
        Map<String, Integer> output,
        int processTicks,
        int energyCost
) {
}
