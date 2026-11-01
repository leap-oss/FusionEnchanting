package org.leap.enchantmentfusion.listener;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.entity.VillagerAcquireTradeEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.world.LootGenerateEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.plugin.Plugin;
import org.leap.enchantmentfusion.model.FusionDefinition;
import org.leap.enchantmentfusion.model.FusionLevels;
import org.leap.enchantmentfusion.service.FusionItems;
import org.leap.enchantmentfusion.service.FusionProgression;
import org.leap.enchantmentfusion.service.FusionService;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/** Natural discovery via Loot Chests, Treasure Fishing, Enchanting Tables, and Librarian Villagers. */
public final class LootDiscoveryListener implements Listener {
    private static final Set<String> HIGH_TIER_TABLES = Set.of(
            "chests/ancient_city", "chests/end_city_treasure", "chests/bastion_treasure",
            "chests/stronghold_library", "chests/woodland_mansion"
    );

    private final Plugin plugin;
    private final FusionService service;
    private final FusionItems items;
    private final FusionProgression progression;

    public LootDiscoveryListener(FusionService service, FusionItems items) {
        this(null, service, items, new FusionProgression());
    }

    public LootDiscoveryListener(Plugin plugin, FusionService service, FusionItems items, FusionProgression progression) {
        this.plugin = plugin;
        this.service = service;
        this.items = items;
        this.progression = progression;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onLootGenerate(LootGenerateEvent event) {
        if (service.settings().disabledWorlds().contains(event.getWorld().getName())) return;
        String key = event.getLootTable().getKey().getKey();
        if (!key.startsWith("chests/")) return;
        double chance = HIGH_TIER_TABLES.contains(key) || key.contains("trial_chambers") ? 0.12 : 0.05;
        if (ThreadLocalRandom.current().nextDouble() >= chance) return;
        randomNaturalBook(ThreadLocalRandom.current()).ifPresent(book -> event.getLoot().add(book));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onTreasureFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) return;
        if (!service.allowed(event.getPlayer())) return;
        if (!(event.getCaught() instanceof Item caught)) return;
        ItemStack stack = caught.getItemStack();
        if (stack.getType() != Material.ENCHANTED_BOOK || items.marked(stack)) return;
        if (ThreadLocalRandom.current().nextDouble() >= 0.20) return;
        randomNaturalBook(ThreadLocalRandom.current()).ifPresent(caught::setItemStack);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEnchantItem(EnchantItemEvent event) {
        Player player = event.getEnchanter();
        if (!service.allowed(player)) return;
        ItemStack target = event.getItem();
        if (items.marked(target)) return;

        Map<Enchantment, Integer> toAdd = event.getEnchantsToAdd();
        boolean isBook = target.getType() == Material.BOOK;

        if (isBook && event.getExpLevelCost() >= 20 && toAdd.size() == 1
                && ThreadLocalRandom.current().nextDouble() < 0.20) {
            Enchantment single = toAdd.keySet().iterator().next();
            String singleKey = single.getKey().getKey();
            List<FusionDefinition> matching = service.definitions().stream()
                    .filter(d -> service.tuning(d.id()).enabled() && d.ingredients().containsKey(singleKey))
                    .toList();
            if (!matching.isEmpty()) {
                FusionDefinition chosen = matching.get(ThreadLocalRandom.current().nextInt(matching.size()));
                for (String ingKey : chosen.ingredients().keySet()) {
                    if (!ingKey.equals(singleKey)) {
                        toAdd.put(FusionItems.enchantment(ingKey), 1);
                        break;
                    }
                }
            }
        }

        Map<String, Integer> rolled = new HashMap<>();
        toAdd.forEach((ench, lvl) -> {
            if (ench.getKey().getNamespace().equals("minecraft")) {
                rolled.put(ench.getKey().getKey(), lvl);
            }
        });

        Optional<FusionDefinition> match = findMatchingFusion(target.getType(), rolled);
        if (match.isEmpty() || plugin == null) return;

        FusionDefinition definition = match.get();
        Inventory inv = event.getInventory();
        Bukkit.getScheduler().runTask(plugin, () -> {
            ItemStack slotItem = inv.getItem(0);
            if (slotItem == null || items.marked(slotItem)) return;
            ItemStack fused = items.fuse(slotItem, definition, UUID.randomUUID());
            var state = items.read(fused);
            if (state.isEmpty()) return;
            inv.setItem(0, fused);
            int tier = state.get().level();
            if (slotItem.getType() == Material.ENCHANTED_BOOK) progression.recordBookCreated(player, tier);
            else progression.recordBookApplied(player, tier);
            if (progression.notifyEnabled(player)) {
                player.sendMessage(Component.text("✦ Enchanting Table Resonance: ", NamedTextColor.LIGHT_PURPLE)
                        .append(Component.text(definition.name() + " " + FusionLevels.roman(tier), NamedTextColor.GOLD)));
                player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 0.8f, 1.3f);
            }
        });
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onVillagerAcquireTrade(VillagerAcquireTradeEvent event) {
        if (!(event.getEntity() instanceof Villager villager)) return;
        if (villager.getProfession() != Villager.Profession.LIBRARIAN) return;
        if (service.settings().disabledWorlds().contains(villager.getWorld().getName())) return;
        MerchantRecipe original = event.getRecipe();
        ItemStack result = original.getResult();
        boolean hasFusion = villager.getRecipes().stream().anyMatch(r -> items.marked(r.getResult()));

        if (result.getType() != Material.ENCHANTED_BOOK || items.marked(result)) {
            if (villager.getVillagerLevel() >= 4 && !hasFusion && plugin != null) {
                Bukkit.getScheduler().runTask(plugin, () -> ensureExpertLibrarianFusion(villager));
            }
            return;
        }

        double chance = (villager.getVillagerLevel() >= 4 && !hasFusion) ? 1.0
                : (villager.getVillagerLevel() >= 3 ? 0.50 : 0.35);
        if (ThreadLocalRandom.current().nextDouble() >= chance) return;

        Map<String, Integer> bookEnchants = FusionItems.enchantments(result);
        Optional<ItemStack> fusionBookOpt = selectLibrarianBook(bookEnchants, ThreadLocalRandom.current());
        if (fusionBookOpt.isEmpty()) return;

        event.setRecipe(createLibrarianRecipe(original, fusionBookOpt.get()));
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onLibrarianInteract(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof Villager villager)) return;
        if (villager.getProfession() != Villager.Profession.LIBRARIAN) return;
        if (service.settings().disabledWorlds().contains(villager.getWorld().getName())) return;
        if (villager.getVillagerLevel() >= 4) {
            ensureExpertLibrarianFusion(villager);
        }
    }

    private void ensureExpertLibrarianFusion(Villager villager) {
        List<MerchantRecipe> recipes = new ArrayList<>(villager.getRecipes());
        if (recipes.stream().anyMatch(r -> items.marked(r.getResult()))) return;

        int bookIndex = -1;
        for (int i = recipes.size() - 1; i >= 0; i--) {
            if (recipes.get(i).getResult().getType() == Material.ENCHANTED_BOOK) {
                bookIndex = i;
                break;
            }
        }
        if (bookIndex >= 0) {
            MerchantRecipe oldBookRecipe = recipes.get(bookIndex);
            Map<String, Integer> enchants = FusionItems.enchantments(oldBookRecipe.getResult());
            Optional<ItemStack> fusionBook = selectLibrarianBook(enchants, ThreadLocalRandom.current());
            if (fusionBook.isPresent()) {
                recipes.set(bookIndex, createLibrarianRecipe(oldBookRecipe, fusionBook.get()));
                villager.setRecipes(recipes);
            }
        } else {
            Optional<ItemStack> fusionBook = randomNaturalBook(ThreadLocalRandom.current());
            if (fusionBook.isPresent()) {
                MerchantRecipe fallback = new MerchantRecipe(fusionBook.get(), 0, 5, true, 15, 0.2f);
                int tier = items.read(fusionBook.get()).map(FusionItems.State::level).orElse(1);
                int emeraldCost = tier == 1 ? 20 : 30;
                fallback.setIngredients(List.of(new ItemStack(Material.EMERALD, emeraldCost), new ItemStack(Material.BOOK, 1)));
                recipes.add(fallback);
                villager.setRecipes(recipes);
            }
        }
    }

    private MerchantRecipe createLibrarianRecipe(MerchantRecipe original, ItemStack fusionBook) {
        int tier = items.read(fusionBook).map(FusionItems.State::level).orElse(1);
        int emeraldCost = tier == 1 ? 18 + ThreadLocalRandom.current().nextInt(9) : 28 + ThreadLocalRandom.current().nextInt(9);
        MerchantRecipe upgraded = new MerchantRecipe(
                fusionBook,
                original.getUses(),
                Math.max(3, original.getMaxUses()),
                original.hasExperienceReward(),
                Math.max(10, original.getVillagerExperience()),
                original.getPriceMultiplier(),
                original.getDemand(),
                original.getSpecialPrice(),
                original.shouldIgnoreDiscounts()
        );
        upgraded.setIngredients(List.of(new ItemStack(Material.EMERALD, emeraldCost), new ItemStack(Material.BOOK, 1)));
        return upgraded;
    }

    public Optional<FusionDefinition> findMatchingFusion(Material material, Map<String, Integer> enchants) {
        boolean isBook = material == Material.BOOK || material == Material.ENCHANTED_BOOK;
        return service.definitions().stream()
                .filter(d -> service.tuning(d.id()).enabled())
                .filter(d -> isBook || d.equipment().accepts(material.name()))
                .filter(d -> d.ingredients().keySet().stream().allMatch(k -> enchants.getOrDefault(k, 0) >= 1))
                .max(Comparator.comparingInt(d -> FusionLevels.level(d, enchants)));
    }

    public Optional<ItemStack> selectLibrarianBook(Map<String, Integer> existingEnchants, Random random) {
        List<FusionDefinition> matching = service.definitions().stream()
                .filter(d -> service.tuning(d.id()).enabled())
                .filter(d -> existingEnchants.keySet().stream().anyMatch(d.ingredients()::containsKey))
                .toList();
        if (matching.isEmpty()) return randomNaturalBook(random);
        FusionDefinition chosen = matching.get(random.nextInt(matching.size()));
        int tier = random.nextDouble() < 0.30 && FusionLevels.example(chosen, 2).isPresent() ? 2 : 1;
        if (FusionLevels.example(chosen, tier).isEmpty()) tier = 3;
        return Optional.of(items.fuse(FusionItems.book(chosen, tier), chosen, UUID.randomUUID()));
    }

    public Optional<ItemStack> randomNaturalBook(Random random) {
        List<FusionDefinition> enabled = service.definitions().stream()
                .filter(d -> service.tuning(d.id()).enabled())
                .toList();
        if (enabled.isEmpty()) return Optional.empty();
        FusionDefinition chosen = enabled.get(random.nextInt(enabled.size()));
        int tier = random.nextDouble() < 0.25 && FusionLevels.example(chosen, 2).isPresent() ? 2 : 1;
        if (FusionLevels.example(chosen, tier).isEmpty()) tier = 3;
        ItemStack baseBook = FusionItems.book(chosen, tier);
        return Optional.of(items.fuse(baseBook, chosen, UUID.randomUUID()));
    }
}
