package com.extremecraft.platform.module;

import com.extremecraft.api.module.ExtremeCraftModule;
import com.extremecraft.api.registration.ExtremeCraftApiProvider;
import com.extremecraft.platform.CompatibilityGate;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraftforge.fml.loading.FMLPaths;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

public final class ExtremeCraftModuleLoader {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Gson GSON = new Gson();
    private static final List<URLClassLoader> EXTERNAL_LOADERS = new ArrayList<>();

    private ExtremeCraftModuleLoader() {
    }

    public static void loadAll(ExtremeCraftApiProvider apiProvider) {
        Map<String, ExtremeCraftModule> discovered = new LinkedHashMap<>();

        ClassLoader contextLoader = Thread.currentThread().getContextClassLoader();
        discoverServiceLoaderModules(discovered, contextLoader);
        discoverManifestModules(discovered, contextLoader);
        discoverExternalModuleJars(discovered, contextLoader);

        int loaded = 0;
        for (ExtremeCraftModule module : discovered.values()) {
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

        LOGGER.info("ExtremeCraft module discovery complete. discovered={}, loaded={}", discovered.size(), loaded);
    }

    private static void discoverExternalModuleJars(Map<String, ExtremeCraftModule> out, ClassLoader parentLoader) {
        Path modulesDir = FMLPaths.GAMEDIR.get().resolve("extremecraft").resolve("modules");
        if (!Files.isDirectory(modulesDir)) {
            return;
        }

        List<URL> jarUrls = new ArrayList<>();
        try (var stream = Files.list(modulesDir)) {
            stream.filter(path -> path.getFileName().toString().endsWith(".jar"))
                    .forEach(path -> {
                        try {
                            jarUrls.add(path.toUri().toURL());
                        } catch (Exception ex) {
                            LOGGER.warn("Skipping invalid module jar path {}", path, ex);
                        }
                    });
        } catch (Exception ex) {
            LOGGER.warn("Failed to scan external module directory {}", modulesDir, ex);
            return;
        }

        if (jarUrls.isEmpty()) {
            return;
        }

        URLClassLoader externalLoader = new URLClassLoader(jarUrls.toArray(new URL[0]), parentLoader);
        EXTERNAL_LOADERS.add(externalLoader);

        discoverServiceLoaderModules(out, externalLoader);
        discoverManifestModules(out, externalLoader);
        LOGGER.info("Discovered external ExtremeCraft module jars: {}", jarUrls.size());
    }

    private static void discoverServiceLoaderModules(Map<String, ExtremeCraftModule> out, ClassLoader classLoader) {
        for (ExtremeCraftModule module : ServiceLoader.load(ExtremeCraftModule.class, classLoader)) {
            out.putIfAbsent(module.moduleId(), module);
        }
    }

    private static void discoverManifestModules(Map<String, ExtremeCraftModule> out, ClassLoader classLoader) {
        try {
            Enumeration<URL> resources = classLoader.getResources("META-INF/extremecraft.modules.json");

            while (resources.hasMoreElements()) {
                URL url = resources.nextElement();
                try (var stream = url.openStream();
                     var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                    JsonObject root = GSON.fromJson(reader, JsonObject.class);
                    if (root == null || !root.has("modules") || !root.get("modules").isJsonArray()) {
                        continue;
                    }

                    JsonArray modules = root.getAsJsonArray("modules");
                    for (var element : modules) {
                        String className = element.getAsString().trim();
                        if (className.isBlank()) {
                            continue;
                        }

                        try {
                            Class<?> clazz = Class.forName(className, true, classLoader);
                            Object instance = clazz.getDeclaredConstructor().newInstance();
                            if (instance instanceof ExtremeCraftModule module) {
                                out.putIfAbsent(module.moduleId(), module);
                            }
                        } catch (Exception ex) {
                            LOGGER.warn("Failed loading module class from manifest {}: {}", className, ex.getMessage());
                        }
                    }
                }
            }
        } catch (Exception ex) {
            LOGGER.warn("Failed scanning META-INF/extremecraft.modules.json manifests", ex);
        }
    }
}
