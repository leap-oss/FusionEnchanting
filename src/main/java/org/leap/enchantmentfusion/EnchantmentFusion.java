package org.leap.enchantmentfusion;

import org.leap.enchantmentfusion.ability.*;
import org.leap.enchantmentfusion.command.FusionCommand;
import org.leap.enchantmentfusion.discovery.*;
import org.leap.enchantmentfusion.listener.*;
import org.leap.enchantmentfusion.model.*;
import org.leap.enchantmentfusion.service.*;
import org.bukkit.event.Listener;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public final class EnchantmentFusion extends JavaPlugin {
    private FusionService service;
    private FusionItems items;
    private Cooldowns cooldowns;
    private FusionProgression progression;
    private RecipeGui gui;
    private AnvilDiscovery discovery;
    private Set<String> registered = Set.of();

    @Override public void onEnable() {
        saveDefaultConfig();
        try {
            var definitions = FusionCatalog.definitions();
            Map<String, FusionDefinition> byId = definitions.stream().collect(Collectors.toMap(FusionDefinition::id, d -> d));
            items = new FusionItems(this, byId::get);
            service = new FusionService(definitions, FusionSettings.loadAndMigrate(new File(getDataFolder(), "config.yml"), definitions), items);
            items.tunings(service::tuning);
            cooldowns = new Cooldowns();
            progression = new FusionProgression();
            AbilitySupport support = new AbilitySupport(service, items, cooldowns, progression);
            ProjectileSnapshots projectiles = new ProjectileSnapshots(this, support);
            CombatAbilities combat = new CombatAbilities(this, support, projectiles);
            ArmorAbilities armor = new ArmorAbilities(this, support);
            MiningAbilities mining = new MiningAbilities(this, support);
            UtilityAbilities utility = new UtilityAbilities(this, support, projectiles);
            Set<String> ids = new HashSet<>();
            for (Set<String> set : List.of(combat.registered(), armor.registered(), mining.registered(), utility.registered()))
                for (String id : set) if (!ids.add(id)) throw new IllegalStateException("Duplicate ability: " + id);
            registered = Set.copyOf(ids);
            if (!registered.equals(byId.keySet())) throw new IllegalStateException("Catalogue and registered abilities differ");
            RecipeGuide guide = new RecipeGuide(service);
            gui = new RecipeGui(service, items, progression, guide);
            guide.attachGui(gui);
            discovery = new AnvilDiscovery(this, service, items);
            LootDiscoveryListener loot = new LootDiscoveryListener(this, service, items, progression);
            for (Listener listener : List.of(new FusionAnvilListener(this, service, items, progression), discovery, gui,
                    new ItemLifecycleListener(items, progression), new ItemDisplayListener(this, items), loot,
                    projectiles, combat, armor, mining, utility)) getServer().getPluginManager().registerEvents(listener, this);
            var command = Objects.requireNonNull(getCommand("fusion"));
            FusionCommand handler = new FusionCommand(this, service, items, guide, progression);
            command.setExecutor(handler); command.setTabCompleter(handler);
            getServer().getScheduler().runTaskTimer(this, utility, 10, 10);
            getServer().getScheduler().runTaskTimer(this, cooldowns::prune, 1200, 1200);
            getLogger().info("Enabled " + definitions.size() + " fusion recipes and " + registered.size() + " ability handlers. Baseline: Paper 1.21 / Java 21.");
        } catch (Exception failure) {
            getLogger().log(java.util.logging.Level.SEVERE, "Could not enable FusionEnchanting", failure);
            getServer().getPluginManager().disablePlugin(this);
        }
    }
    public Set<String> registeredAbilities() { return registered; }
    public void reloadSettings() throws Exception {
        FusionSettings replacement = FusionSettings.loadAndMigrate(new File(getDataFolder(), "config.yml"), service.definitions());
        service.settings(replacement);
        discovery.clear();
        invalidatePreviews();
    }
    private void invalidatePreviews() {
        if (items == null) return;
        for (var player : getServer().getOnlinePlayers()) {
            if (player.getOpenInventory().getTopInventory() instanceof AnvilInventory inventory
                    && items.marked(inventory.getItem(2))) {
                inventory.setItem(2, null);
                if (player.getOpenInventory() instanceof org.bukkit.inventory.view.AnvilView view) view.setRepairCost(-1);
            }
        }
    }
    @Override public void onDisable() { invalidatePreviews(); if (cooldowns != null) cooldowns.clear(); if (discovery != null) discovery.clear(); }
}
