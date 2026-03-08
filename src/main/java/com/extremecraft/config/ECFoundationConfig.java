package com.extremecraft.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

public final class ECFoundationConfig {
    public static final ForgeConfigSpec COMMON_SPEC;
    public static final Common COMMON;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        COMMON = new Common(builder);
        COMMON_SPEC = builder.build();
    }

    private ECFoundationConfig() {
    }

    public static void register() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, COMMON_SPEC, "extremecraft-foundation.toml");
    }

    public static boolean isProfilerEnabled() {
        return COMMON.performance.profilerEnabled.get();
    }

    public static int structureInvalidationRange() {
        return COMMON.performance.structureInvalidationRange.get();
    }

    public static int aetherChunkBaseReserve() {
        return COMMON.aether.chunkBaseReserve.get();
    }

    public static int aetherChunkMaxReserve() {
        return COMMON.aether.chunkMaxReserve.get();
    }

    public static int aetherRechargeIntervalTicks() {
        return COMMON.aether.rechargeIntervalTicks.get();
    }

    public static int aetherRechargePerPulse() {
        return COMMON.aether.rechargePerPulse.get();
    }

    public static boolean isRadiationEnabled() {
        return COMMON.radiation.enableRadiation.get();
    }

    public static int radiationSampleIntervalTicks() {
        return COMMON.radiation.sampleIntervalTicks.get();
    }

    public static int radiationMaxSampleBlocks() {
        return COMMON.radiation.maxSampleBlocks.get();
    }

    public static int contaminationDecayIntervalTicks() {
        return COMMON.radiation.contaminationDecayIntervalTicks.get();
    }

    public static double contaminationDecayAmount() {
        return COMMON.radiation.contaminationDecayAmount.get();
    }

    public static int reactorValidationIntervalTicks() {
        return COMMON.reactors.validationIntervalTicks.get();
    }

    public static double reactorMaxHeat() {
        return COMMON.reactors.maxHeat.get();
    }

    public static double reactorScramHeatThreshold() {
        return COMMON.reactors.scramHeatThreshold.get();
    }

    public static int reactorMeltdownRadius() {
        return COMMON.reactors.meltdownRadius.get();
    }

    public static int reactorMeltdownBlockBudget() {
        return COMMON.reactors.meltdownBlockBudget.get();
    }

    public static int endgameValidationIntervalTicks() {
        return COMMON.endgame.validationIntervalTicks.get();
    }

    public static double endgameMaxInstability() {
        return COMMON.endgame.maxInstability.get();
    }

    public static int endgameOverchargePulseIntervalTicks() {
        return COMMON.endgame.overchargePulseIntervalTicks.get();
    }

    public static boolean enableWorldEdits() {
        return COMMON.safety.enableWorldEdits.get();
    }

    public static int catastrophicMaxRadius() {
        return COMMON.safety.catastrophicMaxRadius.get();
    }

    public static int catastrophicMaxAffectedBlocks() {
        return COMMON.safety.catastrophicMaxAffectedBlocks.get();
    }

    public static int destructivePulseBatchSize() {
        return COMMON.safety.destructivePulseBatchSize.get();
    }

    public static final class Common {
        public final Aether aether;
        public final Radiation radiation;
        public final Reactors reactors;
        public final Endgame endgame;
        public final Safety safety;
        public final Performance performance;

        private Common(ForgeConfigSpec.Builder builder) {
            aether = new Aether(builder);
            radiation = new Radiation(builder);
            reactors = new Reactors(builder);
            endgame = new Endgame(builder);
            safety = new Safety(builder);
            performance = new Performance(builder);
        }
    }

    public static final class Aether {
        public final ForgeConfigSpec.IntValue chunkBaseReserve;
        public final ForgeConfigSpec.IntValue chunkMaxReserve;
        public final ForgeConfigSpec.IntValue rechargeIntervalTicks;
        public final ForgeConfigSpec.IntValue rechargePerPulse;

        private Aether(ForgeConfigSpec.Builder builder) {
            builder.push("aether");
            chunkBaseReserve = builder.defineInRange("chunkBaseReserve", 250, 0, 100000);
            chunkMaxReserve = builder.defineInRange("chunkMaxReserve", 1200, 0, 100000);
            rechargeIntervalTicks = builder.defineInRange("rechargeIntervalTicks", 40, 1, 2400);
            rechargePerPulse = builder.defineInRange("rechargePerPulse", 30, 0, 10000);
            builder.pop();
        }
    }

    public static final class Radiation {
        public final ForgeConfigSpec.BooleanValue enableRadiation;
        public final ForgeConfigSpec.IntValue sampleIntervalTicks;
        public final ForgeConfigSpec.IntValue maxSampleBlocks;
        public final ForgeConfigSpec.IntValue contaminationDecayIntervalTicks;
        public final ForgeConfigSpec.DoubleValue contaminationDecayAmount;

        private Radiation(ForgeConfigSpec.Builder builder) {
            builder.push("radiation");
            enableRadiation = builder.define("enableRadiation", true);
            sampleIntervalTicks = builder.defineInRange("sampleIntervalTicks", 40, 5, 1200);
            maxSampleBlocks = builder.defineInRange("maxSampleBlocks", 96, 8, 4096);
            contaminationDecayIntervalTicks = builder.defineInRange("contaminationDecayIntervalTicks", 200, 20, 24000);
            contaminationDecayAmount = builder.defineInRange("contaminationDecayAmount", 1.0D, 0.0D, 1000.0D);
            builder.pop();
        }
    }

    public static final class Reactors {
        public final ForgeConfigSpec.IntValue validationIntervalTicks;
        public final ForgeConfigSpec.DoubleValue maxHeat;
        public final ForgeConfigSpec.DoubleValue scramHeatThreshold;
        public final ForgeConfigSpec.IntValue meltdownRadius;
        public final ForgeConfigSpec.IntValue meltdownBlockBudget;

        private Reactors(ForgeConfigSpec.Builder builder) {
            builder.push("reactors");
            validationIntervalTicks = builder.defineInRange("validationIntervalTicks", 40, 1, 2400);
            maxHeat = builder.defineInRange("maxHeat", 2000.0D, 100.0D, 100000.0D);
            scramHeatThreshold = builder.defineInRange("scramHeatThreshold", 1400.0D, 50.0D, 100000.0D);
            meltdownRadius = builder.defineInRange("meltdownRadius", 6, 1, 64);
            meltdownBlockBudget = builder.defineInRange("meltdownBlockBudget", 96, 0, 10000);
            builder.pop();
        }
    }

    public static final class Endgame {
        public final ForgeConfigSpec.IntValue validationIntervalTicks;
        public final ForgeConfigSpec.DoubleValue maxInstability;
        public final ForgeConfigSpec.IntValue overchargePulseIntervalTicks;

        private Endgame(ForgeConfigSpec.Builder builder) {
            builder.push("endgame");
            validationIntervalTicks = builder.defineInRange("validationIntervalTicks", 40, 1, 2400);
            maxInstability = builder.defineInRange("maxInstability", 100.0D, 1.0D, 100000.0D);
            overchargePulseIntervalTicks = builder.defineInRange("overchargePulseIntervalTicks", 20, 1, 2400);
            builder.pop();
        }
    }

    public static final class Safety {
        public final ForgeConfigSpec.BooleanValue enableWorldEdits;
        public final ForgeConfigSpec.IntValue catastrophicMaxRadius;
        public final ForgeConfigSpec.IntValue catastrophicMaxAffectedBlocks;
        public final ForgeConfigSpec.IntValue destructivePulseBatchSize;

        private Safety(ForgeConfigSpec.Builder builder) {
            builder.push("safety");
            enableWorldEdits = builder.define("enableWorldEdits", true);
            catastrophicMaxRadius = builder.defineInRange("catastrophicMaxRadius", 12, 0, 128);
            catastrophicMaxAffectedBlocks = builder.defineInRange("catastrophicMaxAffectedBlocks", 128, 0, 20000);
            destructivePulseBatchSize = builder.defineInRange("destructivePulseBatchSize", 16, 1, 1024);
            builder.pop();
        }
    }

    public static final class Performance {
        public final ForgeConfigSpec.BooleanValue profilerEnabled;
        public final ForgeConfigSpec.IntValue structureInvalidationRange;

        private Performance(ForgeConfigSpec.Builder builder) {
            builder.push("performance");
            profilerEnabled = builder.define("profilerEnabled", true);
            structureInvalidationRange = builder.defineInRange("structureInvalidationRange", 8, 1, 32);
            builder.pop();
        }
    }
}
