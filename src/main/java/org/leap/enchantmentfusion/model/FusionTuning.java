package org.leap.enchantmentfusion.model;

public record FusionTuning(boolean enabled, int extraLevels, double cooldown, double chance, double power) {
    public FusionTuning {
        if (extraLevels < 0 || extraLevels > 12
                || !Double.isFinite(cooldown) || cooldown < 0 || cooldown > 300
                || !Double.isFinite(chance) || chance < 0 || chance > 1
                || !Double.isFinite(power) || power < .25 || power > 2)
            throw new IllegalArgumentException("Invalid tuning (extra-levels 0..12, cooldown 0..300, chance 0..1, power .25..2)");
    }
}
