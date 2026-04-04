package com.extremecraft.dev.validation;

import com.extremecraft.core.ECConstants;
import com.extremecraft.machine.core.MachineCatalog;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Central ownership map for developer-facing runtime audits.
 *
 * <p>This service exists to make the active source-of-truth model obvious in code, logs, and
 * validation output. It does not mutate gameplay state.</p>
 */
public final class ECRuntimeOwnershipAudit {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final int SAMPLE_LIMIT = 5;

    private ECRuntimeOwnershipAudit() {
    }

    public static void logStartupSummary() {
        for (String line : startupLines()) {
            LOGGER.info("[OwnershipAudit] {}", line);
        }
    }

    public static String summary() {
        return String.join(" | ", startupLines());
    }

    public static void validateDatapackLayout(Path resourcesRoot, Consumer<String> warningSink) {
        if (resourcesRoot == null || warningSink == null) {
            return;
        }

        Path dataRoot = resourcesRoot.resolve("data").resolve(ECConstants.MODID);
        warnMetadataDirectory(dataRoot.resolve("machines"), warningSink,
                "data/extremecraft/machines is METADATA ONLY. Live tech machines use MachineCatalog + data/extremecraft/recipes/machine_processing.");
        warnMetadataDirectory(dataRoot.resolve("quests"), warningSink,
                "data/extremecraft/quests is METADATA ONLY. Live quest gameplay uses data/extremecraft/extremecraft_quests + QuestManager.");
        warnMetadataDirectory(dataRoot.resolve("materials"), warningSink,
                "data/extremecraft/materials is METADATA ONLY. Live material registration still starts in OreMaterialCatalog + future.registry.");
        warnMetadataDirectory(dataRoot.resolve("world_generation"), warningSink,
                "data/extremecraft/world_generation is METADATA ONLY. Live placement uses data/extremecraft/worldgen + data/extremecraft/forge.");
        warnMetadataDirectory(dataRoot.resolve("abilities_platform"), warningSink,
                "data/extremecraft/abilities_platform is METADATA ONLY. Live generic abilities use data/extremecraft/abilities + AbilityRegistry.");

        warnLegacyDirectory(dataRoot.resolve("skilltrees"), warningSink,
                "Legacy datapack directory detected: data/extremecraft/skilltrees. Canonical live skill tree path is data/extremecraft/skill_trees.");
        warnLegacyDirectory(dataRoot.resolve("machine"), warningSink,
                "Legacy datapack directory detected: data/extremecraft/machine. Canonical machine metadata mirror path is data/extremecraft/machines.");

        if (countJsonFiles(dataRoot.resolve("module_abilities")) <= 0 && countJsonFiles(dataRoot.resolve("abilities")) > 0) {
            warningSink.accept("data/extremecraft/module_abilities is empty while abilities/ is populated. Canonical module-trigger ability ownership is module_abilities; abilities/ trigger payloads are compatibility fallback only.");
        }

        if (countJsonFiles(dataRoot.resolve("classes")) > 0) {
            warningSink.accept("data/extremecraft/classes is the CANONICAL gameplay class source, but compatibility adapters still mirror it into ClassRegistry/ClassDataLoader. Gameplay ownership is progression.classsystem.data.ClassDefinitionLoader.");
        }

        if (countJsonFiles(dataRoot.resolve("research")) > 0) {
            warningSink.accept("data/extremecraft/research is the CANONICAL gameplay research source, but platform ResearchDataLoader still mirrors it for snapshots/debug output. Gameplay ownership is ResearchManager.");
        }

        warnLegacyModuleAbilityLocation(dataRoot.resolve("abilities"), warningSink);
        warnOverlappingAbilityAndSpellIds(dataRoot.resolve("abilities"), dataRoot.resolve("spells"), warningSink);
        warnMachineMetadataDrift(dataRoot.resolve("machines"), warningSink);
    }

    private static List<String> startupLines() {
        return List.of(
                "Machines: MachineCatalog + TechMachineBlockEntity + recipes/machine_processing",
                "Abilities: AbilityRegistry/AbilityEngine trigger active casts; AbilityExecutor.executeDefinition(...) is the active compiled-definition execution path",
                "Module triggers: ModuleRuntimeService + data/extremecraft/module_abilities (abilities/ trigger payloads are legacy fallback only)",
                "Spells: SpellRegistry + SpellExecutor + data/extremecraft/spells",
                "Classes: ClassDefinitionLoader/ClassDefinitions + data/extremecraft/classes; ClassRegistry is compatibility-only",
                "Progression: ProgressionFacade for external callers, SkillProgressionService for gameplay skill XP, ClassProgressionService for guild-quest class XP",
                "Quests: QuestManager + data/extremecraft/extremecraft_quests; quests/ is metadata-only",
                "Legacy warning: game/ProgressionSystem is historical reference-only; live progression writes must stay in com.extremecraft.progression",
                "Materials/worldgen metadata: materials/ and world_generation/ are not live registry or placement owners"
        );
    }

    private static void warnMetadataDirectory(Path root, Consumer<String> warningSink, String message) {
        if (countJsonFiles(root) > 0) {
            warningSink.accept(message);
        }
    }

    private static void warnLegacyDirectory(Path root, Consumer<String> warningSink, String message) {
        if (countJsonFiles(root) > 0) {
            warningSink.accept(message);
        }
    }

    private static void warnLegacyModuleAbilityLocation(Path abilitiesRoot, Consumer<String> warningSink) {
        if (!Files.isDirectory(abilitiesRoot)) {
            return;
        }

        List<String> legacyModuleFiles = new ArrayList<>();
        try (Stream<Path> files = Files.walk(abilitiesRoot)) {
            files.filter(path -> path.toString().endsWith(".json"))
                    .forEach(path -> {
                        String contents = readString(path);
                        if (contents.contains("\"trigger\"")) {
                            legacyModuleFiles.add(stripJsonExtension(path.getFileName().toString()));
                        }
                    });
        } catch (IOException ignored) {
            return;
        }

        if (!legacyModuleFiles.isEmpty()) {
            warningSink.accept("Legacy module-trigger ability files still exist under data/extremecraft/abilities: "
                    + summarizeIds(legacyModuleFiles)
                    + ". Canonical module trigger path is data/extremecraft/module_abilities.");
        }
    }

    private static void warnOverlappingAbilityAndSpellIds(Path abilitiesRoot, Path spellsRoot, Consumer<String> warningSink) {
        Set<String> abilityIds = collectJsonStems(abilitiesRoot);
        Set<String> spellIds = collectJsonStems(spellsRoot);
        abilityIds.retainAll(spellIds);
        if (abilityIds.isEmpty()) {
            return;
        }

        warningSink.accept("Ability and spell ids overlap across data/extremecraft/abilities and data/extremecraft/spells: "
                + summarizeIds(abilityIds)
                + ". Edit abilities/ for active ability casts and spells/ for spell behavior; they are separate trigger domains even though both compile through AbilityExecutor.");
    }

    private static void warnMachineMetadataDrift(Path machinesRoot, Consumer<String> warningSink) {
        Set<String> metadataIds = collectJsonStems(machinesRoot);
        if (metadataIds.isEmpty()) {
            return;
        }

        Set<String> extraMetadataIds = new LinkedHashSet<>(metadataIds);
        extraMetadataIds.removeAll(MachineCatalog.DEFINITIONS.keySet());
        if (!extraMetadataIds.isEmpty()) {
            warningSink.accept("Machine metadata ids without a live tech-machine owner in MachineCatalog: "
                    + summarizeIds(extraMetadataIds)
                    + ". Editing these files will not activate a tech machine runtime by itself.");
        }
    }

    private static long countJsonFiles(Path root) {
        if (!Files.isDirectory(root)) {
            return 0L;
        }

        try (Stream<Path> files = Files.walk(root)) {
            return files.filter(path -> path.toString().endsWith(".json")).count();
        } catch (IOException ignored) {
            return 0L;
        }
    }

    private static Set<String> collectJsonStems(Path root) {
        if (!Files.isDirectory(root)) {
            return Set.of();
        }

        try (Stream<Path> files = Files.walk(root)) {
            return files.filter(path -> path.toString().endsWith(".json"))
                    .map(path -> stripJsonExtension(path.getFileName().toString()).toLowerCase(Locale.ROOT))
                    .filter(id -> !id.isBlank())
                    .collect(Collectors.toCollection(LinkedHashSet::new));
        } catch (IOException ignored) {
            return Set.of();
        }
    }

    private static String summarizeIds(Iterable<String> ids) {
        List<String> values = new ArrayList<>();
        for (String id : ids) {
            if (id != null && !id.isBlank()) {
                values.add(id);
            }
        }

        if (values.size() <= SAMPLE_LIMIT) {
            return String.join(", ", values);
        }

        List<String> head = values.subList(0, SAMPLE_LIMIT);
        return String.join(", ", head) + " (+" + (values.size() - SAMPLE_LIMIT) + " more)";
    }

    private static String stripJsonExtension(String fileName) {
        return fileName != null && fileName.endsWith(".json")
                ? fileName.substring(0, fileName.length() - 5)
                : fileName == null ? "" : fileName;
    }

    private static String readString(Path path) {
        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException ignored) {
            return "";
        }
    }
}
