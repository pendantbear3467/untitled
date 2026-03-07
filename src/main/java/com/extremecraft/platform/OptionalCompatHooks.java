package com.extremecraft.platform;

import com.extremecraft.config.Config;
import net.minecraftforge.fml.ModList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Lightweight optional-mod bootstrap.
 *
 * <p>Keep this class free of hard references to optional mod classes so missing
 * dependencies never crash dedicated servers or modpacks.</p>
 */
public final class OptionalCompatHooks {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Map<String, Runnable> COMPAT_BOOTSTRAP = createCompatBootstrap();

    private OptionalCompatHooks() {
    }

    public static void bootstrap() {
        for (Map.Entry<String, Runnable> entry : COMPAT_BOOTSTRAP.entrySet()) {
            String modId = entry.getKey();
            if (!isCompatEnabled(modId)) {
                LOGGER.debug("[Compat] Optional dependency '{}' is disabled by config; skipping compat hooks", modId);
                continue;
            }

            if (!ModList.get().isLoaded(modId)) {
                LOGGER.debug("[Compat] Optional dependency '{}' not present; skipping compat hooks", modId);
                continue;
            }

            try {
                entry.getValue().run();
                LOGGER.info("[Compat] Enabled optional compatibility hooks for '{}'", modId);
            } catch (RuntimeException ex) {
                LOGGER.warn("[Compat] Failed to initialize optional compatibility for '{}': {}", modId, ex.getMessage());
            }
        }
    }

    private static Map<String, Runnable> createCompatBootstrap() {
        Map<String, Runnable> hooks = new LinkedHashMap<>();
        hooks.put("curios", OptionalCompatHooks::initCuriosCompat);
        hooks.put("jei", OptionalCompatHooks::initJeiCompat);
        hooks.put("geckolib", OptionalCompatHooks::initGeckoLibCompat);
        return Map.copyOf(hooks);
    }

    private static boolean isCompatEnabled(String modId) {
        return switch (modId) {
            case "curios" -> Config.isCuriosCompatEnabled();
            case "jei" -> Config.isJeiCompatEnabled();
            case "geckolib" -> Config.isGeckoLibCompatEnabled();
            default -> true;
        };
    }

    private static void initCuriosCompat() {
        // Placeholder for future Curios integration.
    }

    private static void initJeiCompat() {
        // Placeholder for future JEI integration.
    }

    private static void initGeckoLibCompat() {
        // Placeholder for future GeckoLib animation integration.
    }
}

