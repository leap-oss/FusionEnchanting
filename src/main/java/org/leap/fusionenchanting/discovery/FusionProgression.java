package org.leap.fusionenchanting.discovery;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import org.leap.fusionenchanting.api.FusionPlayer;

import java.util.*;

/** Tracks per-player fusion statistics, milestone achievements, and HUD notification preferences in player NBT. */
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

    public boolean notifyEnabled(ServerPlayer player) {
        CompoundTag tag = stats(player);
        return !tag.contains("hud_notify") || tag.getBoolean("hud_notify");
    }

    public boolean toggleNotify(ServerPlayer player) {
        boolean next = !notifyEnabled(player);
        stats(player).putBoolean("hud_notify", next);
        return next;
    }

    public void recordBookCreated(ServerPlayer player, int tier) {
        increment(player, "stat_created");
        updateHighestTier(player, tier);
        unlock(player, Milestone.FIRST_BOOK);
        if (tier >= 3) unlock(player, Milestone.TIER_THREE);
    }

    public void recordBookApplied(ServerPlayer player, int tier) {
        increment(player, "stat_applied");
        updateHighestTier(player, tier);
        unlock(player, Milestone.FIRST_APPLY);
        if (tier >= 3) unlock(player, Milestone.TIER_THREE);
    }

    public void recordExtracted(ServerPlayer player) {
        increment(player, "stat_extracted");
        unlock(player, Milestone.SOUL_UNBINDER);
    }

    public void recordSmithingUpgrade(ServerPlayer player) {
        increment(player, "stat_smithing");
        unlock(player, Milestone.NETHERITE_ASCENDANT);
    }

    public void recordAbilityTriggered(ServerPlayer player) {
        int count = increment(player, "stat_abilities");
        if (count >= 25) unlock(player, Milestone.VETERAN_WIELDER);
    }

    public int created(ServerPlayer player) { return stats(player).getInt("stat_created"); }
    public int applied(ServerPlayer player) { return stats(player).getInt("stat_applied"); }
    public int extracted(ServerPlayer player) { return stats(player).getInt("stat_extracted"); }
    public int smithingUpgrades(ServerPlayer player) { return stats(player).getInt("stat_smithing"); }
    public int abilitiesTriggered(ServerPlayer player) { return stats(player).getInt("stat_abilities"); }
    public int highestTier(ServerPlayer player) { return stats(player).getInt("stat_highest_tier"); }

    public Set<Milestone> unlocked(ServerPlayer player) {
        String raw = stats(player).getString("milestones");
        if (raw.isBlank()) return EnumSet.noneOf(Milestone.class);
        Set<String> tokens = new HashSet<>(Arrays.asList(raw.split(",")));
        EnumSet<Milestone> result = EnumSet.noneOf(Milestone.class);
        for (Milestone m : Milestone.values()) if (tokens.contains(m.key)) result.add(m);
        return result;
    }

    public boolean unlock(ServerPlayer player, Milestone milestone) {
        Set<Milestone> current = unlocked(player);
        if (!current.add(milestone)) return false;
        String serialized = String.join(",", current.stream().map(Milestone::key).toList());
        stats(player).putString("milestones", serialized);
        if (notifyEnabled(player)) {
            player.sendSystemMessage(Component.literal("✦ Milestone Unlocked: ").withStyle(ChatFormatting.GOLD)
                    .append(Component.literal("[" + milestone.title() + "] ").withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD))
                    .append(Component.literal("— " + milestone.description()).withStyle(ChatFormatting.GRAY)));
            player.level().playSound(null, player.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 0.6f, 1.2f);
        }
        return true;
    }

    private static CompoundTag stats(ServerPlayer player) {
        return ((FusionPlayer) player).fusion$stats();
    }

    private static int increment(ServerPlayer player, String key) {
        CompoundTag tag = stats(player);
        int next = tag.getInt(key) + 1;
        tag.putInt(key, next);
        return next;
    }

    private static void updateHighestTier(ServerPlayer player, int tier) {
        CompoundTag tag = stats(player);
        if (tier > tag.getInt("stat_highest_tier")) tag.putInt("stat_highest_tier", tier);
    }
}
