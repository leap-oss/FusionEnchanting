package org.leap.enchantmentfusion.service;

import org.leap.enchantmentfusion.model.*;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public final class FusionService {
    private final Map<String, FusionDefinition> catalog;
    private final FusionItems items;
    private FusionSettings settings;

    public FusionService(Collection<FusionDefinition> definitions, FusionSettings settings, FusionItems items) {
        Map<String, FusionDefinition> map = new LinkedHashMap<>();
        Set<Set<String>> pairs = new HashSet<>();
        for (FusionDefinition d : definitions) {
            if (map.put(d.id(), d) != null || !pairs.add(d.ingredients().keySet())) throw new IllegalArgumentException("Duplicate fusion ID or enchantment pair");
            d.ingredients().keySet().forEach(FusionItems::enchantment);
        }
        catalog = Collections.unmodifiableMap(map);
        this.settings = settings; this.items = items;
    }
    public Collection<FusionDefinition> definitions() { return catalog.values(); }
    public FusionDefinition definition(String id) { return catalog.get(id); }
    public FusionTuning tuning(String id) { return settings.tunings().get(id); }
    public FusionSettings settings() { return settings; }
    public void settings(FusionSettings value) { settings = value; }
    public boolean allowed(Player player) {
        return player.hasPermission("enchantmentfusion.use") && !settings.disabledWorlds().contains(player.getWorld().getName());
    }
    public List<FusionDefinition> bookCandidates(ItemStack left, ItemStack right, ItemStack nativeResult) {
        if (!book(left) || !book(right) || !book(nativeResult) || items.marked(left) || items.marked(right)) return List.of();
        var a = FusionItems.enchantments(left); var b = FusionItems.enchantments(right); var merged = FusionItems.enchantments(nativeResult);
        return catalog.values().stream().filter(d -> d.ingredients().keySet().stream().allMatch(key -> {
                    int max = FusionItems.enchantment(key).getMaxLevel();
                    return a.getOrDefault(key, 0) <= max && b.getOrDefault(key, 0) <= max && merged.getOrDefault(key, 0) <= max;
                })).filter(d -> FusionRules.matchesBooks(d, a, b, merged, false, tuning(d.id()))).toList();
    }
    public Optional<FusionItems.State> active(Player player, ItemStack item) {
        if (!allowed(player) || item == null || item.getType() == Material.ENCHANTED_BOOK) return Optional.empty();
        return items.read(item).filter(state -> tuning(state.definition().id()).enabled());
    }
    public static boolean book(ItemStack item) { return item != null && item.getType() == Material.ENCHANTED_BOOK && item.getAmount() == 1; }
}
