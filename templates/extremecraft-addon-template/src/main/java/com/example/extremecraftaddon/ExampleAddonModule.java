package com.example.extremecraftaddon;

import com.extremecraft.api.ExtremeCraftApiVersions;
import com.extremecraft.api.definition.SkillTreeDefinition;
import com.extremecraft.api.module.ExtremeCraftModule;
import com.extremecraft.api.registration.ExtremeCraftApiProvider;

public final class ExampleAddonModule implements ExtremeCraftModule {
    @Override
    public String moduleId() {
        return "extremecraftspace";
    }

    @Override
    public String moduleName() {
        return "ExtremeCraft Space";
    }

    @Override
    public int apiVersion() {
        return ExtremeCraftApiVersions.EXTREMECRAFT_API_VERSION;
    }

    @Override
    public int protocolVersion() {
        return ExtremeCraftApiVersions.EXTREMECRAFT_PROTOCOL_VERSION;
    }

    @Override
    public void register(ExtremeCraftApiProvider api) {
        api.registerSkillTree(new SkillTreeDefinition("space_navigation", "Space Navigation", "extremecraftspace:skills/space_navigation"));
    }
}
