package org.leap.enchantmentfusion.model;

import java.util.Map;
import java.util.Objects;

public record FusionDefinition(String id, String name, Equipment equipment,
        Map<String, Integer> ingredients, int extraLevels,
        double cooldown, double chance, double power, String description, String limitation) {
    public FusionDefinition {
        if (!id.matches("[a-z][a-z0-9_]{0,47}")) throw new IllegalArgumentException("Invalid fusion ID");
        Objects.requireNonNull(equipment);
        ingredients = Map.copyOf(ingredients);
        if (ingredients.size() != 2 || ingredients.values().stream().anyMatch(n -> n < 1 || n > 10))
            throw new IllegalArgumentException("Fusion must have two valid ingredients: " + id);
        new FusionTuning(true, extraLevels, cooldown, chance, power);
    }
    public FusionTuning defaults() { return new FusionTuning(true, extraLevels, cooldown, chance, power); }
}
