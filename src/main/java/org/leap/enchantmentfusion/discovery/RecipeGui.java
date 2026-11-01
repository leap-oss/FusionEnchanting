package org.leap.enchantmentfusion.discovery;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.leap.enchantmentfusion.model.*;
import org.leap.enchantmentfusion.service.*;

import java.util.*;

/** Interactive Chest Inventory GUI Wiki for browsing categories, 3-tier anvil recipes, and player milestones. */
public final class RecipeGui implements Listener {
    public enum View { CATALOG, DETAIL, STATS }

    public static final class GuiHolder implements InventoryHolder {
        private final View view;
        private final String filter;
        private final int page;
        private final Map<Integer, Runnable> actions = new HashMap<>();
        private Inventory inventory;

        public GuiHolder(View view, String filter, int page) {
            this.view = view;
            this.filter = filter == null ? "" : filter;
            this.page = page;
        }
        public View view() { return view; }
        public String filter() { return filter; }
        public int page() { return page; }
        @Override public Inventory getInventory() { return inventory; }
    }

    private static final int[] GRID_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };

    private final FusionService service;
    private final FusionItems items;
    private final FusionProgression progression;
    private final RecipeGuide bookGuide;

    public RecipeGui(FusionService service, FusionItems items, FusionProgression progression, RecipeGuide bookGuide) {
        this.service = service;
        this.items = items;
        this.progression = progression;
        this.bookGuide = bookGuide;
    }

    public boolean open(Player player, String rawQuery) {
        if (!player.hasPermission("enchantmentfusion.recipes")) return false;
        String query = rawQuery == null ? "" : rawQuery.trim().toLowerCase(Locale.ROOT);
        if (query.equals("stats") || query.equals("milestones")) {
            openStats(player);
            return true;
        }
        FusionDefinition exact = service.definition(query);
        if (exact != null && service.tuning(exact.id()).enabled()) {
            openDetail(player, exact, "");
            return true;
        }
        List<FusionDefinition> matches = filterRecipes(query);
        if (matches.isEmpty()) return false;
        openCatalog(player, query, 0);
        return true;
    }

    public void openCatalog(Player player, String filter, int page) {
        List<FusionDefinition> matches = filterRecipes(filter);
        int totalPages = Math.max(1, (matches.size() + GRID_SLOTS.length - 1) / GRID_SLOTS.length);
        int safePage = Math.max(0, Math.min(page, totalPages - 1));
        GuiHolder holder = new GuiHolder(View.CATALOG, filter, safePage);
        String titleSuffix = filter.isEmpty() ? "All Recipes" : categoryLabel(filter);
        Inventory inv = Bukkit.createInventory(holder, 54,
                Component.text("Fusion Wiki • " + titleSuffix + (totalPages > 1 ? " (" + (safePage + 1) + "/" + totalPages + ")" : ""), NamedTextColor.DARK_PURPLE));
        holder.inventory = inv;
        fillBorder(inv);

        // Top category filter tabs
        setTab(holder, inv, 0, Material.NETHER_STAR, "All Recipes", "", filter, player);
        setTab(holder, inv, 1, Material.DIAMOND_SWORD, "Swords", "cat:swords", filter, player);
        setTab(holder, inv, 2, Material.BOW, "Bows & Crossbows", "cat:ranged", filter, player);
        setTab(holder, inv, 3, Material.DIAMOND_PICKAXE, "Mining Tools", "cat:mining", filter, player);
        setTab(holder, inv, 4, Material.DIAMOND_CHESTPLATE, "Armor", "cat:armor", filter, player);
        setTab(holder, inv, 5, Material.DIAMOND_BOOTS, "Boots", "cat:boots", filter, player);
        setTab(holder, inv, 6, Material.TRIDENT, "Tridents & Maces", "cat:heavy", filter, player);
        setTab(holder, inv, 7, Material.FISHING_ROD, "Utility", "cat:utility", filter, player);

        inv.setItem(8, icon(Material.DRAGON_BREATH, "✦ Player Stats & Milestones", NamedTextColor.GOLD, List.of(
                line("View your fusion statistics", NamedTextColor.GRAY),
                line("and unlocked milestones.", NamedTextColor.GRAY),
                Component.empty(),
                line("▸ Click to open", NamedTextColor.YELLOW)
        )));
        holder.actions.put(8, () -> openStats(player));

        int start = safePage * GRID_SLOTS.length;
        int end = Math.min(start + GRID_SLOTS.length, matches.size());
        for (int i = start; i < end; i++) {
            FusionDefinition d = matches.get(i);
            int slot = GRID_SLOTS[i - start];
            inv.setItem(slot, recipeCard(d));
            holder.actions.put(slot, () -> openDetail(player, d, filter));
        }

        if (safePage > 0) {
            inv.setItem(45, icon(Material.ARROW, "← Previous Page", NamedTextColor.AQUA, List.of(
                    line("Go to page " + safePage, NamedTextColor.GRAY)
            )));
            holder.actions.put(45, () -> openCatalog(player, filter, safePage - 1));
        }

        inv.setItem(47, icon(Material.WRITABLE_BOOK, "Open Classic Book Guide", NamedTextColor.AQUA, List.of(
                line("Switch to the native written-book", NamedTextColor.GRAY),
                line("version of the Fusion Guide.", NamedTextColor.GRAY),
                Component.empty(),
                line("▸ Click to open book", NamedTextColor.YELLOW)
        )));
        holder.actions.put(47, () -> bookGuide.openBook(player, filter.startsWith("cat:") ? "" : filter));

        inv.setItem(49, icon(Material.ANVIL, "How Fusion Enchanting Works", NamedTextColor.GOLD, List.of(
                line("1. Combine 2 source Enchanted Books", NamedTextColor.GRAY),
                line("   in an Anvil → Fusion Book I–III.", NamedTextColor.GRAY),
                line("2. Apply the Fusion Book to compatible", NamedTextColor.GRAY),
                line("   equipment in an Anvil.", NamedTextColor.GRAY),
                line("3. Smithing Table Netherite upgrades", NamedTextColor.GRAY),
                line("   and Armor Trims preserve Fusions!", NamedTextColor.GREEN),
                line("4. Use /fusion extract with a plain", NamedTextColor.GRAY),
                line("   Book to unbind a Fusion Book.", NamedTextColor.AQUA)
        )));

        boolean notify = progression.notifyEnabled(player);
        inv.setItem(51, icon(Material.BELL, "HUD Notifications: " + (notify ? "ON" : "OFF"),
                notify ? NamedTextColor.GREEN : NamedTextColor.RED, List.of(
                        line("Toggles Action Bar & sound feedback", NamedTextColor.GRAY),
                        line("when abilities trigger or are on cooldown.", NamedTextColor.GRAY),
                        Component.empty(),
                        line("▸ Click to toggle", NamedTextColor.YELLOW)
                )));
        holder.actions.put(51, () -> {
            progression.toggleNotify(player);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);
            openCatalog(player, filter, safePage);
        });

        if (safePage + 1 < totalPages) {
            inv.setItem(53, icon(Material.ARROW, "Next Page →", NamedTextColor.AQUA, List.of(
                    line("Go to page " + (safePage + 2), NamedTextColor.GRAY)
            )));
            holder.actions.put(53, () -> openCatalog(player, filter, safePage + 1));
        }

        player.closeInventory();
        player.openInventory(inv);
    }

    public void openDetail(Player player, FusionDefinition d, String returnFilter) {
        GuiHolder holder = new GuiHolder(View.DETAIL, d.id(), 0);
        Inventory inv = Bukkit.createInventory(holder, 54,
                Component.text("Fusion • " + d.name(), NamedTextColor.DARK_PURPLE));
        holder.inventory = inv;
        fillBorder(inv);

        FusionTuning tuning = service.tuning(d.id());
        inv.setItem(4, recipeCard(d));

        var sortedKeys = d.ingredients().keySet().stream().sorted(Comparator.reverseOrder()).toList();
        for (int tier = 1; tier <= 3; tier++) {
            int rowBase = tier * 9;
            Optional<Map<String, Integer>> sample = FusionLevels.example(d, tier);
            if (sample.isEmpty()) {
                inv.setItem(rowBase + 4, icon(Material.BARRIER, "Tier " + FusionLevels.roman(tier) + " (Not Reachable)", NamedTextColor.RED, List.of(
                        line("Source enchantments for this recipe", NamedTextColor.GRAY),
                        line("only have 1 vanilla max level,", NamedTextColor.GRAY),
                        line("so it jumps directly to Tier III.", NamedTextColor.GRAY)
                )));
                continue;
            }
            Map<String, Integer> sources = sample.get();
            String firstKey = sortedKeys.get(0);
            String secondKey = sortedKeys.get(1);
            int firstLvl = sources.get(firstKey);
            int secondLvl = sources.get(secondKey);

            inv.setItem(rowBase + 1, sourceBookIcon(firstKey, firstLvl, tier));
            inv.setItem(rowBase + 2, icon(Material.ANVIL, "+ Combine in Anvil", NamedTextColor.YELLOW, List.of(
                    line(RecipeText.ingredient(firstKey, firstLvl) + " + " + RecipeText.ingredient(secondKey, secondLvl), NamedTextColor.WHITE),
                    line("Creation Fee: Vanilla + " + tuning.extraLevels() + " XP levels", NamedTextColor.AQUA)
            )));
            inv.setItem(rowBase + 3, sourceBookIcon(secondKey, secondLvl, tier));

            FusionLevels.Profile profile = FusionLevels.profile(d, tuning, tier);
            inv.setItem(rowBase + 4, icon(Material.EXPERIENCE_BOTTLE, "→ Tier " + FusionLevels.roman(tier) + " Profile", NamedTextColor.GOLD, List.of(
                    line("Effect: " + FusionDescriptions.describe(d, profile), NamedTextColor.WHITE),
                    line("Power Multiplier: ×" + FusionDescriptions.number(profile.power()), NamedTextColor.GREEN),
                    line("Cooldown: " + FusionDescriptions.number(profile.cooldown()) + "s", NamedTextColor.AQUA),
                    line("Proc Chance: " + FusionDescriptions.number(profile.chance() * 100) + "%", NamedTextColor.YELLOW),
                    line("Limits: " + d.limitation(), NamedTextColor.DARK_GRAY)
            )));

            ItemStack previewBook = items.fuse(FusionItems.book(d, tier), d, UUID.randomUUID());
            inv.setItem(rowBase + 5, previewBook);

            ItemStack previewGear = items.fuse(FusionItems.source(d, tier), d, UUID.randomUUID());
            inv.setItem(rowBase + 7, previewGear);
        }

        inv.setItem(45, icon(Material.ARROW, "← Back to Catalog", NamedTextColor.AQUA, List.of(
                line("Return to recipe list", NamedTextColor.GRAY)
        )));
        holder.actions.put(45, () -> openCatalog(player, returnFilter, 0));

        inv.setItem(49, icon(Material.WRITABLE_BOOK, "Open in Written Book", NamedTextColor.AQUA, List.of(
                line("View " + d.name() + " in the classic book", NamedTextColor.GRAY)
        )));
        holder.actions.put(49, () -> bookGuide.openBook(player, d.id()));

        player.closeInventory();
        player.openInventory(inv);
    }

    public void openStats(Player player) {
        GuiHolder holder = new GuiHolder(View.STATS, "stats", 0);
        Inventory inv = Bukkit.createInventory(holder, 54,
                Component.text("Fusion Wiki • Stats & Milestones", NamedTextColor.DARK_PURPLE));
        holder.inventory = inv;
        fillBorder(inv);

        int highest = progression.highestTier(player);
        inv.setItem(4, icon(Material.NETHER_STAR, "✦ " + player.getName() + "'s Fusion Profile", NamedTextColor.GOLD, List.of(
                line("Fusion Books Created: " + progression.created(player), NamedTextColor.YELLOW),
                line("Fusions Applied to Gear: " + progression.applied(player), NamedTextColor.GREEN),
                line("Fusions Extracted: " + progression.extracted(player), NamedTextColor.AQUA),
                line("Smithing Table Upgrades: " + progression.smithingUpgrades(player), NamedTextColor.LIGHT_PURPLE),
                line("Abilities Triggered: " + progression.abilitiesTriggered(player), NamedTextColor.GOLD),
                line("Highest Tier Reached: " + (highest > 0 ? "Tier " + FusionLevels.roman(highest) : "None yet"), NamedTextColor.WHITE)
        )));

        Set<FusionProgression.Milestone> unlocked = progression.unlocked(player);
        FusionProgression.Milestone[] milestones = FusionProgression.Milestone.values();
        int[] slots = {20, 21, 22, 23, 24, 31};
        for (int i = 0; i < milestones.length && i < slots.length; i++) {
            FusionProgression.Milestone m = milestones[i];
            boolean done = unlocked.contains(m);
            inv.setItem(slots[i], icon(done ? Material.ENCHANTED_BOOK : Material.GRAY_DYE,
                    (done ? "✔ " : "🔒 ") + m.title(),
                    done ? NamedTextColor.GREEN : NamedTextColor.GRAY,
                    List.of(
                            line(m.description(), NamedTextColor.WHITE),
                            Component.empty(),
                            line(done ? "Status: UNLOCKED" : "Status: LOCKED", done ? NamedTextColor.GOLD : NamedTextColor.DARK_GRAY)
                    )));
        }

        inv.setItem(45, icon(Material.ARROW, "← Back to Catalog", NamedTextColor.AQUA, List.of(
                line("Return to recipe catalog", NamedTextColor.GRAY)
        )));
        holder.actions.put(45, () -> openCatalog(player, "", 0));

        player.closeInventory();
        player.openInventory(inv);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void click(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof GuiHolder holder)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getClickedInventory() != holder.inventory) return;
        Runnable action = holder.actions.get(event.getRawSlot());
        if (action != null) action.run();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void drag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof GuiHolder) {
            event.setCancelled(true);
        }
    }

    private List<FusionDefinition> filterRecipes(String query) {
        String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        return service.definitions().stream()
                .filter(d -> service.tuning(d.id()).enabled())
                .filter(d -> {
                    if (q.isEmpty()) return true;
                    if (q.startsWith("cat:")) return matchesCategory(d.equipment(), q);
                    return d.id().equals(q) || d.ingredients().containsKey(q)
                            || d.equipment().name().toLowerCase(Locale.ROOT).equals(q);
                })
                .toList();
    }

    private static boolean matchesCategory(Equipment eq, String cat) {
        return switch (cat) {
            case "cat:swords" -> eq == Equipment.SWORD;
            case "cat:ranged" -> eq == Equipment.BOW || eq == Equipment.CROSSBOW;
            case "cat:mining" -> eq == Equipment.TOOL || eq == Equipment.PICKAXE;
            case "cat:armor" -> eq == Equipment.ARMOR;
            case "cat:boots" -> eq == Equipment.BOOTS;
            case "cat:heavy" -> eq == Equipment.TRIDENT || eq == Equipment.MACE;
            case "cat:utility" -> eq == Equipment.HELMET || eq == Equipment.FISHING_ROD;
            default -> true;
        };
    }

    private static String categoryLabel(String filter) {
        return switch (filter) {
            case "cat:swords" -> "Swords";
            case "cat:ranged" -> "Bows & Crossbows";
            case "cat:mining" -> "Mining Tools";
            case "cat:armor" -> "Armor";
            case "cat:boots" -> "Boots";
            case "cat:heavy" -> "Tridents & Maces";
            case "cat:utility" -> "Utility";
            default -> filter.toUpperCase(Locale.ROOT);
        };
    }

    private void setTab(GuiHolder holder, Inventory inv, int slot, Material mat, String name, String targetFilter, String activeFilter, Player player) {
        boolean active = Objects.equals(targetFilter, activeFilter);
        List<Component> lore = new ArrayList<>();
        lore.add(line("Filter recipes by " + name, NamedTextColor.GRAY));
        lore.add(Component.empty());
        lore.add(line(active ? "● Currently Selected" : "▸ Click to filter", active ? NamedTextColor.GREEN : NamedTextColor.YELLOW));
        ItemStack tab = icon(mat, (active ? "✦ " : "") + name, active ? NamedTextColor.GOLD : NamedTextColor.AQUA, lore);
        if (active) {
            ItemMeta meta = tab.getItemMeta();
            meta.setEnchantmentGlintOverride(true);
            tab.setItemMeta(meta);
        }
        inv.setItem(slot, tab);
        holder.actions.put(slot, () -> {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);
            openCatalog(player, targetFilter, 0);
        });
    }

    private ItemStack recipeCard(FusionDefinition d) {
        Material mat = FusionItems.baseMaterial(d.equipment());
        FusionTuning tuning = service.tuning(d.id());
        List<Component> lore = new ArrayList<>();
        lore.add(line("Applies to: " + d.equipment().label(), NamedTextColor.DARK_GRAY));
        lore.add(Component.empty());
        lore.add(line("Full-Tier Source Books:", NamedTextColor.GOLD));
        d.ingredients().entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.reverseOrder()))
                .forEach(e -> lore.add(line(" • " + RecipeText.ingredient(e.getKey(), e.getValue()), NamedTextColor.WHITE)));
        lore.add(line("Anvil Fee: Vanilla + " + tuning.extraLevels() + " XP levels", NamedTextColor.AQUA));
        lore.add(Component.empty());
        for (int tier = 1; tier <= 3; tier++) {
            if (FusionLevels.example(d, tier).isPresent()) {
                FusionLevels.Profile profile = FusionLevels.profile(d, tuning, tier);
                lore.add(line("Tier " + FusionLevels.roman(tier) + ": " + FusionDescriptions.describe(d, profile),
                        tier == 3 ? NamedTextColor.YELLOW : NamedTextColor.GRAY));
            }
        }
        lore.add(Component.empty());
        lore.add(line("Limits: " + d.limitation(), NamedTextColor.DARK_GRAY));
        lore.add(line("▸ Click for Tier I / II / III recipes", NamedTextColor.GREEN));

        ItemStack stack = icon(mat, "✦ " + d.name() + " I–III", NamedTextColor.GOLD, lore);
        ItemMeta meta = stack.getItemMeta();
        meta.setEnchantmentGlintOverride(true);
        stack.setItemMeta(meta);
        return stack;
    }

    private static ItemStack sourceBookIcon(String key, int level, int tier) {
        ItemStack book = FusionItems.ingredientBook(key, level);
        ItemMeta meta = book.getItemMeta();
        meta.displayName(Component.text("Source Book: " + RecipeText.ingredient(key, level), NamedTextColor.AQUA)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
                line("Required for Tier " + FusionLevels.roman(tier), NamedTextColor.GRAY)
        ));
        book.setItemMeta(meta);
        return book;
    }

    private static void fillBorder(Inventory inv) {
        ItemStack pane = icon(Material.BLACK_STAINED_GLASS_PANE, " ", NamedTextColor.DARK_GRAY, List.of());
        for (int i = 0; i < inv.getSize(); i++) {
            if (i < 9 || i >= 45 || i % 9 == 0 || i % 9 == 8) {
                inv.setItem(i, pane);
            }
        }
    }

    private static ItemStack icon(Material material, String title, NamedTextColor color, List<Component> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(title, color).decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_STORED_ENCHANTS, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        item.setItemMeta(meta);
        return item;
    }

    private static Component line(String text, NamedTextColor color) {
        return Component.text(text, color).decoration(TextDecoration.ITALIC, false);
    }
}
