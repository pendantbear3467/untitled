package com.extremecraft.client;

import com.extremecraft.ExtremeCraftMod;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

public final class ExtremeCraftKeybinds {
    public static KeyMapping ABILITY_SLOT_1;
    public static KeyMapping ABILITY_SLOT_2;
    public static KeyMapping ABILITY_SLOT_3;
    public static KeyMapping ABILITY_SLOT_4;

    private ExtremeCraftKeybinds() {
    }

    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        ABILITY_SLOT_1 = new KeyMapping(
                "key." + ExtremeCraftMod.MODID + ".ability_slot_1",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_Q,
                "key.categories." + ExtremeCraftMod.MODID
        );

        ABILITY_SLOT_2 = new KeyMapping(
                "key." + ExtremeCraftMod.MODID + ".ability_slot_2",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_E,
                "key.categories." + ExtremeCraftMod.MODID
        );

        ABILITY_SLOT_3 = new KeyMapping(
                "key." + ExtremeCraftMod.MODID + ".ability_slot_3",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_R,
                "key.categories." + ExtremeCraftMod.MODID
        );

        ABILITY_SLOT_4 = new KeyMapping(
                "key." + ExtremeCraftMod.MODID + ".ability_slot_4",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_F,
                "key.categories." + ExtremeCraftMod.MODID
        );

        event.register(ABILITY_SLOT_1);
        event.register(ABILITY_SLOT_2);
        event.register(ABILITY_SLOT_3);
        event.register(ABILITY_SLOT_4);
    }
}
