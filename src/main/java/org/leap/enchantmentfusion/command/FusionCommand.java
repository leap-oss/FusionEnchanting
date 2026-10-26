package org.leap.enchantmentfusion.command;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.leap.enchantmentfusion.EnchantmentFusion;
import org.leap.enchantmentfusion.discovery.RecipeGuide;
import org.leap.enchantmentfusion.model.FusionDefinition;
import org.leap.enchantmentfusion.service.*;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.stream.Collectors;

public final class FusionCommand implements TabExecutor {
    private final EnchantmentFusion plugin;
    private final FusionService service;
    private final FusionItems items;
    private final RecipeGuide guide;
    public FusionCommand(EnchantmentFusion plugin, FusionService service, FusionItems items, RecipeGuide guide) {
        this.plugin = plugin; this.service = service; this.items = items; this.guide = guide;
    }
    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String action = args.length == 0 ? "help" : args[0].toLowerCase(Locale.ROOT);
        if (Set.of("guide", "recipes", "info", "inspect").contains(action) && !sender.hasPermission("enchantmentfusion.recipes")) {
            sender.sendMessage("§cTidak punya izin melihat resep."); return true;
        }
        switch (action) {
            case "help" -> {
                sender.sendMessage("§6FusionEnchanting §7— 36 fusion, satu per item.");
                sender.sendMessage(Component.text("[Buka panduan resep]", NamedTextColor.AQUA).clickEvent(ClickEvent.runCommand("/fusion guide")));
                sender.sendMessage("§e/fusion recipes [halaman] §7— katalog resep (klik nama)");
                sender.sendMessage("§e/fusion info <id> §7— bahan, efek, batasan, dan biaya");
                sender.sendMessage("§e/fusion inspect §7— periksa item di tangan utama");
                sender.sendMessage("§7Anvil: buku enchant + buku enchant → buku fusion. Pasang buku fusion ke equipment lewat anvil.");
                if (sender.hasPermission("enchantmentfusion.admin")) sender.sendMessage("§eAdmin: /fusion give <player> <id>, /fusion selftest, /fusion reload");
            }
            case "guide" -> {
                if (!(sender instanceof Player player)) { sender.sendMessage("Gunakan di dalam game; konsol dapat memakai /fusion recipes."); break; }
                if (args.length > 2 || !guide.open(player, args.length == 2 ? args[1] : ""))
                    sender.sendMessage("§eResep aktif tidak ditemukan. Gunakan /fusion guide [id/enchant], misalnya sharpness.");
            }
            case "recipes" -> recipes(sender, args);
            case "info" -> {
                FusionDefinition definition = args.length == 2 ? service.definition(args[1].toLowerCase(Locale.ROOT)) : null;
                if (definition == null) sender.sendMessage("§eGunakan /fusion info <id>. Lihat /fusion recipes.");
                else info(sender, definition);
            }
            case "inspect" -> {
                if (!(sender instanceof Player player)) { sender.sendMessage("Gunakan di dalam game."); break; }
                ItemStack item = player.getInventory().getItemInMainHand();
                var state = items.read(item);
                if (state.isPresent()) {
                    player.getInventory().setItemInMainHand(items.refreshLore(item));
                    info(sender, state.get().definition()); sender.sendMessage("§8Item ID: " + state.get().instance());
                    if (!service.allowed(player)) sender.sendMessage("§eAbility tidak aktif di world ini / izin use tidak ada.");
                } else sender.sendMessage(items.marked(item) ? "§cData fusion tidak valid atau enchantment sumber telah berubah. Grindstone dapat membersihkannya." : "§7Item ini belum memiliki fusion.");
            }
            case "reload" -> {
                if (!sender.hasPermission("enchantmentfusion.reload")) { sender.sendMessage("§cTidak punya izin reload."); break; }
                try { plugin.reloadSettings(); sender.sendMessage("§aKonfigurasi dimuat. Preview fusion lama dibatalkan; masukkan ulang input anvil."); }
                catch (Exception error) { sender.sendMessage("§cReload ditolak: " + error.getMessage() + ". Konfigurasi aktif sebelumnya tetap digunakan."); }
            }
            case "give" -> {
                if (!sender.hasPermission("enchantmentfusion.admin")) { sender.sendMessage("§cKhusus admin."); break; }
                Player target = args.length == 3 ? Bukkit.getPlayerExact(args[1]) : null;
                FusionDefinition definition = args.length == 3 ? service.definition(args[2].toLowerCase(Locale.ROOT)) : null;
                if (target == null || definition == null) { sender.sendMessage("§e/fusion give <player-online> <id>"); break; }
                if (target.getInventory().firstEmpty() < 0) { sender.sendMessage("§cInventory pemain penuh; item tidak dibuat."); break; }
                ItemStack result = items.fuse(FusionDiagnostics.book(definition), definition, UUID.randomUUID());
                target.getInventory().addItem(result);
                sender.sendMessage("§aDiberikan: " + definition.name() + " → " + target.getName());
            }
            case "selftest" -> {
                if (!sender.hasPermission("enchantmentfusion.admin")) { sender.sendMessage("§cKhusus admin."); break; }
                try {
                    int checks = FusionDiagnostics.run(service.definitions(), items, plugin.registeredAbilities());
                    sender.sendMessage("§aFUSION SELFTEST PASS: " + service.definitions().size() + " recipes, " + checks + " metadata/serialization checks. Gameplay event testing remains separate.");
                } catch (RuntimeException failure) {
                    sender.sendMessage("§cFUSION SELFTEST FAIL: " + failure.getMessage());
                    plugin.getLogger().log(java.util.logging.Level.SEVERE, "Fusion diagnostics failed", failure);
                }
            }
            default -> sender.sendMessage("§eGunakan /fusion help.");
        }
        return true;
    }
    private void recipes(CommandSender sender, String[] args) {
        List<FusionDefinition> definitions = new ArrayList<>(service.definitions());
        int page = 1, pages = (definitions.size() + 5) / 6;
        try { if (args.length > 1) page = Integer.parseInt(args[1]); } catch (NumberFormatException ignored) { page = 0; }
        if (page < 1 || page > pages) { sender.sendMessage("§eHalaman tersedia: 1–" + pages); return; }
        sender.sendMessage("§6Fusion Recipes §7" + page + "/" + pages);
        sender.sendMessage(Component.text("[Buka buku resep]", NamedTextColor.AQUA).clickEvent(ClickEvent.runCommand("/fusion guide")));
        definitions.subList((page - 1) * 6, Math.min(page * 6, definitions.size())).forEach(d -> {
            Component entry = Component.text(d.name() + " [" + d.equipment() + "]" + (service.tuning(d.id()).enabled() ? "" : " (disabled)"), NamedTextColor.YELLOW)
                        .clickEvent(ClickEvent.runCommand("/fusion info " + d.id()))
                        .hoverEvent(HoverEvent.showText(Component.text(d.description())));
            sender.sendMessage(entry);
        });
        if (page < pages) sender.sendMessage(Component.text("Halaman berikutnya →", NamedTextColor.AQUA).clickEvent(ClickEvent.runCommand("/fusion recipes " + (page + 1))));
    }
    private void info(CommandSender sender, FusionDefinition d) {
        var tuning = service.tuning(d.id());
        sender.sendMessage("§6" + d.name() + " I §8(" + d.id() + ") §7" + d.equipment());
        sender.sendMessage("§eBahan: §f" + d.ingredients().entrySet().stream().sorted(Map.Entry.comparingByKey()).map(e -> e.getKey() + " " + e.getValue()).collect(Collectors.joining(" + ")));
        sender.sendMessage("§eBiaya buku fusion: §fbiaya anvil vanilla + " + tuning.extraLevels() + " level. Pemasangan: biaya vanilla.");
        sender.sendMessage("§7Default: " + d.description());
        sender.sendMessage("§8Batasan default: " + d.limitation());
        sender.sendMessage("§7Tuning aktif: cooldown " + tuning.cooldown() + "s, peluang " + Math.round(tuning.chance() * 100) + "%, power ×" + tuning.power() + (tuning.enabled() ? "" : " §cDISABLED"));
    }
    @Override public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> options = new ArrayList<>();
        if (args.length == 1) {
            options.add("help");
            if (sender.hasPermission("enchantmentfusion.recipes")) options.addAll(List.of("guide", "recipes", "info", "inspect"));
            if (sender.hasPermission("enchantmentfusion.reload")) options.add("reload");
            if (sender.hasPermission("enchantmentfusion.admin")) options.addAll(List.of("give", "selftest"));
        } else if (args.length == 2 && args[0].equalsIgnoreCase("guide") && sender.hasPermission("enchantmentfusion.recipes")) {
            service.definitions().stream().filter(d -> service.tuning(d.id()).enabled()).forEach(d -> { options.add(d.id()); options.addAll(d.ingredients().keySet()); });
        } else if (args.length == 2 && args[0].equalsIgnoreCase("info") && sender.hasPermission("enchantmentfusion.recipes")
                || args.length == 3 && args[0].equalsIgnoreCase("give") && sender.hasPermission("enchantmentfusion.admin")) {
            service.definitions().forEach(d -> options.add(d.id()));
        } else if (args.length == 2 && args[0].equalsIgnoreCase("give") && sender.hasPermission("enchantmentfusion.admin")) {
            Bukkit.getOnlinePlayers().forEach(player -> options.add(player.getName()));
        }
        String prefix = args.length == 0 ? "" : args[args.length - 1].toLowerCase(Locale.ROOT);
        return options.stream().distinct().filter(s -> s.toLowerCase(Locale.ROOT).startsWith(prefix)).sorted().toList();
    }
}
