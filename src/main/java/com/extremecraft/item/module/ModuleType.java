package com.extremecraft.item.module;

public enum ModuleType {
    ARMOR,
    TOOL,
    UNIVERSAL;

    public static ModuleType byName(String raw) {
        if (raw == null || raw.isBlank()) {
            return UNIVERSAL;
        }

        return switch (raw.trim().toLowerCase()) {
            case "armor" -> ARMOR;
            case "tool" -> TOOL;
            default -> UNIVERSAL;
        };
    }
}
