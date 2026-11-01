package org.leap.fusionenchanting.core;

import java.util.List;
import java.util.Map;

public final class FusionCatalog {
    private FusionCatalog() {}
    public static List<FusionDefinition> definitions() {
        return List.of(
            new FusionDefinition("infernal_edge", "Infernal Edge", Equipment.SWORD, Map.of("sharpness", 5, "fire_aspect", 2), 3, 4, 1, 1, "", "Requires a fully charged attack. Native Fire Aspect remains active."),
            new FusionDefinition("blade_storm", "Blade Storm", Equipment.SWORD, Map.of("sharpness", 5, "sweeping_edge", 3), 3, 6, 1, 1, "", "Secondary targets must be visible monsters within 3 blocks; no extra area damage."),
            new FusionDefinition("brutal_force", "Brutal Force", Equipment.SWORD, Map.of("sharpness", 5, "knockback", 2), 2, 5, 1, 1, "", "Adds momentum without weakening stronger vanilla knockback."),
            new FusionDefinition("executioner", "Executioner", Equipment.SWORD, Map.of("sharpness", 5, "looting", 3), 4, 8, 1, 1, "", "Only works at or below 25% target health; loot remains vanilla."),
            new FusionDefinition("holy_flame", "Holy Flame", Equipment.SWORD, Map.of("smite", 5, "fire_aspect", 2), 3, 5, 1, 1, "", "Undead only; soul flames are cosmetic."),
            new FusionDefinition("soul_reaper", "Soul Reaper", Equipment.SWORD, Map.of("smite", 5, "looting", 3), 3, 12, 1, 1, "", "Requires a direct melee kill on undead."),
            new FusionDefinition("scorched_venom", "Scorched Venom", Equipment.SWORD, Map.of("bane_of_arthropods", 5, "fire_aspect", 2), 3, 5, 1, 1, "", "Arthropods only; Wither also works on poison-immune spiders."),
            new FusionDefinition("flame_burst", "Flame Burst", Equipment.SWORD, Map.of("fire_aspect", 2, "knockback", 2), 2, 6, 1, 1, "", "Target must already be burning. Does not damage blocks."),
            new FusionDefinition("hellfire", "Hellfire", Equipment.BOW, Map.of("power", 5, "flame", 1), 3, 6, 1, 1, "", "One successful ability hit per projectile; shared player cooldown."),
            new FusionDefinition("impact", "Impact", Equipment.BOW, Map.of("power", 5, "punch", 2), 2, 5, 1, 1, "", "Requires a fully drawn bow; one successful ability hit per projectile."),
            new FusionDefinition("blast_arrow", "Blast Arrow", Equipment.BOW, Map.of("flame", 1, "punch", 2), 3, 8, 1, 1, "", "Particles only; no area damage or block destruction."),
            new FusionDefinition("volley", "Volley", Equipment.CROSSBOW, Map.of("multishot", 1, "quick_charge", 3), 3, 8, 1, 1, "", "Shared cooldown prevents repeated procs from the same Multishot volley."),
            new FusionDefinition("railshot", "Railshot", Equipment.CROSSBOW, Map.of("piercing", 4, "quick_charge", 3), 4, 6, 1, 1, "", "Later piercing hits remain vanilla; does not duplicate projectiles."),
            new FusionDefinition("prospector", "Prospector", Equipment.PICKAXE, Map.of("efficiency", 5, "fortune", 3), 2, 10, 1, 1, "", "Ore only; no extra drops or XP."),
            new FusionDefinition("excavator", "Excavator", Equipment.TOOL, Map.of("efficiency", 5, "silk_touch", 1), 3, 8, 1, 1, "", "Requires sneaking; no area mining."),
            new FusionDefinition("workhorse", "Workhorse", Equipment.TOOL, Map.of("efficiency", 5, "unbreaking", 3), 2, 0, 0.15, 1, "", "Runs after vanilla Unbreaking; never repairs the tool."),
            new FusionDefinition("miners_luck", "Miner’s Luck", Equipment.PICKAXE, Map.of("fortune", 3, "unbreaking", 3), 2, 0, 0.2, 1, "", "Only applies to the matching ore-mining tool within 2 ticks."),
            new FusionDefinition("preservation", "Preservation", Equipment.TOOL, Map.of("silk_touch", 1, "unbreaking", 3), 2, 0, 0.2, 1, "", "Requires sneaking; drops remain vanilla."),
            new FusionDefinition("prosperity", "Prosperity", Equipment.PICKAXE, Map.of("fortune", 3, "mending", 1), 3, 0, 1, 1, "", "Every 2 repaired durability spends 1 existing dropped XP; no XP creation."),
            new FusionDefinition("retribution", "Retribution", Equipment.ARMOR, Map.of("protection", 4, "thorns", 3), 3, 8, 1, 1, "", "One proc per fusion type, regardless of how many armor pieces are worn."),
            new FusionDefinition("molten_guard", "Molten Guard", Equipment.ARMOR, Map.of("fire_protection", 4, "thorns", 3), 3, 8, 1, 1, "", "Melee attackers only; additional PvP effects are disabled by default."),
            new FusionDefinition("reactive_armor", "Reactive Armor", Equipment.ARMOR, Map.of("blast_protection", 4, "thorns", 3), 3, 10, 1, 1, "", "Does not reduce the explosion damage that triggered the effect."),
            new FusionDefinition("deflection", "Deflection", Equipment.ARMOR, Map.of("projectile_protection", 4, "thorns", 3), 2, 8, 1, 1, "", "Does not reflect or duplicate projectiles."),
            new FusionDefinition("fortified", "Fortified", Equipment.ARMOR, Map.of("protection", 4, "unbreaking", 3), 2, 0, 0.1, 1, "", "Does not add direct damage reduction; wear prevention is capped at 60%."),
            new FusionDefinition("bloodforged", "Bloodforged", Equipment.ARMOR, Map.of("thorns", 3, "mending", 1), 3, 10, 1, 1, "", "Direct melee monster kills only; repairs one armor piece and never heals health."),
            new FusionDefinition("amphibious", "Amphibious", Equipment.BOOTS, Map.of("feather_falling", 4, "depth_strider", 3), 2, 10, 1, 1, "", "Requires a recent water-to-ground transition."),
            new FusionDefinition("glacial_step", "Glacial Step", Equipment.BOOTS, Map.of("feather_falling", 4, "frost_walker", 2), 2, 8, 1, 1, "", "Requires a landing after falling at least 3 blocks; no extra ice placement."),
            new FusionDefinition("soulbound_steps", "Soulbound Steps", Equipment.BOOTS, Map.of("feather_falling", 4, "soul_speed", 3), 2, 0, 1, 1, "", "Only refreshes while standing on soul sand or soul soil."),
            new FusionDefinition("stormcaller", "Stormcaller", Equipment.TRIDENT, Map.of("loyalty", 3, "channeling", 1), 4, 10, 1, 1, "", "The added lightning is cosmetic; native Channeling keeps its normal rules."),
            new FusionDefinition("tempest", "Tempest", Equipment.TRIDENT, Map.of("impaling", 5, "channeling", 1), 4, 8, 1, 1, "", "Requires a thunderstorm."),
            new FusionDefinition("wave_rider", "Wave Rider", Equipment.TRIDENT, Map.of("riptide", 3, "unbreaking", 3), 3, 10, 1, 1, "", "Native Riptide water requirements and launch speed remain unchanged."),
            new FusionDefinition("meteor", "Meteor", Equipment.MACE, Map.of("density", 5, "wind_burst", 1), 5, 8, 1, 1, "", "Requires a mace smash; no extra area damage."),
            new FusionDefinition("shockwave", "Shockwave", Equipment.MACE, Map.of("breach", 4, "wind_burst", 1), 5, 8, 1, 1, "", "Requires a mace smash; no block destruction or extra armor penetration."),
            new FusionDefinition("comet", "Comet", Equipment.MACE, Map.of("density", 5, "fire_aspect", 2), 4, 8, 1, 1, "", "Requires a mace smash; does not place fire blocks."),
            new FusionDefinition("master_angler", "Master Angler", Equipment.FISHING_ROD, Map.of("luck_of_the_sea", 3, "lure", 3), 2, 0, 0.2, 1, "", "Uses the same rod that was cast; does not repair the rod."),
            new FusionDefinition("aquanaut", "Aquanaut", Equipment.HELMET, Map.of("respiration", 3, "aqua_affinity", 1), 2, 0, 1, 1, "", "Only refreshes underwater; does not grant Water Breathing.")
        ).stream().map(d -> new FusionDefinition(d.id(), d.name(), d.equipment(), d.ingredients(), d.extraLevels(), d.cooldown(), d.chance(), d.power(),
                FusionDescriptions.describe(d, FusionLevels.profile(d, d.defaults(), 1)), d.limitation())).toList();
    }
}
