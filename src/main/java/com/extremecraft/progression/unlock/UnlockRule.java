package com.extremecraft.progression.unlock;

import java.util.LinkedHashSet;
import java.util.Set;

public record UnlockRule(
                String unlock,
                String requiredClass,
                String requiredSkill,
                int requiredSkillLevel,
                String requiredQuest,
                String requiredStage,
                Set<String> requiredUnlocks
) {
        public UnlockRule {
                requiredUnlocks = sanitize(requiredUnlocks);
        }

        private static Set<String> sanitize(Set<String> values) {
                Set<String> clean = new LinkedHashSet<>();
                if (values == null) {
                        return clean;
                }

                for (String value : values) {
                        if (value == null) {
                                continue;
                        }

                        String normalized = value.trim().toLowerCase();
                        if (!normalized.isBlank()) {
                                clean.add(normalized);
                        }
                }
                return clean;
        }
}
