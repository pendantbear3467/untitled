package com.extremecraft.worldgen.validation;

import com.extremecraft.core.ECConstants;
import com.extremecraft.machine.material.OreMaterialCatalog;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Validates that ore catalog ids are represented by configured/placed/biome files.
 */
public class WorldgenConsistencyValidator {
    private static final Logger LOGGER = Logger.getLogger("ExtremeCraft");

    @SubscribeEvent
    public void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new Loader());
    }

    private static final class Loader extends SimplePreparableReloadListener<ValidationReport> {
        @Override
        protected ValidationReport prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
            Set<String> configured = collectIds(resourceManager, "worldgen/configured_feature");
            Set<String> placed = collectIds(resourceManager, "worldgen/placed_feature");
            Set<String> modifiers = collectIds(resourceManager, "forge/biome_modifier");
            return new ValidationReport(configured, placed, modifiers);
        }

        @Override
        protected void apply(ValidationReport report, ResourceManager resourceManager, ProfilerFiller profiler) {
            for (String materialId : OreMaterialCatalog.MATERIALS.keySet()) {
                String expected = featureIdFor(materialId);
                String modifierId = "add_" + expected;

                if (!report.configured().contains(expected)) {
                    LOGGER.warning("[Worldgen] Missing configured feature for material: " + materialId + " (expected " + expected + ")");
                }
                if (!report.placed().contains(expected)) {
                    LOGGER.warning("[Worldgen] Missing placed feature for material: " + materialId + " (expected " + expected + ")");
                }
                if (!report.modifiers().contains(modifierId)) {
                    LOGGER.warning("[Worldgen] Missing biome modifier for material: " + materialId + " (expected " + modifierId + ")");
                }
            }

            report.configured().stream()
                    .filter(id -> id.contains("_ore_ore"))
                    .forEach(id -> LOGGER.warning("[Worldgen] Suspicious duplicated ore suffix found: " + id));
        }

        private static Set<String> collectIds(ResourceManager resourceManager, String folder) {
            Map<ResourceLocation, net.minecraft.server.packs.resources.Resource> resources = resourceManager.listResources(
                    folder,
                    path -> path.getPath().endsWith(".json") && ECConstants.MODID.equals(path.getNamespace())
            );

            Set<String> ids = new HashSet<>();
            for (ResourceLocation path : resources.keySet()) {
                String fullPath = path.getPath();
                int folderIdx = fullPath.lastIndexOf('/');
                String fileName = folderIdx >= 0 ? fullPath.substring(folderIdx + 1) : fullPath;
                if (fileName.endsWith(".json")) {
                    ids.add(fileName.substring(0, fileName.length() - 5));
                }
            }
            return ids;
        }

        private static String featureIdFor(String materialId) {
            String id = materialId == null ? "" : materialId.trim().toLowerCase();
            return id.endsWith("_ore") ? id : id + "_ore";
        }
    }

    private record ValidationReport(Set<String> configured, Set<String> placed, Set<String> modifiers) {
    }
}
