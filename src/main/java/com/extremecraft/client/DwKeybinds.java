package com.extremecraft.client;

import com.extremecraft.ExtremeCraftMod;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraft.client.KeyMapping;
import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;

public final class DwKeybinds {
    public static KeyMapping OFFHAND_OVERRIDE; // bound to Right Mouse by default

    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent e) {
        OFFHAND_OVERRIDE = new KeyMapping(
                "key." + ExtremeCraftMod.MODID + ".offhand_action",
                InputConstants.Type.MOUSE,
                GLFW.GLFW_MOUSE_BUTTON_RIGHT, // yes, right-click (you can rebind)
                "key.categories." + ExtremeCraftMod.MODID
        );
        e.register(OFFHAND_OVERRIDE);
    }
}
