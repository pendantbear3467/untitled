package com.extremecraft.future.registry;

import com.extremecraft.core.ECConstants;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.Comparator;

public final class TechCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ECConstants.MODID);

    public static final RegistryObject<CreativeModeTab> EXTREMECRAFT = TABS.register("extremecraft", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.extremecraft"))
            .icon(() -> {
                RegistryObject<Item> iconItem = TechItems.INGOTS.get("copper");
                return iconItem != null ? new ItemStack(iconItem.get()) : new ItemStack(Items.IRON_INGOT);
            })
            .displayItems((parameters, output) -> ForgeRegistries.ITEMS.getValues().stream()
                    .filter(item -> {
                        ResourceLocation key = ForgeRegistries.ITEMS.getKey(item);
                        return key != null && ECConstants.MODID.equals(key.getNamespace());
                    })
                    .sorted(Comparator.comparing(item -> ForgeRegistries.ITEMS.getKey(item).getPath()))
                    .forEach(output::accept))
            .build());

    private TechCreativeTabs() {
    }
}
