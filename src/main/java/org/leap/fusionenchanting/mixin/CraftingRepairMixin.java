package org.leap.fusionenchanting.mixin;
import org.leap.fusionenchanting.FusionEnchanting;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RepairItemRecipe.class)
public abstract class CraftingRepairMixin {
    @Inject(method="matches(Lnet/minecraft/world/item/crafting/CraftingInput;Lnet/minecraft/world/level/Level;)Z",at=@At("HEAD"),cancellable=true)
    private void fusion$match(CraftingInput input,Level level,CallbackInfoReturnable<Boolean> cir){if(input.items().stream().anyMatch(FusionEnchanting.runtime().items::marked))cir.setReturnValue(false);}
    @Inject(method="assemble(Lnet/minecraft/world/item/crafting/CraftingInput;Lnet/minecraft/core/HolderLookup$Provider;)Lnet/minecraft/world/item/ItemStack;",at=@At("HEAD"),cancellable=true)
    private void fusion$craft(CraftingInput input,HolderLookup.Provider registries,CallbackInfoReturnable<ItemStack> cir){if(input.items().stream().anyMatch(FusionEnchanting.runtime().items::marked))cir.setReturnValue(ItemStack.EMPTY);}
}
