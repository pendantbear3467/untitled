package com.extremecraft.platform.module;

import com.extremecraft.api.ExtremeCraftApiVersions;
import com.extremecraft.api.definition.SkillTreeDefinition;
import com.extremecraft.api.definition.TechTreeDefinition;
import com.extremecraft.api.module.ExtremeCraftModule;
import com.extremecraft.api.registration.ExtremeCraftApiProvider;

public final class CoreGameplayModule implements ExtremeCraftModule {
    @Override
    public String moduleId() {
        return "extremecraft-core";
    }

    @Override
    public String moduleName() {
        return "ExtremeCraft Core";
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
        api.registerSkillTree(new SkillTreeDefinition("combat", "Combat", "extremecraft:skill_trees/combat"));
        api.registerSkillTree(new SkillTreeDefinition("magic", "Magic", "extremecraft:skill_trees/magic"));
        api.registerTechTree(new TechTreeDefinition("baseline_automation", "Baseline Automation", "extremecraft:tech_trees/baseline_automation"));
    }
}
