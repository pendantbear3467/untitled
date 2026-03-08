package com.extremecraft.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

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
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, CLIENT_SPEC, "extremecraft-client.toml");
    }

    public static final class Client {
        public final ForgeConfigSpec.BooleanValue enableDualWield;
        public final ForgeConfigSpec.BooleanValue enableXPBarOverlay;
        public final ForgeConfigSpec.DoubleValue guiScaleMultiplier;
        public final ForgeConfigSpec.DoubleValue skillTreeZoom;
        public final ForgeConfigSpec.BooleanValue showSkillNodeTooltips;
        public final ForgeConfigSpec.BooleanValue enableManaHudOverlay;
        public final ForgeConfigSpec.BooleanValue enableRadiationHudOverlay;
        public final ForgeConfigSpec.IntValue hudAnchorX;
        public final ForgeConfigSpec.IntValue hudAnchorY;

        public final ForgeConfigSpec.BooleanValue allowOffhandBlockBreaking;
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> blacklistedItems;

        Client(ForgeConfigSpec.Builder b) {
            b.push("client");
            enableDualWield = b.define("enableDualWield", true);
            enableXPBarOverlay = b.define("enableXPBarOverlay", true);
            guiScaleMultiplier = b.defineInRange("guiScaleMultiplier", 1.0D, 0.75D, 1.75D);
            skillTreeZoom = b.defineInRange("skillTreeZoom", 1.0D, 0.50D, 2.00D);
            showSkillNodeTooltips = b.define("showSkillNodeTooltips", true);
            enableManaHudOverlay = b.define("enableManaHudOverlay", true);
            enableRadiationHudOverlay = b.define("enableRadiationHudOverlay", true);
            hudAnchorX = b.defineInRange("hudAnchorX", 0, -3200, 3200);
            hudAnchorY = b.defineInRange("hudAnchorY", 0, -3200, 3200);

            allowOffhandBlockBreaking = b.define("allowOffhandBlockBreaking", true);
            blacklistedItems = b.defineList(
                    "blacklistedItems",
                    List.of("minecraft:debug_stick"),
                    o -> o instanceof String s && !s.isBlank()
            );
            b.pop();
        }
    }

    private DwConfig() {
    }
}
