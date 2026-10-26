package org.leap.enchantmentfusion.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.inventory.ItemStack;

/** Protection hook for creating/applying a book. Item getters return defensive copies. */
public final class FusionCreateEvent extends PlayerEvent implements Cancellable {
    public enum Operation { CREATE_BOOK, APPLY_BOOK, MAINTAIN }
    private static final HandlerList HANDLERS = new HandlerList();
    private final String fusionId;
    private final ItemStack source, right, result;
    private final int levels;
    private final Operation operation;
    private boolean cancelled;
    public FusionCreateEvent(Player player, String fusionId, ItemStack source, ItemStack right, ItemStack result, int levels, Operation operation) {
        super(player); this.fusionId = fusionId; this.source = source.clone(); this.right = right.clone();
        this.result = result.clone(); this.levels = levels; this.operation = operation;
    }
    public String getFusionId() { return fusionId; }
    public ItemStack getSource() { return source.clone(); }
    public ItemStack getRightInput() { return right.clone(); }
    public ItemStack getResult() { return result.clone(); }
    public int getLevels() { return levels; }
    public Operation getOperation() { return operation; }
    /** @deprecated Legacy 1.0 hook; no catalyst is required, the right input is one book. */
    @Deprecated public int getCatalystAmount() { return 1; }
    @Override public boolean isCancelled() { return cancelled; }
    @Override public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }
    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
