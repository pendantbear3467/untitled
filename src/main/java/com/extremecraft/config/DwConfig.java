// com/darius/ExtremeCraftMod/config/DwConfig.java
package com.extremecraft.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;   // << correct
import net.minecraftforge.fml.config.ModConfig;  // << correct

import java.util.List;

public final class DwConfig {
    public static final ForgeConfigSpec CLIENT_SPEC;
    public static final Client CLIENT;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        CLIENT = new Client(b);
        CLIENT_SPEC = b.build();
    }

    public static void register() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, CLIENT_SPEC);
    }

    public static final class Client {
        public final ForgeConfigSpec.BooleanValue allowOffhandBlockBreaking;
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> blacklistedItems;

        Client(ForgeConfigSpec.Builder b) {
            b.push("client");
            allowOffhandBlockBreaking = b.define("allowOffhandBlockBreaking", true);
            blacklistedItems = b.defineList(
                    "blacklistedItems",
                    List.of("minecraft:debug_stick"),
                    o -> o instanceof String s && !s.isBlank()
            );
            b.pop();
        }
    }

    private DwConfig() {}
}
