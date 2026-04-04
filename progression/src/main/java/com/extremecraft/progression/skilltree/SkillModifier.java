package com.extremecraft.progression.skilltree;

public record SkillModifier(String modifierId, double value, Operation operation) {
    public enum Operation {
        ADD,
        MULTIPLY,
        PERCENT;

        public static Operation byName(String raw) {
            if (raw == null || raw.isBlank()) {
                return ADD;
            }

            return switch (raw.trim().toUpperCase()) {
                case "MULTIPLY" -> MULTIPLY;
                case "PERCENT" -> PERCENT;
                default -> ADD;
            };
        }
    }
}
