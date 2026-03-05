package com.extremecraft.client;

import com.extremecraft.ExtremeCraftMod;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

public final class DwKeybinds {
    public static KeyMapping OFFHAND_OVERRIDE;
    public static KeyMapping CLASS_ABILITY;

    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent e) {
        OFFHAND_OVERRIDE = new KeyMapping(
                "key." + ExtremeCraftMod.MODID + ".offhand_action",
                InputConstants.Type.MOUSE,
                GLFW.GLFW_MOUSE_BUTTON_RIGHT,
                "key.categories." + ExtremeCraftMod.MODID
        );

        CLASS_ABILITY = new KeyMapping(
            "key." + ExtremeCraftMod.MODID + ".class_ability",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_R,
            "key.categories." + ExtremeCraftMod.MODID
        );

        e.register(OFFHAND_OVERRIDE);
        e.register(CLASS_ABILITY);
    }
}
