package org.leap.fusionenchanting.mixin;
import org.leap.fusionenchanting.FusionEnchanting;
import org.leap.fusionenchanting.api.FusionFishing;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FishingHook.class)
public abstract class FishingHookMixin implements FusionFishing {
    @Shadow private int nibble;
    @Shadow private Entity hookedIn;
    @Unique private ItemStack fusion$rod=ItemStack.EMPTY;
    @Unique private boolean fusion$catch;
    @Inject(method="retrieve(Lnet/minecraft/world/item/ItemStack;)I",at=@At("HEAD"))
    private void fusion$before(ItemStack used,CallbackInfoReturnable<Integer> cir){fusion$catch=nibble>0&&hookedIn==null;}
    @Inject(method="retrieve(Lnet/minecraft/world/item/ItemStack;)I",at=@At("RETURN"))
    private void fusion$caught(ItemStack used,CallbackInfoReturnable<Integer> cir){
        if(fusion$catch&&cir.getReturnValueI()==1&&((FishingHook)(Object)this).getOwner() instanceof ServerPlayer player)FusionEnchanting.runtime().abilities.caught(player,fusion$rod,used);
        fusion$catch=false;
    }
    @Override public ItemStack fusion$rod(){return fusion$rod.copy();}
    @Override public void fusion$rod(ItemStack rod){fusion$rod=rod.copy();}
}
