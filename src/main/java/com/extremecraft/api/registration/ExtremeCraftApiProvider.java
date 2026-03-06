package com.extremecraft.api.registration;

import com.extremecraft.api.definition.AbilityDefinition;
import com.extremecraft.api.definition.MachineDefinition;
import com.extremecraft.api.definition.MaterialDefinition;
import com.extremecraft.api.definition.ModuleDefinition;
import com.extremecraft.api.definition.QuestDefinition;
import com.extremecraft.api.definition.SkillTreeDefinition;
import com.extremecraft.api.definition.TechTreeDefinition;

import java.util.Collection;

public interface ExtremeCraftApiProvider {
    void registerMachine(MachineDefinition definition);

    void registerMaterial(MaterialDefinition definition);

    void registerSkillTree(SkillTreeDefinition definition);

    void registerQuest(QuestDefinition definition);

    void registerModule(ModuleDefinition definition);

    void registerAbility(AbilityDefinition definition);

    void registerTechTree(TechTreeDefinition definition);

    Collection<MachineDefinition> machines();

    Collection<MaterialDefinition> materials();

    Collection<SkillTreeDefinition> skillTrees();

    Collection<QuestDefinition> quests();

    Collection<ModuleDefinition> modules();

    Collection<AbilityDefinition> abilities();

    Collection<TechTreeDefinition> techTrees();
}
