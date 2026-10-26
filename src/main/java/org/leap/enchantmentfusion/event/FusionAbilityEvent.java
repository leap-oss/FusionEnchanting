package org.leap.enchantmentfusion.event;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

/** Fired before a proc (and per secondary target); cancellation spends no cooldown. */
public final class FusionAbilityEvent extends PlayerEvent implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();
    private final String fusionId;
    private final LivingEntity target;
    private boolean cancelled;
    public FusionAbilityEvent(Player player, String fusionId, LivingEntity target) {
        super(player); this.fusionId = fusionId; this.target = target;
    }
    public String getFusionId() { return fusionId; }
    public LivingEntity getTarget() { return target; }
    @Override public boolean isCancelled() { return cancelled; }
    @Override public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }
    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
