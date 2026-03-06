package com.extremecraft.api;

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
import java.util.Objects;

public final class ExtremeCraftAPI {
    private static volatile ExtremeCraftApiProvider provider;

    private ExtremeCraftAPI() {
    }

    public static synchronized void bootstrap(ExtremeCraftApiProvider apiProvider) {
        if (provider != null) {
            throw new IllegalStateException("ExtremeCraftAPI provider already bootstrapped");
        }
        provider = Objects.requireNonNull(apiProvider, "apiProvider");
    }

    public static void registerMachine(MachineDefinition definition) {
        requireProvider().registerMachine(definition);
    }

    public static void registerMaterial(MaterialDefinition definition) {
        requireProvider().registerMaterial(definition);
    }

    public static void registerSkillTree(SkillTreeDefinition definition) {
        requireProvider().registerSkillTree(definition);
    }

    public static void registerQuest(QuestDefinition definition) {
        requireProvider().registerQuest(definition);
    }

    public static void registerModule(ModuleDefinition definition) {
        requireProvider().registerModule(definition);
    }

    public static void registerAbility(AbilityDefinition definition) {
        requireProvider().registerAbility(definition);
    }

    public static void registerSpell(SpellDefinition definition) {
        requireProvider().registerSpell(definition);
    }

    public static void registerClass(ClassDefinition definition) {
        requireProvider().registerClass(definition);
    }

    public static void registerWorldgenFeature(WorldgenFeatureDefinition definition) {
        requireProvider().registerWorldgenFeature(definition);
    }

    public static void registerTechTree(TechTreeDefinition definition) {
        requireProvider().registerTechTree(definition);
    }

    public static void registerRecipe(RecipeDefinition definition) {
        requireProvider().registerRecipe(definition);
    }

    public static Collection<MachineDefinition> machines() {
        return requireProvider().machines();
    }

    public static Collection<MaterialDefinition> materials() {
        return requireProvider().materials();
    }

    public static Collection<SkillTreeDefinition> skillTrees() {
        return requireProvider().skillTrees();
    }

    public static Collection<QuestDefinition> quests() {
        return requireProvider().quests();
    }

    public static Collection<ModuleDefinition> modules() {
        return requireProvider().modules();
    }

    public static Collection<AbilityDefinition> abilities() {
        return requireProvider().abilities();
    }

    public static Collection<SpellDefinition> spells() {
        return requireProvider().spells();
    }

    public static Collection<ClassDefinition> classes() {
        return requireProvider().classes();
    }

    public static Collection<WorldgenFeatureDefinition> worldgenFeatures() {
        return requireProvider().worldgenFeatures();
    }

    public static Collection<TechTreeDefinition> techTrees() {
        return requireProvider().techTrees();
    }

    public static Collection<RecipeDefinition> recipes() {
        return requireProvider().recipes();
    }

    private static ExtremeCraftApiProvider requireProvider() {
        if (provider == null) {
            throw new IllegalStateException("ExtremeCraftAPI not bootstrapped yet");
        }
        return provider;
    }
}
