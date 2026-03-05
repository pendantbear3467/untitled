package com.extremecraft.client;

import com.extremecraft.ExtremeCraftMod;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

public final class DwKeybinds {
    public static KeyMapping OFFHAND_OVERRIDE;

    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent e) {
        OFFHAND_OVERRIDE = new KeyMapping(
                "key." + ExtremeCraftMod.MODID + ".offhand_action",
                InputConstants.Type.MOUSE,
                GLFW.GLFW_MOUSE_BUTTON_RIGHT,
                "key.categories." + ExtremeCraftMod.MODID
        );

        e.register(OFFHAND_OVERRIDE);
    }
}
