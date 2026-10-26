package org.leap.enchantmentfusion.service;

import net.kyori.adventure.text.Component;
import org.leap.enchantmentfusion.model.*;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

/** Exercises real ItemMeta/PDC and serialization without modifying the world or player inventories. */
public final class FusionDiagnostics {
    private FusionDiagnostics() {}
    public static Material baseMaterial(Equipment category) {
        return switch (category) {
            case SWORD -> Material.DIAMOND_SWORD;
            case TOOL, PICKAXE -> Material.DIAMOND_PICKAXE;
            case ARMOR -> Material.DIAMOND_CHESTPLATE;
            case BOOTS -> Material.DIAMOND_BOOTS;
            case HELMET -> Material.DIAMOND_HELMET;
            default -> Material.valueOf(category.name());
        };
    }
    public static ItemStack source(FusionDefinition definition) {
        ItemStack item = new ItemStack(baseMaterial(definition.equipment()));
        definition.ingredients().forEach((key, level) -> {
            var enchant = FusionItems.enchantment(key);
            if (!enchant.canEnchantItem(item)) throw new IllegalStateException(definition.id() + ": invalid native ingredient " + key);
            item.addEnchantment(enchant, level);
        });
        return item;
    }
    public static ItemStack book(FusionDefinition definition) {
        ItemStack item = new ItemStack(Material.ENCHANTED_BOOK);
        EnchantmentStorageMeta meta = (EnchantmentStorageMeta) item.getItemMeta();
        definition.ingredients().forEach((key, level) -> meta.addStoredEnchant(FusionItems.enchantment(key), level, false));
        item.setItemMeta(meta); return item;
    }
    public static ItemStack ingredientBook(String key, int level) {
        ItemStack item = new ItemStack(Material.ENCHANTED_BOOK);
        EnchantmentStorageMeta meta = (EnchantmentStorageMeta) item.getItemMeta();
        meta.addStoredEnchant(FusionItems.enchantment(key), level, false); item.setItemMeta(meta); return item;
    }
    public static int run(Collection<FusionDefinition> definitions, FusionItems items, Set<String> handlers) {
        int checks = 0;
        Set<String> ids = new HashSet<>();
        for (FusionDefinition definition : definitions) {
            ids.add(definition.id());
            ItemStack source = source(definition);
            var originalMeta = source.getItemMeta();
            originalMeta.displayName(Component.text("Metadata test " + definition.id()));
            originalMeta.lore(List.of(Component.text("Original lore")));
            originalMeta.getPersistentDataContainer().set(new NamespacedKey("otherplugin", "retained"), PersistentDataType.STRING, "yes");
            source.setItemMeta(originalMeta);
            ItemStack before = source.clone();
            UUID identity = UUID.randomUUID();
            ItemStack fused = items.fuse(source, definition, identity);
            require(source.equals(before), "Input was mutated: " + definition.id()); checks++;
            var state = items.read(fused).orElseThrow(() -> new IllegalStateException("Unreadable fusion: " + definition.id()));
            require(state.instance().equals(identity) && state.definition().id().equals(definition.id()), "Wrong fusion identity"); checks++;
            require(fused.getEnchantments().equals(source.getEnchantments()), "Native carriers changed"); checks++;
            require(Objects.equals(fused.getItemMeta().displayName(), originalMeta.displayName()), "Name lost"); checks++;
            require("yes".equals(fused.getItemMeta().getPersistentDataContainer().get(new NamespacedKey("otherplugin", "retained"), PersistentDataType.STRING)), "External PDC lost"); checks++;
            require(items.strip(fused).equals(before), "Cleanse changed unrelated metadata"); checks++;
            require(items.read(ItemStack.deserializeBytes(fused.serializeAsBytes())).isPresent(), "Serialization lost fusion"); checks++;
            ItemStack fakeLore = source.clone();
            var fakeMeta = fakeLore.getItemMeta(); fakeMeta.lore(fused.getItemMeta().lore()); fakeLore.setItemMeta(fakeMeta);
            require(items.read(fakeLore).isEmpty(), "Lore forgery accepted"); checks++;
            ItemStack corrupted = fused.clone();
            var bad = corrupted.getItemMeta(); bad.getPersistentDataContainer().set(new NamespacedKey("enchantmentfusion", "schema"), PersistentDataType.INTEGER, 999); corrupted.setItemMeta(bad);
            require(items.marked(corrupted) && items.read(corrupted).isEmpty(), "Unknown schema accepted"); checks++;
            ItemStack changed = fused.clone(); changed.removeEnchantment(FusionItems.enchantment(definition.ingredients().keySet().iterator().next()));
            require(items.read(changed).isEmpty(), "Missing bound source accepted"); checks++;
            ItemStack stacked = fused.clone(); stacked.setAmount(2);
            require(items.read(stacked).isEmpty(), "Stacked fusion accepted"); checks++;
            ItemStack fusionBook = items.fuse(book(definition), definition, UUID.randomUUID());
            require(items.read(fusionBook).isPresent(), "Fusion book unreadable"); checks++;
            require(items.read(ItemStack.deserializeBytes(fusionBook.serializeAsBytes())).isPresent(), "Book serialization lost identity"); checks++;
            require(FusionItems.enchantments(fusionBook).equals(definition.ingredients()), "Stored enchants changed"); checks++;
        }
        require(ids.equals(handlers), "Catalogue and handlers differ: " + ids + " / " + handlers); checks++;
        return checks + settingsChecks(definitions) + malformedChecks(definitions.iterator().next(), items);
    }
    private static int malformedChecks(FusionDefinition definition, FusionItems items) {
        ItemStack base = items.fuse(source(definition), definition, UUID.randomUUID());
        int checks = 0;
        for (String key : List.of("fusion_id", "schema", "instance", "bound_sources")) {
            ItemStack item = base.clone();
            var meta = item.getItemMeta();
            meta.getPersistentDataContainer().set(new NamespacedKey("enchantmentfusion", key), PersistentDataType.BYTE_ARRAY, new byte[]{1});
            item.setItemMeta(meta);
            require(items.read(item).isEmpty(), "Wrong PDC primitive accepted: " + key); checks++;
        }
        for (var entry : Map.of("fusion_id", "unknown_fusion", "instance", "invalid-uuid", "bound_sources", "invalid=sources").entrySet()) {
            ItemStack item = base.clone(); var meta = item.getItemMeta();
            meta.getPersistentDataContainer().set(new NamespacedKey("enchantmentfusion", entry.getKey()), PersistentDataType.STRING, entry.getValue()); item.setItemMeta(meta);
            require(items.read(item).isEmpty(), "Malformed PDC accepted: " + entry.getKey()); checks++;
        }
        return checks;
    }
    private static int settingsChecks(Collection<FusionDefinition> definitions) {
        int checks = 0;
        java.nio.file.Path file = null;
        try {
            file = java.nio.file.Files.createTempFile("fusion-settings-test-", ".yml");
            java.nio.file.Files.writeString(file, "config-version: 2\nfusions:\n  infernal_edge:\n    extra-levels: 4\n    chance: 0.5\n");
            var settings = FusionSettings.load(file.toFile(), definitions);
            require(settings.tunings().get("infernal_edge").extraLevels() == 4 && settings.tunings().get("infernal_edge").chance() == .5,
                    "Valid configuration override rejected"); checks++;
            for (String invalid : List.of(
                    "allow-pvp-abilities: yes-please\n", "disabled-worlds: [123]\n", "unknown-root: true\n",
                    "fusions: false\n", "fusions:\n  typo_id: {}\n", "fusions:\n  infernal_edge: false\n",
                    "fusions:\n  infernal_edge:\n    typo_setting: 1\n", "fusions:\n  infernal_edge:\n    enabled: 1\n",
                    "fusions:\n  infernal_edge:\n    levels: 4294967314\n", "fusions:\n  infernal_edge:\n    levels: 18.5\n",
                    "fusions:\n  infernal_edge:\n    levels: 40\n", "config-version: 2\nfusions:\n  infernal_edge:\n    catalyst-amount: 1\n",
                    "fusions:\n  infernal_edge:\n    cooldown-seconds: .nan\n", "fusions:\n  infernal_edge:\n    chance: .inf\n",
                    "fusions:\n  infernal_edge:\n    power: 999\n")) {
                java.nio.file.Files.writeString(file, invalid);
                boolean rejected = false;
                try { FusionSettings.load(file.toFile(), definitions); } catch (Exception expected) { rejected = true; }
                require(rejected, "Invalid YAML settings accepted: " + invalid); checks++;
            }
            java.nio.file.Files.writeString(file, "fusions:\n  infernal_edge:\n    levels: 18\n    catalyst-amount: 4\n  workhorse:\n    chance: 0.25\n");
            var migrated = FusionSettings.loadAndMigrate(file.toFile(), definitions);
            require(migrated.tunings().get("infernal_edge").extraLevels() == 3, "Old fixed fee was reused as new surcharge"); checks++;
            require(migrated.tunings().get("workhorse").chance() == .15, "Old default chance was not rebalanced"); checks++;
            require(!java.nio.file.Files.readString(file).contains("catalyst-amount"), "Legacy catalyst survived migration"); checks++;
            var backupPrefix = file.getFileName().toString() + ".v1-backup-";
            try (var backups = java.nio.file.Files.list(file.getParent())) {
                var paths = backups.filter(path -> path.getFileName().toString().startsWith(backupPrefix)).toList();
                require(paths.size() == 1, "Migration did not preserve original config"); checks++;
                for (var backup : paths) java.nio.file.Files.delete(backup);
            }
            return checks;
        } catch (Exception failure) { throw new IllegalStateException("Settings diagnostics failed", failure); }
        finally { if (file != null) try { java.nio.file.Files.deleteIfExists(file); } catch (java.io.IOException ignored) {} }
    }
    private static void require(boolean condition, String message) { if (!condition) throw new IllegalStateException(message); }
}
