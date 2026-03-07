package com.extremecraft.registry;

import com.extremecraft.core.ECConstants;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.MissingMappingsEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Remaps known legacy IDs so old saves keep loading after content ID normalization.
 */
@Mod.EventBusSubscriber(modid = ECConstants.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class LegacyMappingHandler {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Map<String, String> EXPLICIT_REMAPS = createExplicitRemaps();

    private LegacyMappingHandler() {
    }

    @SubscribeEvent
    public static void onMissingMappings(MissingMappingsEvent event) {
        remapBlockMappings(event);
        remapItemMappings(event);
    }

    private static void remapBlockMappings(MissingMappingsEvent event) {
        for (MissingMappingsEvent.Mapping<Block> mapping : event.getMappings(ForgeRegistries.Keys.BLOCKS, ECConstants.MODID)) {
            String legacyPath = mapping.getKey().getPath();
            String replacementPath = remapPathFor(legacyPath);
            if (replacementPath == null) {
                continue;
            }

            Block replacement = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(ECConstants.MODID, replacementPath));
            if (replacement != null) {
                mapping.remap(replacement);
                LOGGER.info("[Registry] Remapped legacy block id {}:{} -> {}:{}",
                        ECConstants.MODID, legacyPath, ECConstants.MODID, replacementPath);
            } else {
                LOGGER.warn("[Registry] Could not remap legacy block id {}:{} because replacement is missing",
                        ECConstants.MODID, replacementPath);
            }
        }
    }

    private static void remapItemMappings(MissingMappingsEvent event) {
        for (MissingMappingsEvent.Mapping<Item> mapping : event.getMappings(ForgeRegistries.Keys.ITEMS, ECConstants.MODID)) {
            String legacyPath = mapping.getKey().getPath();
            String replacementPath = remapPathFor(legacyPath);
            if (replacementPath == null) {
                continue;
            }

            Item replacement = ForgeRegistries.ITEMS.getValue(new ResourceLocation(ECConstants.MODID, replacementPath));
            if (replacement != null) {
                mapping.remap(replacement);
                LOGGER.info("[Registry] Remapped legacy item id {}:{} -> {}:{}",
                        ECConstants.MODID, legacyPath, ECConstants.MODID, replacementPath);
            } else {
                LOGGER.warn("[Registry] Could not remap legacy item id {}:{} because replacement is missing",
                        ECConstants.MODID, replacementPath);
            }
        }
    }

    private static Map<String, String> createExplicitRemaps() {
        Map<String, String> remaps = new LinkedHashMap<>();
        remaps.put("singularity_ore_ore", "singularity_ore");
        return Map.copyOf(remaps);
    }

    private static String remapPathFor(String legacyPath) {
        if (legacyPath == null || legacyPath.isBlank()) {
            return null;
        }

        String explicit = EXPLICIT_REMAPS.get(legacyPath);
        if (explicit != null) {
            return explicit;
        }

        if (legacyPath.contains("_ore_ore")) {
            return legacyPath.replace("_ore_ore", "_ore");
        }

        return null;
    }
}
