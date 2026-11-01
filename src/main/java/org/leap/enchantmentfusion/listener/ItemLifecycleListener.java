package org.leap.enchantmentfusion.listener;

import org.leap.enchantmentfusion.service.FusionItems;
import org.leap.enchantmentfusion.service.FusionProgression;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.SmithingInventory;

import java.util.Arrays;

public final class ItemLifecycleListener implements Listener {
    private final FusionItems items;
    private final FusionProgression progression;
    public ItemLifecycleListener(FusionItems items) { this(items, new FusionProgression()); }
    public ItemLifecycleListener(FusionItems items, FusionProgression progression) {
        this.items = items;
        this.progression = progression;
    }

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
        SmithingInventory inv = event.getInventory();
        ItemStack template = inv.getItem(0), base = inv.getItem(1), addition = inv.getItem(2), result = event.getResult();
        if (items.marked(template) || items.marked(addition)) {
            event.setResult(null);
            return;
        }
        if (!items.marked(base)) return;
        var state = items.read(base);
        if (state.isEmpty() || result == null || result.getType().isAir()) {
            event.setResult(null);
            return;
        }
        ItemStack fusedResult = items.fuse(result, state.get().definition(), state.get().instance());
        if (items.read(fusedResult).isEmpty()) {
            event.setResult(null);
            return;
        }
        event.setResult(fusedResult);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void resultGuard(InventoryClickEvent event) {
        var inventory = event.getView().getTopInventory();
        boolean unsupportedCraft = inventory.getType() == InventoryType.WORKBENCH || inventory.getType() == InventoryType.CRAFTING;
        if (unsupportedCraft && event.getSlotType() == InventoryType.SlotType.RESULT
                && Arrays.stream(inventory.getContents()).anyMatch(items::marked)) {
            event.setCancelled(true);
            return;
        }
        if (inventory instanceof SmithingInventory smithing && event.getRawSlot() == 3) {
            if (items.marked(smithing.getItem(0)) || items.marked(smithing.getItem(2))) {
                event.setCancelled(true);
                return;
            }
            if (items.marked(smithing.getItem(1))) {
                var baseState = items.read(smithing.getItem(1));
                var resultState = items.read(smithing.getItem(3));
                if (baseState.isEmpty() || resultState.isEmpty() || !baseState.get().instance().equals(resultState.get().instance())) {
                    event.setCancelled(true);
                    return;
                }
                if (event.getWhoClicked() instanceof Player player && event.getAction() != InventoryAction.NOTHING) {
                    progression.recordSmithingUpgrade(player);
                }
            }
        }
        if (inventory.getType() == InventoryType.GRINDSTONE && event.getRawSlot() == 2
                && items.marked(inventory.getItem(2))) event.setCancelled(true);
    }
}
