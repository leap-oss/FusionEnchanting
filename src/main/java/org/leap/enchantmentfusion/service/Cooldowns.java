package org.leap.enchantmentfusion.service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.LongSupplier;

/** Per-player/per-ability, so equipment swaps and extra Multishot arrows cannot reset it. */
public final class Cooldowns {
    private record Key(UUID player, String ability) {}
    private final Map<Key, Long> deadlines = new HashMap<>();
    private final LongSupplier clock;
    public Cooldowns() { this(System::nanoTime); }
    public Cooldowns(LongSupplier clock) { this.clock = clock; }
    public boolean ready(UUID player, String ability) {
        return clock.getAsLong() >= deadlines.getOrDefault(new Key(player, ability), Long.MIN_VALUE);
    }
    public double remainingSeconds(UUID player, String ability) {
        long remaining = deadlines.getOrDefault(new Key(player, ability), Long.MIN_VALUE) - clock.getAsLong();
        return remaining <= 0 ? 0.0 : remaining / 1_000_000_000.0;
    }
    public void start(UUID player, String ability, double seconds) {
        if (seconds > 0) deadlines.put(new Key(player, ability), clock.getAsLong() + (long) (seconds * 1_000_000_000L));
    }
    public void prune() { long now = clock.getAsLong(); deadlines.values().removeIf(deadline -> deadline <= now); }
    public void clear() { deadlines.clear(); }
}
