package com.extremecraft.item.tool;

import com.extremecraft.modules.item.AbstractModularToolItem;
import com.extremecraft.modules.item.ModuleStackData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Live modular drill item wrapper for the canonical module runtime.
 *
 * <p>Gameplay module behavior for this item comes from {@code modules.runtime.ModuleRuntimeService}
 * plus {@code data/extremecraft/tool_modules}. Do not add new drill module logic through the
 * legacy {@code item.module} package.</p>
 */
public class ModularDrillItem extends AbstractModularToolItem {
    public ModularDrillItem(Tier tier, int attackDamageModifier, float attackSpeedModifier, Properties properties) {
        super(tier, attackDamageModifier, attackSpeedModifier, 3, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);

        List<String> modules = ModuleStackData.readModules(stack);
        if (modules.isEmpty()) {
            tooltip.add(Component.literal("No modules installed").withStyle(ChatFormatting.DARK_GRAY));
            return;
        }

        tooltip.add(Component.literal("Installed Modules:").withStyle(ChatFormatting.AQUA));
        for (String moduleId : modules) {
            tooltip.add(Component.literal("- " + moduleId).withStyle(ChatFormatting.GRAY));
        }
    }
}
