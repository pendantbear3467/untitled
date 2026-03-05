package com.extremecraft.machine;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;

public final class MachineTooltipHandler {
    public static void onTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        ResourceLocation id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem());
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
                tooltip.add(Component.literal("Hint: Crusher -> Pulverizer -> Smelter").withStyle(ChatFormatting.YELLOW));
            }
            case "pulverizer" -> {
                tooltip.add(Component.literal("Uses: 24 FE/t").withStyle(ChatFormatting.GRAY));
                tooltip.add(Component.literal("Function: Converts raw materials to dust").withStyle(ChatFormatting.DARK_GRAY));
                tooltip.add(Component.literal("Hint: Dusts smelt better in the Smelter").withStyle(ChatFormatting.YELLOW));
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
