package org.leap.fusionenchanting.mixin;

import org.leap.fusionenchanting.FusionEnchanting;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SmithingMenu.class)
public abstract class SmithingMixin extends ItemCombinerMenu {
    protected SmithingMixin(MenuType<?> type, int id, Inventory inventory, ContainerLevelAccess access) {
        super(type, id, inventory, access);
    }

    @Inject(method = "createResult", at = @At("RETURN"))
    private void fusion$preserveSmithing(CallbackInfo ci) {
        var items = FusionEnchanting.runtime().items;
        ItemStack template = inputSlots.getItem(0);
        ItemStack base = inputSlots.getItem(1);
        ItemStack addition = inputSlots.getItem(2);
        ItemStack result = resultSlots.getItem(0);
        if (items.marked(template) || items.marked(addition)) {
            resultSlots.setItem(0, ItemStack.EMPTY);
            broadcastChanges();
            return;
        }
        if (!items.marked(base)) return;
        var state = items.read(base);
        if (state.isEmpty() || result.isEmpty()) {
            resultSlots.setItem(0, ItemStack.EMPTY);
            broadcastChanges();
            return;
        }
        ItemStack fused = items.fuse(result, state.get().definition(), state.get().instance());
        if (items.read(fused).isEmpty()) {
            resultSlots.setItem(0, ItemStack.EMPTY);
            broadcastChanges();
            return;
        }
        resultSlots.setItem(0, fused);
        broadcastChanges();
    }

    @Inject(method = "mayPickup", at = @At("HEAD"), cancellable = true)
    private void fusion$validatePickup(Player actor, boolean present, CallbackInfoReturnable<Boolean> cir) {
        var items = FusionEnchanting.runtime().items;
        if (items.marked(inputSlots.getItem(0)) || items.marked(inputSlots.getItem(2))) {
            cir.setReturnValue(false);
            return;
        }
        if (items.marked(inputSlots.getItem(1))) {
            var baseState = items.read(inputSlots.getItem(1));
            var resultState = items.read(resultSlots.getItem(0));
            if (baseState.isEmpty() || resultState.isEmpty() || !baseState.get().instance().equals(resultState.get().instance())) {
                cir.setReturnValue(false);
            }
        }
    }

    @Inject(method = "onTake", at = @At("HEAD"))
    private void fusion$onTake(Player actor, ItemStack stack, CallbackInfo ci) {
        if (actor instanceof ServerPlayer serverPlayer && FusionEnchanting.runtime().items.read(stack).isPresent()) {
            FusionEnchanting.runtime().progression.recordSmithingUpgrade(serverPlayer);
        }
    }
}
