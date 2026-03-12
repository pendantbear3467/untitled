package com.extremecraft.client;

import com.extremecraft.ExtremeCraftMod;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

public final class DwKeybinds {
    public static KeyMapping OFFHAND_OVERRIDE;
    public static KeyMapping CYCLE_LOADOUT;
    public static KeyMapping CLASS_ABILITY;
    public static KeyMapping CAST_SPELL;

    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent e) {
        OFFHAND_OVERRIDE = new KeyMapping(
                "key." + ExtremeCraftMod.MODID + ".offhand_action",
                InputConstants.Type.MOUSE,
                GLFW.GLFW_MOUSE_BUTTON_RIGHT,
                "key.categories." + ExtremeCraftMod.MODID
        );

        CYCLE_LOADOUT = new KeyMapping(
                "key." + ExtremeCraftMod.MODID + ".cycle_loadout",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_V,
                "key.categories." + ExtremeCraftMod.MODID
        );

        CLASS_ABILITY = new KeyMapping(
                "key." + ExtremeCraftMod.MODID + ".class_ability",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_C,
                "key.categories." + ExtremeCraftMod.MODID
        );

        CAST_SPELL = new KeyMapping(
                "key." + ExtremeCraftMod.MODID + ".cast_spell",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_G,
                "key.categories." + ExtremeCraftMod.MODID
        );

        e.register(OFFHAND_OVERRIDE);
        e.register(CYCLE_LOADOUT);
        e.register(CLASS_ABILITY);
        e.register(CAST_SPELL);
    }
}
