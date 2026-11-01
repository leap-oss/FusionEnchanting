package org.leap.fusionenchanting.mixin;

import org.leap.fusionenchanting.*;
import org.leap.fusionenchanting.anvil.AnvilLogic;
import org.leap.fusionenchanting.api.FusionAnvil;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(AnvilMenu.class)
public abstract class AnvilMenuMixin extends ItemCombinerMenu implements FusionAnvil {
    @Shadow @Final private DataSlot cost;
    @Shadow private int repairItemCountCost;
    @Unique private AnvilLogic.Plan fusion$plan;
    protected AnvilMenuMixin(MenuType<?> type,int id,Inventory inventory,ContainerLevelAccess access){super(type,id,inventory,access);}
    @Inject(method="createResult",at=@At("HEAD"))
    private void fusion$reset(CallbackInfo ci){fusion$plan=null;}
    @Inject(method="createResult",at=@At("RETURN"))
    private void fusion$preview(CallbackInfo ci){
        if(!(player instanceof ServerPlayer serverPlayer))return;
        var preview=FusionEnchanting.runtime().anvil.prepare(serverPlayer,inputSlots.getItem(0),inputSlots.getItem(1),resultSlots.getItem(0),cost.get(),repairItemCountCost);
        if(preview==null)return;resultSlots.setItem(0,preview.output());cost.set(preview.cost());repairItemCountCost=preview.rightCost();fusion$plan=preview.plan();broadcastChanges();
    }
    @Inject(method="mayPickup",at=@At("HEAD"),cancellable=true)
    private void fusion$validate(Player actor,boolean present,CallbackInfoReturnable<Boolean> cir){
        if(!(actor instanceof ServerPlayer serverPlayer)||!FusionEnchanting.runtime().items.marked(resultSlots.getItem(0)))return;
        var logic=FusionEnchanting.runtime().anvil;
        if(serverPlayer.containerMenu!=(Object)this||!logic.permit(fusion$plan,serverPlayer,inputSlots.getItem(0),inputSlots.getItem(1),resultSlots.getItem(0),cost.get(),repairItemCountCost)
                ||serverPlayer.containerMenu!=(Object)this
                ||!logic.valid(fusion$plan,serverPlayer,inputSlots.getItem(0),inputSlots.getItem(1),resultSlots.getItem(0),cost.get(),repairItemCountCost))cir.setReturnValue(false);
    }
    @Inject(method="onTake",at=@At("HEAD"))
    private void fusion$onTake(Player actor,ItemStack stack,CallbackInfo ci){
        if(!(actor instanceof ServerPlayer serverPlayer)||fusion$plan==null)return;
        var state=FusionEnchanting.runtime().items.read(stack);
        if(state.isEmpty())return;
        int tier=state.get().level();
        if(fusion$plan.operation()==org.leap.fusionenchanting.api.FusionEvents.Operation.CREATE_BOOK)
            FusionEnchanting.runtime().progression.recordBookCreated(serverPlayer,tier);
        else if(fusion$plan.operation()==org.leap.fusionenchanting.api.FusionEvents.Operation.APPLY_BOOK)
            FusionEnchanting.runtime().progression.recordBookApplied(serverPlayer,tier);
    }
    @Override public int fusion$getCost(){return cost.get();}
    @Override public void fusion$invalidate(){if(FusionEnchanting.runtime().items.marked(resultSlots.getItem(0))){resultSlots.setItem(0,ItemStack.EMPTY);cost.set(0);}fusion$plan=null;broadcastChanges();}
}
