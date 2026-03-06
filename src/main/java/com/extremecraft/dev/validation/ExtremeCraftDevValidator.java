package com.extremecraft.dev.validation;

import com.extremecraft.dev.validation.ValidatorRules.Severity;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Standalone static validator for development-time quality checks.
 */
public final class ExtremeCraftDevValidator {
    private static final String MOD_ID = "extremecraft";
    private static final int MAX_FINDINGS = 1000;

    private ExtremeCraftDevValidator() {
    }

    public static void main(String[] args) {
        Path root = Paths.get("").toAbsolutePath().normalize();
        try {
            ValidationResult result = run(root);
            System.out.println("[ExtremeCraftDevValidator] Report written to " + result.reportPath());
            System.out.println("[ExtremeCraftDevValidator] Findings: " + result.findings().size());
        } catch (Exception ex) {
            System.err.println("[ExtremeCraftDevValidator] Validation failed: " + ex.getMessage());
            ex.printStackTrace(System.err);
            System.exit(1);
        }
    }

    public static ValidationResult run(Path projectRoot) throws IOException {
        Path javaRoot = projectRoot.resolve("src/main/java/com/extremecraft");
        Path resourcesRoot = projectRoot.resolve("src/main/resources");
        Path reportPath = projectRoot.resolve("build/extremecraft-validation-report.txt");

        List<Path> javaFiles = listFiles(javaRoot, ".java");
        List<Path> jsonFiles = listFiles(resourcesRoot, ".json");

        List<Finding> findings = new ArrayList<>();
        Set<String> dedupe = new HashSet<>();

        scanRecursionRisks(projectRoot, javaFiles, findings, dedupe);
        scanToolAoeValidation(projectRoot, javaFiles, findings, dedupe);
        scanClientServerDesyncRisks(projectRoot, javaFiles, findings, dedupe);
        scanNetworkPacketLoops(projectRoot, javaFiles, findings, dedupe);
        scanEntityDamageLoops(projectRoot, javaFiles, findings, dedupe);
        scanStaticCollectionLeaks(projectRoot, javaFiles, findings, dedupe);

        KnownResources knownResources = buildKnownResources(resourcesRoot);
        scanRegistryAndDatapackIssues(projectRoot, javaFiles, jsonFiles, knownResources, findings, dedupe);
        scanModelTextureLangConsistency(projectRoot, resourcesRoot, knownResources, findings, dedupe);

        writeReport(reportPath, findings);
        return new ValidationResult(reportPath, findings);
    }

    private static void scanRecursionRisks(Path root, List<Path> javaFiles, List<Finding> findings, Set<String> dedupe) {
        for (Path file : javaFiles) {
            if (reachedFindingLimit(findings)) {
                return;
            }

            List<String> lines = readLines(file);
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);

                boolean blockBreakContext = matches(ValidatorRules.ON_BLOCK_START_BREAK, line)
                        || matches(ValidatorRules.BLOCK_BREAK_EVENT, line);
                if (blockBreakContext) {
                    int end = findBlockEnd(lines, i);
                    for (int j = i; j <= end; j++) {
                        String bodyLine = lines.get(j);
                        if (matches(ValidatorRules.DESTROY_BLOCK_CALL, bodyLine) || matches(ValidatorRules.BREAK_BLOCK_CALL, bodyLine)) {
                            addFinding(
                                    root,
                                    file,
                                    j + 1,
                                    Severity.CRITICAL,
                                    "Potential recursion risk (block break handler calling block destruction API).",
                                    findings,
                                    dedupe
                            );
                        }
                    }
                }

                boolean damageContext = matches(ValidatorRules.LIVING_HURT_EVENT, line)
                        || matches(ValidatorRules.LIVING_ATTACK_EVENT, line);
                if (damageContext) {
                    int end = findBlockEnd(lines, i);
                    for (int j = i; j <= end; j++) {
                        String bodyLine = lines.get(j);
                        if (matches(ValidatorRules.DAMAGE_CALL, bodyLine) || matches(ValidatorRules.ATTACK_CALL, bodyLine)) {
                            addFinding(
                                    root,
                                    file,
                                    j + 1,
                                    Severity.HIGH,
                                    "Potential recursion risk (damage event handler calling damage/attack again).",
                                    findings,
                                    dedupe
                            );
                        }
                    }
                }
            }
        }
    }

    private static void scanToolAoeValidation(Path root, List<Path> javaFiles, List<Finding> findings, Set<String> dedupe) {
        for (Path file : javaFiles) {
            if (reachedFindingLimit(findings)) {
                return;
            }

            String normalized = normalizePath(file);
            boolean likelyToolClass = normalized.contains("/item/tool/")
                    || file.getFileName().toString().toLowerCase().contains("hammer")
                    || file.getFileName().toString().toLowerCase().contains("tool");
            if (!likelyToolClass) {
                continue;
            }

            List<String> lines = readLines(file);
            int breakCallLine = findFirstLine(lines, ValidatorRules.DESTROY_BLOCK_CALL, ValidatorRules.BREAK_BLOCK_CALL);
            if (breakCallLine < 0) {
                continue;
            }

            boolean hasGuard = contains(lines, ValidatorRules.TOOL_GUARD);
            if (!hasGuard) {
                addFinding(
                        root,
                        file,
                        breakCallLine,
                        Severity.HIGH,
                        "Tool performs block breaking without an explicit recursion/processed-block guard.",
                        findings,
                        dedupe
                );
            }
        }
    }

    private static void scanClientServerDesyncRisks(Path root, List<Path> javaFiles, List<Finding> findings, Set<String> dedupe) {
        for (Path file : javaFiles) {
            if (reachedFindingLimit(findings)) {
                return;
            }

            String normalized = normalizePath(file);
            if (normalized.contains("/client/")) {
                continue;
            }

            List<String> lines = readLines(file);
            int fileFindings = 0;

            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                if (!matches(ValidatorRules.GAMEPLAY_MUTATION, line)) {
                    continue;
                }

                if (hasNearbyGuard(lines, i, 12, 18, ValidatorRules.SERVER_GUARD)) {
                    continue;
                }

                addFinding(
                        root,
                        file,
                        i + 1,
                        Severity.MEDIUM,
                        "Gameplay mutation call without nearby explicit server-side guard (possible client/server desync risk).",
                        findings,
                        dedupe
                );

                fileFindings++;
                if (fileFindings >= 3) {
                    break;
                }
            }
        }
    }
    private static void scanNetworkPacketLoops(Path root, List<Path> javaFiles, List<Finding> findings, Set<String> dedupe) {
        for (Path file : javaFiles) {
            if (reachedFindingLimit(findings)) {
                return;
            }

            String normalized = normalizePath(file);
            if (!(normalized.contains("/network/") || normalized.contains("/net/"))) {
                continue;
            }

            List<String> lines = readLines(file);
            String className = file.getFileName().toString().replace(".java", "");

            for (int i = 0; i < lines.size(); i++) {
                if (!matches(ValidatorRules.NETWORK_HANDLER, lines.get(i))) {
                    continue;
                }

                int end = findBlockEnd(lines, i);
                int sendLine = -1;
                boolean hasGuard = false;
                boolean selfRebroadcast = false;

                for (int j = i; j <= end; j++) {
                    String bodyLine = lines.get(j);
                    if (sendLine < 0 && matches(ValidatorRules.NETWORK_SEND, bodyLine)) {
                        sendLine = j + 1;
                    }
                    if (matches(ValidatorRules.NETWORK_GUARD, bodyLine)) {
                        hasGuard = true;
                    }
                    if (bodyLine.contains("new " + className + "(")) {
                        selfRebroadcast = true;
                    }
                }

                if (sendLine > 0) {
                    Severity severity = hasGuard ? Severity.MEDIUM : Severity.HIGH;
                    String message = "Packet handler sends packets during handling (potential packet feedback loop).";
                    if (selfRebroadcast) {
                        severity = Severity.CRITICAL;
                        message = "Packet handler appears to enqueue/send same packet type during handling (high loop risk).";
                    }

                    addFinding(root, file, sendLine, severity, message, findings, dedupe);
                }
            }
        }
    }

    private static void scanEntityDamageLoops(Path root, List<Path> javaFiles, List<Finding> findings, Set<String> dedupe) {
        for (Path file : javaFiles) {
            if (reachedFindingLimit(findings)) {
                return;
            }

            List<String> lines = readLines(file);
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                if (!(matches(ValidatorRules.LIVING_HURT_EVENT, line) || matches(ValidatorRules.LIVING_ATTACK_EVENT, line))) {
                    continue;
                }

                int end = findBlockEnd(lines, i);
                for (int j = i; j <= end; j++) {
                    String bodyLine = lines.get(j);
                    if (matches(ValidatorRules.DAMAGE_CALL, bodyLine) || matches(ValidatorRules.ATTACK_CALL, bodyLine)) {
                        addFinding(
                                root,
                                file,
                                j + 1,
                                Severity.HIGH,
                                "entity.hurt/entity.attack used inside LivingHurtEvent/LivingAttackEvent handler (loop risk).",
                                findings,
                                dedupe
                        );
                    }
                }
            }
        }
    }

    private static void scanStaticCollectionLeaks(Path root, List<Path> javaFiles, List<Finding> findings, Set<String> dedupe) {
        for (Path file : javaFiles) {
            if (reachedFindingLimit(findings)) {
                return;
            }

            String text = readString(file);
            List<String> lines = splitLines(text);
            boolean hasCleanup = ValidatorRules.CLEANUP_HINT.matcher(text).find();

            for (int i = 0; i < lines.size(); i++) {
                Matcher matcher = ValidatorRules.STATIC_COLLECTION.matcher(lines.get(i));
                if (!matcher.find()) {
                    continue;
                }

                String genericType = matcher.group(1);
                String variable = matcher.group(2);
                boolean gameplaySensitive = genericType.contains("Player")
                        || genericType.contains("Entity")
                        || genericType.contains("BlockPos");

                if (!hasCleanup) {
                    Severity severity = gameplaySensitive ? Severity.HIGH : Severity.MEDIUM;
                    addFinding(
                            root,
                            file,
                            i + 1,
                            severity,
                            "Static collection '" + variable + "' has no obvious cleanup path (possible memory leak).",
                            findings,
                            dedupe
                    );
                }
            }
        }
    }

    private static void scanRegistryAndDatapackIssues(
            Path root,
            List<Path> javaFiles,
            List<Path> jsonFiles,
            Path resourcesRoot,
            KnownResources known,
            List<Finding> findings,
            Set<String> dedupe
    ) {
        scanDuplicateRegistryNames(root, javaFiles, findings, dedupe);

        for (Path json : jsonFiles) {
            if (reachedFindingLimit(findings)) {
                return;
            }

            String text = readString(json);
            String normalized = normalizePath(json);

            Matcher suspiciousMatcher = ValidatorRules.SUSPICIOUS_DUPLICATE_ORE.matcher(text);
            while (suspiciousMatcher.find()) {
                addFinding(
                        root,
                        json,
                        lineFromOffset(text, suspiciousMatcher.start()),
                        Severity.MEDIUM,
                        "Suspicious duplicated ore suffix reference: " + suspiciousMatcher.group(),
                        findings,
                        dedupe
                );
            }

            if (!(normalized.contains("/data/" + MOD_ID + "/recipes/")
                    || normalized.contains("/data/" + MOD_ID + "/loot_tables/")
                    || normalized.contains("/data/" + MOD_ID + "/tags/"))) {
                continue;
            }

            Matcher itemResultMatcher = ValidatorRules.ITEM_OR_RESULT_ID.matcher(text);
            while (itemResultMatcher.find()) {
                String fullId = itemResultMatcher.group(1);
                String idPath = fullId.substring((MOD_ID + ":").length());
                if (!known.registryIds().contains(idPath)) {
                    addFinding(
                            root,
                            json,
                            lineFromOffset(text, itemResultMatcher.start()),
                            Severity.MEDIUM,
                            "Unknown registry ID referenced in datapack: " + fullId,
                            findings,
                            dedupe
                    );
                }
            }

            if (normalized.contains("/data/" + MOD_ID + "/tags/")) {
                Set<String> expectedTagSet = expectedTagSetForPath(json, known);
                Matcher tagMatcher = ValidatorRules.TAG_VALUE_ID.matcher(text);
                while (tagMatcher.find()) {
                    String ref = tagMatcher.group(1);
                    if (!ref.startsWith("#")) {
                        String idPath = ref.substring((MOD_ID + ":").length());
                        if (!known.registryIds().contains(idPath)) {
                            addFinding(
                                    root,
                                    json,
                                    lineFromOffset(text, tagMatcher.start()),
                                    Severity.MEDIUM,
                                    "Invalid tag value reference to unknown ID: " + ref,
                                    findings,
                                    dedupe
                            );
                        }
                        continue;
                    }

                    String tagPath = ref.substring(("#" + MOD_ID + ":").length());
                    if (!expectedTagSet.isEmpty() && !expectedTagSet.contains(tagPath)) {
                        addFinding(
                                root,
                                json,
                                lineFromOffset(text, tagMatcher.start()),
                                Severity.MEDIUM,
                                "Invalid tag reference (target tag file not found): " + ref,
                                findings,
                                dedupe
                        );
                    }
                }
            }
        }
    }

    private static void scanDuplicateRegistryNames(Path root, List<Path> javaFiles, List<Finding> findings, Set<String> dedupe) {
        Map<String, List<Integer>> perFileIds = new LinkedHashMap<>();

        for (Path file : javaFiles) {
            List<String> lines = readLines(file);
            for (int i = 0; i < lines.size(); i++) {
                Matcher matcher = ValidatorRules.REGISTRY_REGISTER.matcher(lines.get(i));
                while (matcher.find()) {
                    String id = matcher.group(1);
                    String key = normalizePath(file) + "::" + id;
                    perFileIds.computeIfAbsent(key, ignored -> new ArrayList<>()).add(i + 1);
                }
            }
        }

        for (Map.Entry<String, List<Integer>> entry : perFileIds.entrySet()) {
            if (entry.getValue().size() < 2) {
                continue;
            }

            String key = entry.getKey();
            int split = key.lastIndexOf("::");
            String normalizedFile = key.substring(0, split);
            String id = key.substring(split + 2);
            Path absoluteFile = root.resolve(normalizedFile.replace('/', java.io.File.separatorChar));

            addFinding(
                    root,
                    absoluteFile,
                    entry.getValue().get(0),
                    Severity.HIGH,
                    "Duplicate registry name in same source file: '" + id + "'.",
                    findings,
                    dedupe
            );
        }
    }
    private static void scanModelTextureLangConsistency(
            Path root,
            Path resourcesRoot,
            KnownResources known,
            List<Finding> findings,
            Set<String> dedupe
    ) {
        Path assetsRoot = resourcesRoot.resolve("assets").resolve(MOD_ID);
        Path modelsRoot = assetsRoot.resolve("models");
        Path blockstatesRoot = assetsRoot.resolve("blockstates");

        List<Path> modelFiles = listFiles(modelsRoot, ".json");
        for (Path model : modelFiles) {
            if (reachedFindingLimit(findings)) {
                return;
            }

            String text = readString(model);
            Matcher textureMatcher = ValidatorRules.MODEL_TEXTURE_REF.matcher(text);
            while (textureMatcher.find()) {
                String texturePath = textureMatcher.group(1);
                if (!known.textureIds().contains(texturePath)) {
                    addFinding(
                            root,
                            model,
                            lineFromOffset(text, textureMatcher.start()),
                            Severity.MEDIUM,
                            "Model references missing texture: " + MOD_ID + ":" + texturePath,
                            findings,
                            dedupe
                    );
                }
            }
        }

        List<Path> blockstateFiles = listFiles(blockstatesRoot, ".json");
        for (Path blockstate : blockstateFiles) {
            if (reachedFindingLimit(findings)) {
                return;
            }

            String text = readString(blockstate);
            Matcher modelMatcher = ValidatorRules.MODEL_REF.matcher(text);
            while (modelMatcher.find()) {
                String modelPath = modelMatcher.group(1);
                Path expectedModel = modelsRoot.resolve(modelPath + ".json");
                if (!Files.exists(expectedModel)) {
                    addFinding(
                            root,
                            blockstate,
                            lineFromOffset(text, modelMatcher.start()),
                            Severity.MEDIUM,
                            "Blockstate references missing model: " + MOD_ID + ":" + modelPath,
                            findings,
                            dedupe
                    );
                }
            }
        }

        Path enUs = resourcesRoot.resolve("assets").resolve(MOD_ID).resolve("lang/en_us.json");
        for (String itemId : known.itemModelIds()) {
            if (reachedFindingLimit(findings)) {
                return;
            }

            String itemKey = "item." + MOD_ID + "." + itemId;
            String blockKey = "block." + MOD_ID + "." + itemId;
            if (!known.langKeys().contains(itemKey) && !known.langKeys().contains(blockKey)) {
                addFinding(
                        root,
                        enUs,
                        1,
                        Severity.LOW,
                        "Missing lang key for item model id: " + itemId,
                        findings,
                        dedupe
                );
            }
        }

        for (String blockId : known.blockStateIds()) {
            if (reachedFindingLimit(findings)) {
                return;
            }

            String blockKey = "block." + MOD_ID + "." + blockId;
            if (!known.langKeys().contains(blockKey)) {
                addFinding(
                        root,
                        enUs,
                        1,
                        Severity.LOW,
                        "Missing lang key for blockstate id: " + blockId,
                        findings,
                        dedupe
                );
            }
        }
    }

    private static KnownResources buildKnownResources(Path resourcesRoot) {
        Path assetsRoot = resourcesRoot.resolve("assets").resolve(MOD_ID);
        Path dataRoot = resourcesRoot.resolve("data").resolve(MOD_ID);

        Set<String> itemModelIds = collectJsonStems(assetsRoot.resolve("models/item"));
        Set<String> blockModelIds = collectJsonStems(assetsRoot.resolve("models/block"));
        Set<String> blockStateIds = collectJsonStems(assetsRoot.resolve("blockstates"));
        Set<String> textureIds = collectPngStems(assetsRoot.resolve("textures"));
        Set<String> itemTagIds = collectJsonStems(dataRoot.resolve("tags/items"));
        Set<String> blockTagIds = collectJsonStems(dataRoot.resolve("tags/blocks"));

        Set<String> registryIds = new LinkedHashSet<>();
        registryIds.addAll(itemModelIds);
        registryIds.addAll(blockModelIds);
        registryIds.addAll(blockStateIds);

        Set<String> langKeys = new LinkedHashSet<>();
        Path enUs = assetsRoot.resolve("lang/en_us.json");
        if (Files.exists(enUs)) {
            String text = readString(enUs);
            Matcher matcher = ValidatorRules.LANG_KEY.matcher(text);
            while (matcher.find()) {
                langKeys.add(matcher.group(1));
            }
        }

        return new KnownResources(registryIds, itemModelIds, blockStateIds, textureIds, itemTagIds, blockTagIds, langKeys);
    }

    private static Set<String> expectedTagSetForPath(Path tagFile, KnownResources known) {
        String normalized = normalizePath(tagFile);
        if (normalized.contains("/data/" + MOD_ID + "/tags/items/")) {
            return known.itemTagIds();
        }
        if (normalized.contains("/data/" + MOD_ID + "/tags/blocks/")) {
            return known.blockTagIds();
        }
        return Collections.emptySet();
    }

    private static void writeReport(Path reportPath, List<Finding> findings) throws IOException {
        Files.createDirectories(reportPath.getParent());

        Map<Severity, Integer> order = new EnumMap<>(Severity.class);
        for (int i = 0; i < ValidatorRules.ORDERED_SEVERITIES.size(); i++) {
            order.put(ValidatorRules.ORDERED_SEVERITIES.get(i), i);
        }

        List<Finding> sorted = findings.stream()
                .sorted(Comparator
                        .comparingInt((Finding f) -> order.getOrDefault(f.severity(), Integer.MAX_VALUE))
                        .thenComparing(Finding::file)
                        .thenComparingInt(Finding::line))
                .collect(Collectors.toList());

        try (BufferedWriter writer = Files.newBufferedWriter(reportPath, StandardCharsets.UTF_8)) {
            writer.write("ExtremeCraft Dev Validation Report");
            writer.newLine();
            writer.write("Generated: " + DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(LocalDateTime.now()));
            writer.newLine();
            writer.write("Total findings: " + sorted.size());
            writer.newLine();
            writer.newLine();

            if (sorted.isEmpty()) {
                writer.write("No issues detected by static validator rules.");
                writer.newLine();
                return;
            }

            for (Finding finding : sorted) {
                writer.write(finding.severity().name());
                writer.newLine();
                writer.write(finding.file() + ":" + finding.line());
                writer.newLine();
                writer.write(finding.message());
                writer.newLine();
                writer.newLine();
            }
        }
    }

    private static void addFinding(
            Path root,
            Path file,
            int line,
            Severity severity,
            String message,
            List<Finding> findings,
            Set<String> dedupe
    ) {
        if (reachedFindingLimit(findings)) {
            return;
        }

        String relative = toRelativePath(root, file);
        Finding finding = new Finding(severity, relative, Math.max(1, line), message);
        String key = finding.severity() + "|" + finding.file() + "|" + finding.line() + "|" + finding.message();
        if (dedupe.add(key)) {
            findings.add(finding);
        }
    }

    private static boolean reachedFindingLimit(List<Finding> findings) {
        return findings.size() >= MAX_FINDINGS;
    }

    private static boolean hasNearbyGuard(List<String> lines, int lineIndex, int back, int forward, Pattern guardPattern) {
        int start = Math.max(0, lineIndex - back);
        int end = Math.min(lines.size() - 1, lineIndex + forward);
        for (int i = start; i <= end; i++) {
            if (matches(guardPattern, lines.get(i))) {
                return true;
            }
        }
        return false;
    }

    private static int findFirstLine(List<String> lines, Pattern... patterns) {
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            for (Pattern pattern : patterns) {
                if (matches(pattern, line)) {
                    return i + 1;
                }
            }
        }
        return -1;
    }
    private static boolean contains(List<String> lines, Pattern pattern) {
        for (String line : lines) {
            if (matches(pattern, line)) {
                return true;
            }
        }
        return false;
    }

    private static int findBlockEnd(List<String> lines, int startIndex) {
        int depth = 0;
        boolean seenBrace = false;

        for (int i = startIndex; i < lines.size(); i++) {
            String line = lines.get(i);
            int open = countChar(line, '{');
            int close = countChar(line, '}');

            if (open > 0) {
                seenBrace = true;
            }

            depth += open;
            depth -= close;

            if (seenBrace && depth <= 0 && i > startIndex) {
                return i;
            }
        }

        return Math.min(lines.size() - 1, startIndex + 120);
    }

    private static int countChar(String line, char token) {
        int count = 0;
        for (int i = 0; i < line.length(); i++) {
            if (line.charAt(i) == token) {
                count++;
            }
        }
        return count;
    }

    private static boolean matches(Pattern pattern, String line) {
        return pattern.matcher(line).find();
    }

    private static int lineFromOffset(String text, int offset) {
        int line = 1;
        int max = Math.min(offset, text.length());
        for (int i = 0; i < max; i++) {
            if (text.charAt(i) == '\n') {
                line++;
            }
        }
        return line;
    }

    private static Set<String> collectJsonStems(Path root) {
        List<Path> files = listFiles(root, ".json");
        Set<String> stems = new LinkedHashSet<>();
        for (Path file : files) {
            String relative = normalizePath(root.relativize(file));
            if (!relative.endsWith(".json")) {
                continue;
            }
            stems.add(relative.substring(0, relative.length() - ".json".length()));
        }
        return stems;
    }

    private static Set<String> collectPngStems(Path root) {
        List<Path> files = listFiles(root, ".png");
        Set<String> stems = new LinkedHashSet<>();
        for (Path file : files) {
            String relative = normalizePath(root.relativize(file));
            if (!relative.endsWith(".png")) {
                continue;
            }
            stems.add(relative.substring(0, relative.length() - ".png".length()));
        }
        return stems;
    }

    private static List<Path> listFiles(Path root, String extension) {
        if (!Files.exists(root)) {
            return Collections.emptyList();
        }
        try (Stream<Path> stream = Files.walk(root)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(extension))
                    .sorted()
                    .collect(Collectors.toList());
        } catch (IOException ex) {
            return Collections.emptyList();
        }
    }

    private static List<String> readLines(Path file) {
        try {
            return Files.readAllLines(file, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            return Collections.emptyList();
        }
    }

    private static String readString(Path file) {
        try {
            return Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            return "";
        }
    }

    private static List<String> splitLines(String text) {
        if (text.isEmpty()) {
            return Collections.emptyList();
        }
        return List.of(text.split("\\R", -1));
    }

    private static String toRelativePath(Path root, Path file) {
        Path absoluteRoot = root.toAbsolutePath().normalize();
        Path absoluteFile = file.toAbsolutePath().normalize();
        if (absoluteFile.startsWith(absoluteRoot)) {
            return normalizePath(absoluteRoot.relativize(absoluteFile));
        }
        return normalizePath(absoluteFile);
    }

    private static String normalizePath(Path path) {
        return path.toString().replace('\\', '/');
    }

    public record ValidationResult(Path reportPath, List<Finding> findings) {
    }

    public record Finding(Severity severity, String file, int line, String message) {
    }

    private record KnownResources(
            Set<String> registryIds,
            Set<String> itemModelIds,
            Set<String> blockStateIds,
            Set<String> textureIds,
            Set<String> itemTagIds,
            Set<String> blockTagIds,
            Set<String> langKeys
    ) {
    }
}



