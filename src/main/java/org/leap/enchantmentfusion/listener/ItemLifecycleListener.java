package org.leap.enchantmentfusion.listener;

import org.leap.enchantmentfusion.service.FusionItems;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;

public final class ItemLifecycleListener implements Listener {
    private final FusionItems items;
    public ItemLifecycleListener(FusionItems items) { this.items = items; }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void grindstone(PrepareGrindstoneEvent event) {
        ItemStack a = event.getInventory().getItem(0), b = event.getInventory().getItem(1);
        if (!items.marked(a) && !items.marked(b)) return;
        // No merging two fusion identities, even when both are valid.
        if (items.marked(a) && items.marked(b)) { event.setResult(null); return; }
        if (event.getResult() != null) event.setResult(items.strip(event.getResult()));
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void craft(PrepareItemCraftEvent event) {
        if (Arrays.stream(event.getInventory().getMatrix()).anyMatch(items::marked)) event.getInventory().setResult(null);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void smith(PrepareSmithingEvent event) {
        for (int slot = 0; slot < 3; slot++) if (items.marked(event.getInventory().getItem(slot))) { event.setResult(null); return; }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void resultGuard(InventoryClickEvent event) {
        var inventory = event.getView().getTopInventory();
        boolean unsupported = inventory.getType() == InventoryType.SMITHING || inventory.getType() == InventoryType.WORKBENCH
                || inventory.getType() == InventoryType.CRAFTING;
        if (unsupported && event.getSlotType() == InventoryType.SlotType.RESULT
                && Arrays.stream(inventory.getContents()).anyMatch(items::marked)) event.setCancelled(true);
        if (inventory.getType() == InventoryType.GRINDSTONE && event.getRawSlot() == 2
                && items.marked(inventory.getItem(2))) event.setCancelled(true);
    }
}
