package org.leap.enchantmentfusion.ability;

import org.bukkit.Material;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

public final class UtilityAbilities implements Listener, Runnable {
    private final Plugin plugin;
    private final AbilitySupport support;
    private final ProjectileSnapshots snapshots;
    private final Map<UUID, Integer> water = new HashMap<>();
    private final Map<UUID, Float> falls = new HashMap<>();
    private record Catch(UUID instance, int tick) {}
    private final Map<UUID, Catch> catches = new HashMap<>();
    public UtilityAbilities(Plugin plugin, AbilitySupport support, ProjectileSnapshots snapshots) {
        this.plugin = plugin; this.support = support; this.snapshots = snapshots;
    }
    public Set<String> registered() { return Set.of("amphibious", "glacial_step", "soulbound_steps", "wave_rider", "master_angler", "aquanaut"); }

    /** A bounded pulse only visits online players, never blocks/chunks/entities globally. */
    @Override public void run() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (!support.eligible(player)) { water.remove(player.getUniqueId()); falls.remove(player.getUniqueId()); continue; }
            boolean wet = player.isInWaterOrBubbleColumn();
            if (wet) water.put(player.getUniqueId(), Bukkit.getCurrentTick());
            Integer lastWet = water.get(player.getUniqueId());
            boolean leftWater = !wet && player.isOnGround() && lastWet != null && Bukkit.getCurrentTick() - lastWet <= 40;
            support.active(player, player.getInventory().getBoots()).ifPresent(state -> {
                String id = state.definition().id();
                if (id.equals("amphibious") && leftWater && support.proc(player, id))
                    support.potion(player, PotionEffectType.SPEED, 4, 0, support.tuning(id).power());
                if (id.equals("soulbound_steps")) {
                    Material floor = player.getLocation().subtract(0, .1, 0).getBlock().getType();
                    if ((floor == Material.SOUL_SAND || floor == Material.SOUL_SOIL) && support.proc(player, id))
                        support.potion(player, PotionEffectType.JUMP_BOOST, 2, 0, support.tuning(id).power());
                }
                if (id.equals("glacial_step") && player.isOnGround() && falls.getOrDefault(player.getUniqueId(), 0f) >= 3
                        && support.proc(player, id)) support.potion(player, PotionEffectType.SPEED, 3, 0, support.tuning(id).power());
            });
            if (!wet && (player.isOnGround() || lastWet != null && Bukkit.getCurrentTick() - lastWet > 40)) water.remove(player.getUniqueId());
            if (player.isOnGround() || wet || player.isGliding() || player.isFlying()) falls.remove(player.getUniqueId());
            support.active(player, player.getInventory().getHelmet()).filter(state -> state.definition().id().equals("aquanaut"))
                    .ifPresent(state -> { if (wet && support.proc(player, "aquanaut")) support.potion(player, PotionEffectType.NIGHT_VISION, 12, 0, support.tuning("aquanaut").power()); });
        }
    }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void move(PlayerMoveEvent event) {
        float fall = event.getPlayer().getFallDistance();
        if (fall >= 3) falls.merge(event.getPlayer().getUniqueId(), fall, Math::max);
    }
    @EventHandler public void quit(PlayerQuitEvent event) { water.remove(event.getPlayer().getUniqueId()); falls.remove(event.getPlayer().getUniqueId()); catches.remove(event.getPlayer().getUniqueId()); }
    @EventHandler public void teleport(PlayerTeleportEvent event) { falls.remove(event.getPlayer().getUniqueId()); water.remove(event.getPlayer().getUniqueId()); }

    @EventHandler(priority = EventPriority.MONITOR)
    public void riptide(PlayerRiptideEvent event) {
        Player player = event.getPlayer();
        support.active(player, event.getItem()).filter(state -> state.definition().id().equals("wave_rider"))
                .ifPresent(state -> { if (support.proc(player, "wave_rider")) support.potion(player, PotionEffectType.DOLPHINS_GRACE, 4, 0, support.tuning("wave_rider").power()); });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void fish(PlayerFishEvent event) {
        Player player = event.getPlayer();
        if (event.getState() == PlayerFishEvent.State.FISHING && event.getHand() != null) {
            ItemStack rod = player.getInventory().getItem(event.getHand());
            if (support.active(player, rod).filter(state -> state.definition().id().equals("master_angler")).isPresent()) snapshots.capture(event.getHook(), rod, 1);
        }
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) return;
        snapshots.read(event.getHook()).flatMap(snapshot -> support.active(player, snapshot.item()))
                .filter(state -> state.definition().id().equals("master_angler")).ifPresent(state -> {
                    snapshots.consume(event.getHook());
                    catches.put(player.getUniqueId(), new Catch(state.instance(), Bukkit.getCurrentTick()));
                });
    }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void rodWear(PlayerItemDamageEvent event) {
        Catch caught = catches.remove(event.getPlayer().getUniqueId());
        if (caught == null || Bukkit.getCurrentTick() - caught.tick() > 2) return;
        support.active(event.getPlayer(), event.getItem()).filter(state -> state.definition().id().equals("master_angler")
                && state.instance().equals(caught.instance())).ifPresent(state -> {
                    if (support.proc(event.getPlayer(), "master_angler")) event.setCancelled(true);
                });
    }

}
