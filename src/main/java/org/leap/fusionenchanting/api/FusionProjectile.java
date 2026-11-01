package org.leap.fusionenchanting.api;
import net.minecraft.world.item.ItemStack;
public interface FusionProjectile {
    ItemStack fusion$weapon();
    void fusion$weapon(ItemStack weapon,float force);
    float fusion$force();
    boolean fusion$used();
    void fusion$used(boolean value);
}
