package com.extremecraft.config;

import com.extremecraft.core.ECConstants;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class Config {
    private static final Logger LOGGER = LogManager.getLogger();

    public static final ForgeConfigSpec COMMON_SPEC;
    public static final Common COMMON;

    private static volatile Set<String> DISABLED_MACHINE_IDS = Set.of();
    private static volatile Set<String> DISABLED_MOB_IDS = Set.of();

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        COMMON = new Common(builder);
        COMMON_SPEC = builder.build();
    }

    private Config() {
    }

    public static void register() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, COMMON_SPEC, "extremecraft-common.toml");
        LOGGER.debug("[Config] Registered common config extremecraft-common.toml");
    }

    public static boolean isMachineEnabled(String machineId) {
        if (!safeBoolean(COMMON.machines.enableMachines, true)) {
            return false;
        }

        String normalized = normalizeMachineId(machineId);
        return normalized.isBlank() || !DISABLED_MACHINE_IDS.contains(normalized);
    }

    public static boolean isMobDisabled(EntityType<?> entityType) {
        if (entityType == null) {
            return false;
        }

        var key = ForgeRegistries.ENTITY_TYPES.getKey(entityType);
        if (key == null) {
            return false;
        }

        return DISABLED_MOB_IDS.contains(normalizeResourceId(key.toString()));
    }

    public static boolean isNaturalMobSpawningEnabledFor(EntityType<?> entityType) {
        if (!safeBoolean(COMMON.mobs.enableNaturalSpawns, true)) {
            return false;
        }
        return !isMobDisabled(entityType);
    }

    private static void rebuildCaches() {
        try {
            DISABLED_MACHINE_IDS = parseMachineIds(COMMON.machines.disabledMachineIds.get());
            DISABLED_MOB_IDS = parseResourceIds(COMMON.mobs.disabledMobIds.get());
        } catch (IllegalStateException ignored) {
            // Config values are not available yet in early mod bootstrap.
            LOGGER.debug("[Config] Deferred cache rebuild because config values are not loaded yet");
        }
    }

    private static boolean safeBoolean(ForgeConfigSpec.BooleanValue value, boolean defaultValue) {
        try {
            return value.get();
        } catch (IllegalStateException ignored) {
            return defaultValue;
        }
    }

    private static Set<String> parseMachineIds(List<? extends String> rawIds) {
        Set<String> parsed = new LinkedHashSet<>();
        for (String raw : rawIds) {
            String normalized = normalizeMachineId(raw);
            if (!normalized.isBlank()) {
                parsed.add(normalized);
            }
        }
        return Set.copyOf(parsed);
    }

    private static Set<String> parseResourceIds(List<? extends String> rawIds) {
        Set<String> parsed = new LinkedHashSet<>();
        for (String raw : rawIds) {
            String normalized = normalizeResourceId(raw);
            if (!normalized.isBlank()) {
                parsed.add(normalized);
            }
        }
        return Set.copyOf(parsed);
    }

    private static String normalizeMachineId(String id) {
        if (id == null) {
            return "";
        }

        String normalized = id.trim().toLowerCase(Locale.ROOT);
        if (normalized.isBlank()) {
            return "";
        }

        int separator = normalized.indexOf(':');
        if (separator >= 0 && separator < normalized.length() - 1) {
            return normalized.substring(separator + 1);
        }

        return normalized;
    }

    private static String normalizeResourceId(String id) {
        if (id == null) {
            return "";
        }

        String normalized = id.trim().toLowerCase(Locale.ROOT);
        if (normalized.isBlank()) {
            return "";
        }

        if (!normalized.contains(":")) {
            return ECConstants.MODID + ":" + normalized;
        }

        return normalized;
    }

    public static final class Common {
        public final Machines machines;
        public final Tools tools;
        public final Mobs mobs;

        private Common(ForgeConfigSpec.Builder builder) {
            this.machines = new Machines(builder);
            this.tools = new Tools(builder);
            this.mobs = new Mobs(builder);
        }
    }

    public static final class Machines {
        public final ForgeConfigSpec.BooleanValue enableMachines;
        public final ForgeConfigSpec.IntValue machineTickInterval;
        public final ForgeConfigSpec.IntValue cableTickInterval;
        public final ForgeConfigSpec.IntValue recipeLookupIntervalTicks;
        public final ForgeConfigSpec.IntValue neighborEnergyPushPerSide;
        public final ForgeConfigSpec.BooleanValue enableFallbackRecipes;
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> disabledMachineIds;

        private Machines(ForgeConfigSpec.Builder builder) {
            builder.push("machines");
            enableMachines = builder.comment("Master switch for machine processing and generation ticks.")
                    .define("enableMachines", true);
            machineTickInterval = builder.comment("How often machines run (1 = every tick, 2 = every other tick).")
                    .defineInRange("machineTickInterval", 1, 1, 20);
            cableTickInterval = builder.comment("How often cables transfer energy.")
                    .defineInRange("cableTickInterval", 1, 1, 20);
            recipeLookupIntervalTicks = builder.comment("Cooldown between expensive recipe lookups per machine.")
                    .defineInRange("recipeLookupIntervalTicks", 8, 1, 200);
            neighborEnergyPushPerSide = builder.comment("Maximum FE pushed from machines to each neighbor per machine tick.")
                    .defineInRange("neighborEnergyPushPerSide", 1200, 0, 200_000);
            enableFallbackRecipes = builder.comment("Enable heuristic fallback processing for ores/dusts when no recipe exists.")
                    .define("enableFallbackRecipes", true);
            disabledMachineIds = builder.comment("Machine IDs to disable, e.g. [\"crusher\", \"extremecraft:quantum_fabricator\"].")
                    .defineListAllowEmpty(
                            List.of("disabledMachineIds"),
                            List::of,
                            Config::isMachineIdLike
                    );
            builder.pop();
        }
    }

    public static final class Tools {
        public final ForgeConfigSpec.BooleanValue enableHammerAoe;
        public final ForgeConfigSpec.IntValue hammerAoeRadius;
        public final ForgeConfigSpec.DoubleValue hammerMaxMineableHardness;

        public final ForgeConfigSpec.BooleanValue enableDrillTeleport;
        public final ForgeConfigSpec.DoubleValue drillTeleportBaseDistance;
        public final ForgeConfigSpec.DoubleValue drillTeleportDistancePerLevel;
        public final ForgeConfigSpec.IntValue drillTeleportCooldownBaseTicks;
        public final ForgeConfigSpec.IntValue drillTeleportCooldownReductionPerLevel;
        public final ForgeConfigSpec.IntValue drillTeleportMinCooldownTicks;

        private Tools(ForgeConfigSpec.Builder builder) {
            builder.push("tools");
            enableHammerAoe = builder.comment("Enable hammer multi-block mining.")
                    .define("enableHammerAoe", true);
            hammerAoeRadius = builder.comment("Hammer AOE radius (0=single block, 1=3x3, 2=5x5).")
                    .defineInRange("hammerAoeRadius", 1, 0, 2);
            hammerMaxMineableHardness = builder.comment("Maximum hardness hammer AOE can break.")
                    .defineInRange("hammerMaxMineableHardness", 50.0D, 0.0D, 500.0D);

            enableDrillTeleport = builder.comment("Enable shift-right-click teleport on modular drill.")
                    .define("enableDrillTeleport", true);
            drillTeleportBaseDistance = builder.comment("Base drill teleport distance.")
                    .defineInRange("drillTeleportBaseDistance", 4.0D, 1.0D, 32.0D);
            drillTeleportDistancePerLevel = builder.comment("Extra drill teleport distance per module level.")
                    .defineInRange("drillTeleportDistancePerLevel", 2.0D, 0.0D, 16.0D);
            drillTeleportCooldownBaseTicks = builder.comment("Base cooldown for drill teleport.")
                    .defineInRange("drillTeleportCooldownBaseTicks", 80, 1, 1200);
            drillTeleportCooldownReductionPerLevel = builder.comment("Cooldown reduction per teleport module level.")
                    .defineInRange("drillTeleportCooldownReductionPerLevel", 10, 0, 200);
            drillTeleportMinCooldownTicks = builder.comment("Minimum cooldown for drill teleport.")
                    .defineInRange("drillTeleportMinCooldownTicks", 20, 0, 1200);
            builder.pop();
        }
    }

    public static final class Mobs {
        public final ForgeConfigSpec.BooleanValue enableNaturalSpawns;
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> disabledMobIds;
        public final ForgeConfigSpec.IntValue maxNearbySameMob;
        public final ForgeConfigSpec.DoubleValue nearbyMobCheckRadius;

        public final ForgeConfigSpec.BooleanValue enableBossArenaSpawns;
        public final ForgeConfigSpec.IntValue bossArenaCheckIntervalTicks;

        public final ForgeConfigSpec.IntValue energyParasiteDrainIntervalTicks;
        public final ForgeConfigSpec.IntValue energyParasiteDrainRadius;
        public final ForgeConfigSpec.IntValue energyParasiteMaxDrainPerPulse;

        public final ForgeConfigSpec.BooleanValue enableMachineHazard;
        public final ForgeConfigSpec.IntValue machineHazardScanIntervalTicks;
        public final ForgeConfigSpec.IntValue machineHazardHorizontalRadius;
        public final ForgeConfigSpec.IntValue machineHazardVerticalRadius;
        public final ForgeConfigSpec.IntValue machineHazardRequiredMachines;
        public final ForgeConfigSpec.DoubleValue machineHazardDamage;

        private Mobs(ForgeConfigSpec.Builder builder) {
            builder.push("mobs");
            enableNaturalSpawns = builder.comment("Master switch for natural/chunk-generated ExtremeCraft mob spawns.")
                    .define("enableNaturalSpawns", true);
            disabledMobIds = builder.comment("Mob IDs to disable from natural spawn checks, e.g. [\"extremecraft:arcane_wraith\"].")
                    .defineListAllowEmpty(
                            List.of("disabledMobIds"),
                            List::of,
                            Config::isResourceIdLike
                    );
            maxNearbySameMob = builder.comment("Natural spawn density cap per EC mob type around spawn attempts.")
                    .defineInRange("maxNearbySameMob", 14, 0, 512);
            nearbyMobCheckRadius = builder.comment("Radius for the mob density cap check.")
                    .defineInRange("nearbyMobCheckRadius", 48.0D, 4.0D, 256.0D);

            enableBossArenaSpawns = builder.comment("Allow structure-triggered boss arena spawns.")
                    .define("enableBossArenaSpawns", true);
            bossArenaCheckIntervalTicks = builder.comment("How often players run boss arena structure checks.")
                    .defineInRange("bossArenaCheckIntervalTicks", 40, 10, 1200);

            energyParasiteDrainIntervalTicks = builder.comment("Ticks between Energy Parasite machine-drain pulses.")
                    .defineInRange("energyParasiteDrainIntervalTicks", 40, 10, 1200);
            energyParasiteDrainRadius = builder.comment("Energy Parasite machine-drain horizontal radius.")
                    .defineInRange("energyParasiteDrainRadius", 4, 1, 12);
            energyParasiteMaxDrainPerPulse = builder.comment("Maximum FE drained per Energy Parasite pulse.")
                    .defineInRange("energyParasiteMaxDrainPerPulse", 260, 0, 20_000);

            enableMachineHazard = builder.comment("Enable machine-overload hazard checks near players.")
                    .define("enableMachineHazard", true);
            machineHazardScanIntervalTicks = builder.comment("Ticks between machine hazard scans per player.")
                    .defineInRange("machineHazardScanIntervalTicks", 100, 20, 2400);
            machineHazardHorizontalRadius = builder.comment("Horizontal scan radius for machine hazard checks.")
                    .defineInRange("machineHazardHorizontalRadius", 8, 1, 32);
            machineHazardVerticalRadius = builder.comment("Vertical scan radius for machine hazard checks.")
                    .defineInRange("machineHazardVerticalRadius", 3, 1, 16);
            machineHazardRequiredMachines = builder.comment("How many charged nearby machines trigger hazard effects.")
                    .defineInRange("machineHazardRequiredMachines", 4, 1, 64);
            machineHazardDamage = builder.comment("Damage dealt when machine hazard triggers without protection.")
                    .defineInRange("machineHazardDamage", 2.0D, 0.0D, 40.0D);
            builder.pop();
        }
    }

    private static boolean isMachineIdLike(Object value) {
        if (!(value instanceof String raw)) {
            return false;
        }
        return !normalizeMachineId(raw).isBlank();
    }

    private static boolean isResourceIdLike(Object value) {
        if (!(value instanceof String raw)) {
            return false;
        }
        return !normalizeResourceId(raw).isBlank();
    }

    @Mod.EventBusSubscriber(modid = ECConstants.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Events {
        private Events() {
        }

        @SubscribeEvent
        public static void onConfigLoad(ModConfigEvent.Loading event) {
            if (event.getConfig().getSpec() == COMMON_SPEC) {
                rebuildCaches();
                LOGGER.info("[Config] Loaded common config: disabledMachines={}, disabledMobs={}",
                        DISABLED_MACHINE_IDS.size(), DISABLED_MOB_IDS.size());
            }
        }

        @SubscribeEvent
        public static void onConfigReload(ModConfigEvent.Reloading event) {
            if (event.getConfig().getSpec() == COMMON_SPEC) {
                rebuildCaches();
                LOGGER.info("[Config] Reloaded common config: disabledMachines={}, disabledMobs={}",
                        DISABLED_MACHINE_IDS.size(), DISABLED_MOB_IDS.size());
            }
        }
    }
}
