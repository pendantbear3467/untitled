package com.extremecraft.entity.extension;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public final class EntityExtensionRegistry {
    private static final Map<ResourceLocation, List<EntityExtension>> EXTENSIONS = new ConcurrentHashMap<>();

    private EntityExtensionRegistry() {
    }

    public static void register(EntityType<?> entityType, EntityExtension extension) {
        if (entityType == null || extension == null) {
            return;
        }

        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(entityType);
        if (id == null) {
            return;
        }

        EXTENSIONS.computeIfAbsent(id, ignored -> new CopyOnWriteArrayList<>()).add(extension);
    }

    public static void runServerTick(LivingEntity entity) {
        if (entity == null || entity.level().isClientSide) {
            return;
        }

        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        if (id == null) {
            return;
        }

        List<EntityExtension> extensions = EXTENSIONS.get(id);
        if (extensions == null || extensions.isEmpty()) {
            return;
        }

        for (EntityExtension extension : extensions) {
            extension.onServerTick(entity);
        }
    }

    public static boolean hasAnyExtensions() {
        return !EXTENSIONS.isEmpty();
    }
}
