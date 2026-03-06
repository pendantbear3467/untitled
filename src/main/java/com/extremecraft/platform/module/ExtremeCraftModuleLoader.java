package com.extremecraft.platform.module;

import com.extremecraft.api.module.ExtremeCraftModule;
import com.extremecraft.api.registration.ExtremeCraftApiProvider;
import com.extremecraft.platform.CompatibilityGate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ServiceLoader;

public final class ExtremeCraftModuleLoader {
    private static final Logger LOGGER = LogManager.getLogger();

    private ExtremeCraftModuleLoader() {
    }

    public static void loadAll(ExtremeCraftApiProvider apiProvider) {
        int discovered = 0;
        int loaded = 0;

        for (ExtremeCraftModule module : ServiceLoader.load(ExtremeCraftModule.class)) {
            discovered++;
            if (!CompatibilityGate.isCompatible(module)) {
                LOGGER.warn("Skipping incompatible module {} (api={}, protocol={})",
                        module.moduleId(), module.apiVersion(), module.protocolVersion());
                continue;
            }

            try {
                module.register(apiProvider);
                ModuleRegistry.register(module);
                loaded++;
                LOGGER.info("Loaded ExtremeCraft module {} ({})", module.moduleId(), module.moduleName());
            } catch (Exception ex) {
                LOGGER.error("Failed to load module {}", module.moduleId(), ex);
            }
        }

        LOGGER.info("ExtremeCraft module discovery complete. discovered={}, loaded={}", discovered, loaded);
    }
}
