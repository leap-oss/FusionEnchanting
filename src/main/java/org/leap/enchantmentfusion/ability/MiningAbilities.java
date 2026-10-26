package org.leap.enchantmentfusion.ability;

import org.leap.enchantmentfusion.service.FusionItems;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

public final class MiningAbilities implements Listener {
    private record Repair(FusionItems.State state, int durability) {}
    private record OreUse(UUID instance, int tick) {}
    private final Map<BlockBreakEvent, Repair> repairs = new IdentityHashMap<>();
    private final Map<UUID, OreUse> ores = new HashMap<>();
    private final Plugin plugin;
    private final AbilitySupport support;
    public MiningAbilities(Plugin plugin, AbilitySupport support) { this.plugin = plugin; this.support = support; }
    public Set<String> registered() { return Set.of("prospector", "excavator", "workhorse", "miners_luck", "preservation", "prosperity"); }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void convertExperience(BlockBreakEvent event) {
        var item = event.getPlayer().getInventory().getItemInMainHand();
        var state = support.active(event.getPlayer(), item);
        if (state.isEmpty() || !state.get().definition().id().equals("prosperity") || !ore(event.getBlock().getType())
                || event.getExpToDrop() < 1 || !(item.getItemMeta() instanceof Damageable meta) || meta.getDamage() == 0
                || !support.roll(event.getPlayer(), "prosperity")) return;
        int points = Math.min(2, Math.min(event.getExpToDrop(), (meta.getDamage() + 1) / 2));
        event.setExpToDrop(event.getExpToDrop() - points);
        repairs.put(event, new Repair(state.get(), points * 2));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void mined(BlockBreakEvent event) {
        Repair repair = repairs.remove(event);
        if (event.isCancelled()) return;
        Player player = event.getPlayer();
        if (repair != null) {
            support.start(player, "prosperity");
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (support.eligible(player)) support.repair(player, repair.state.instance(), repair.durability);
            });
        }
        support.active(player, player.getInventory().getItemInMainHand()).ifPresent(state -> {
            String id = state.definition().id();
            if (ore(event.getBlock().getType())) {
                if (id.equals("miners_luck")) ores.put(player.getUniqueId(), new OreUse(state.instance(), Bukkit.getCurrentTick()));
                if (id.equals("prospector") && support.proc(player, id))
                    support.potion(player, PotionEffectType.NIGHT_VISION, 12, 0, support.tuning(id).power());
            }
            if (id.equals("excavator") && player.isSneaking() && support.proc(player, id))
                support.potion(player, PotionEffectType.HASTE, 5, 0, support.tuning(id).power());
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void wear(PlayerItemDamageEvent event) {
        var state = support.active(event.getPlayer(), event.getItem());
        if (state.isEmpty()) return;
        String id = state.get().definition().id();
        boolean condition = switch (id) {
            case "workhorse" -> event.getPlayer().getFoodLevel() >= 16;
            case "preservation" -> event.getPlayer().isSneaking();
            case "miners_luck" -> {
                OreUse use = ores.remove(event.getPlayer().getUniqueId());
                yield use != null && use.instance.equals(state.get().instance()) && Bukkit.getCurrentTick() - use.tick <= 2;
            }
            default -> false;
        };
        if (condition && support.proc(event.getPlayer(), id)) event.setCancelled(true);
    }
    @EventHandler public void quit(PlayerQuitEvent event) { ores.remove(event.getPlayer().getUniqueId()); }
    private static boolean ore(Material material) { return material.name().endsWith("_ORE") || material == Material.ANCIENT_DEBRIS; }
}
