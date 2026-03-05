package com.extremecraft.item.module;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * ItemStack NBT helpers for installed item modules.
 */
public final class ItemModuleStorage {
    private static final String TAG_ROOT = "ec_modules";
    private static final String TAG_LIST = "installed";
    private static final String TAG_ID = "id";
    private static final String TAG_LEVEL = "level";

    private ItemModuleStorage() {
    }

    public static List<InstalledModule> getModules(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return List.of();
        }

        CompoundTag root = stack.getTag();
        if (root == null || !root.contains(TAG_ROOT, Tag.TAG_COMPOUND)) {
            return List.of();
        }

        CompoundTag modulesTag = root.getCompound(TAG_ROOT);
        ListTag installed = modulesTag.getList(TAG_LIST, Tag.TAG_COMPOUND);
        List<InstalledModule> result = new ArrayList<>(installed.size());

        for (Tag rawTag : installed) {
            if (!(rawTag instanceof CompoundTag moduleTag)) {
                continue;
            }

            String id = moduleTag.getString(TAG_ID).trim().toLowerCase();
            int level = Math.max(1, moduleTag.getInt(TAG_LEVEL));
            if (!id.isBlank()) {
                result.add(new InstalledModule(id, level));
            }
        }

        return result;
    }

    public static boolean hasModule(ItemStack stack, String moduleId) {
        return levelOf(stack, moduleId) > 0;
    }

    public static int levelOf(ItemStack stack, String moduleId) {
        if (moduleId == null || moduleId.isBlank()) {
            return 0;
        }

        String normalizedId = moduleId.trim().toLowerCase();
        for (InstalledModule installed : getModules(stack)) {
            if (installed.id().equals(normalizedId)) {
                return installed.level();
            }
        }
        return 0;
    }

    public static void upsertModule(ItemStack stack, String moduleId, int level) {
        if (stack == null || stack.isEmpty() || moduleId == null || moduleId.isBlank()) {
            return;
        }

        String normalizedId = moduleId.trim().toLowerCase();
        int clampedLevel = Math.max(1, level);

        CompoundTag root = stack.getOrCreateTag();
        CompoundTag modulesTag = root.contains(TAG_ROOT, Tag.TAG_COMPOUND) ? root.getCompound(TAG_ROOT) : new CompoundTag();
        ListTag installed = modulesTag.getList(TAG_LIST, Tag.TAG_COMPOUND);

        boolean updated = false;
        for (Tag rawTag : installed) {
            if (!(rawTag instanceof CompoundTag moduleTag)) {
                continue;
            }

            if (normalizedId.equals(moduleTag.getString(TAG_ID).trim().toLowerCase())) {
                moduleTag.putInt(TAG_LEVEL, clampedLevel);
                updated = true;
                break;
            }
        }

        if (!updated) {
            CompoundTag moduleTag = new CompoundTag();
            moduleTag.putString(TAG_ID, normalizedId);
            moduleTag.putInt(TAG_LEVEL, clampedLevel);
            installed.add(moduleTag);
        }

        modulesTag.put(TAG_LIST, installed);
        root.put(TAG_ROOT, modulesTag);
        stack.setTag(root);
    }

    public record InstalledModule(String id, int level) {
    }
}
