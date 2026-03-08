package com.extremecraft.client;

import com.extremecraft.ExtremeCraftMod;
import com.extremecraft.classsystem.ClassAccessResolver;
import com.extremecraft.progression.capability.ProgressApi;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public final class ExtremeCraftKeybinds {
    public static KeyMapping ABILITY_SLOT_1;
    public static KeyMapping ABILITY_SLOT_2;
    public static KeyMapping ABILITY_SLOT_3;
    public static KeyMapping ABILITY_SLOT_4;
    public static KeyMapping DEV_DEBUG_OVERLAY;

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

        DEV_DEBUG_OVERLAY = new KeyMapping(
                "key." + ExtremeCraftMod.MODID + ".dev_debug_overlay",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_F10,
                "key.categories." + ExtremeCraftMod.MODID
        );

        event.register(ABILITY_SLOT_1);
        event.register(ABILITY_SLOT_2);
        event.register(ABILITY_SLOT_3);
        event.register(ABILITY_SLOT_4);
        event.register(DEV_DEBUG_OVERLAY);
    }

    public static String resolveAbilityForSlot(LocalPlayer player, int slotIndex) {
        if (player == null || slotIndex < 0 || slotIndex > 3) {
            return "";
        }

        String classId = ProgressApi.get(player).map(data -> data.currentClass()).orElse("warrior");
        List<String> abilities = ClassAccessResolver.abilityAccess(classId);
        if (slotIndex >= abilities.size()) {
            return "";
        }

        String abilityId = abilities.get(slotIndex);
        return abilityId == null ? "" : abilityId.trim().toLowerCase();
    }

    public static String resolveAbilityForSlot(int slotIndex) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return "";
        }
        return resolveAbilityForSlot(minecraft.player, slotIndex);
    }
}
