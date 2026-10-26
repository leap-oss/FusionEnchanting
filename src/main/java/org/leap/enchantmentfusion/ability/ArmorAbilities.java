package org.leap.enchantmentfusion.ability;

import org.leap.enchantmentfusion.service.FusionItems;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffectType;

import java.util.*;
import java.util.function.BiConsumer;

public final class ArmorAbilities implements Listener {
    private final Plugin plugin;
    private final AbilitySupport support;
    private final Map<String, BiConsumer<Player, LivingEntity>> melee = new HashMap<>();
    public ArmorAbilities(Plugin plugin, AbilitySupport support) {
        this.plugin = plugin; this.support = support;
        melee.put("retribution", (wearer, attacker) -> support.potion(attacker, PotionEffectType.WEAKNESS, 3, 0, support.tuning("retribution").power()));
        melee.put("molten_guard", (wearer, attacker) -> attacker.setFireTicks(Math.max(attacker.getFireTicks(), (int) (60 * support.tuning("molten_guard").power()))));
    }
    public Set<String> registered() { return Set.of("retribution", "molten_guard", "reactive_armor", "deflection", "fortified", "bloodforged"); }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void damaged(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player wearer) || !support.eligible(wearer) || event.getFinalDamage() <= 0) return;
        Entity attacker = event instanceof EntityDamageByEntityEvent hit ? hit.getDamager() : null;
        if (!support.service.settings().pvp() && (attacker instanceof Player
                || attacker instanceof Projectile projectile && projectile.getShooter() instanceof Player)) return;
        Set<String> equipped = new HashSet<>();
        for (ItemStack armor : wearer.getInventory().getArmorContents()) support.active(wearer, armor).ifPresent(state -> equipped.add(state.definition().id()));
        for (String id : equipped) {
            Runnable effect = null;
            if (attacker instanceof LivingEntity target && event.getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK
                    && support.canAffect(wearer, target) && melee.containsKey(id)) effect = () -> {
                if (support.canAffect(wearer, target)) melee.get(id).accept(wearer, target);
            };
            if (id.equals("reactive_armor") && (event.getCause() == EntityDamageEvent.DamageCause.ENTITY_EXPLOSION
                    || event.getCause() == EntityDamageEvent.DamageCause.BLOCK_EXPLOSION))
                effect = () -> support.potion(wearer, PotionEffectType.RESISTANCE, 3, 0, support.tuning(id).power());
            if (id.equals("deflection") && event.getCause() == EntityDamageEvent.DamageCause.PROJECTILE)
                effect = () -> support.potion(wearer, PotionEffectType.SPEED, 3, 0, support.tuning(id).power());
            if (effect == null || !support.proc(wearer, id, attacker instanceof LivingEntity living ? living : wearer)) continue;
            Runnable confirmed = effect;
            plugin.getServer().getScheduler().runTask(plugin, () -> { if (support.eligible(wearer) && support.tuning(id).enabled()) confirmed.run(); });
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void wear(PlayerItemDamageEvent event) {
        support.active(event.getPlayer(), event.getItem()).filter(state -> state.definition().id().equals("fortified"))
                .ifPresent(state -> { if (support.proc(event.getPlayer(), "fortified")) event.setCancelled(true); });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void kill(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null || !(event.getEntity() instanceof Monster) || !support.eligible(killer)
                || !(event.getEntity().getLastDamageCause() instanceof EntityDamageByEntityEvent damage)
                || damage.getDamager() != killer || damage.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK) return;
        ItemStack selected = Arrays.stream(killer.getInventory().getArmorContents()).filter(Objects::nonNull)
                .filter(item -> item.getItemMeta() instanceof Damageable meta && meta.getDamage() > 0)
                .filter(item -> support.active(killer, item).filter(state -> state.definition().id().equals("bloodforged")).isPresent())
                .max(Comparator.comparingInt(item -> ((Damageable) item.getItemMeta()).getDamage())).orElse(null);
        if (selected == null || !support.proc(killer, "bloodforged")) return;
        var state = support.items.read(selected).orElseThrow();
        support.repair(killer, state.instance(), Math.max(1, (int) Math.round(4 * support.tuning("bloodforged").power())));
    }
}
