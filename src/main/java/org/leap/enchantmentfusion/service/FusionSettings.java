package org.leap.enchantmentfusion.service;

import org.leap.enchantmentfusion.model.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.nio.file.*;
import java.util.*;

public record FusionSettings(boolean pvp, Set<String> disabledWorlds, Map<String, FusionTuning> tunings) {
    public FusionSettings { disabledWorlds = Set.copyOf(disabledWorlds); tunings = Map.copyOf(tunings); }
    public static FusionSettings load(File file, Collection<FusionDefinition> definitions) throws Exception {
        YamlConfiguration yaml = new YamlConfiguration(); yaml.load(file);
        for (String key : yaml.getKeys(false)) if (!Set.of("config-version", "allow-pvp-abilities", "disabled-worlds", "fusions").contains(key))
            throw new IllegalArgumentException("Unknown root setting: " + key);
        if (yaml.contains("config-version") && (!yaml.isInt("config-version") || yaml.getInt("config-version") < 1 || yaml.getInt("config-version") > 2))
            throw new IllegalArgumentException("Unsupported config-version");
        boolean legacy = yaml.getInt("config-version", 1) == 1;
        if (yaml.contains("allow-pvp-abilities") && !yaml.isBoolean("allow-pvp-abilities")) throw new IllegalArgumentException("allow-pvp-abilities must be boolean");
        if (yaml.contains("disabled-worlds") && (!yaml.isList("disabled-worlds")
                || yaml.getList("disabled-worlds").stream().anyMatch(x -> !(x instanceof String)))) throw new IllegalArgumentException("disabled-worlds must be a list");
        Map<String, FusionTuning> result = new LinkedHashMap<>();
        for (FusionDefinition definition : definitions) {
            String path = "fusions." + definition.id(); ConfigurationSection section = yaml.getConfigurationSection(path);
            if (yaml.contains(path) && section == null) throw new IllegalArgumentException(path + " must be a section");
            FusionTuning d = definition.defaults();
            if (section == null) { result.put(definition.id(), d); continue; }
            Set<String> keys = legacy ? Set.of("enabled", "extra-levels", "levels", "catalyst-amount", "cooldown-seconds", "chance", "power")
                    : Set.of("enabled", "extra-levels", "cooldown-seconds", "chance", "power");
            for (String key : section.getKeys(false)) {
                if (!keys.contains(key)) throw new IllegalArgumentException("Unknown setting: " + path + "." + key);
                if (key.equals("enabled")) { if (!section.isBoolean(key)) throw new IllegalArgumentException(path + ".enabled must be boolean"); }
                else if (!(section.get(key) instanceof Number n) || !Double.isFinite(n.doubleValue())) throw new IllegalArgumentException(path + "." + key + " must be finite number");
            }
            for (String key : List.of("extra-levels", "levels", "catalyst-amount")) if (section.contains(key)) {
                double n = section.getDouble(key);
                if (n != Math.rint(n) || n < 0 || n > (key.equals("catalyst-amount") ? 64 : key.equals("levels") ? 39 : 12)
                        || !key.equals("extra-levels") && n < 1) throw new IllegalArgumentException(path + "." + key + " out of range");
            }
            double chance = section.getDouble("chance", d.chance()), cooldown = section.getDouble("cooldown-seconds", d.cooldown());
            if (legacy) {
                Double old = Map.of("workhorse", .25, "miners_luck", .4, "preservation", .35, "fortified", .2, "master_angler", 1.0).get(definition.id());
                if (old != null && Double.compare(chance, old) == 0) chance = d.chance();
                if (definition.id().equals("master_angler") && cooldown == 12) cooldown = d.cooldown();
            }
            result.put(definition.id(), new FusionTuning(section.getBoolean("enabled", d.enabled()),
                    section.getInt("extra-levels", d.extraLevels()), cooldown, chance, section.getDouble("power", d.power())));
        }
        ConfigurationSection fusions = yaml.getConfigurationSection("fusions");
        if (yaml.contains("fusions") && fusions == null) throw new IllegalArgumentException("fusions must be a section");
        if (fusions != null) for (String key : fusions.getKeys(false)) if (!result.containsKey(key)) throw new IllegalArgumentException("Unknown fusion: " + key);
        return new FusionSettings(yaml.getBoolean("allow-pvp-abilities", false), new HashSet<>(yaml.getStringList("disabled-worlds")), result);
    }
    public static FusionSettings loadAndMigrate(File file, Collection<FusionDefinition> definitions) throws Exception {
        FusionSettings settings = load(file, definitions);
        YamlConfiguration old = new YamlConfiguration(); old.load(file);
        if (old.getInt("config-version", 1) >= 2) return settings;
        YamlConfiguration migrated = new YamlConfiguration();
        migrated.set("config-version", 2); migrated.set("allow-pvp-abilities", settings.pvp()); migrated.set("disabled-worlds", new ArrayList<>(settings.disabledWorlds()));
        settings.tunings().forEach((id, t) -> {
            String p = "fusions." + id + ".";
            migrated.set(p + "enabled", t.enabled()); migrated.set(p + "extra-levels", t.extraLevels());
            migrated.set(p + "cooldown-seconds", t.cooldown()); migrated.set(p + "chance", t.chance()); migrated.set(p + "power", t.power());
        });
        Path original = file.toPath(), backup = original.resolveSibling(file.getName() + ".v1-backup-" + UUID.randomUUID());
        Files.copy(original, backup, StandardCopyOption.COPY_ATTRIBUTES);
        Path temporary = Files.createTempFile(original.toAbsolutePath().getParent(), "fusion-config-", ".tmp");
        try {
            migrated.save(temporary.toFile());
            try { Files.move(temporary, original, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
            catch (AtomicMoveNotSupportedException unsupported) { Files.move(temporary, original, StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(temporary); }
        return settings;
    }
}
