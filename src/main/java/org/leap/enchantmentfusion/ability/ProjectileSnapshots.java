package org.leap.enchantmentfusion.ability;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Trident;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.Optional;

public final class ProjectileSnapshots implements Listener {
    public record Snapshot(ItemStack item, double force) {}
    private final NamespacedKey snapshot, force, used;
    private final AbilitySupport support;
    public ProjectileSnapshots(Plugin plugin, AbilitySupport support) {
        this.support = support;
        snapshot = new NamespacedKey(plugin, "shot_snapshot");
        force = new NamespacedKey(plugin, "shot_force");
        used = new NamespacedKey(plugin, "shot_used");
    }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void shoot(EntityShootBowEvent event) {
        if (event.getEntity() instanceof Player player && event.getProjectile() instanceof Projectile projectile
                && support.active(player, event.getBow()).isPresent()) capture(projectile, event.getBow(), event.getForce());
    }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void launch(ProjectileLaunchEvent event) {
        if (event.getEntity() instanceof Trident trident && trident.getShooter() instanceof Player player
                && support.active(player, trident.getItem()).isPresent()) capture(trident, trident.getItem(), 1);
    }
    public void capture(Projectile projectile, ItemStack item, double strength) {
        byte[] bytes = item.serializeAsBytes();
        if (bytes.length > 16_384) return;
        var data = projectile.getPersistentDataContainer();
        data.set(snapshot, PersistentDataType.BYTE_ARRAY, bytes);
        data.set(force, PersistentDataType.DOUBLE, strength);
    }
    public Optional<Snapshot> read(Projectile projectile) {
        var data = projectile.getPersistentDataContainer();
        if (data.has(used)) return Optional.empty();
        if (!data.has(snapshot, PersistentDataType.BYTE_ARRAY) || !data.has(force, PersistentDataType.DOUBLE)) return Optional.empty();
        byte[] bytes = data.get(snapshot, PersistentDataType.BYTE_ARRAY);
        Double strength = data.get(force, PersistentDataType.DOUBLE);
        if (bytes == null || bytes.length > 16_384 || strength == null || !Double.isFinite(strength)) return Optional.empty();
        try { return Optional.of(new Snapshot(ItemStack.deserializeBytes(bytes), strength)); }
        catch (RuntimeException exception) { return Optional.empty(); }
    }
    public void consume(Projectile projectile) { projectile.getPersistentDataContainer().set(used, PersistentDataType.BYTE, (byte) 1); }
}
