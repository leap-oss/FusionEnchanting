package org.leap.enchantmentfusion.command;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.leap.enchantmentfusion.EnchantmentFusion;
import org.leap.enchantmentfusion.discovery.RecipeGuide;
import org.leap.enchantmentfusion.model.FusionDefinition;
import org.leap.enchantmentfusion.model.*;
import org.leap.enchantmentfusion.service.*;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;

import java.util.*;

public final class FusionCommand implements TabExecutor {
    private final EnchantmentFusion plugin;
    private final FusionService service;
    private final FusionItems items;
    private final RecipeGuide guide;
    private final FusionProgression progression;
    public FusionCommand(EnchantmentFusion plugin, FusionService service, FusionItems items, RecipeGuide guide) {
        this(plugin, service, items, guide, new FusionProgression());
    }
    public FusionCommand(EnchantmentFusion plugin, FusionService service, FusionItems items, RecipeGuide guide, FusionProgression progression) {
        this.plugin = plugin; this.service = service; this.items = items; this.guide = guide; this.progression = progression;
    }
    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String action = args.length == 0 ? "help" : args[0].toLowerCase(Locale.ROOT);
        if (Set.of("guide", "gui", "wiki", "book", "recipes", "info", "inspect", "stats").contains(action) && !sender.hasPermission("enchantmentfusion.recipes")) {
            sender.sendMessage("§cYou do not have permission to view recipes."); return true;
        }
        switch (action) {
            case "help" -> {
                sender.sendMessage("§6FusionEnchanting §7— 36 fusion recipes; one fusion per item.");
                sender.sendMessage(Component.text("[Open Fusion GUI Wiki]", NamedTextColor.AQUA).clickEvent(ClickEvent.runCommand("/fusion guide"))
                        .append(Component.text("  ", NamedTextColor.GRAY))
                        .append(Component.text("[Open Book Guide]", NamedTextColor.YELLOW).clickEvent(ClickEvent.runCommand("/fusion book"))));
                sender.sendMessage("§e/fusion guide [id/enchant] §7— interactive Chest GUI Wiki");
                sender.sendMessage("§e/fusion book [id/enchant] §7— classic written-book guide");
                sender.sendMessage("§e/fusion recipes [page] §7— recipe catalog in chat");
                sender.sendMessage("§e/fusion info <id> §7— ingredients, effects, limits and costs");
                sender.sendMessage("§e/fusion inspect §7— inspect the item in your main hand");
                sender.sendMessage("§e/fusion extract §7— unbind fusion into a Book (needs 1 Book + 3 XP levels)");
                sender.sendMessage("§e/fusion notify §7— toggle Action Bar cooldown & proc notifications");
                sender.sendMessage("§e/fusion stats §7— view your fusion statistics and milestones");
                if (sender.hasPermission("enchantmentfusion.admin")) sender.sendMessage("§eAdmin: /fusion give <player> <id> [level], /fusion reload");
            }
            case "guide", "gui", "wiki" -> {
                if (!(sender instanceof Player player)) { sender.sendMessage("Use this in game; the console can use /fusion recipes."); break; }
                if (args.length == 2 && args[1].equalsIgnoreCase("book")) {
                    if (!guide.openBook(player, "")) sender.sendMessage("§eNo enabled recipe found.");
                    break;
                }
                if (args.length > 2 || !guide.open(player, args.length == 2 ? args[1] : ""))
                    sender.sendMessage("§eNo enabled recipe found. Use /fusion guide [id/enchant], e.g. sharpness.");
            }
            case "book" -> {
                if (!(sender instanceof Player player)) { sender.sendMessage("Use this in game; the console can use /fusion recipes."); break; }
                if (args.length > 2 || !guide.openBook(player, args.length == 2 ? args[1] : ""))
                    sender.sendMessage("§eNo enabled recipe found. Use /fusion book [id/enchant].");
            }
            case "extract", "unbind" -> {
                if (!(sender instanceof Player player)) { sender.sendMessage("Use this in game."); break; }
                extract(player);
            }
            case "notify" -> {
                if (!(sender instanceof Player player)) { sender.sendMessage("Use this in game."); break; }
                boolean enabled = progression.toggleNotify(player);
                player.sendMessage("§6Fusion HUD Notifications: " + (enabled ? "§aENABLED" : "§cDISABLED"));
            }
            case "stats", "milestones" -> {
                if (!(sender instanceof Player player)) { sender.sendMessage("Use this in game."); break; }
                int highest = progression.highestTier(player);
                player.sendMessage("§6✦ Fusion Profile — " + player.getName());
                player.sendMessage("§7Books Created: §e" + progression.created(player) + " §8| §7Applied: §a" + progression.applied(player)
                        + " §8| §7Extracted: §b" + progression.extracted(player));
                player.sendMessage("§7Smithing Upgrades: §d" + progression.smithingUpgrades(player) + " §8| §7Abilities Triggered: §6"
                        + progression.abilitiesTriggered(player) + " §8| §7Highest Tier: §f" + (highest > 0 ? FusionLevels.roman(highest) : "-"));
                player.sendMessage("§7Milestones Unlocked: §e" + progression.unlocked(player).size() + "/" + FusionProgression.Milestone.values().length);
                player.sendMessage(Component.text("[Open Milestones GUI]", NamedTextColor.AQUA).clickEvent(ClickEvent.runCommand("/fusion guide stats")));
            }
            case "recipes" -> recipes(sender, args);
            case "info" -> {
                FusionDefinition definition = args.length == 2 ? service.definition(args[1].toLowerCase(Locale.ROOT)) : null;
                if (definition == null) sender.sendMessage("§eUse /fusion info <id>. See /fusion recipes.");
                else info(sender, definition);
            }
            case "inspect" -> {
                if (!(sender instanceof Player player)) { sender.sendMessage("Use this in game."); break; }
                ItemStack item = player.getInventory().getItemInMainHand();
                var state = items.read(item);
                if (state.isPresent()) {
                    player.getInventory().setItemInMainHand(items.refreshLore(item));
                    info(sender, state.get().definition()); sender.sendMessage("§8Item ID: " + state.get().instance());
                    if (!service.allowed(player)) sender.sendMessage("§eThe ability is disabled in this world or you do not have use permission.");
                } else sender.sendMessage(items.marked(item) ? "§cInvalid fusion data or changed source enchantments. A grindstone can cleanse it." : "§7This item has no fusion.");
            }
            case "reload" -> {
                if (!sender.hasPermission("enchantmentfusion.reload")) { sender.sendMessage("§cYou do not have permission to reload settings."); break; }
                try { plugin.reloadSettings(); sender.sendMessage("§aSettings loaded. Old fusion previews were invalidated; reinsert your anvil inputs."); }
                catch (Exception error) { sender.sendMessage("§cReload rejected: " + error.getMessage() + ". The previous active configuration is still in use."); }
            }
            case "give" -> {
                if (!sender.hasPermission("enchantmentfusion.admin")) { sender.sendMessage("§cAdministrators only."); break; }
                Player target = args.length >= 3 && args.length <= 4 ? Bukkit.getPlayerExact(args[1]) : null;
                FusionDefinition definition = args.length >= 3 && args.length <= 4 ? service.definition(args[2].toLowerCase(Locale.ROOT)) : null;
                if (target == null || definition == null) { sender.sendMessage("§e/fusion give <online-player> <id> [level]"); break; }
                if (target.getInventory().firstEmpty() < 0) { sender.sendMessage("§cThe player inventory is full; no item was created."); break; }
                int tier=3;
                try{if(args.length==4)tier=Integer.parseInt(args[3]);}catch(NumberFormatException ignored){tier=0;}
                if(tier<1||tier>3||FusionLevels.example(definition,tier).isEmpty()){sender.sendMessage("§cThis recipe has no reachable tier at that level. Check /fusion guide "+definition.id());break;}
                ItemStack result = items.fuse(FusionItems.book(definition,tier), definition, UUID.randomUUID());
                target.getInventory().addItem(result);
                sender.sendMessage("§aGiven: " + definition.name() + " → " + target.getName());
            }
            default -> sender.sendMessage("§eUse /fusion help.");
        }
        return true;
    }
    private void recipes(CommandSender sender, String[] args) {
        List<FusionDefinition> definitions = new ArrayList<>(service.definitions());
        int page = 1, pages = (definitions.size() + 5) / 6;
        try { if (args.length > 1) page = Integer.parseInt(args[1]); } catch (NumberFormatException ignored) { page = 0; }
        if (page < 1 || page > pages) { sender.sendMessage("§eAvailable pages: 1–" + pages); return; }
        sender.sendMessage("§6Fusion Recipes §7" + page + "/" + pages);
        sender.sendMessage(Component.text("[Open recipe book]", NamedTextColor.AQUA).clickEvent(ClickEvent.runCommand("/fusion guide")));
        definitions.subList((page - 1) * 6, Math.min(page * 6, definitions.size())).forEach(d -> {
            Component entry = Component.text(d.name() + " [" + d.equipment() + "]" + (service.tuning(d.id()).enabled() ? "" : " (disabled)"), NamedTextColor.YELLOW)
                        .clickEvent(ClickEvent.runCommand("/fusion info " + d.id()))
                        .hoverEvent(HoverEvent.showText(Component.text(d.description())));
            sender.sendMessage(entry);
        });
        if (page < pages) sender.sendMessage(Component.text("Next page →", NamedTextColor.AQUA).clickEvent(ClickEvent.runCommand("/fusion recipes " + (page + 1))));
    }
    private void info(CommandSender sender, FusionDefinition d) {
        var tuning = service.tuning(d.id());
        sender.sendMessage("§6" + d.name() + " §8(" + d.id() + ") §7" + d.equipment().label()+(tuning.enabled()?"":" §cDISABLED"));
        sender.sendMessage("§eFull-tier recipe: §f"+RecipeText.equation(d));
        sender.sendMessage("§eBook cost: §fvanilla + " + tuning.extraLevels() + " levels. Applying: vanilla.");
        for(int tier=1;tier<=3;tier++)if(FusionLevels.example(d,tier).isPresent()){
            sender.sendMessage("§6"+FusionLevels.roman(tier)+" §7"+FusionDescriptions.describe(d,FusionLevels.profile(d,tuning,tier)));
        }
        sender.sendMessage("§8Limits: " + d.limitation());
    }
    private void extract(Player player) {
        if (!service.allowed(player)) {
            player.sendMessage("§cYou do not have permission to use fusions in this world.");
            return;
        }
        ItemStack held = player.getInventory().getItemInMainHand();
        if (held.getType() == Material.ENCHANTED_BOOK) {
            player.sendMessage("§eThis item is already a Fusion Book.");
            return;
        }
        var stateOpt = items.read(held);
        if (stateOpt.isEmpty()) {
            player.sendMessage("§eHold fused equipment in your main hand to extract its Fusion Book.");
            return;
        }
        boolean creative = player.getGameMode() == GameMode.CREATIVE;
        if (!creative && player.getLevel() < 3) {
            player.sendMessage("§cExtracting a fusion requires 3 XP levels and 1 plain Book.");
            return;
        }
        int bookSlot = -1;
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack slotItem = player.getInventory().getItem(i);
            if (slotItem != null && slotItem.getType() == Material.BOOK && !slotItem.hasItemMeta()) {
                bookSlot = i;
                break;
            }
        }
        if (bookSlot < 0) {
            player.sendMessage("§cYou need 1 plain Book in your inventory to extract the fusion into.");
            return;
        }
        ItemStack bookStack = player.getInventory().getItem(bookSlot);
        if (bookStack.getAmount() > 1 && player.getInventory().firstEmpty() < 0) {
            player.sendMessage("§cYour inventory is full; make space before extracting.");
            return;
        }
        FusionItems.State state = stateOpt.get();
        ItemStack extractedCarrier = new ItemStack(Material.ENCHANTED_BOOK);
        EnchantmentStorageMeta bookMeta = (EnchantmentStorageMeta) extractedCarrier.getItemMeta();
        state.sources().forEach((key, lvl) -> bookMeta.addStoredEnchant(FusionItems.enchantment(key), lvl, false));
        extractedCarrier.setItemMeta(bookMeta);
        ItemStack fusionBook = items.fuse(extractedCarrier, state.definition(), state.instance());

        ItemStack cleansed = items.strip(held);
        for (String key : state.sources().keySet()) {
            cleansed.removeEnchantment(FusionItems.enchantment(key));
        }
        player.getInventory().setItemInMainHand(cleansed);
        if (!creative) player.setLevel(player.getLevel() - 3);
        if (bookStack.getAmount() == 1) {
            player.getInventory().setItem(bookSlot, fusionBook);
        } else {
            bookStack.setAmount(bookStack.getAmount() - 1);
            player.getInventory().setItem(bookSlot, bookStack);
            player.getInventory().addItem(fusionBook);
        }
        progression.recordExtracted(player);
        player.sendMessage("§aExtracted " + state.definition().name() + " " + FusionLevels.roman(state.level()) + " into a Fusion Book!");
    }
    @Override public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> options = new ArrayList<>();
        if (args.length == 1) {
            options.add("help");
            if (sender.hasPermission("enchantmentfusion.recipes")) options.addAll(List.of("guide", "gui", "book", "recipes", "info", "inspect", "stats"));
            if (sender.hasPermission("enchantmentfusion.use")) options.addAll(List.of("extract", "notify"));
            if (sender.hasPermission("enchantmentfusion.reload")) options.add("reload");
            if (sender.hasPermission("enchantmentfusion.admin")) options.add("give");
        } else if (args.length == 2 && (args[0].equalsIgnoreCase("guide") || args[0].equalsIgnoreCase("gui") || args[0].equalsIgnoreCase("book")) && sender.hasPermission("enchantmentfusion.recipes")) {
            options.add("stats");
            service.definitions().stream().filter(d -> service.tuning(d.id()).enabled()).forEach(d -> { options.add(d.id()); options.addAll(d.ingredients().keySet()); });
        } else if (args.length == 2 && args[0].equalsIgnoreCase("info") && sender.hasPermission("enchantmentfusion.recipes")
                || args.length == 3 && args[0].equalsIgnoreCase("give") && sender.hasPermission("enchantmentfusion.admin")) {
            service.definitions().forEach(d -> options.add(d.id()));
        } else if (args.length == 2 && args[0].equalsIgnoreCase("give") && sender.hasPermission("enchantmentfusion.admin")) {
            Bukkit.getOnlinePlayers().forEach(player -> options.add(player.getName()));
        } else if(args.length==4&&args[0].equalsIgnoreCase("give")&&sender.hasPermission("enchantmentfusion.admin")){
            var d=service.definition(args[2]);if(d!=null)for(int tier=1;tier<=3;tier++)if(FusionLevels.example(d,tier).isPresent())options.add(Integer.toString(tier));
        }
        String prefix = args.length == 0 ? "" : args[args.length - 1].toLowerCase(Locale.ROOT);
        return options.stream().distinct().filter(s -> s.toLowerCase(Locale.ROOT).startsWith(prefix)).sorted().toList();
    }
}
