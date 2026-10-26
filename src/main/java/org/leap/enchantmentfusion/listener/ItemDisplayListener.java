package org.leap.enchantmentfusion.listener;

import org.leap.enchantmentfusion.service.FusionItems;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.*;

/** Update old display text through normal play; no refresh command is required. */
public final class ItemDisplayListener implements Listener {
    private final Plugin plugin;
    private final FusionItems items;
    private final Map<UUID, Boolean> queued = new HashMap<>();
    public ItemDisplayListener(Plugin plugin, FusionItems items) { this.plugin=plugin; this.items=items; }
    @EventHandler public void join(PlayerJoinEvent event) { queue(event.getPlayer(),true); }
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true)
    public void held(PlayerItemHeldEvent event) { queue(event.getPlayer(),false); }
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true)
    public void swap(PlayerSwapHandItemsEvent event) { queue(event.getPlayer(),false); }
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true)
    public void click(InventoryClickEvent event) {
        if(event.getWhoClicked() instanceof Player player && (items.marked(event.getCurrentItem()) || items.marked(event.getCursor()))) queue(player,true);
    }
    private void queue(Player player,boolean all) {
        Boolean previous=queued.put(player.getUniqueId(),all || queued.getOrDefault(player.getUniqueId(),false));
        if(previous!=null)return;
        plugin.getServer().getScheduler().runTask(plugin,()->{
            boolean full=Boolean.TRUE.equals(queued.remove(player.getUniqueId()));
            if(!player.isOnline())return;
            if(full)for(int slot=0;slot<player.getInventory().getSize();slot++) {
                ItemStack old=player.getInventory().getItem(slot),fresh=refresh(old);
                if(!Objects.equals(old,fresh))player.getInventory().setItem(slot,fresh);
            } else {
                var inventory=player.getInventory();
                ItemStack old=inventory.getItemInMainHand(),fresh=refresh(old);if(!Objects.equals(old,fresh))inventory.setItemInMainHand(fresh);
                old=inventory.getItemInOffHand();fresh=refresh(old);if(!Objects.equals(old,fresh))inventory.setItemInOffHand(fresh);
            }
            ItemStack old=player.getItemOnCursor(),fresh=refresh(old);if(!Objects.equals(old,fresh))player.setItemOnCursor(fresh);
        });
    }
    private ItemStack refresh(ItemStack item) { return items.read(item).isPresent()?items.refreshLore(item):item; }
}
