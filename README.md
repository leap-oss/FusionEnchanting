# FusionEnchanting — Fabric

**36 enchantment fusions. Native anvils. Source-based tiers. Interactive Chest GUI Wiki.**

Combine two enchanted books into a fusion book, then apply it to equipment in a normal anvil. Gameplay runs on the logical server using vanilla components, effects, and chest menus—players joining a dedicated server do not need a client mod or resource pack. Also works in singleplayer/integrated servers.

**Version 1.0.0+mc1.21 · Minecraft 1.21 · Fabric Loader ≥ 0.16.14 · Java 21 · Apache-2.0**

## How it works

```text
Enchanted book A + Enchanted book B
                ↓ anvil + XP
           Fusion book I–III
                ↓ anvil + compatible equipment + XP
          Equipment with fusion
```

| Source books | Result |
|---|---|
| Sharpness I + Fire Aspect I | Infernal Edge I |
| Sharpness III + Fire Aspect I | Infernal Edge II |
| Sharpness V + Fire Aspect II | Infernal Edge III |

Full reference pairs produce **tier III**. Lower source books produce lower tiers, and upgrading a fusion's bound source enchantments through the anvil can raise its tier.

## Key Features

- **36 fusions** covering swords, bows, crossbows, mining tools, armor, boots, tridents, maces, fishing rods, and helmets.
- **Interactive Chest GUI Wiki (`/fusion guide` or `/fusion gui`):** 54-slot server-side menu with category filter tabs, 3-tier anvil recipe matrices, player milestone tracking, HUD notification toggles, and an option to open the classic written-book guide (`/fusion book`).
- **Clean tooltips:** the fusion name and Roman tier replace the visible source enchantment lines while keeping their vanilla effects intact.
- **Stronger upper tiers:** Tier II/III apply **1.75×/2.5× power** and **0.85×/0.70× cooldown**, and Tier III raises applicable potion amplifiers by one.
- **Smithing Table support:** Upgrading diamond fusion gear to **Netherite** or applying **Armor Trims** preserves the fusion identity, bound sources, and lore.
- **Fusion Extractor (`/fusion extract`):** Unbind a fusion from equipment back into a Fusion Book using 1 plain Book and 3 XP levels while keeping your equipment intact.
- **Natural World Discovery:** Discover Fusion Books organically through Treasure Fishing, Enchanting Table resonance, and Librarian Villagers (guaranteed at least 1 Fusion Book by Expert level).
- **HUD Feedback & Milestones:** Action Bar notifications for ability procs and cooldowns (`/fusion notify`) and 6 unlockable player milestones (`/fusion stats`).

## Commands

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
| `/fusion give <player> <id> [level]` | Give a fusion book (defaults to III; operator level 2) |
| `/fusion reload` | Reload `config/fusionenchanting.json` and invalidate active anvil previews |

## Installation & Build

1. Place `fusionenchanting-fabric-1.0.0+mc1.21.jar` into your Fabric 1.21 `mods/` folder (Fabric API is not required).
2. Start the server or game. Settings are stored at `config/fusionenchanting.json`.

To build from source with JDK 21:

```bash
./gradlew build
```

Output: `build/libs/fusionenchanting-fabric-1.0.0+mc1.21.jar`.

Developed by **LEAP**. Licensed under the [Apache License 2.0](LICENSE).
