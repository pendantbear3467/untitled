package com.extremecraft.platform;

import com.extremecraft.api.definition.AbilityDefinition;
import com.extremecraft.api.definition.ClassDefinition;
import com.extremecraft.api.definition.MachineDefinition;
import com.extremecraft.api.definition.MaterialDefinition;
import com.extremecraft.api.definition.ModuleDefinition;
import com.extremecraft.api.definition.QuestDefinition;
import com.extremecraft.api.definition.RecipeDefinition;
import com.extremecraft.api.definition.SkillTreeDefinition;
import com.extremecraft.api.definition.SpellDefinition;
import com.extremecraft.api.definition.TechTreeDefinition;
import com.extremecraft.api.definition.WorldgenFeatureDefinition;
import com.extremecraft.api.registration.ExtremeCraftApiProvider;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ExtremeCraftApiProviderImpl implements ExtremeCraftApiProvider {
    private final Map<String, MachineDefinition> machines = new LinkedHashMap<>();
    private final Map<String, MaterialDefinition> materials = new LinkedHashMap<>();
    private final Map<String, SkillTreeDefinition> skillTrees = new LinkedHashMap<>();
    private final Map<String, QuestDefinition> quests = new LinkedHashMap<>();
    private final Map<String, ModuleDefinition> modules = new LinkedHashMap<>();
    private final Map<String, AbilityDefinition> abilities = new LinkedHashMap<>();
    private final Map<String, SpellDefinition> spells = new LinkedHashMap<>();
    private final Map<String, ClassDefinition> classes = new LinkedHashMap<>();
    private final Map<String, WorldgenFeatureDefinition> worldgenFeatures = new LinkedHashMap<>();
    private final Map<String, TechTreeDefinition> techTrees = new LinkedHashMap<>();
    private final Map<String, RecipeDefinition> recipes = new LinkedHashMap<>();

    @Override
    public synchronized void registerMachine(MachineDefinition definition) {
        machines.put(definition.id(), definition);
    }

    @Override
    public synchronized void registerMaterial(MaterialDefinition definition) {
        materials.put(definition.id(), definition);
    }

    @Override
    public synchronized void registerSkillTree(SkillTreeDefinition definition) {
        skillTrees.put(definition.id(), definition);
    }

    @Override
    public synchronized void registerQuest(QuestDefinition definition) {
        quests.put(definition.id(), definition);
    }

    @Override
    public synchronized void registerModule(ModuleDefinition definition) {
        modules.put(definition.id(), definition);
    }

    @Override
    public synchronized void registerAbility(AbilityDefinition definition) {
        abilities.put(definition.id(), definition);
    }

    @Override
    public synchronized void registerSpell(SpellDefinition definition) {
        spells.put(definition.id(), definition);
    }

    @Override
    public synchronized void registerClass(ClassDefinition definition) {
        classes.put(definition.id(), definition);
    }

    @Override
    public synchronized void registerWorldgenFeature(WorldgenFeatureDefinition definition) {
        worldgenFeatures.put(definition.id(), definition);
    }

    @Override
    public synchronized void registerTechTree(TechTreeDefinition definition) {
        techTrees.put(definition.id(), definition);
    }

    @Override
    public synchronized void registerRecipe(RecipeDefinition definition) {
        recipes.put(definition.id(), definition);
    }

    @Override
    public synchronized Collection<MachineDefinition> machines() {
        return java.util.List.copyOf(machines.values());
    }

    @Override
    public synchronized Collection<MaterialDefinition> materials() {
        return java.util.List.copyOf(materials.values());
    }

    @Override
    public synchronized Collection<SkillTreeDefinition> skillTrees() {
        return java.util.List.copyOf(skillTrees.values());
    }

    @Override
    public synchronized Collection<QuestDefinition> quests() {
        return java.util.List.copyOf(quests.values());
    }

    @Override
    public synchronized Collection<ModuleDefinition> modules() {
        return java.util.List.copyOf(modules.values());
    }

    @Override
    public synchronized Collection<AbilityDefinition> abilities() {
        return java.util.List.copyOf(abilities.values());
    }

    @Override
    public synchronized Collection<SpellDefinition> spells() {
        return java.util.List.copyOf(spells.values());
    }

    @Override
    public synchronized Collection<ClassDefinition> classes() {
        return java.util.List.copyOf(classes.values());
    }

    @Override
    public synchronized Collection<WorldgenFeatureDefinition> worldgenFeatures() {
        return java.util.List.copyOf(worldgenFeatures.values());
    }

    @Override
    public synchronized Collection<TechTreeDefinition> techTrees() {
        return java.util.List.copyOf(techTrees.values());
    }

    @Override
    public synchronized Collection<RecipeDefinition> recipes() {
        return java.util.List.copyOf(recipes.values());
    }
}
