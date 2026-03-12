package com.extremecraft.machine;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.registries.ForgeRegistries;

public final class MachineTooltipHandler {
    public static void onTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (id == null || !"extremecraft".equals(id.getNamespace())) {
            return;
        }

        String path = id.getPath();
        List<Component> tooltip = event.getToolTip();

        switch (path) {
            case "coal_generator" -> {
                tooltip.add(Component.literal("Generates: 40 FE/t while fuel burns").withStyle(ChatFormatting.GRAY));
                tooltip.add(Component.literal("Fuel: Coal or Charcoal").withStyle(ChatFormatting.DARK_GRAY));
                tooltip.add(Component.literal("Hint: Feed this into cables first").withStyle(ChatFormatting.YELLOW));
            }
            case "crusher" -> {
                tooltip.add(Component.literal("Uses: 16 FE/t").withStyle(ChatFormatting.GRAY));
                tooltip.add(Component.literal("Function: Doubles ores into raw materials").withStyle(ChatFormatting.DARK_GRAY));
                tooltip.add(Component.literal("Hint: Crusher -> Smelter is the canonical early metal chain").withStyle(ChatFormatting.YELLOW));
            }
            case "pulverizer" -> {
                tooltip.add(Component.literal("Uses: 24 FE/t").withStyle(ChatFormatting.GRAY));
                tooltip.add(Component.literal("Function: Legacy dust processor kept for compatibility").withStyle(ChatFormatting.DARK_GRAY));
                tooltip.add(Component.literal("Hint: Crusher is the canonical early unlock; this block stays for legacy setups").withStyle(ChatFormatting.YELLOW));
            }
            case "advanced_pulverizer" -> {
                tooltip.add(Component.literal("Uses: 24 FE/t").withStyle(ChatFormatting.GRAY));
                tooltip.add(Component.literal("Function: Converts processed materials into refined dusts").withStyle(ChatFormatting.DARK_GRAY));
                tooltip.add(Component.literal("Hint: Later-tier refinement machine that builds on the Crusher").withStyle(ChatFormatting.YELLOW));
            }
            case "fusion_reactor" -> {
                tooltip.add(Component.literal("Generates: high-output FE from the first-release reactor line").withStyle(ChatFormatting.GRAY));
                tooltip.add(Component.literal("Risk: heat, radiation, and contamination if safety fails").withStyle(ChatFormatting.DARK_GRAY));
                tooltip.add(Component.literal("Hint: Treat this as the fission-age reactor and keep SCRAM margin ready").withStyle(ChatFormatting.YELLOW));
            }
            case "smelter" -> {
                tooltip.add(Component.literal("Uses: 18 FE/t").withStyle(ChatFormatting.GRAY));
                tooltip.add(Component.literal("Function: Smelts dusts into ingots").withStyle(ChatFormatting.DARK_GRAY));
                tooltip.add(Component.literal("Hint: Start with copper and tin for bronze").withStyle(ChatFormatting.YELLOW));
            }
            case "copper_cable" -> {
                tooltip.add(Component.literal("Transfers: up to 120 FE/t").withStyle(ChatFormatting.GRAY));
                tooltip.add(Component.literal("Connects generators to machines").withStyle(ChatFormatting.DARK_GRAY));
            }
            default -> {
            }
        }
    }

    private MachineTooltipHandler() {
    }
}
