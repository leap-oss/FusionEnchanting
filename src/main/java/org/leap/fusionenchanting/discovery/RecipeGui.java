package org.leap.fusionenchanting.discovery;

import net.minecraft.ChatFormatting;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.leap.fusionenchanting.FusionRuntime;
import org.leap.fusionenchanting.core.*;
import org.leap.fusionenchanting.item.FusionItems;

import java.util.*;

/** Server-side 54-slot Chest Menu GUI Wiki for Fabric (no client mod required). */
public final class RecipeGui {
    public enum View { CATALOG, DETAIL, STATS }

    public static final class FusionGuiMenu extends ChestMenu {
        private final Map<Integer, Runnable> actions;

        public FusionGuiMenu(int syncId, Inventory playerInv, SimpleContainer container, Map<Integer, Runnable> actions) {
            super(MenuType.GENERIC_9x6, syncId, playerInv, container, 6);
            this.actions = actions;
        }

        @Override
        public void clicked(int slotId, int button, ClickType clickType, Player player) {
            if (slotId >= 0 && slotId < 54) {
                Runnable action = actions.get(slotId);
                if (action != null) action.run();
            }
            broadcastChanges();
        }

        @Override
        public ItemStack quickMoveStack(Player player, int index) {
            return ItemStack.EMPTY;
        }

        @Override
        public boolean stillValid(Player player) {
            return true;
        }
    }

    private static final int[] GRID_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };

    private final FusionRuntime runtime;
    private final RecipeGuide bookGuide;

    public RecipeGui(FusionRuntime runtime, RecipeGuide bookGuide) {
        this.runtime = runtime;
        this.bookGuide = bookGuide;
    }

    public boolean open(ServerPlayer player, String rawQuery) {
        if (!runtime.recipes(player)) return false;
        String query = rawQuery == null ? "" : rawQuery.trim().toLowerCase(Locale.ROOT);
        if (query.equals("stats") || query.equals("milestones")) {
            openStats(player);
            return true;
        }
        FusionDefinition exact = runtime.items.definition(query);
        if (exact != null && runtime.tuning(exact.id()).enabled()) {
            openDetail(player, exact, "");
            return true;
        }
        List<FusionDefinition> matches = filterRecipes(query);
        if (matches.isEmpty()) return false;
        openCatalog(player, query, 0);
        return true;
    }

    public void openCatalog(ServerPlayer player, String filter, int page) {
        List<FusionDefinition> matches = filterRecipes(filter);
        int totalPages = Math.max(1, (matches.size() + GRID_SLOTS.length - 1) / GRID_SLOTS.length);
        int safePage = Math.max(0, Math.min(page, totalPages - 1));
        SimpleContainer container = new SimpleContainer(54);
        Map<Integer, Runnable> actions = new HashMap<>();
        fillBorder(container);

        setTab(container, actions, 0, Items.NETHER_STAR, "All Recipes", "", filter, player);
        setTab(container, actions, 1, Items.DIAMOND_SWORD, "Swords", "cat:swords", filter, player);
        setTab(container, actions, 2, Items.BOW, "Bows & Crossbows", "cat:ranged", filter, player);
        setTab(container, actions, 3, Items.DIAMOND_PICKAXE, "Mining Tools", "cat:mining", filter, player);
        setTab(container, actions, 4, Items.DIAMOND_CHESTPLATE, "Armor", "cat:armor", filter, player);
        setTab(container, actions, 5, Items.DIAMOND_BOOTS, "Boots", "cat:boots", filter, player);
        setTab(container, actions, 6, Items.TRIDENT, "Tridents & Maces", "cat:heavy", filter, player);
        setTab(container, actions, 7, Items.FISHING_ROD, "Utility", "cat:utility", filter, player);

        container.setItem(8, icon(Items.DRAGON_BREATH, "✦ Player Stats & Milestones", ChatFormatting.GOLD, List.of(
                line("View your fusion statistics", ChatFormatting.GRAY),
                line("and unlocked milestones.", ChatFormatting.GRAY),
                Component.empty(),
                line("▸ Click to open", ChatFormatting.YELLOW)
        )));
        actions.put(8, () -> openStats(player));

        int start = safePage * GRID_SLOTS.length;
        int end = Math.min(start + GRID_SLOTS.length, matches.size());
        for (int i = start; i < end; i++) {
            FusionDefinition d = matches.get(i);
            int slot = GRID_SLOTS[i - start];
            container.setItem(slot, recipeCard(d));
            actions.put(slot, () -> openDetail(player, d, filter));
        }

        if (safePage > 0) {
            container.setItem(45, icon(Items.ARROW, "← Previous Page", ChatFormatting.AQUA, List.of(
                    line("Go to page " + safePage, ChatFormatting.GRAY)
            )));
            actions.put(45, () -> openCatalog(player, filter, safePage - 1));
        }

        container.setItem(47, icon(Items.WRITABLE_BOOK, "Open Classic Book Guide", ChatFormatting.AQUA, List.of(
                line("Switch to the native written-book", ChatFormatting.GRAY),
                line("version of the Fusion Guide.", ChatFormatting.GRAY),
                Component.empty(),
                line("▸ Click to open book", ChatFormatting.YELLOW)
        )));
        actions.put(47, () -> bookGuide.openBook(player, filter.startsWith("cat:") ? "" : filter));

        container.setItem(49, icon(Items.ANVIL, "How Fusion Enchanting Works", ChatFormatting.GOLD, List.of(
                line("1. Combine 2 source Enchanted Books", ChatFormatting.GRAY),
                line("   in an Anvil → Fusion Book I–III.", ChatFormatting.GRAY),
                line("2. Apply the Fusion Book to compatible", ChatFormatting.GRAY),
                line("   equipment in an Anvil.", ChatFormatting.GRAY),
                line("3. Smithing Table Netherite upgrades", ChatFormatting.GRAY),
                line("   and Armor Trims preserve Fusions!", ChatFormatting.GREEN),
                line("4. Use /fusion extract with a plain", ChatFormatting.GRAY),
                line("   Book to unbind a Fusion Book.", ChatFormatting.AQUA)
        )));

        boolean notify = runtime.progression.notifyEnabled(player);
        container.setItem(51, icon(Items.BELL, "HUD Notifications: " + (notify ? "ON" : "OFF"),
                notify ? ChatFormatting.GREEN : ChatFormatting.RED, List.of(
                        line("Toggles Action Bar & sound feedback", ChatFormatting.GRAY),
                        line("when abilities trigger or are on cooldown.", ChatFormatting.GRAY),
                        Component.empty(),
                        line("▸ Click to toggle", ChatFormatting.YELLOW)
                )));
        actions.put(51, () -> {
            runtime.progression.toggleNotify(player);
            player.level().playSound(null, player.blockPosition(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.5f, 1.2f);
            openCatalog(player, filter, safePage);
        });

        if (safePage + 1 < totalPages) {
            container.setItem(53, icon(Items.ARROW, "Next Page →", ChatFormatting.AQUA, List.of(
                    line("Go to page " + (safePage + 2), ChatFormatting.GRAY)
            )));
            actions.put(53, () -> openCatalog(player, filter, safePage + 1));
        }

        String titleSuffix = filter.isEmpty() ? "All Recipes" : categoryLabel(filter);
        Component title = Component.literal("Fusion Wiki • " + titleSuffix + (totalPages > 1 ? " (" + (safePage + 1) + "/" + totalPages + ")" : ""))
                .withStyle(ChatFormatting.DARK_PURPLE);
        player.openMenu(new SimpleMenuProvider((syncId, inv, p) -> new FusionGuiMenu(syncId, inv, container, actions), title));
    }

    public void openDetail(ServerPlayer player, FusionDefinition d, String returnFilter) {
        SimpleContainer container = new SimpleContainer(54);
        Map<Integer, Runnable> actions = new HashMap<>();
        fillBorder(container);

        FusionTuning tuning = runtime.tuning(d.id());
        container.setItem(4, recipeCard(d));

        HolderLookup.Provider registries = player.server.registryAccess();
        var sortedKeys = d.ingredients().keySet().stream().sorted(Comparator.reverseOrder()).toList();
        for (int tier = 1; tier <= 3; tier++) {
            int rowBase = tier * 9;
            Optional<Map<String, Integer>> sample = FusionLevels.example(d, tier);
            if (sample.isEmpty()) {
                container.setItem(rowBase + 4, icon(Items.BARRIER, "Tier " + FusionLevels.roman(tier) + " (Not Reachable)", ChatFormatting.RED, List.of(
                        line("Source enchantments for this recipe", ChatFormatting.GRAY),
                        line("only have 1 vanilla max level,", ChatFormatting.GRAY),
                        line("so it jumps directly to Tier III.", ChatFormatting.GRAY)
                )));
                continue;
            }
            Map<String, Integer> sources = sample.get();
            String firstKey = sortedKeys.get(0);
            String secondKey = sortedKeys.get(1);
            int firstLvl = sources.get(firstKey);
            int secondLvl = sources.get(secondKey);

            container.setItem(rowBase + 1, sourceBookIcon(registries, firstKey, firstLvl, tier));
            container.setItem(rowBase + 2, icon(Items.ANVIL, "+ Combine in Anvil", ChatFormatting.YELLOW, List.of(
                    line(RecipeText.ingredient(firstKey, firstLvl) + " + " + RecipeText.ingredient(secondKey, secondLvl), ChatFormatting.WHITE),
                    line("Creation Fee: Vanilla + " + tuning.extraLevels() + " XP levels", ChatFormatting.AQUA)
            )));
            container.setItem(rowBase + 3, sourceBookIcon(registries, secondKey, secondLvl, tier));

            FusionLevels.Profile profile = FusionLevels.profile(d, tuning, tier);
            container.setItem(rowBase + 4, icon(Items.EXPERIENCE_BOTTLE, "→ Tier " + FusionLevels.roman(tier) + " Profile", ChatFormatting.GOLD, List.of(
                    line("Effect: " + FusionDescriptions.describe(d, profile), ChatFormatting.WHITE),
                    line("Power Multiplier: ×" + FusionDescriptions.number(profile.power()), ChatFormatting.GREEN),
                    line("Cooldown: " + FusionDescriptions.number(profile.cooldown()) + "s", ChatFormatting.AQUA),
                    line("Proc Chance: " + FusionDescriptions.number(profile.chance() * 100) + "%", ChatFormatting.YELLOW),
                    line("Limits: " + d.limitation(), ChatFormatting.DARK_GRAY)
            )));

            ItemStack previewBook = runtime.items.fuse(FusionItems.sourceBook(registries, d, tier), d, UUID.randomUUID());
            container.setItem(rowBase + 5, previewBook);

            ItemStack previewGear = runtime.items.fuse(sourceGear(registries, d, sources), d, UUID.randomUUID());
            container.setItem(rowBase + 7, previewGear);
        }

        container.setItem(45, icon(Items.ARROW, "← Back to Catalog", ChatFormatting.AQUA, List.of(
                line("Return to recipe list", ChatFormatting.GRAY)
        )));
        actions.put(45, () -> openCatalog(player, returnFilter, 0));

        container.setItem(49, icon(Items.WRITABLE_BOOK, "Open in Written Book", ChatFormatting.AQUA, List.of(
                line("View " + d.name() + " in the classic book", ChatFormatting.GRAY)
        )));
        actions.put(49, () -> bookGuide.openBook(player, d.id()));

        Component title = Component.literal("Fusion • " + d.name()).withStyle(ChatFormatting.DARK_PURPLE);
        player.openMenu(new SimpleMenuProvider((syncId, inv, p) -> new FusionGuiMenu(syncId, inv, container, actions), title));
    }

    public void openStats(ServerPlayer player) {
        SimpleContainer container = new SimpleContainer(54);
        Map<Integer, Runnable> actions = new HashMap<>();
        fillBorder(container);

        FusionProgression prog = runtime.progression;
        int highest = prog.highestTier(player);
        container.setItem(4, icon(Items.NETHER_STAR, "✦ " + player.getGameProfile().getName() + "'s Fusion Profile", ChatFormatting.GOLD, List.of(
                line("Fusion Books Created: " + prog.created(player), ChatFormatting.YELLOW),
                line("Fusions Applied to Gear: " + prog.applied(player), ChatFormatting.GREEN),
                line("Fusions Extracted: " + prog.extracted(player), ChatFormatting.AQUA),
                line("Smithing Table Upgrades: " + prog.smithingUpgrades(player), ChatFormatting.LIGHT_PURPLE),
                line("Abilities Triggered: " + prog.abilitiesTriggered(player), ChatFormatting.GOLD),
                line("Highest Tier Reached: " + (highest > 0 ? "Tier " + FusionLevels.roman(highest) : "None yet"), ChatFormatting.WHITE)
        )));

        Set<FusionProgression.Milestone> unlocked = prog.unlocked(player);
        FusionProgression.Milestone[] milestones = FusionProgression.Milestone.values();
        int[] slots = {20, 21, 22, 23, 24, 31};
        for (int i = 0; i < milestones.length && i < slots.length; i++) {
            FusionProgression.Milestone m = milestones[i];
            boolean done = unlocked.contains(m);
            container.setItem(slots[i], icon(done ? Items.ENCHANTED_BOOK : Items.GRAY_DYE,
                    (done ? "✔ " : "🔒 ") + m.title(),
                    done ? ChatFormatting.GREEN : ChatFormatting.GRAY,
                    List.of(
                            line(m.description(), ChatFormatting.WHITE),
                            Component.empty(),
                            line(done ? "Status: UNLOCKED" : "Status: LOCKED", done ? ChatFormatting.GOLD : ChatFormatting.DARK_GRAY)
                    )));
        }

        container.setItem(45, icon(Items.ARROW, "← Back to Catalog", ChatFormatting.AQUA, List.of(
                line("Return to recipe catalog", ChatFormatting.GRAY)
        )));
        actions.put(45, () -> openCatalog(player, "", 0));

        Component title = Component.literal("Fusion Wiki • Stats & Milestones").withStyle(ChatFormatting.DARK_PURPLE);
        player.openMenu(new SimpleMenuProvider((syncId, inv, p) -> new FusionGuiMenu(syncId, inv, container, actions), title));
    }

    private List<FusionDefinition> filterRecipes(String query) {
        String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        return FusionCatalog.definitions().stream()
                .filter(d -> runtime.tuning(d.id()).enabled())
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

    private void setTab(SimpleContainer container, Map<Integer, Runnable> actions, int slot, Item item, String name, String targetFilter, String activeFilter, ServerPlayer player) {
        boolean active = Objects.equals(targetFilter, activeFilter);
        List<Component> lore = new ArrayList<>();
        lore.add(line("Filter recipes by " + name, ChatFormatting.GRAY));
        lore.add(Component.empty());
        lore.add(line(active ? "● Currently Selected" : "▸ Click to filter", active ? ChatFormatting.GREEN : ChatFormatting.YELLOW));
        ItemStack tab = icon(item, (active ? "✦ " : "") + name, active ? ChatFormatting.GOLD : ChatFormatting.AQUA, lore);
        if (active) tab.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        container.setItem(slot, tab);
        actions.put(slot, () -> {
            player.level().playSound(null, player.blockPosition(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.5f, 1.2f);
            openCatalog(player, targetFilter, 0);
        });
    }

    private ItemStack recipeCard(FusionDefinition d) {
        Item mat = baseItem(d.equipment());
        FusionTuning tuning = runtime.tuning(d.id());
        List<Component> lore = new ArrayList<>();
        lore.add(line("Applies to: " + d.equipment().label(), ChatFormatting.DARK_GRAY));
        lore.add(Component.empty());
        lore.add(line("Full-Tier Source Books:", ChatFormatting.GOLD));
        d.ingredients().entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.reverseOrder()))
                .forEach(e -> lore.add(line(" • " + RecipeText.ingredient(e.getKey(), e.getValue()), ChatFormatting.WHITE)));
        lore.add(line("Anvil Fee: Vanilla + " + tuning.extraLevels() + " XP levels", ChatFormatting.AQUA));
        lore.add(Component.empty());
        for (int tier = 1; tier <= 3; tier++) {
            if (FusionLevels.example(d, tier).isPresent()) {
                FusionLevels.Profile profile = FusionLevels.profile(d, tuning, tier);
                lore.add(line("Tier " + FusionLevels.roman(tier) + ": " + FusionDescriptions.describe(d, profile),
                        tier == 3 ? ChatFormatting.YELLOW : ChatFormatting.GRAY));
            }
        }
        lore.add(Component.empty());
        lore.add(line("Limits: " + d.limitation(), ChatFormatting.DARK_GRAY));
        lore.add(line("▸ Click for Tier I / II / III recipes", ChatFormatting.GREEN));

        ItemStack stack = icon(mat, "✦ " + d.name() + " I–III", ChatFormatting.GOLD, lore);
        stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        return stack;
    }

    private static Item baseItem(Equipment category) {
        return switch (category) {
            case SWORD -> Items.DIAMOND_SWORD;
            case BOW -> Items.BOW;
            case CROSSBOW -> Items.CROSSBOW;
            case PICKAXE, TOOL -> Items.DIAMOND_PICKAXE;
            case ARMOR -> Items.DIAMOND_CHESTPLATE;
            case BOOTS -> Items.DIAMOND_BOOTS;
            case TRIDENT -> Items.TRIDENT;
            case MACE -> Items.MACE;
            case FISHING_ROD -> Items.FISHING_ROD;
            case HELMET -> Items.DIAMOND_HELMET;
        };
    }

    private static ItemStack sourceGear(HolderLookup.Provider registries, FusionDefinition d, Map<String, Integer> sources) {
        ItemStack gear = new ItemStack(baseItem(d.equipment()));
        var enchants = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        sources.forEach((key, level) -> enchants.set(registries.lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(ResourceKey.create(Registries.ENCHANTMENT, ResourceLocation.withDefaultNamespace(key))), level));
        gear.set(DataComponents.ENCHANTMENTS, enchants.toImmutable());
        return gear;
    }

    private static ItemStack sourceBookIcon(HolderLookup.Provider registries, String key, int level, int tier) {
        ItemStack book = FusionItems.ingredientBook(registries, key, level);
        book.set(DataComponents.CUSTOM_NAME, line("Source Book: " + RecipeText.ingredient(key, level), ChatFormatting.AQUA));
        book.set(DataComponents.LORE, new ItemLore(List.of(
                line("Required for Tier " + FusionLevels.roman(tier), ChatFormatting.GRAY)
        )));
        return book;
    }

    private static void fillBorder(SimpleContainer container) {
        ItemStack pane = icon(Items.BLACK_STAINED_GLASS_PANE, " ", ChatFormatting.DARK_GRAY, List.of());
        for (int i = 0; i < container.getContainerSize(); i++) {
            if (i < 9 || i >= 45 || i % 9 == 0 || i % 9 == 8) {
                container.setItem(i, pane);
            }
        }
    }

    private static ItemStack icon(Item item, String title, ChatFormatting color, List<Component> lore) {
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponents.CUSTOM_NAME, line(title, color));
        stack.set(DataComponents.LORE, new ItemLore(lore));
        return stack;
    }

    private static Component line(String text, ChatFormatting color) {
        return Component.literal(text).withStyle(s -> s.withColor(color).withItalic(false));
    }
}
