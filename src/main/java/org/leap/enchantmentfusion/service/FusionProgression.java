package org.leap.enchantmentfusion.service;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

/** Tracks per-player fusion statistics, milestone achievements, and HUD notification preferences in PDC. */
public final class FusionProgression {
    public enum Milestone {
        FIRST_BOOK("first_book", "Apprentice Alchemist", "Create your first Fusion Book in an anvil"),
        FIRST_APPLY("first_apply", "Runesmith", "Apply a Fusion Book onto compatible equipment"),
        TIER_THREE("tier_three", "Apex Forger", "Create or apply a Tier III Fusion"),
        NETHERITE_ASCENDANT("netherite_ascendant", "Netherite Ascendant", "Upgrade or trim a Fusion item at a Smithing Table"),
        SOUL_UNBINDER("soul_unbinder", "Soul Unbinder", "Extract a Fusion Book from fused equipment"),
        VETERAN_WIELDER("veteran_wielder", "Master of Elements", "Trigger 25 Fusion abilities in survival");

        private final String key, title, description;
        Milestone(String key, String title, String description) {
            this.key = key; this.title = title; this.description = description;
        }
        public String key() { return key; }
        public String title() { return title; }
        public String description() { return description; }
    }

    private final NamespacedKey createdKey = new NamespacedKey("enchantmentfusion", "stat_created");
    private final NamespacedKey appliedKey = new NamespacedKey("enchantmentfusion", "stat_applied");
    private final NamespacedKey extractedKey = new NamespacedKey("enchantmentfusion", "stat_extracted");
    private final NamespacedKey smithingKey = new NamespacedKey("enchantmentfusion", "stat_smithing");
    private final NamespacedKey abilitiesKey = new NamespacedKey("enchantmentfusion", "stat_abilities");
    private final NamespacedKey highestTierKey = new NamespacedKey("enchantmentfusion", "stat_highest_tier");
    private final NamespacedKey notifyKey = new NamespacedKey("enchantmentfusion", "hud_notify");
    private final NamespacedKey milestonesKey = new NamespacedKey("enchantmentfusion", "milestones");

    public boolean notifyEnabled(Player player) {
        Byte val = player.getPersistentDataContainer().get(notifyKey, PersistentDataType.BYTE);
        return val == null || val != 0;
    }

    public boolean toggleNotify(Player player) {
        boolean next = !notifyEnabled(player);
        player.getPersistentDataContainer().set(notifyKey, PersistentDataType.BYTE, (byte) (next ? 1 : 0));
        return next;
    }

    public void recordBookCreated(Player player, int tier) {
        increment(player, createdKey);
        updateHighestTier(player, tier);
        unlock(player, Milestone.FIRST_BOOK);
        if (tier >= 3) unlock(player, Milestone.TIER_THREE);
    }

    public void recordBookApplied(Player player, int tier) {
        increment(player, appliedKey);
        updateHighestTier(player, tier);
        unlock(player, Milestone.FIRST_APPLY);
        if (tier >= 3) unlock(player, Milestone.TIER_THREE);
    }

    public void recordExtracted(Player player) {
        increment(player, extractedKey);
        unlock(player, Milestone.SOUL_UNBINDER);
    }

    public void recordSmithingUpgrade(Player player) {
        increment(player, smithingKey);
        unlock(player, Milestone.NETHERITE_ASCENDANT);
    }

    public void recordAbilityTriggered(Player player) {
        int count = increment(player, abilitiesKey);
        if (count >= 25) unlock(player, Milestone.VETERAN_WIELDER);
    }

    public int created(Player player) { return getInt(player, createdKey); }
    public int applied(Player player) { return getInt(player, appliedKey); }
    public int extracted(Player player) { return getInt(player, extractedKey); }
    public int smithingUpgrades(Player player) { return getInt(player, smithingKey); }
    public int abilitiesTriggered(Player player) { return getInt(player, abilitiesKey); }
    public int highestTier(Player player) { return getInt(player, highestTierKey); }

    public Set<Milestone> unlocked(Player player) {
        String raw = player.getPersistentDataContainer().get(milestonesKey, PersistentDataType.STRING);
        if (raw == null || raw.isBlank()) return EnumSet.noneOf(Milestone.class);
        Set<String> tokens = new HashSet<>(Arrays.asList(raw.split(",")));
        EnumSet<Milestone> result = EnumSet.noneOf(Milestone.class);
        for (Milestone m : Milestone.values()) if (tokens.contains(m.key)) result.add(m);
        return result;
    }

    public boolean unlock(Player player, Milestone milestone) {
        Set<Milestone> current = unlocked(player);
        if (!current.add(milestone)) return false;
        String serialized = String.join(",", current.stream().map(Milestone::key).toList());
        player.getPersistentDataContainer().set(milestonesKey, PersistentDataType.STRING, serialized);
        if (notifyEnabled(player)) {
            player.sendMessage(Component.text("✦ Milestone Unlocked: ", NamedTextColor.GOLD)
                    .append(Component.text("[" + milestone.title() + "] ", NamedTextColor.YELLOW, TextDecoration.BOLD))
                    .append(Component.text("— " + milestone.description(), NamedTextColor.GRAY)));
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.6f, 1.2f);
        }
        return true;
    }

    private static int getInt(Player player, NamespacedKey key) {
        return player.getPersistentDataContainer().getOrDefault(key, PersistentDataType.INTEGER, 0);
    }

    private static int increment(Player player, NamespacedKey key) {
        PersistentDataContainer pdc = player.getPersistentDataContainer();
        int next = pdc.getOrDefault(key, PersistentDataType.INTEGER, 0) + 1;
        pdc.set(key, PersistentDataType.INTEGER, next);
        return next;
    }

    private void updateHighestTier(Player player, int tier) {
        PersistentDataContainer pdc = player.getPersistentDataContainer();
        int current = pdc.getOrDefault(highestTierKey, PersistentDataType.INTEGER, 0);
        if (tier > current) pdc.set(highestTierKey, PersistentDataType.INTEGER, tier);
    }
}
