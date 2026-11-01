# FusionEnchanting

**36 enchantment fusions. Native anvils. Source-based tiers. Interactive Chest GUI Wiki.**

Combine two enchanted books into a fusion book, then apply it to equipment in a normal anvil. The source enchantments keep their vanilla behavior while the tooltip presents the fusion name, tier, and effective abilities in English.

**Version 1.0.0 · Paper 1.21 · Java 21 · Apache-2.0**

## How it works

```text
Enchanted book A + Enchanted book B
                ↓ anvil + XP
           Fusion book I–III
                ↓ anvil + compatible equipment + XP
          Equipment with fusion
```

Fusion tier comes directly from the levels of the two source enchantments:

| Source books | Result |
|---|---|
| Sharpness I + Fire Aspect I | Infernal Edge I |
| Sharpness III + Fire Aspect I | Infernal Edge II |
| Sharpness V + Fire Aspect II | Infernal Edge III |

The full-strength pair listed for every recipe produces **tier III**. Lower source levels produce lower tiers. Existing fusion items can gain tiers when their source enchantments are upgraded through normal anvil enchanting.

## Key Features

- **36 fusions** covering swords, bows, crossbows, mining tools, armor, boots, tridents, maces, fishing rods, and helmets.
- **Interactive Chest GUI Wiki (`/fusion guide` or `/fusion gui`):** 54-slot interactive menu with category filter tabs, 3-tier anvil recipe matrices, player milestone tracking, HUD notification toggles, and an option to open the classic written-book guide (`/fusion book`).
- **Clean tooltips:** the fusion name and tier replace the visible source enchantment lines. Native enchantments, attack attributes, glint, and their actual effects remain intact.
- **Stronger upper tiers:** Tier II/III apply **1.75×/2.5× power** and **0.85×/0.70× cooldown**, and Tier III raises applicable potion amplifiers by one (e.g. Haste I → Haste II).
- **Smithing Table support:** Upgrading diamond fusion gear to **Netherite** or applying **Armor Trims** preserves the fusion identity, bound sources, and lore.
- **Fusion Extractor (`/fusion extract`):** Unbind a fusion from equipment back into a Fusion Book using 1 plain Book and 3 XP levels while keeping your equipment intact.
- **Natural World Discovery:** Discover Fusion Books organically through structure Loot Chests, Treasure Fishing, Enchanting Table resonance, and Librarian Villagers (guaranteed at least 1 Fusion Book by Expert level).
- **HUD Feedback & Milestones:** Action Bar notifications for ability procs and cooldowns (`/fusion notify`) and 6 unlockable player milestones (`/fusion stats`).

## Commands & Permissions

Alias: `/efusion`.

| Command | Purpose |
|---|---|
| `/fusion help` | Show help and clickable guide links |
| `/fusion guide [id/enchant]` | Open the interactive Chest GUI Wiki (or filter by recipe/ingredient) |
| `/fusion book [id/enchant]` | Open the classic written-book guide |
| `/fusion recipes [page]` | Show the recipe catalog in chat |
| `/fusion info <id>` | Show tier effects, ingredients, costs, and limits |
| `/fusion inspect` | Inspect and refresh the fusion in your main hand |
| `/fusion extract` | Extract the fusion from held equipment into a plain Book (costs 3 XP levels) |
| `/fusion notify` | Toggle Action Bar cooldown & proc notifications |
| `/fusion stats` | View your personal fusion statistics and unlocked milestones |
| `/fusion give <player> <id> [level]` | Give an online player a fusion book (defaults to III; admin only) |
| `/fusion reload` | Reload `config.yml` and invalidate active anvil previews |

| Permission | Default | Purpose |
|---|---|---|
| `enchantmentfusion.use` | Everyone | Create, apply, extract, and use fusions |
| `enchantmentfusion.recipes` | Everyone | GUI wiki, book guide, recipes, info, inspect, and stats |
| `enchantmentfusion.admin` | OP | Give fusion books |
| `enchantmentfusion.reload` | OP | Reload settings |

## Installation & Build

1. Place `fusionenchanting-1.0.0.jar` into your Paper 1.21 server's `plugins/` folder.
2. Start or restart the server. Configuration is generated at `plugins/FusionEnchanting/config.yml`.

To build from source with JDK 21 and Maven:

```bash
mvn clean package
```

Developed by **LEAP**. Licensed under the [Apache License 2.0](LICENSE).
