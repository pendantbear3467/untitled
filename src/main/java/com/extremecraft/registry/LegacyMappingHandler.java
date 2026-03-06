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

/**
 * Remaps known legacy IDs so old saves keep loading after content ID normalization.
 */
@Mod.EventBusSubscriber(modid = ECConstants.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class LegacyMappingHandler {
    private static final Logger LOGGER = LogManager.getLogger();

    private static final String LEGACY_SINGULARITY_ORE = "singularity_ore_ore";
    private static final String CURRENT_SINGULARITY_ORE = "singularity_ore";

    private LegacyMappingHandler() {
    }

    @SubscribeEvent
    public static void onMissingMappings(MissingMappingsEvent event) {
        remapBlock(event);
        remapItem(event);
    }

    private static void remapBlock(MissingMappingsEvent event) {
        for (MissingMappingsEvent.Mapping<Block> mapping : event.getMappings(ForgeRegistries.Keys.BLOCKS, ECConstants.MODID)) {
            if (!LEGACY_SINGULARITY_ORE.equals(mapping.getKey().getPath())) {
                continue;
            }

            Block replacement = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(ECConstants.MODID, CURRENT_SINGULARITY_ORE));
            if (replacement != null) {
                mapping.remap(replacement);
                LOGGER.info("[Registry] Remapped legacy block id {}:{} -> {}:{}",
                        ECConstants.MODID, LEGACY_SINGULARITY_ORE, ECConstants.MODID, CURRENT_SINGULARITY_ORE);
            } else {
                LOGGER.warn("[Registry] Could not remap legacy block id {}:{} because replacement is missing",
                        ECConstants.MODID, CURRENT_SINGULARITY_ORE);
            }
        }
    }

    private static void remapItem(MissingMappingsEvent event) {
        for (MissingMappingsEvent.Mapping<Item> mapping : event.getMappings(ForgeRegistries.Keys.ITEMS, ECConstants.MODID)) {
            if (!LEGACY_SINGULARITY_ORE.equals(mapping.getKey().getPath())) {
                continue;
            }

            Item replacement = ForgeRegistries.ITEMS.getValue(new ResourceLocation(ECConstants.MODID, CURRENT_SINGULARITY_ORE));
            if (replacement != null) {
                mapping.remap(replacement);
                LOGGER.info("[Registry] Remapped legacy item id {}:{} -> {}:{}",
                        ECConstants.MODID, LEGACY_SINGULARITY_ORE, ECConstants.MODID, CURRENT_SINGULARITY_ORE);
            } else {
                LOGGER.warn("[Registry] Could not remap legacy item id {}:{} because replacement is missing",
                        ECConstants.MODID, CURRENT_SINGULARITY_ORE);
            }
        }
    }
}
