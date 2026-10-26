package org.leap.enchantmentfusion.model;

import java.util.*;

/** Book recipes and XP arithmetic only; normal equipment enchanting never creates a fusion. */
public final class FusionRules {
    private FusionRules() {}
    public static boolean matchesBooks(FusionDefinition recipe, Map<String, Integer> left,
                                       Map<String, Integer> right, Map<String, Integer> merged,
                                       boolean alreadyFused, FusionTuning tuning) {
        if (alreadyFused || !tuning.enabled() || recipe.ingredients().entrySet().stream()
                .anyMatch(e -> merged.getOrDefault(e.getKey(), 0) < e.getValue())) return false;
        List<String> keys = new ArrayList<>(recipe.ingredients().keySet());
        String a = keys.get(0), b = keys.get(1);
        return left.getOrDefault(a, 0) > 0 && right.getOrDefault(b, 0) > 0
                || left.getOrDefault(b, 0) > 0 && right.getOrDefault(a, 0) > 0;
    }
    public static int creationCost(int vanillaCost, int extraLevels) {
        if (vanillaCost < 1 || extraLevels < 0 || extraLevels > 12) throw new IllegalArgumentException("Invalid anvil cost");
        return (int) Math.min(Integer.MAX_VALUE, (long) vanillaCost + extraLevels);
    }
}
