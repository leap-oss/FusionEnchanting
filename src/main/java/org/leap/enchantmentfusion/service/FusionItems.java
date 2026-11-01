package org.leap.enchantmentfusion.service;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.leap.enchantmentfusion.model.FusionDefinition;
import org.leap.enchantmentfusion.model.*;
import org.bukkit.NamespacedKey;
import org.bukkit.Material;
import org.bukkit.Registry;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class FusionItems {
    public record State(FusionDefinition definition, UUID instance, Map<String, Integer> sources) {
        public int level(){return FusionLevels.level(definition,sources);}
    }
    private static final String LORE_MARKER = "leap:enchantment_fusion";
    private final NamespacedKey id, schema, instance, sources, displayFlags;
    private final Function<String, FusionDefinition> definitions;
    private Function<String,FusionTuning> tunings;

    public FusionItems(Plugin plugin, Function<String, FusionDefinition> definitions) {
        this.definitions = definitions;
        tunings = key -> definitions.apply(key).defaults();
        id = new NamespacedKey("enchantmentfusion", "fusion_id");
        schema = new NamespacedKey("enchantmentfusion", "schema");
        instance = new NamespacedKey("enchantmentfusion", "instance");
        sources = new NamespacedKey("enchantmentfusion", "bound_sources");
        displayFlags = new NamespacedKey("enchantmentfusion", "original_display_flags");
    }
    public void tunings(Function<String,FusionTuning> value){tunings=Objects.requireNonNull(value);}

    public boolean marked(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        var data = item.getItemMeta().getPersistentDataContainer();
        return data.getKeys().contains(id) || data.getKeys().contains(schema)
                || data.getKeys().contains(instance) || data.getKeys().contains(sources) || data.getKeys().contains(displayFlags);
    }

    public Optional<State> read(ItemStack item) {
        if (!marked(item) || item.getAmount() != 1) return Optional.empty();
        var data = item.getItemMeta().getPersistentDataContainer();
        if (!data.has(schema, PersistentDataType.INTEGER) || !data.has(id, PersistentDataType.STRING)
                || !data.has(instance, PersistentDataType.STRING) || !data.has(sources, PersistentDataType.STRING)
                || !Integer.valueOf(1).equals(data.get(schema, PersistentDataType.INTEGER))) return Optional.empty();
        String name = data.get(id, PersistentDataType.STRING);
        String rawInstance = data.get(instance, PersistentDataType.STRING);
        String rawSources = data.get(sources, PersistentDataType.STRING);
        if (name == null || name.length() > 48 || rawInstance == null || rawInstance.length() != 36
                || rawSources == null || rawSources.length() > 256) return Optional.empty();
        FusionDefinition definition = definitions.apply(name);
        if (definition == null || item.getType() != Material.ENCHANTED_BOOK && !definition.equipment().accepts(item.getType().name())) return Optional.empty();
        if(data.getKeys().contains(displayFlags)&&(!data.has(displayFlags,PersistentDataType.INTEGER)
                ||data.get(displayFlags,PersistentDataType.INTEGER)<0||data.get(displayFlags,PersistentDataType.INTEGER)>3))return Optional.empty();
        try {
            Map<String, Integer> bound = new HashMap<>();
            for (String part : rawSources.split(";")) {
                String[] pair = part.split("=");
                if (pair.length != 2 || bound.put(pair[0], Integer.parseInt(pair[1])) != null) return Optional.empty();
            }
            if (!bound.keySet().equals(definition.ingredients().keySet())) return Optional.empty();
            for (var entry : bound.entrySet()) {
                Enchantment enchantment = enchantment(entry.getKey());
                if (entry.getValue() < 1
                        || entry.getValue() > enchantment.getMaxLevel()
                        || enchantments(item).getOrDefault(entry.getKey(), 0).intValue() != entry.getValue()) return Optional.empty();
            }
            return Optional.of(new State(definition, UUID.fromString(rawInstance), Map.copyOf(bound)));
        } catch (IllegalArgumentException exception) { return Optional.empty(); }
    }

    public ItemStack fuse(ItemStack input, FusionDefinition definition, UUID identity) {
        ItemStack output = input.clone();
        output.setAmount(1);
        ItemMeta meta = output.getItemMeta();
        var data = meta.getPersistentDataContainer();
        if(!data.has(displayFlags,PersistentDataType.INTEGER))data.set(displayFlags,PersistentDataType.INTEGER,
                (meta.hasItemFlag(ItemFlag.HIDE_ENCHANTS)?1:0)|(meta.hasItemFlag(ItemFlag.HIDE_STORED_ENCHANTS)?2:0));
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS,ItemFlag.HIDE_STORED_ENCHANTS);
        data.set(id, PersistentDataType.STRING, definition.id());
        data.set(schema, PersistentDataType.INTEGER, 1);
        data.set(instance, PersistentDataType.STRING, identity.toString());
        String bound = definition.ingredients().keySet().stream().sorted()
                .map(key -> key + "=" + enchantments(input).getOrDefault(key, 0)).collect(Collectors.joining(";"));
        data.set(sources, PersistentDataType.STRING, bound);
        List<Component> lore = meta.hasLore() ? new ArrayList<>(Objects.requireNonNull(meta.lore()).stream()
                .filter(entry -> !LORE_MARKER.equals(entry.insertion())).toList()) : new ArrayList<>();
        int level=FusionLevels.level(definition,enchantments(input));
        // Invalid native carriers remain unreadable and are rejected by the anvil validator.
        level=Math.max(1,level);
        lore.add(line(definition.name()+" "+FusionLevels.roman(level),NamedTextColor.GOLD));
        if(output.getType()==Material.ENCHANTED_BOOK)lore.add(line("Applies to: "+definition.equipment().label(),NamedTextColor.DARK_GRAY));
        wrap(FusionDescriptions.describe(definition,FusionLevels.profile(definition,tunings.apply(definition.id()),level)),50)
                .forEach(text -> lore.add(line(text, NamedTextColor.GRAY)));
        meta.lore(lore);
        output.setItemMeta(meta);
        return output;
    }

    public ItemStack strip(ItemStack input) {
        ItemStack result = input.clone();
        if (!result.hasItemMeta()) return result;
        ItemMeta meta = result.getItemMeta();
        var data = meta.getPersistentDataContainer();
        if(data.has(displayFlags,PersistentDataType.INTEGER)){
            int flags=data.get(displayFlags,PersistentDataType.INTEGER);
            meta.removeItemFlags(ItemFlag.HIDE_ENCHANTS,ItemFlag.HIDE_STORED_ENCHANTS);
            if((flags&1)!=0)meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            if((flags&2)!=0)meta.addItemFlags(ItemFlag.HIDE_STORED_ENCHANTS);
        }
        for (NamespacedKey key : List.of(id, schema, instance, sources,displayFlags)) data.remove(key);
        if (meta.hasLore()) meta.lore(meta.lore().stream().filter(line -> !LORE_MARKER.equals(line.insertion())).toList());
        result.setItemMeta(meta);
        return result;
    }

    /** Refresh display text after an update without changing the item identity or native data. */
    public ItemStack refreshLore(ItemStack input) {
        State state = read(input).orElseThrow(() -> new IllegalArgumentException("Invalid fusion item"));
        return fuse(input, state.definition(), state.instance());
    }

    public static Map<String, Integer> enchantments(ItemStack item) {
        if (item == null) return Map.of();
        Map<Enchantment, Integer> enchants = item.getType() == Material.ENCHANTED_BOOK
                && item.getItemMeta() instanceof EnchantmentStorageMeta book ? book.getStoredEnchants() : item.getEnchantments();
        return enchants.entrySet().stream().filter(e -> e.getKey().getKey().getNamespace().equals("minecraft"))
                .collect(Collectors.toMap(e -> e.getKey().getKey().getKey(), Map.Entry::getValue));
    }
    public static Enchantment enchantment(String key) {
        return Objects.requireNonNull(Registry.ENCHANTMENT.get(NamespacedKey.minecraft(key)), "Unknown enchantment: " + key);
    }
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
    public static ItemStack source(FusionDefinition definition, int tier) {
        Map<String, Integer> ingredients = FusionLevels.example(definition, tier).orElseThrow();
        ItemStack item = new ItemStack(baseMaterial(definition.equipment()));
        ingredients.forEach((key, level) -> item.addEnchantment(enchantment(key), level));
        return item;
    }
    public static ItemStack book(FusionDefinition definition, int tier) {
        Map<String, Integer> ingredients = FusionLevels.example(definition, tier).orElseThrow();
        ItemStack item = new ItemStack(Material.ENCHANTED_BOOK);
        EnchantmentStorageMeta meta = (EnchantmentStorageMeta) item.getItemMeta();
        ingredients.forEach((key, level) -> meta.addStoredEnchant(enchantment(key), level, false));
        item.setItemMeta(meta);
        return item;
    }
    public static ItemStack ingredientBook(String key, int level) {
        ItemStack item = new ItemStack(Material.ENCHANTED_BOOK);
        EnchantmentStorageMeta meta = (EnchantmentStorageMeta) item.getItemMeta();
        meta.addStoredEnchant(enchantment(key), level, false);
        item.setItemMeta(meta);
        return item;
    }
    private static Component line(String text, NamedTextColor color) {
        return Component.text(text, color).decoration(TextDecoration.ITALIC, false).insertion(LORE_MARKER);
    }
    private static List<String> wrap(String text, int width) {
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : text.split(" ")) {
            if (line.length() + word.length() > width && !line.isEmpty()) { lines.add(line.toString().trim()); line.setLength(0); }
            line.append(word).append(' ');
        }
        if (!line.isEmpty()) lines.add(line.toString().trim());
        return lines;
    }
}
