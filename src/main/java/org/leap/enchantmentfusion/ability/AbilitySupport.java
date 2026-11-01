package org.leap.enchantmentfusion.ability;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.leap.enchantmentfusion.model.FusionDescriptions;
import org.leap.enchantmentfusion.model.FusionTuning;
import org.leap.enchantmentfusion.model.FusionLevels;
import org.leap.enchantmentfusion.service.*;
import org.leap.enchantmentfusion.event.FusionAbilityEvent;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Particle;
import org.bukkit.entity.*;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public final class AbilitySupport {
    public final FusionService service;
    public final FusionItems items;
    public final Cooldowns cooldowns;
    public final FusionProgression progression;
    private final Map<UUID, Long> lastCooldownHint = new HashMap<>();
    public AbilitySupport(FusionService service, FusionItems items, Cooldowns cooldowns) {
        this(service, items, cooldowns, new FusionProgression());
    }
    public AbilitySupport(FusionService service, FusionItems items, Cooldowns cooldowns, FusionProgression progression) {
        this.service = service; this.items = items; this.cooldowns = cooldowns; this.progression = progression;
    }
    public boolean eligible(Player player) {
        return player.isOnline() && !player.isDead() && service.allowed(player)
                && (player.getGameMode() == GameMode.SURVIVAL || player.getGameMode() == GameMode.ADVENTURE);
    }
    public Optional<FusionItems.State> active(Player player, ItemStack item) {
        return eligible(player) ? service.active(player, item) : Optional.empty();
    }
    public boolean canAffect(Player attacker, LivingEntity target) {
        return target.isValid() && !target.isDead() && !(target instanceof ArmorStand)
                && attacker.getWorld().equals(target.getWorld()) && !target.hasMetadata("NPC")
                && !(target instanceof Tameable tameable && tameable.isTamed())
                && !attacker.getUniqueId().equals(target.getUniqueId())
                && (!(target instanceof Player) || service.settings().pvp());
    }
    public boolean ready(Player player, String id) {
        return cooldowns.ready(player.getUniqueId(), id);
    }
    public boolean roll(Player player, String id) {
        return roll(player, id, player);
    }
    public FusionLevels.Profile profile(FusionItems.State state){return FusionLevels.profile(state.definition(),tuning(state.definition().id()),state.level());}
    public boolean roll(Player player,FusionItems.State state){return roll(player,state,player);}
    public boolean roll(Player player,FusionItems.State state,LivingEntity target){
        String id=state.definition().id();
        if(!eligible(player)||!tuning(id).enabled())return false;
        if(!ready(player,id)){
            notifyCooldown(player,state);
            return false;
        }
        return ThreadLocalRandom.current().nextDouble()<profile(state).chance()&&permitted(player,id,target);
    }
    public void start(Player player,FusionItems.State state){
        double cd=profile(state).cooldown();
        cooldowns.start(player.getUniqueId(),state.definition().id(),cd);
        progression.recordAbilityTriggered(player);
        notifyProc(player,state,cd);
    }
    private void notifyProc(Player player,FusionItems.State state,double cd){
        if(!progression.notifyEnabled(player)||player.getOpenInventory().getTopInventory() instanceof AnvilInventory)return;
        String suffix=cd>0?" ("+FusionDescriptions.number(cd)+"s cooldown)":"";
        player.sendActionBar(Component.text("✦ "+state.definition().name()+" "+FusionLevels.roman(state.level())+" activated!"+suffix,NamedTextColor.GOLD));
    }
    private void notifyCooldown(Player player,FusionItems.State state){
        if(!progression.notifyEnabled(player)||player.getOpenInventory().getTopInventory() instanceof AnvilInventory)return;
        long now=System.nanoTime();
        if(now-lastCooldownHint.getOrDefault(player.getUniqueId(),0L)<1_500_000_000L)return;
        lastCooldownHint.put(player.getUniqueId(),now);
        double rem=cooldowns.remainingSeconds(player.getUniqueId(),state.definition().id());
        if(rem>0.05)player.sendActionBar(Component.text("⏳ "+state.definition().name()+" "+FusionLevels.roman(state.level())+" ("+FusionDescriptions.number(rem)+"s)",NamedTextColor.RED));
    }
    public boolean proc(Player player,FusionItems.State state){return proc(player,state,player);}
    public boolean proc(Player player,FusionItems.State state,LivingEntity target){if(!roll(player,state,target))return false;start(player,state);return true;}
    public boolean permitted(Player player, String id, LivingEntity target) {
        FusionAbilityEvent event = new FusionAbilityEvent(player, id, target);
        Bukkit.getPluginManager().callEvent(event);
        return !event.isCancelled();
    }
    public boolean roll(Player player, String id, LivingEntity target) {
        return eligible(player) && service.tuning(id).enabled() && ready(player, id)
                && ThreadLocalRandom.current().nextDouble() < service.tuning(id).chance() && permitted(player, id, target);
    }
    public void start(Player player, String id) {
        cooldowns.start(player.getUniqueId(), id, service.tuning(id).cooldown());
    }
    public boolean proc(Player player, String id) {
        return proc(player, id, player);
    }
    public boolean proc(Player player, String id, LivingEntity target) {
        if (!roll(player, id, target)) return false;
        start(player, id); return true;
    }
    public FusionTuning tuning(String id) { return service.tuning(id); }
    public void potion(LivingEntity target, PotionEffectType type, double seconds, int amplifier, double power) {
        int duration = Math.max(1, (int) Math.round(seconds * 20 * power));
        PotionEffect current = target.getPotionEffect(type);
        if (current != null && (current.isInfinite() || current.getAmplifier() > amplifier
                || (current.getAmplifier() == amplifier && current.getDuration() >= duration))) return;
        target.addPotionEffect(new PotionEffect(type, duration, amplifier, true, false, true));
    }
    public void potion(LivingEntity target,PotionEffectType type,double seconds,int amplifier,FusionLevels.Profile profile){
        boolean fixed=type.equals(PotionEffectType.NIGHT_VISION)||type.equals(PotionEffectType.DOLPHINS_GRACE);
        potion(target,type,seconds,fixed?amplifier:profile.amplifier(amplifier),profile.power());
    }
    public void particles(LivingEntity target, Particle type) {
        target.getWorld().spawnParticle(type, target.getLocation().add(0, 1, 0), 8, .35, .4, .35, .02);
    }
    public boolean repair(Player player, UUID instance, int amount) {
        if (amount <= 0) return false;
        for (int slot = 0; slot < player.getInventory().getSize(); slot++) {
            ItemStack item = player.getInventory().getItem(slot);
            var state = items.read(item);
            if (state.isEmpty() || !state.get().instance().equals(instance) || !(item.getItemMeta() instanceof Damageable meta)) continue;
            if (meta.getDamage() == 0) return false;
            meta.setDamage(Math.max(0, meta.getDamage() - amount));
            item.setItemMeta(meta); player.getInventory().setItem(slot, item); return true;
        }
        return false;
    }
    public Optional<ItemStack> owned(Player player, UUID instance) {
        return Arrays.stream(player.getInventory().getContents()).filter(Objects::nonNull)
                .filter(item -> items.read(item).filter(state -> state.instance().equals(instance)).isPresent()).findFirst();
    }
}
