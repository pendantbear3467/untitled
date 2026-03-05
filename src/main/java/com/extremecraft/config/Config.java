package com.extremecraft.config;

import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.common.ForgeConfigSpec;

public final class Config {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.BooleanValue ENABLE_REACH_LIMIT =
            BUILDER.comment("Enable reach limit for offhand actions.").define("reach_limit.enable", true);

    public static final ForgeConfigSpec.BooleanValue ALLOW_OFFHAND_BOW =
            BUILDER.comment("Allow offhand bow/crossbow charging.").define("offhand_bow.allow", true);

    public static final ForgeConfigSpec.ConfigValue<String> ITEM_BLACKLIST =
            BUILDER.comment("Comma-separated list of blacklisted items for offhand.")
                   .define("item.blacklist", "");

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    private Config() {}

    public static void register(ModLoadingContext context) {
        context.registerConfig(ModConfig.Type.COMMON, SPEC);
    }
}
