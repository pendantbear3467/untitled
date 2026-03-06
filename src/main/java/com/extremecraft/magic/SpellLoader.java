package com.extremecraft.magic;

import com.extremecraft.api.ExtremeCraftAPI;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.LinkedHashMap;
import java.util.Map;

public final class SpellLoader extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new Gson();

    public SpellLoader() {
        super(GSON, "spells");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> jsonMap, ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<String, Spell> loaded = new LinkedHashMap<>();

        for (Map.Entry<ResourceLocation, JsonElement> entry : jsonMap.entrySet()) {
            if (!entry.getValue().isJsonObject()) {
                continue;
            }

            Spell spell = Spell.fromJson(entry.getKey(), entry.getValue().getAsJsonObject());
            if (!spell.id().isBlank()) {
                loaded.put(spell.id(), spell);
            }
        }

        ExtremeCraftAPI.spells().forEach(apiSpell -> loaded.putIfAbsent(apiSpell.id(),
                new Spell(apiSpell.id(), Spell.SpellType.byName(apiSpell.type()), 0, 0, 0.0D, 1.5D, 4.0D, 24.0D,
                        80, 60, "", "", "", "", java.util.List.of(), java.util.List.of())));

        SpellRegistry.replaceAll(loaded);
    }
}
