package com.extremecraft.gameplay;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

class ProgressionBoundarySourceTest {
    private static final Path ROOT = Path.of("").toAbsolutePath();
    private static final Path MAIN_JAVA = ROOT.resolve("src/main/java");

    @Test
    void nonProgressionPackagesDoNotBypassFacadeMutationAuthority() throws IOException {
        List<String> violations = new ArrayList<>();

        try (Stream<Path> files = Files.walk(MAIN_JAVA)) {
            files.filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> !isAllowedDirectMutationOwner(path))
                    .forEach(path -> {
                        try {
                            String source = Files.readString(path, StandardCharsets.UTF_8);
                            check(source, path, violations, "ProgressionService.addXp(");
                            check(source, path, violations, "ProgressionService.setLevel(");
                            check(source, path, violations, "ProgressionMutationService.grantXp(");
                            check(source, path, violations, "ProgressionMutationService.setLevel(");
                            check(source, path, violations, "LevelService.grantXp(");
                            check(source, path, violations, "LevelService.setLevel(");
                            check(source, path, violations, "SkillProgressionService.grantSkillXp(");
                            check(source, path, violations, "ClassProgressionService.grantClassXp(");
                            check(source, path, violations, "QuestRewardService.claimQuestReward(");
                            check(source, path, violations, "GuildQuestRewardService.claimQuestReward(");
                        } catch (IOException exception) {
                            throw new RuntimeException(exception);
                        }
                    });
        }

        assertTrue(violations.isEmpty(), () -> "Progression mutation boundary violations:\n" + String.join("\n", violations));
    }

    private static boolean isAllowedDirectMutationOwner(Path path) {
        String normalized = MAIN_JAVA.relativize(path).toString().replace('\\', '/');
        return normalized.startsWith("com/extremecraft/progression/")
                || normalized.equals("com/extremecraft/game/ProgressionSystem.java");
    }

    private static void check(String source, Path path, List<String> violations, String bannedCall) {
        if (!source.contains(bannedCall)) {
            return;
        }

        String relative = ROOT.relativize(path).toString().replace('\\', '/');
        violations.add(relative + " contains " + bannedCall);
    }
}
