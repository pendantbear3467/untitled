package com.extremecraft.world;

import com.extremecraft.worldgen.OreGenerationProfiles;

/**
 * World module entrypoint for staged ore generation rollouts.
 */
public final class WorldModule {
    public static int profileCount() {
        return OreGenerationProfiles.all().size();
    }

    private WorldModule() {}
}
