package com.extremecraft.progression;

import java.util.LinkedHashMap;
import java.util.Map;

public final class AttributeModifiers {
    private final Map<String, Double> additive = new LinkedHashMap<>();
    private final Map<String, Double> multiplicative = new LinkedHashMap<>();

    public void add(String stat, double value) {
        if (stat == null || stat.isBlank() || value == 0.0D) {
            return;
        }

        additive.merge(stat.trim().toLowerCase(), value, Double::sum);
    }

    public void multiply(String stat, double multiplier) {
        if (stat == null || stat.isBlank() || multiplier == 0.0D) {
            return;
        }

        multiplicative.merge(stat.trim().toLowerCase(), multiplier, Double::sum);
    }

    public double apply(String stat, double base) {
        String key = stat.trim().toLowerCase();
        double add = additive.getOrDefault(key, 0.0D);
        double mul = multiplicative.getOrDefault(key, 0.0D);
        return (base + add) * (1.0D + mul);
    }

    public Map<String, Double> additive() {
        return Map.copyOf(additive);
    }

    public Map<String, Double> multiplicative() {
        return Map.copyOf(multiplicative);
    }
}
