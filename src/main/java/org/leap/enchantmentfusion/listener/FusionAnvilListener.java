package org.leap.enchantmentfusion.listener;

import org.leap.enchantmentfusion.event.FusionCreateEvent;
import org.leap.enchantmentfusion.model.*;
import org.leap.enchantmentfusion.service.*;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.view.AnvilView;
import org.bukkit.plugin.Plugin;

import java.util.*;

/** Vanilla book merge/apply menus own taking the result, input consumption and XP. */
public final class FusionAnvilListener implements Listener {
    private record Plan(ItemStack left, ItemStack right, ItemStack nativeResult, ItemStack preview,
                        FusionDefinition definition, FusionTuning tuning, UUID identity,
                        int cost, int rightCost, FusionCreateEvent.Operation operation) {}
    private final Map<AnvilInventory, Plan> plans = new WeakHashMap<>();
    private final Plugin plugin;
    private final FusionService service;
    private final FusionItems items;

    public FusionAnvilListener(Plugin plugin, FusionService service, FusionItems items) {
        this.plugin = plugin; this.service = service; this.items = items;
    }
    @EventHandler(priority = EventPriority.HIGHEST)
    public void prepare(PrepareAnvilEvent event) {
        AnvilInventory inventory = event.getInventory(); plans.remove(inventory);
        ItemStack left = inventory.getItem(0), right = inventory.getItem(1), nativeResult = event.getResult();
        if (!(event.getView().getPlayer() instanceof Player player)) return;
        boolean leftMarked = items.marked(left), rightMarked = items.marked(right);
        if (nativeResult == null || nativeResult.getType().isAir()) return;
        FusionDefinition definition; UUID identity; FusionCreateEvent.Operation operation;
        int cost = event.getView().getRepairCost();
        int rightCost = event.getView().getRepairItemCountCost();
        if (leftMarked || rightMarked) {
            if (leftMarked && rightMarked) { event.setResult(null); return; }
            ItemStack source = leftMarked ? left : right;
            var state = items.read(source);
            if (state.isEmpty() || rightMarked && (!FusionService.book(right) || !service.allowed(player)
                    || !service.tuning(state.get().definition().id()).enabled())) { event.setResult(null); return; }
            definition = state.get().definition(); identity = state.get().instance();
            operation = rightMarked ? FusionCreateEvent.Operation.APPLY_BOOK : FusionCreateEvent.Operation.MAINTAIN;
        } else {
            // Normal book + equipment and equipment + equipment are always vanilla.
            if (!service.allowed(player)) return;
            List<FusionDefinition> matches = service.bookCandidates(left, right, nativeResult);
            if (matches.size() != 1) return;
            definition = matches.getFirst(); identity = UUID.randomUUID(); operation = FusionCreateEvent.Operation.CREATE_BOOK;
            if (cost < 1) return;
            cost = FusionRules.creationCost(cost, service.tuning(definition.id()).extraLevels());
            rightCost = 1;
        }
        if (cost < 1) return;
        ItemStack preview = items.fuse(nativeResult, definition, identity);
        if (items.read(preview).isEmpty()) { event.setResult(null); return; }
        // Preserve vanilla Too Expensive. A forged pickup packet must not bypass it.
        if (player.getGameMode() != GameMode.CREATIVE && cost >= event.getView().getMaximumRepairCost()) {
            event.setResult(null); event.getView().setRepairCost(cost); return;
        }
        event.setResult(preview); event.getView().setRepairCost(cost); event.getView().setRepairItemCountCost(rightCost);
        plans.put(inventory, new Plan(copy(left), copy(right), nativeResult.clone(), preview.clone(), definition,
                service.tuning(definition.id()), identity, cost, rightCost, operation));
    }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void take(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory() instanceof AnvilInventory inventory) || event.getRawSlot() != 2) return;
        if (!items.marked(inventory.getItem(2))) return;
        if (!(event.getWhoClicked() instanceof Player player) || !(event.getView() instanceof AnvilView view)) {
            event.setCancelled(true); return;
        }
        Plan plan = plans.get(inventory);
        if (!valid(plan, inventory, view, player)) { event.setCancelled(true); return; }
        if (event.getAction() == InventoryAction.NOTHING) return;
        if (plan.operation != FusionCreateEvent.Operation.MAINTAIN) {
            FusionCreateEvent attempt = new FusionCreateEvent(player, plan.definition.id(), plan.left, plan.right,
                    plan.preview, plan.cost, plan.operation);
            plugin.getServer().getPluginManager().callEvent(attempt);
            if (attempt.isCancelled() || !valid(plan, inventory, view, player)) event.setCancelled(true);
        }
        // All normal native pickup/shift/hotbar/drop actions remain available.
    }
    private boolean valid(Plan plan, AnvilInventory inventory, AnvilView view, Player player) {
        if (plan == null || !player.isOnline() || player.isDead()
                || player.getOpenInventory().getTopInventory() != inventory
                || !Objects.equals(plan.left, inventory.getItem(0)) || !Objects.equals(plan.right, inventory.getItem(1))
                || !Objects.equals(plan.preview, inventory.getItem(2))
                || !Objects.equals(items.fuse(plan.nativeResult, plan.definition, plan.identity), plan.preview)
                || !plan.tuning.equals(service.tuning(plan.definition.id()))
                || view.getRepairCost() != plan.cost || view.getRepairItemCountCost() != plan.rightCost) return false;
        boolean creative = player.getGameMode() == GameMode.CREATIVE;
        if (!creative && (player.getLevel() < plan.cost || plan.cost >= view.getMaximumRepairCost())) return false;
        return plan.operation == FusionCreateEvent.Operation.MAINTAIN
                || service.allowed(player) && plan.tuning.enabled();
    }
    @EventHandler public void close(InventoryCloseEvent event) {
        if (event.getInventory() instanceof AnvilInventory inventory) plans.remove(inventory);
    }
    private static ItemStack copy(ItemStack item) { return item == null ? null : item.clone(); }
}
