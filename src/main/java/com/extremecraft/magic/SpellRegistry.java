package com.extremecraft.magic;

import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class SpellRegistry {
    private static final Map<String, Spell> DEFINITIONS = new LinkedHashMap<>();
    private static List<Spell> CACHE = List.of();

    @SubscribeEvent
    public void onAddReloadListener(AddReloadListenerEvent event) {
        event.addListener(new SpellLoader());
    }

    public static synchronized Spell get(String id) {
        return id == null ? null : DEFINITIONS.get(id.trim().toLowerCase());
    }

    public static synchronized Collection<Spell> all() {
        return CACHE;
    }

    public static synchronized int size() {
        return DEFINITIONS.size();
    }

    public static synchronized String firstSpellId() {
        return CACHE.isEmpty() ? "" : CACHE.get(0).id();
    }

    static synchronized void replaceAll(Map<String, Spell> loaded) {
        DEFINITIONS.clear();
        DEFINITIONS.putAll(loaded);
        CACHE = List.copyOf(DEFINITIONS.values());
    }
}
