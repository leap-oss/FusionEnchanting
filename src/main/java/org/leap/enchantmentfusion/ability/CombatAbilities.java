package org.leap.enchantmentfusion.ability;

import org.leap.enchantmentfusion.service.FusionItems;
import org.bukkit.Particle;
import org.bukkit.Tag;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.function.Function;

/** Rules generate a plan; damage changes before resolution, side effects only after a successful hit. */
public final class CombatAbilities implements Listener {
    private record Hit(Player player, LivingEntity target, EntityDamageByEntityEvent event,
                       FusionItems.State state, Projectile projectile, boolean charged, float fall, double power) {}
    private record Plan(double bonus, Runnable after) {}
    private record Pending(Hit hit, Plan plan) {}
    private final Map<String, Function<Hit, Plan>> handlers = new HashMap<>();
    private final Map<EntityDamageByEntityEvent, Pending> pending = new IdentityHashMap<>();
    private final Plugin plugin;
    private final AbilitySupport support;
    private final ProjectileSnapshots snapshots;

    public CombatAbilities(Plugin plugin, AbilitySupport support, ProjectileSnapshots snapshots) {
        this.plugin = plugin; this.support = support; this.snapshots = snapshots;
        handlers.put("infernal_edge", h -> h.charged ? new Plan(h.target.getFireTicks() > 0 ? 2 * h.power : 0,
                () -> burn(h, 3)) : null);
        handlers.put("blade_storm", h -> h.charged ? new Plan(0, () -> {
            debuff(h, PotionEffectType.SLOWNESS, 2, 0);
            h.target.getNearbyEntities(3, 2, 3).stream().filter(Monster.class::isInstance).map(Monster.class::cast)
                    .filter(mob -> support.canAffect(h.player, mob) && h.player.hasLineOfSight(mob)
                            && mob.getLocation().distanceSquared(h.target.getLocation()) <= 9
                            && support.permitted(h.player, "blade_storm", mob))
                    .limit(3).forEach(mob -> support.potion(mob, PotionEffectType.SLOWNESS, 2, 0, h.power));
            support.particles(h.target, Particle.SWEEP_ATTACK);
        }) : null);
        handlers.put("brutal_force", h -> h.charged ? new Plan(h.power, () -> {
            Vector direction = h.target.getLocation().toVector().subtract(h.player.getLocation().toVector()).setY(0);
            if (direction.lengthSquared() > .001) {
                Vector current = h.target.getVelocity(); double oldSpeed = current.clone().setY(0).length();
                Vector boosted = current.clone().setY(0).add(direction.normalize().multiply(.15 * h.power));
                double cap = Math.max(1.2, oldSpeed);
                if (boosted.length() > cap) boosted.normalize().multiply(cap);
                boosted.setY(Math.max(current.getY(), .15)); h.target.setVelocity(boosted);
            }
        }) : null);
        handlers.put("executioner", h -> h.charged && healthFraction(h.target) <= .25 ? new Plan(4 * h.power,
                () -> support.particles(h.target, Particle.DAMAGE_INDICATOR)) : null);
        handlers.put("holy_flame", h -> h.charged && undead(h.target) ? new Plan(3 * h.power,
                () -> support.particles(h.target, Particle.SOUL_FIRE_FLAME)) : null);
        handlers.put("scorched_venom", h -> h.charged && Tag.ENTITY_TYPES_ARTHROPOD.isTagged(h.target.getType())
                ? new Plan(0, () -> debuff(h, PotionEffectType.WITHER, 4, 0)) : null);
        handlers.put("flame_burst", h -> h.charged && h.target.getFireTicks() > 0
                ? new Plan(0, () -> debuff(h, PotionEffectType.WEAKNESS, 3, 0)) : null);
        handlers.put("hellfire", h -> h.projectile != null && h.charged ? new Plan(2 * h.power, () -> burn(h, 5)) : null);
        handlers.put("impact", h -> h.projectile != null && h.charged ? new Plan(0,
                () -> debuff(h, PotionEffectType.SLOWNESS, 2, 1)) : null);
        handlers.put("blast_arrow", h -> h.projectile != null && h.charged ? new Plan(h.power, () -> {
            debuff(h, PotionEffectType.WEAKNESS, 3, 0); support.particles(h.target, Particle.FLAME);
        }) : null);
        handlers.put("volley", h -> h.projectile != null ? new Plan(0, () -> {
            debuff(h, PotionEffectType.SLOWNESS, 3, 0);
            support.potion(h.player, PotionEffectType.SPEED, 3, 0, h.power);
        }) : null);
        handlers.put("railshot", h -> h.projectile != null ? new Plan(3 * h.power,
                () -> support.particles(h.target, Particle.ELECTRIC_SPARK)) : null);
        handlers.put("stormcaller", h -> h.projectile instanceof Trident && h.target.isInRain()
                ? new Plan(2 * h.power, () -> h.target.getWorld().strikeLightningEffect(h.target.getLocation())) : null);
        handlers.put("tempest", h -> h.projectile instanceof Trident && h.target.getWorld().isThundering()
                ? new Plan(0, () -> debuff(h, PotionEffectType.SLOWNESS, 3, 1)) : null);
        handlers.put("meteor", h -> h.projectile == null && h.fall >= 3 ? new Plan(3 * h.power,
                () -> support.particles(h.target, Particle.CLOUD)) : null);
        handlers.put("shockwave", h -> h.projectile == null && h.fall >= 3 ? new Plan(0,
                () -> debuff(h, PotionEffectType.WEAKNESS, 3, 0)) : null);
        handlers.put("comet", h -> h.projectile == null && h.fall >= 3 ? new Plan(2 * h.power, () -> burn(h, 5)) : null);
    }

    public Set<String> registered() {
        Set<String> ids = new HashSet<>(handlers.keySet()); ids.add("soul_reaper"); return Set.copyOf(ids);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void beforeHit(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof LivingEntity target)) return;
        Player player;
        Projectile projectile = null;
        FusionItems.State state;
        boolean charged;
        if (event.getDamager() instanceof Player attacker && event.getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK) {
            player = attacker;
            var active = support.active(player, player.getInventory().getItemInMainHand());
            if (active.isEmpty()) return;
            state = active.get(); charged = player.getCooledAttackStrength(0) >= .9f;
        } else if (event.getDamager() instanceof Projectile shot && shot.getShooter() instanceof Player shooter) {
            player = shooter; projectile = shot;
            var snapshot = snapshots.read(shot);
            if (snapshot.isEmpty()) return;
            var active = support.active(player, snapshot.get().item());
            if (active.isEmpty()) return;
            state = active.get(); charged = snapshot.get().force() >= .9;
        } else return;
        if (!support.canAffect(player, target)) return;
        Function<Hit, Plan> handler = handlers.get(state.definition().id());
        if (handler == null) return;
        Hit hit = new Hit(player, target, event, state, projectile, charged, player.getFallDistance(), support.tuning(state.definition().id()).power());
        Plan plan = handler.apply(hit);
        if (plan == null || !support.roll(player, state.definition().id(), target)) return;
        event.setDamage(event.getDamage() + plan.bonus);
        pending.put(event, new Pending(hit, plan));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void afterHit(EntityDamageByEntityEvent event) {
        Pending result = pending.remove(event);
        if (result == null || event.isCancelled() || event.getFinalDamage() <= 0) return;
        Hit hit = result.hit;
        support.start(hit.player, hit.state.definition().id());
        if (hit.projectile != null) snapshots.consume(hit.projectile);
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (support.eligible(hit.player) && hit.target.isValid() && !hit.target.isDead()
                    && support.tuning(hit.state.definition().id()).enabled()
                    && hit.player.getWorld().equals(hit.target.getWorld()) && support.canAffect(hit.player, hit.target)) result.plan.after.run();
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void undeadKill(EntityDeathEvent event) {
        LivingEntity target = event.getEntity();
        Player player = target.getKiller();
        if (player == null || !undead(target) || !(target.getLastDamageCause() instanceof EntityDamageByEntityEvent damage)
                || damage.getDamager() != player || damage.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK) return;
        support.active(player, player.getInventory().getItemInMainHand()).filter(state -> state.definition().id().equals("soul_reaper"))
                .ifPresent(state -> { if (support.proc(player, "soul_reaper")) support.potion(player, PotionEffectType.REGENERATION, 4, 0, support.tuning("soul_reaper").power()); });
    }
    private void debuff(Hit hit, PotionEffectType type, double seconds, int amplifier) {
        support.potion(hit.target, type, seconds, amplifier, hit.power);
    }
    private void burn(Hit hit, double seconds) {
        hit.target.setFireTicks(Math.max(hit.target.getFireTicks(), (int) (seconds * 20 * hit.power)));
        support.particles(hit.target, Particle.FLAME);
    }
    private static double healthFraction(LivingEntity target) {
        var maximum = target.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        return maximum == null || maximum.getValue() <= 0 ? 1 : target.getHealth() / maximum.getValue();
    }
    private static boolean undead(LivingEntity target) { return Tag.ENTITY_TYPES_SENSITIVE_TO_SMITE.isTagged(target.getType()); }
}
