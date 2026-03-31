# ExtremeCraft API Surface
## AbilityDefinition
- Package: `com.extremecraft.api.definition`
### Public Members
- `public record AbilityDefinition(String id, String trigger, String datapackPath) {`

## ClassDefinition
- Package: `com.extremecraft.api.definition`
### Public Members
- `public record ClassDefinition(String id, String displayName, String datapackPath) {`

## MachineDefinition
- Package: `com.extremecraft.api.definition`
### Public Members
- `public record MachineDefinition(String id, String displayName, String tier) {`

## MaterialDefinition
- Package: `com.extremecraft.api.definition`
### Public Members
- `public record MaterialDefinition(String id, String displayName, int harvestLevel) {`

## ModuleDefinition
- Package: `com.extremecraft.api.definition`
### Public Members
- `public record ModuleDefinition(String id, String targetType, String datapackPath) {`

## QuestDefinition
- Package: `com.extremecraft.api.definition`
### Public Members
- `public record QuestDefinition(String id, String displayName, String datapackPath) {`

## RecipeDefinition
- Package: `com.extremecraft.api.definition`
### Public Members
- `public record RecipeDefinition(String id, String recipeType, String datapackPath) {`

## SkillTreeDefinition
- Package: `com.extremecraft.api.definition`
### Public Members
- `public record SkillTreeDefinition(String id, String displayName, String datapackPath) {`

## SpellDefinition
- Package: `com.extremecraft.api.definition`
### Public Members
- `public record SpellDefinition(String id, String type, String datapackPath) {`

## TechTreeDefinition
- Package: `com.extremecraft.api.definition`
### Public Members
- `public record TechTreeDefinition(String id, String displayName, String datapackPath) {`

## WorldgenFeatureDefinition
- Package: `com.extremecraft.api.definition`
### Public Members
- `public record WorldgenFeatureDefinition(String id, String featureType, String datapackPath) {`

## ExtremeCraftAPI
- Package: `com.extremecraft.api`
### Public Members
- `public static synchronized void bootstrap(ExtremeCraftApiProvider apiProvider) {`
- `public static void registerMachine(MachineDefinition definition) {`
- `public static void registerMaterial(MaterialDefinition definition) {`
- `public static void registerSkillTree(SkillTreeDefinition definition) {`
- `public static void registerQuest(QuestDefinition definition) {`
- `public static void registerModule(ModuleDefinition definition) {`
- `public static void registerAbility(AbilityDefinition definition) {`
- `public static void registerSpell(SpellDefinition definition) {`
- `public static void registerClass(ClassDefinition definition) {`
- `public static void registerWorldgenFeature(WorldgenFeatureDefinition definition) {`
- `public static void registerTechTree(TechTreeDefinition definition) {`
- `public static void registerRecipe(RecipeDefinition definition) {`
- `public static Collection<MachineDefinition> machines() {`
- `public static Collection<MaterialDefinition> materials() {`
- `public static Collection<SkillTreeDefinition> skillTrees() {`
- `public static Collection<QuestDefinition> quests() {`
- `public static Collection<ModuleDefinition> modules() {`
- `public static Collection<AbilityDefinition> abilities() {`
- `public static Collection<SpellDefinition> spells() {`
- `public static Collection<ClassDefinition> classes() {`
- `public static Collection<WorldgenFeatureDefinition> worldgenFeatures() {`
- `public static Collection<TechTreeDefinition> techTrees() {`
- `public static Collection<RecipeDefinition> recipes() {`

## ExtremeCraftApiVersions
- Package: `com.extremecraft.api`
### Public Members
- `public static final int EXTREMECRAFT_API_VERSION = 1;`
- `public static final int EXTREMECRAFT_PROTOCOL_VERSION = 1;`

## ExtremeCraftModule
- Package: `com.extremecraft.api.module`
- _No public signatures extracted_

## ExtremeCraftApiProvider
- Package: `com.extremecraft.api.registration`
- _No public signatures extracted_

