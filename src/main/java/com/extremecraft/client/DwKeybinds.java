package com.extremecraft.client;

import com.extremecraft.ExtremeCraftMod;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraft.client.KeyMapping;
import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;

public final class DwKeybinds {
    public static KeyMapping OFFHAND_OVERRIDE; // bound to Right Mouse by default
    public static KeyMapping CYCLE_LOADOUT;
    public static KeyMapping OPEN_SKILL_TREE;
    public static KeyMapping OPEN_MAGIC;
    public static KeyMapping OPEN_DUAL_WIELD;
    public static KeyMapping OPEN_PROGRESSION;

    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent e) {
        OFFHAND_OVERRIDE = new KeyMapping(
                "key." + ExtremeCraftMod.MODID + ".offhand_action",
                InputConstants.Type.MOUSE,
                GLFW.GLFW_MOUSE_BUTTON_RIGHT, // yes, right-click (you can rebind)
                "key.categories." + ExtremeCraftMod.MODID
        );
            CYCLE_LOADOUT = new KeyMapping(
                "key." + ExtremeCraftMod.MODID + ".cycle_loadout",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_Z,
                "key.categories." + ExtremeCraftMod.MODID
            );
            OPEN_SKILL_TREE = new KeyMapping(
                "key." + ExtremeCraftMod.MODID + ".open_skill_tree",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_K,
                "key.categories." + ExtremeCraftMod.MODID
            );
            OPEN_MAGIC = new KeyMapping(
                "key." + ExtremeCraftMod.MODID + ".open_magic",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_J,
                "key.categories." + ExtremeCraftMod.MODID
            );
            OPEN_DUAL_WIELD = new KeyMapping(
                "key." + ExtremeCraftMod.MODID + ".open_dual_wield",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_O,
                "key.categories." + ExtremeCraftMod.MODID
            );
            OPEN_PROGRESSION = new KeyMapping(
                "key." + ExtremeCraftMod.MODID + ".open_progression",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_P,
                "key.categories." + ExtremeCraftMod.MODID
            );
        e.register(OFFHAND_OVERRIDE);
            e.register(CYCLE_LOADOUT);
            e.register(OPEN_SKILL_TREE);
            e.register(OPEN_MAGIC);
            e.register(OPEN_DUAL_WIELD);
            e.register(OPEN_PROGRESSION);
    }
}
