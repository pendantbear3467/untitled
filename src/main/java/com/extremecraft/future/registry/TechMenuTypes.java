package com.extremecraft.future.registry;

import com.extremecraft.core.ECConstants;
import com.extremecraft.machine.menu.ClassMenu;
import com.extremecraft.machine.menu.DualWieldMenu;
import com.extremecraft.machine.menu.MagicMenu;
import com.extremecraft.machine.menu.PlayerStatsMenu;
import com.extremecraft.machine.menu.TechMachineMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class TechMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, ECConstants.MODID);

    public static final RegistryObject<MenuType<TechMachineMenu>> TECH_MACHINE = MENUS.register("tech_machine", () ->
            IForgeMenuType.create((windowId, inv, buf) -> new TechMachineMenu(windowId, inv, buf.readBlockPos()))
    );

    public static final RegistryObject<MenuType<DualWieldMenu>> DUAL_WIELD = MENUS.register("dual_wield_tab", () ->
            IForgeMenuType.create((windowId, inv, buf) -> new DualWieldMenu(windowId, inv))
    );

    public static final RegistryObject<MenuType<MagicMenu>> MAGIC = MENUS.register("magic_tab", () ->
            IForgeMenuType.create((windowId, inv, buf) -> new MagicMenu(windowId, inv))
    );

    public static final RegistryObject<MenuType<PlayerStatsMenu>> PLAYER_STATS = MENUS.register("player_stats_tab", () ->
            IForgeMenuType.create((windowId, inv, buf) -> new PlayerStatsMenu(windowId, inv))
    );

    public static final RegistryObject<MenuType<ClassMenu>> CLASS = MENUS.register("class_tab", () ->
            IForgeMenuType.create((windowId, inv, buf) -> new ClassMenu(windowId, inv))
    );

    private TechMenuTypes() {
    }
}
