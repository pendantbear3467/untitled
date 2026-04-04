package com.extremecraft.gameplay;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ProgressionBoundarySourceTest {
    private static final Path ROOT = Path.of("").toAbsolutePath();
    private static final Path JAVA_ROOT = ROOT.resolve("src/main/java");

    @Test
    void lowerLevelMutationServicesStayBehindFacade() throws IOException {
        assertOnlyAllowedCallers("ProgressionMutationService.grantXp(", Set.of(
                "src/main/java/com/extremecraft/progression/ProgressionFacade.java"
        ));
        assertOnlyAllowedCallers("ProgressionMutationService.setLevel(", Set.of(
                "src/main/java/com/extremecraft/progression/ProgressionFacade.java"
        ));
        assertOnlyAllowedCallers("SkillTreeService.tryUnlockByNodeId(", Set.of(
                "src/main/java/com/extremecraft/progression/ProgressionFacade.java"
        ));
        assertOnlyAllowedCallers("SkillTreeService.tryUnlock(", Set.of(
                "src/main/java/com/extremecraft/progression/ProgressionFacade.java"
        ));
        assertOnlyAllowedCallers("SkillProgressionService.grantSkillXp(", Set.of(
                "src/main/java/com/extremecraft/progression/ProgressionFacade.java"
        ));
        assertOnlyAllowedCallers("ClassProgressionService.grantClassXp(", Set.of(
                "src/main/java/com/extremecraft/progression/ProgressionFacade.java"
        ));
        assertOnlyAllowedCallers("QuestRewardService.claimQuestReward(", Set.of(
                "src/main/java/com/extremecraft/progression/GuildQuestRewardService.java",
                "src/main/java/com/extremecraft/progression/ProgressionFacade.java"
        ));
    }

    @Test
    void directBackingStateWritesStayContained() throws IOException {
        assertOnlyAllowedCallers("LevelService.grantXp(", Set.of(
                "src/main/java/com/extremecraft/progression/level/PlayerLevelGameplayEvents.java"
        ));
        assertOnlyAllowedCallers("LevelService.setLevel(", Set.of());
        assertOnlyAllowedCallers("skills.addSkillXp(", Set.of(
                "src/main/java/com/extremecraft/progression/SkillProgressionService.java"
        ));
        assertOnlyAllowedCallers("data.addXp(", Set.of(
                "src/main/java/com/extremecraft/progression/ProgressionService.java"
        ));
        assertOnlyAllowedCallers("data.setLevel(", Set.of(
                "src/main/java/com/extremecraft/progression/ProgressionService.java"
        ));
        assertOnlyAllowedCallers("data.addClassExperience(", Set.of(
                "src/main/java/com/extremecraft/progression/ClassProgressionService.java"
        ));
    }

    private static void assertOnlyAllowedCallers(String needle, Set<String> allowedRelativePaths) throws IOException {
        List<String> offenders = new ArrayList<>();

        try (var files = Files.walk(JAVA_ROOT)) {
            files.filter(path -> path.toString().endsWith(".java")).forEach(path -> {
                try {
                    String relative = ROOT.relativize(path).toString().replace('\\', '/');
                    if (allowedRelativePaths.contains(relative)) {
                        return;
                    }

                    String source = Files.readString(path, StandardCharsets.UTF_8);
                    if (source.contains(needle)) {
                        offenders.add(relative);
                    }
                } catch (IOException exception) {
                    throw new RuntimeException(exception);
                }
            });
        }

        assertTrue(offenders.isEmpty(), () -> "Unexpected callers for " + needle + ": " + String.join(", ", offenders));
    }
}
