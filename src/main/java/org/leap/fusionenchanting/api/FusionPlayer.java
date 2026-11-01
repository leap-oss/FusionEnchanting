package org.leap.fusionenchanting.api;

import net.minecraft.nbt.CompoundTag;

public interface FusionPlayer {
    boolean fusion$introduced();
    void fusion$introduced(boolean value);
    CompoundTag fusion$stats();
    void fusion$stats(CompoundTag value);
}
