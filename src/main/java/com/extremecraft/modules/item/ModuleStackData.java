package com.extremecraft.modules.item;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class ModuleStackData {
    private static final String ROOT = "ec_modules";
    private static final String IDS = "ids";

    private ModuleStackData() {
    }

    public static List<String> readModules(ItemStack stack) {
        CompoundTag root = stack.getTagElement(ROOT);
        if (root == null) {
            return List.of();
        }

        ListTag listTag = root.getList(IDS, Tag.TAG_STRING);
        List<String> modules = new ArrayList<>(listTag.size());
        for (int i = 0; i < listTag.size(); i++) {
            String id = listTag.getString(i);
            if (!id.isBlank()) {
                modules.add(id);
            }
        }
        return List.copyOf(modules);
    }

    public static boolean installModule(ItemStack stack, String moduleId, int maxSlots) {
        if (stack.isEmpty() || moduleId == null || moduleId.isBlank() || maxSlots <= 0) {
            return false;
        }

        Set<String> modules = new LinkedHashSet<>(readModules(stack));
        if (modules.contains(moduleId) || modules.size() >= maxSlots) {
            return false;
        }

        modules.add(moduleId.trim().toLowerCase());
        writeModules(stack, modules);
        return true;
    }

    public static void writeModules(ItemStack stack, Set<String> modules) {
        CompoundTag root = stack.getOrCreateTagElement(ROOT);
        ListTag listTag = new ListTag();
        for (String module : modules) {
            listTag.add(StringTag.valueOf(module));
        }
        root.put(IDS, listTag);
    }
}

