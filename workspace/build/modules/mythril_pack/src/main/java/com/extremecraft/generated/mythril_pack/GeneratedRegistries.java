package com.extremecraft.generated.mythril_pack;

import net.minecraftforge.eventbus.api.IEventBus;

public final class GeneratedRegistries {
    public static final String MODID = "mythril_pack";

    private GeneratedRegistries() {}

    public static void register(IEventBus modBus) {
        GeneratedItems.register(modBus);
        GeneratedBlocks.register(modBus);
        GeneratedMachines.register(modBus);
        GeneratedRecipes.touch();
        GeneratedWorldgen.touch();
    }
}
