package com.extremecraft.magic;

public record SpellModifier(String id, double value, Operation operation) {
    public enum Operation {
        ADD,
        MULTIPLY,
        PERCENT;

        public static Operation byName(String name) {
            if (name == null || name.isBlank()) {
                return ADD;
            }
            return switch (name.trim().toLowerCase()) {
                case "multiply", "mul" -> MULTIPLY;
                case "percent", "pct" -> PERCENT;
                default -> ADD;
            };
        }
    }
}
