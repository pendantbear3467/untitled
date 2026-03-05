package com.extremecraft.registry;

import com.extremecraft.core.ECConstants;
import com.extremecraft.machines.pulverizer.PulverizerMenu;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraft.world.inventory.MenuType;

public final class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, ECConstants.MODID);

    public static final RegistryObject<MenuType<PulverizerMenu>> PULVERIZER_MENU = MENUS.register("pulverizer", () ->
            IForgeMenuType.create((windowId, inv, buf) -> new PulverizerMenu(windowId, inv, buf.readBlockPos()))
    );

    private ModMenuTypes() {}
}
